package llc.alersconsulting.core.client.testers;

import java.util.Arrays;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import gov.nga.common.entities.art.ArtDataSuggestion;
import gov.nga.common.entities.art.ArtObject;
import gov.nga.common.entities.art.ArtObjectConstituent;
import gov.nga.common.entities.art.ArtObjectStorageInfo;
import gov.nga.common.entities.art.QueryResultArtData;
import gov.nga.common.entities.art.QueryResultSuggestion;
import gov.nga.common.entities.art.SuggestType;
import gov.nga.common.search.ResultsPaginator;
import gov.nga.common.search.SearchHelper;
import gov.nga.common.search.SearchHelper.SEARCHOP;
import gov.nga.common.utils.stringfilter.StringFilter;
import llc.alersconsulting.core.client.CoreDataService;
import llc.alersconsulting.core.client.CoreDataServiceFacade.Config;

@Component(immediate = true)
public class ArtObjectTester extends TesterComponent 
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
        testArtObjectFetcher();
        freeTextMockNoSort(Arrays.asList(new String[] {"bagpipe player"}));
        freeTextMockNoSort(Arrays.asList(new String[] {"bagpipe", "player"}));
        testSuggestionTitle();
        testLocationData(Arrays.asList(new Long[] {12L, 50724L, 52173L}));
        testHasImagery(Arrays.asList(new Long[] {157939L, 157941L, 157942L, 12L}));
        testOwners(Arrays.asList(new Long[] {405L, 10L, 318L}));
        testDepartmentCodes(Arrays.asList(new Long[] {857L, 1579L, 157212L, 12L}));
        testSearchDepartmentCodes(Arrays.asList(new String[] {"DCG-E", "DCPH"}));
        testTMSStatus(Arrays.asList(new Long[] {857L, 1579L, 157212L, 12L}));
        testSearchTMSStatus(Arrays.asList(new String[] {ArtObject.TMSSTATUS.NULL.name(), ArtObject.TMSSTATUS.ONLOAN.name()}));
        testAccessionedStatus(Arrays.asList(new String [] {Boolean.TRUE.toString()}));
        testOverviewText(Arrays.asList(new Long[] {96577L}));

	}

    private void testArtObjectFetcher()
    {
        final StringBuilder report = new StringBuilder();
        final List<Long> objIds = Arrays.asList(new Long[]{10L, 13456L, 232L, 2712L});
        final QueryResultArtData<ArtObject> rslt = getManager().fetchByObjectIDs(objIds);
        report.append("Test query fetching objects: " + objIds);
        report.append(String.format("\nRetrieved %d objects", rslt.getResultCount()));
        for (ArtObject obj: rslt.getResults())
        {
            report.append("\n"+obj);
        }
        report.append(report.toString());
    }
    
    private void testSuggestionTitle()
    {
        final String term = "bagpipe player";
        final QueryResultSuggestion<ArtDataSuggestion> rslt = getManager().suggest(SuggestType.ARTOBJECT_TITLE, term);
        final StringBuilder report = new StringBuilder();
        report.append("Test Suggestions for objects with title: " + term);
        report.append(String.format("\nRetrieved %d objects, display first 5", rslt.getResultCount()));
        int count = 1;
        for (ArtDataSuggestion obj: rslt.getResults())
        {
            report.append(String.format("\n%s [%d]: %s", obj.getDisplayString(), obj.getEntityID(), obj.getCompareString()));
            if (++count == 5)
            {
                break;
            }
        }
        report.append(report.toString());
    }
    
    private static SearchHelper<ArtObject> buildSearchHelper(final List<String> terms)
    {
        final SearchHelper<ArtObject> sh = new SearchHelper<ArtObject>();
        sh.addFreeTextFilter(MockArtObjectFreeTextSearcher.MOCKFILTER.MOCKTITLE, terms);
        return sh;
    }
    
    private void freeTextMockNoSort(final List<String> terms)
    {
        final SearchHelper<ArtObject> sh = buildSearchHelper(terms);
        final ResultsPaginator pn = new ResultsPaginator(5, 1);
        final QueryResultArtData<ArtObject> rslt = getManager().searchArtObjects(sh, pn, null, null, null, new MockArtObjectFreeTextSearcher(getManager()));
        final StringBuilder report = new StringBuilder();
        report.append("Free text query (no sort) test on terms: " + terms);
        report.append(String.format("\nRetrieved %d objects, displaying first 5", rslt.getResultCount()));
        for (ArtObject obj: rslt.getResults())
        {
            report.append("\n" + obj);
        }
        report.append(report.toString());
    }
      
    private void testLocationData(final List<Long> ids)
    {
        final StringBuilder report = new StringBuilder();
        final QueryResultArtData<ArtObject> rslt = getManager().fetchByObjectIDs(ids);
        report.append("Test query fetching location information for objects: " + ids);
        report.append(String.format("\nRetrieved %d objects", rslt.getResultCount()));
        for (ArtObject obj: rslt.getResults())
        {
            report.append("\n"+obj);
            report.append(String.format("\n Current Location: %s", obj.getCurrentLocation()));
            if (obj.getCurrentLocation().getStorageInfo() != null)
            {
                ArtObjectStorageInfo storageInfo = obj.getCurrentLocation().getStorageInfo();
                report.append(String.format(" :: Entered Date = %s Project Name = %s", storageInfo.getEnteredDate(), storageInfo.getProjectName()));
            }
            report.append(String.format("\n Home Location: %s", obj.getHomeLocation()));
            if (obj.getHomeLocation().getStorageInfo() != null)
            {
                ArtObjectStorageInfo storageInfo = obj.getHomeLocation().getStorageInfo();
                report.append(String.format(" :: Entered Date = %s Project Name = %s", storageInfo.getEnteredDate(), storageInfo.getProjectName()));
            }
        }
        report.append(report.toString());
    }
    
    private void testHasImagery(final List<Long> ids)
    {
        final StringBuilder report = new StringBuilder();
        final QueryResultArtData<ArtObject> rslt = getManager().fetchByObjectIDs(ids);
        report.append("Test query fetching image information for object: " + ids);
        report.append(String.format("\nRetrieved %d objects", rslt.getResultCount()));
        for (ArtObject obj: rslt.getResults())
        {
            report.append("\n");
            report.append(obj);
            report.append(" has images = ");
            report.append(obj.hasImagery());
        }
        report.append(report.toString());
    }
    
    private void testOwners(final List<Long> ids)
    {
        final StringBuilder report = new StringBuilder();
        final QueryResultArtData<ArtObject> rslt = getManager().fetchByObjectIDs(ids);
        report.append("Test query fetching owner information for object: " + ids);
        report.append(String.format("\nRetrieved %d objects", rslt.getResultCount()));
        for (ArtObject obj: rslt.getResults())
        {
            report.append("\n");
            report.append(obj);
            report.append(" owners = ");
            for (ArtObjectConstituent ac: obj.getHistoricalOwners())
            {
                report.append(String.format("\n%s (display order: %d)", ac.getConstituent().getPreferredDisplayName(), ac.getDisplayOrder()));
            }
        }
        report.append(report.toString());
    }
    
    private void testDepartmentCodes(final List<Long> ids)
    {
        final StringBuilder report = new StringBuilder();
        final QueryResultArtData<ArtObject> rslt = getManager().fetchByObjectIDs(ids);
        report.append("Test query fetching department code information for objects: " + ids);
        report.append(String.format("\nRetrieved %d objects", rslt.getResultCount()));
        for (ArtObject obj: rslt.getResults())
        {
            report.append(String.format("\n %s departmentCode = %s", obj, obj.getDepartmentCode()));
        }
        report.append(report.toString());
    }
    
    private void testSearchDepartmentCodes(final List<String> ids)
    {
        final StringBuilder report = new StringBuilder();
        final ResultsPaginator pn = new ResultsPaginator(10, 1);
        final SearchHelper<ArtObject> searchHlpr = new SearchHelper<ArtObject>();
        searchHlpr.addFilter(ArtObject.SEARCH.DEPARTMENT_CODE, SEARCHOP.EQUALS, ids);
        final QueryResultArtData<ArtObject> rslt = getManager().searchArtObjects(searchHlpr, pn, null);
        report.append("Test query objects for department codes: " + ids);
        report.append(String.format("\nRetrieved %d objects, displaying first %d", rslt.getResultCount(), pn.getPageSize()));
        for (ArtObject obj: rslt.getResults())
        {
            report.append(String.format("\n %s departmentCode = %s", obj, obj.getDepartmentCode()));
        }
        report.append(report.toString());
    }
    
    private void testTMSStatus(final List<Long> ids)
    {
        final StringBuilder report = new StringBuilder();
        final QueryResultArtData<ArtObject> rslt = getManager().fetchByObjectIDs(ids);
        report.append("Test query fetching TMS Status information for objects: " + ids);
        report.append(String.format("\nRetrieved %d objects", rslt.getResultCount()));
        for (ArtObject obj: rslt.getResults())
        {
            report.append(String.format("\n %s status = %s", obj, obj.getTMSStatus()));
        }
        report.append(report.toString());
    }
    
    private void testSearchTMSStatus(final List<String> ids)
    {
        final StringBuilder report = new StringBuilder();
        final ResultsPaginator pn = new ResultsPaginator(10, 1);
        final SearchHelper<ArtObject> searchHlpr = new SearchHelper<ArtObject>();
        searchHlpr.addFilter(ArtObject.SEARCH.TMSSTATUS, SEARCHOP.EQUALS, ids);
        final QueryResultArtData<ArtObject> rslt = getManager().searchArtObjects(searchHlpr, pn, null);
        report.append("Test query objects for TMS Statuses: " + ids);
        report.append(String.format("\nRetrieved %d objects, displaying first %d", rslt.getResultCount(), pn.getPageSize()));
        for (ArtObject obj: rslt.getResults())
        {
            report.append(String.format("\n %s status = %s", obj, obj.getTMSStatus()));
        }
        report.append(report.toString());
    }
    
    private void testAccessionedStatus(final List<String> ids)
    {
        final StringBuilder report = new StringBuilder();
        final ResultsPaginator pn = new ResultsPaginator(10, 1);
        final SearchHelper<ArtObject> searchHlpr = new SearchHelper<ArtObject>();
        searchHlpr.addFilter(ArtObject.SEARCH.ISACCESSIONED, SEARCHOP.EQUALS, ids);
        final QueryResultArtData<ArtObject> rslt = getManager().searchArtObjects(searchHlpr, pn, null);
        report.append("Test query objects for TMS Accessioned: " + ids);
        report.append(String.format("\nRetrieved %d objects, displaying first %d", rslt.getResultCount(), pn.getPageSize()));
        for (ArtObject obj: rslt.getResults())
        {
            report.append(String.format("\n %s isAccessioned = %s", obj, obj.isAccessioned()));
        }
        report.append(report.toString());
    }
    
    private void testOverviewText(final List<Long> ids)
    {
        final StringBuilder report = new StringBuilder();
        final QueryResultArtData<ArtObject> rslt = getManager().fetchByObjectIDs(ids);
        report.append("Test query testing overview text for objects: " + ids);
        report.append(String.format("\nRetrieved %d objects", rslt.getResultCount()));
        for (ArtObject obj: rslt.getResults())
        {
            report.append("\n");
            report.append(obj);
            report.append(" overview text = ");
            report.append(obj.getOverviewText());
            report.append("\n");
            report.append(" overview non filtered text = ");
            report.append(obj.getOverviewText(new DoNothingFilter()));
        }
        report.append(report.toString());
    }
    
    class DoNothingFilter implements StringFilter
    {

        @Override
        public String getFilteredString(String s) 
        {
            return s;
        }
        
    }

}
