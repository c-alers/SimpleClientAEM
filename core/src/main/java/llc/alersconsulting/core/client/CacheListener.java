package llc.alersconsulting.core.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nga.common.entities.art.ArtDataManagerSubscriber;

public class CacheListener extends Thread
{
	private static final Logger LOG = LoggerFactory.getLogger(CacheListener.class);
	
	private ArtDataManagerSubscriber listener;
	
	protected CacheListener(final ArtDataManagerSubscriber l)
	{
		listener = l;
	}
	
	public void run()
	{
		LOG.debug(String.format("Calling %s.artDataUpdated()", listener));
		listener.artDataUpdated();
	}
}
