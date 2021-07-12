package llc.alersconsulting.core.client.testers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nga.common.entities.art.ArtDataManagerSubscriber;
import gov.nga.common.utils.StringUtils;
import llc.alersconsulting.core.client.CoreDataService;


public abstract class TesterComponent implements ArtDataManagerSubscriber
{
	protected final Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	private CoreDataService dataService;
	
	protected void initiate(final CoreDataService dataService)
	{
		try
		{
			dataService.subscribe(this);
	        this.dataService = dataService;
	        LOG.info("Component is active and subscribed");
		}
		catch (final Exception err)
		{
			LOG.error("Caught an exception while initiating..", err);
		}
	}
	
	protected CoreDataService getManager()
	{
		return dataService;
	}
	
	protected void report(final String message)
	{
		if (StringUtils.isNotBlank(message))
		{
			//System.out.println( message);
			LOG.info(message);
		}
	}
	
	protected void report(final String message, final Exception err)
	{
		if (StringUtils.isNotBlank(message))
		{
			//System.out.println( message);
			LOG.error(message, err);
		}
	}
	
}
