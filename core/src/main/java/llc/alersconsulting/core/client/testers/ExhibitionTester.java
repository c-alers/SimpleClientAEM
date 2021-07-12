package llc.alersconsulting.core.client.testers;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import gov.nga.common.entities.art.Exhibition;
import gov.nga.common.entities.art.ExhibitionArtObject;
import gov.nga.common.entities.art.ExhibitionLoan;
import gov.nga.common.entities.art.ExhibitionStatus;
import gov.nga.common.entities.art.QueryResult;
import gov.nga.common.search.ResultsPaginator;
import gov.nga.common.search.SearchHelper;
import gov.nga.common.search.SearchHelper.SEARCHOP;
import gov.nga.common.search.SortHelper;
import gov.nga.common.search.SortOrder;
import gov.nga.common.utils.CollectionUtils;
import llc.alersconsulting.core.client.CoreDataService;
import llc.alersconsulting.core.client.CoreDataServiceFacade.Config;

@Component(immediate = true)
public class ExhibitionTester extends TesterComponent 
{
	@Reference
	private CoreDataService dataService;
	
	@Activate
    protected void activate(final Config config) 
	{
		initiate(dataService);
    }

	@Override
	public void artDataUpdated() 
	{
		testFetcher();
		testNGAQuery();
		testConstituents(Arrays.asList(new Long[]{4764L, 3018L, 3071L, 4712L}));
		testExhibitionRights(4950L);
        for (Long id: new Long[] {4950L, 5133L})
        {
        	testFilterExhibitionObject(id);
        }
	}
    
    private static SearchHelper<ExhibitionArtObject> OBJECT_FILTER;
    

    private static SearchHelper<Exhibition> buildSearchHelper()
    {
        final SearchHelper<Exhibition> sh = new SearchHelper<Exhibition>();
        return sh;
    }

    public void testFetcher()
    {
        final StringBuilder report = new StringBuilder();
        final List<Long> objIds = Arrays.asList(new Long[]{10L, 15L, 22L, 25L});
        final QueryResult<Exhibition> rslt = getManager().fetchByExhibitionIDs(objIds);
        report.append("Test query fetching objects: " + objIds);
        report.append(String.format("\nRetrieved %d objects", rslt.getResultCount()));
        for (Exhibition obj: rslt.getResults())
        {
            report.append("\n"+obj);
        }
        report(report.toString());
    }

    public void testExhibitionRights(final Long id)
    {
        final StringBuilder report = new StringBuilder();
        final QueryResult<Exhibition> rslt = getManager().fetchByExhibitionID(id);
        report.append("Test of Exhibition right for exhibtion id: " + id);
        report.append(String.format("\nRetrieved %d objects", rslt.getResultCount()));
        for (Exhibition obj: rslt.getResults())
        {
            report.append(String.format("\n%s (%d Exhibition Objects) Limit 50", obj, obj.getExhibitionObjects().size()));
            int count = 0;
            for (ExhibitionArtObject artObj: obj.getExhibitionObjects())
            {
                if (++count > 50) break;
                report.append(String.format("\n%s hasRight = %s", artObj, artObj.getHasExhibitionRights()));
            }
        }
        report(report.toString());
    }
    
    public void testConstituents(final List<Long> ids)
    {
        final StringBuilder report = new StringBuilder();
        final QueryResult<Exhibition> rslt = getManager().fetchByExhibitionIDs(ids);
        report.append("Test query fetching constituent information for exhibitions: " + ids);
        report.append(String.format("\nRetrieved %d objects", rslt.getResultCount()));
        for (Exhibition obj: rslt.getResults())
        {
            report.append("\n");
            report.append(obj);
            report.append(" constituents = ");
            report.append(obj.getConstituents());
        }
        report(report.toString());
    }
    
    private static SearchHelper<ExhibitionArtObject> buildObjectFilter()
    {
        if (OBJECT_FILTER == null)
        {
            final SearchHelper<ExhibitionArtObject> sh = new SearchHelper<ExhibitionArtObject>();
            sh.addFilter(ExhibitionArtObject.SEARCH.HAS_VENUES, SEARCHOP.EQUALS, Boolean.TRUE.toString());
            sh.addFilter(ExhibitionArtObject.SEARCH.EXHIBITION_LOAN_PURPOSE, SEARCHOP.EQUALS, ExhibitionLoan.PURPOSE.EXHIBITION.toString());
            sh.addFilter(ExhibitionArtObject.SEARCH.EXHIBITION_LOAN_TYPE, SEARCHOP.EQUALS, ExhibitionLoan.TYPE.INCOMING.toString());
            final List<String> objectStatus = CollectionUtils.newArrayList();
            for (ExhibitionArtObject.LOAN_STATUS cand: ExhibitionArtObject.LOAN_STATUS.values())
            {
                switch (cand)
                {
                    case WITHDRAWN:
                    case DENIED:
                    case APPEAL:
                        //DON'T ADD TO LIST
                        break;
                    default:
                        objectStatus.add(cand.toString());
                }
            }
            sh.addFilter(ExhibitionArtObject.SEARCH.OBJECT_LOAN_STATUS, SEARCHOP.IN, objectStatus);
            OBJECT_FILTER = sh;
        }
        return OBJECT_FILTER;
    }
    
    public void testFilterExhibitionObject(final long id)
    {
        final StringBuilder report = new StringBuilder();
        final QueryResult<Exhibition> rslt = getManager().fetchByExhibitionID(id);
        report.append("Test of Exhibition Object filtering for exhibtion id: " + id);
        if (rslt.getResultCount() > 0)
        {
            final SearchHelper<ExhibitionArtObject> filter = buildObjectFilter();
            final ResultsPaginator pn = new ResultsPaginator(-1, 1);
            for (Exhibition obj: rslt.getResults())
            {
                report.append(String.format("\nExhibition found. Total Objects: %d\n", obj.getExhibitionObjects().size()));
                filter.search(obj.getExhibitionObjects(), pn, null);
                report.append(String.format("Filtered objects count: %d\n\n", pn.getTotalResults()));
            }
        }
        report(report.toString());
    }

    public void testNGAQuery()
    {
        final StringBuilder report = new StringBuilder();
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        final Long dateTime = new Long(cal.getTimeInMillis());
        final ResultsPaginator pn = new ResultsPaginator(20, 1);
        final SearchHelper<Exhibition> searchHlpr = buildSearchHelper();
        
        searchHlpr.addFilter(Exhibition.SEARCH.ISNGA, SEARCHOP.EQUALS, Boolean.TRUE.toString());
        searchHlpr.addFilter(Exhibition.SEARCH.STATUS, SEARCHOP.EQUALS, ExhibitionStatus.ACTIVE.name());
        searchHlpr.addFilter(Exhibition.SEARCH.CLOSEDATE, SEARCHOP.GREATER_THAN, dateTime.toString());
        
        final SortOrder sOrd = new SortOrder(Exhibition.SORT.CLOSEDATE, Exhibition.SORT.TITLE);
        final SortHelper<Exhibition> sortH = new SortHelper<Exhibition>(sOrd);
        
        final QueryResult<Exhibition> rslt = getManager().searchExhibitions(searchHlpr, pn, sortH);
        report.append("Test query fetching current NGA Exhibitions: ");
        report.append(String.format("\nRetrieved %d objects, displaying first %d", rslt.getResultCount(), pn.getPageSize()));
        for (Exhibition obj: rslt.getResults())
        {
            report.append(String.format("\n%s - %s", obj, obj.getCloseDate()));
        }
        report(report.toString());
    }

}
