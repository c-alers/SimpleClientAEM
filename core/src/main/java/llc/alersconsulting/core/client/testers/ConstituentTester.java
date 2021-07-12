package llc.alersconsulting.core.client.testers;

import java.util.Arrays;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import gov.nga.common.entities.art.Constituent;
import gov.nga.common.entities.art.Constituent.SORT;
import gov.nga.common.entities.art.QueryResult;
import gov.nga.common.search.ResultsPaginator;
import gov.nga.common.search.SearchHelper;
import gov.nga.common.search.SearchHelper.SEARCHOP;
import gov.nga.common.utils.CollectionUtils;
import llc.alersconsulting.core.client.CoreDataService;
import llc.alersconsulting.core.client.CoreDataServiceFacade.Config;

@Component(immediate = true)
public class ConstituentTester extends TesterComponent 
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
		final List<Long> ids = Arrays.asList(new Long[] {45L, 457L, 234L, 2456L, 50L, 654L, 823L, 13L, 1L, 22L, 453L, 32L, 102L, 55L});
		testByIDs(ids);
		testByIDsSorted(ids, SORT.HASBIOGRAPHY_ASC, SORT.PREFERRED_DISPLAY_NAME_ASC);
		testByIDsSorted(ids, SORT.PREFERRED_DISPLAY_NAME_ASC);
		testConstituentPagination(new ResultsPaginator(3, 1), ids, SORT.PREFERRED_DISPLAY_NAME_ASC);
        testConstituentPagination(new ResultsPaginator(3, 2), ids, SORT.PREFERRED_DISPLAY_NAME_ASC);

	}
    
    private SearchHelper<Constituent> buildSearchHelper(final List<Long> ids)
    {
    	final List<String> testIDs = CollectionUtils.newArrayList();
    	for (Long id: ids) testIDs.add(id.toString());
        final SearchHelper<Constituent> sh = new SearchHelper<Constituent>();
        sh.addFilter(Constituent.SEARCH.CONSTITUENT_ID, SEARCHOP.IN, testIDs);
        return sh;
    }
	
	public void testByIDs(final List<Long> ids)
	{
		try
		{
			report(String.format("testFetch(%s): Starting....", ids));
			QueryResult<Constituent> rslt = getManager().fetchByConstituentIDs(ids);
			report(String.format("Total numer of results: %d", rslt.getResultCount()));
			for (Constituent obj: rslt.getResults())
			{
				if (obj != null)
				{
					report(obj.toString());
				}
				else
				{
					report("null Constituent???");
				}
			}
			report("\n\n");
		}
		catch (final Exception err)
		{
			report("testFetch(): Threw an exception:", err);
		}
	}
	
	public void testByIDsSorted(final List<Long> ids, final Constituent.SORT...order)
	{
		try
		{
			report(String.format("testFetchBySorted(%s, %s): Starting....", ids, order));
			QueryResult<Constituent> rslt = getManager().fetchByConstituentIDs(ids, order);
			report(String.format("Total numer of results: %d", rslt.getResultCount()));
			for (Constituent obj: rslt.getResults())
			{
				if (obj != null)
				{
					report(obj.toString());
				}
				else
				{
					report("null Constituent???");
				}
			}
			report("\n\n");
		}
		catch (final Exception err)
		{
			report("testFetch(): Threw an exception:", err);
		}
	}
    
    protected void testConstituentPagination(final ResultsPaginator rp, final List<Long> ids, final Enum<?>... order)
    {
    	try
    	{
	        final QueryResult<Constituent> rslt = getManager().searchConstituents(buildSearchHelper(ids), rp, null, order);
	        final StringBuilder report = new StringBuilder();
	        report.append("Test query fetching objects: " + ids);
	        report.append(String.format("\nRetrieving %d objects out of %d results from page %d", rp.getPageSize(), rslt.getResultCount(), rp.getPage()));
	        for (Constituent obj: rslt.getResults())
	        {
	            report.append("\n"+obj);
	        }
	        report.append("\n\n");
	        report(report.toString());
		}
		catch (final Exception err)
		{
			report("testConstituentPagination(): Threw an exception:", err);
		}
    }

}
