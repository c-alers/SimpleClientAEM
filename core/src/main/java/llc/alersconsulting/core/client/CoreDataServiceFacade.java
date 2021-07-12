package llc.alersconsulting.core.client;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nga.common.entities.art.ArtDataManagerSubscriber;
import gov.nga.common.entities.art.ArtDataSuggestion;
import gov.nga.common.entities.art.ArtObject;
import gov.nga.common.entities.art.ArtObjectConstituent;
import gov.nga.common.entities.art.Constituent;
import gov.nga.common.entities.art.Constituent.SORT;
import gov.nga.common.entities.art.DataNotReadyException;
import gov.nga.common.entities.art.Exhibition;
import gov.nga.common.entities.art.Location;
import gov.nga.common.entities.art.Media;
import gov.nga.common.entities.art.Place;
import gov.nga.common.entities.art.QueryResultArtData;
import gov.nga.common.entities.art.QueryResultSuggestion;
import gov.nga.common.entities.art.SuggestType;
import gov.nga.common.entities.art.factory.ArtObjectFactory;
import gov.nga.common.entities.art.factory.ConstituentFactory;
import gov.nga.common.entities.art.factory.ExhibitionFactory;
import gov.nga.common.entities.art.factory.LocationFactory;
import gov.nga.common.rpc.DataServiceManager;
import gov.nga.common.rpc.client.ClientConfiguration;
import gov.nga.common.search.FacetHelper;
import gov.nga.common.search.FreeTextSearchable;
import gov.nga.common.search.ResultsPaginator;
import gov.nga.common.search.SearchHelper;
import gov.nga.common.search.SortHelper;
import gov.nga.common.utils.CollectionUtils;

@Designate(ocd=CoreDataServiceFacade.Config.class)
@Component(service = { CoreDataService.class, Runnable.class}, immediate = true)
@ServiceDescription("Facade for Core Data Service")
public class CoreDataServiceFacade implements CoreDataService, ClientConfiguration, Runnable
{
	@ObjectClassDefinition(name="Cache update checker",
            description = "Configuration for connecting to Data Server.")
	public static @interface Config 
	{

        @AttributeDefinition(name = "Cron-job expression")
        String scheduler_expression() default "*/30 * * * * ?";

        @AttributeDefinition(name = "Concurrent task",
                             description = "Whether or not to schedule this task concurrently")
        boolean scheduler_concurrent() default false;
        
        @AttributeDefinition(name = "Data Server URL",
        					description = "Address to connect to Data Server")
        String server_url() default "ap-artdataservicetst-priv.nga.gov:8081";
    }
	
	private static final Logger LOG = LoggerFactory.getLogger(CoreDataServiceFacade.class);
	
	@Reference
	private Scheduler scheduler;
	
	private Config config;
	private DataServiceManager manager;
	private Set<ArtDataManagerSubscriber> listeners;
	private Date lastSyncCheckRan;
	
	@Activate
    protected void activate(final Config config) 
	{
		try
		{
	        this.config = config;
	        this.manager = DataServiceManager.getManager(this);
	        this.listeners = CollectionUtils.newHashSet();
	        
	        Calendar now = Calendar.getInstance();
	        now.add(Calendar.MINUTE, 2);
	        scheduler.schedule(this, scheduler.AT(now.getTime()));
	        
	        LOG.info(String.format("Service activated with Data Server: %s", getConnectionURL()));
		}
		catch (final Exception err)
		{
			LOG.error("Caught an exception during activation", err);
			throw err;
		}
    }
	
	public String getConnectionURL() 
	{
		return config.server_url();
	}
	
	
	
	private void notifyListeners()
	{
		synchronized(listeners)
		{
			for (ArtDataManagerSubscriber l: listeners)
			{
				new CacheListener(l).start();
				LOG.debug(String.format("Listener %s notified!", l));
			}
		}
	}
	
	private boolean cacheHasUpdated()
	{
		boolean updated = false;
		if (lastSyncCheckRan == null)
		{
			updated = true;
		}
		else
		{
			final Date lastCacheUpdate = manager.getLastSyncTime();
			updated = lastSyncCheckRan.before(lastCacheUpdate);
		}
		lastSyncCheckRan = Calendar.getInstance().getTime();
		return updated;
	}
	
	@Override
	public void run()
	{
		LOG.info("Checking to see if cache has updated...");
		if (cacheHasUpdated())
		{
			LOG.info(String.format("Cache has updated, notifying %d listeners", listeners.size()));
			notifyListeners();
		}
	}
	
	@Override
	public void subscribe(final ArtDataManagerSubscriber listener)
	{
		synchronized(listeners)
		{
			listeners.add(listener);
		}
	}
	
	@Override
	public void unSubscribe(final ArtDataManagerSubscriber listener)
	{
		synchronized(listeners)
		{
			listeners.remove(listener);
		}
	}

	@Override
	public Collection<Long> getAllArtObjectIDs() 
	{
		return manager.getAllArtObjectIDs();
	}

	@Override
	public Collection<Long> getAllConstituentIDs() 
	{
		return manager.getAllConstituentIDs();
	}

	@Override
	public Collection<Long> getAllExhibitionIDs() 
	{
		return manager.getAllExhibitionIDs();
	}

	@Override
	public Date getLastSyncTime() 
	{
		return manager.getLastSyncTime();
	}

	@Override
	public QueryResultArtData<Constituent> fetchByConstituentID(long arg0) 
	{
		return manager.fetchByConstituentID(arg0);
	}

	@Override
	public <C extends Constituent> QueryResultArtData<C> fetchByConstituentID(long arg0, ConstituentFactory<C> arg1)
	{
		return manager.fetchByConstituentID(arg0, arg1);
	}

	@Override
	public QueryResultArtData<Constituent> fetchByConstituentIDs(Collection<Long> arg0) 
	{
		return manager.fetchByConstituentIDs(arg0);
	}

	@Override
	public QueryResultArtData<Constituent> fetchByConstituentIDs(Collection<Long> arg0, SORT... arg1) 
	{
		return manager.fetchByConstituentIDs(arg0, arg1);
	}

	@Override
	public <C extends Constituent> QueryResultArtData<C> fetchByConstituentIDs(Collection<Long> arg0,
			ConstituentFactory<C> arg1) 
	{
		return manager.fetchByConstituentIDs(arg0, arg1);
	}

	@Override
	public <C extends Constituent> QueryResultArtData<C> fetchByConstituentIDs(Collection<Long> arg0,
			ConstituentFactory<C> arg1, SORT... arg2) 
	{
		return manager.fetchByConstituentIDs(arg0, arg1, arg2);
	}

	@Override
	public QueryResultArtData<Exhibition> fetchByExhibitionID(long arg0) throws DataNotReadyException 
	{
		return manager.fetchByExhibitionID(arg0);
	}

	@Override
	public <T extends Exhibition> QueryResultArtData<T> fetchByExhibitionID(long arg0, ExhibitionFactory<T> arg1)
			throws DataNotReadyException 
	{
		return manager.fetchByExhibitionID(arg0, arg1);
	}

	@Override
	public QueryResultArtData<Exhibition> fetchByExhibitionIDs(List<Long> arg0) throws DataNotReadyException 
	{
		return manager.fetchByExhibitionIDs(arg0);
	}

	@Override
	public <T extends Exhibition> QueryResultArtData<T> fetchByExhibitionIDs(List<Long> arg0, ExhibitionFactory<T> arg1)
			throws DataNotReadyException 
	{
		return manager.fetchByExhibitionIDs(arg0, arg1);
	}

	@Override
	public QueryResultArtData<Location> fetchByLocationID(long arg0) throws DataNotReadyException 
	{
		return fetchByLocationID(arg0);
	}

	@Override
	public <T extends Location> QueryResultArtData<T> fetchByLocationID(long arg0, LocationFactory<T> arg1)
			throws DataNotReadyException 
	{
		return fetchByLocationID(arg0, arg1);
	}

	@Override
	public QueryResultArtData<Location> fetchByLocationIDs(List<Long> arg0) throws DataNotReadyException 
	{
		return fetchByLocationIDs(arg0);
	}

	@Override
	public <T extends Location> QueryResultArtData<T> fetchByLocationIDs(List<Long> arg0, LocationFactory<T> arg1)
			throws DataNotReadyException 
	{
		return fetchByLocationIDs(arg0, arg1);
	}

	@Override
	public QueryResultArtData<ArtObject> fetchByObjectID(long arg0) throws DataNotReadyException 
	{
		return manager.fetchByObjectID(arg0);
	}

	@Override
	public <T extends ArtObject> QueryResultArtData<T> fetchByObjectID(long arg0, ArtObjectFactory<T> arg1)
			throws DataNotReadyException  
	{
		return manager.fetchByObjectID(arg0, arg1);
	}

	@Override
	public QueryResultArtData<ArtObject> fetchByObjectIDs(Collection<Long> arg0) throws DataNotReadyException  
	{
		return manager.fetchByObjectIDs(arg0);
	}

	@Override
	public <T extends ArtObject> QueryResultArtData<T> fetchByObjectIDs(Collection<Long> arg0, ArtObjectFactory<T> arg1)
			throws DataNotReadyException  
	{
		return manager.fetchByObjectIDs(arg0, arg1);
	}

	@Override
	public QueryResultArtData<ArtObject> fetchByObjectIDs(Collection<Long> arg0,
			gov.nga.common.entities.art.ArtObject.SORT... arg1) throws DataNotReadyException  
	{
		return manager.fetchByObjectIDs(arg0, arg1);
	}

	@Override
	public <T extends ArtObject> QueryResultArtData<T> fetchByObjectIDs(Collection<Long> arg0, ArtObjectFactory<T> arg1,
			gov.nga.common.entities.art.ArtObject.SORT... arg2) throws DataNotReadyException  
	{
		return manager.fetchByObjectIDs(arg0, arg1, arg2);
	}

	@Override
	public QueryResultArtData<Place> fetchByPlaceKey(String arg0)  
	{
		return manager.fetchByPlaceKey(arg0);
	}

	@Override
	public QueryResultArtData<Place> fetchByTMSLocationID(long arg0) 
	{
		return manager.fetchByTMSLocationID(arg0);
	}

	@Override
	public QueryResultArtData<ArtObject> fetchObjectsByRelationships(List<ArtObjectConstituent> arg0) 
	{
		return manager.fetchObjectsByRelationships(arg0);
	}

	@Override
	public <T extends ArtObject> QueryResultArtData<T> fetchObjectsByRelationships(List<ArtObjectConstituent> arg0,
			ArtObjectFactory<T> arg1) 
	{
		return manager.fetchObjectsByRelationships(arg0, arg1);
	}

	@Override
	public <T extends ArtObject> QueryResultArtData<T> fetchRelatedWorks(ArtObject arg0, ArtObjectFactory<T> arg1)
			throws DataNotReadyException 
	{
		return manager.fetchRelatedWorks(arg0, arg1);
	}

	@Override
	public QueryResultArtData<Media> getMediaByEntityRelationship(String arg0) 
	{
		return manager.getMediaByEntityRelationship(arg0);
	}

	@Override
	public QueryResultArtData<ArtObject> searchArtObjects(SearchHelper<ArtObject> arg0, ResultsPaginator arg1,
			FacetHelper arg2, Enum<?>... arg3) throws DataNotReadyException 
	{
		return manager.searchArtObjects(arg0, arg1, arg2, arg3);
	}

	@Override
	public QueryResultArtData<ArtObject> searchArtObjects(SearchHelper<ArtObject> arg0, ResultsPaginator arg1,
			FacetHelper arg2, SortHelper<ArtObject> arg3) throws DataNotReadyException 
	{
		return manager.searchArtObjects(arg0, arg1, arg2, arg3);
	}

	@Override
	public <T extends ArtObject> QueryResultArtData<T> searchArtObjects(SearchHelper<T> arg0, ResultsPaginator arg1,
			FacetHelper arg2, ArtObjectFactory<T> arg3, Enum<?>... arg4) throws DataNotReadyException 
	{
		return manager.searchArtObjects(arg0, arg1, arg2, arg3, arg4);
	}

	@Override
	public <T extends ArtObject> QueryResultArtData<T> searchArtObjects(SearchHelper<T> arg0, ResultsPaginator arg1,
			FacetHelper arg2, SortHelper<T> arg3, ArtObjectFactory<T> arg4) throws DataNotReadyException 
	{
		return manager.searchArtObjects(arg0, arg1, arg2, arg3, arg4);
	}

	@Override
	public <T extends ArtObject> QueryResultArtData<T> searchArtObjects(SearchHelper<T> arg0, ResultsPaginator arg1,
			FacetHelper arg2, ArtObjectFactory<T> arg3, FreeTextSearchable<T> arg4, Enum<?>... arg5)
			throws DataNotReadyException 
	{
		return manager.searchArtObjects(arg0, arg1, arg2, arg3, arg4, arg5);
	}

	@Override
	public <T extends ArtObject> QueryResultArtData<T> searchArtObjects(SearchHelper<T> arg0, ResultsPaginator arg1,
			FacetHelper arg2, SortHelper<T> arg3, ArtObjectFactory<T> arg4, FreeTextSearchable<T> arg5)
			throws DataNotReadyException  
	{
		return manager.searchArtObjects(arg0, arg1, arg2, arg3, arg4, arg5);
	}

	@Override
	public QueryResultArtData<Constituent> searchConstituents(SearchHelper<Constituent> arg0, ResultsPaginator arg1,
			FacetHelper arg2, Enum<?>... arg3)  
	{
		return manager.searchConstituents(arg0, arg1, arg2, arg3);
	}

	@Override
	public QueryResultArtData<Constituent> searchConstituents(SearchHelper<Constituent> arg0, ResultsPaginator arg1,
			FacetHelper arg2, SortHelper<Constituent> arg3)  
	{
		return manager.searchConstituents(arg0, arg1, arg2, arg3);
	}

	@Override
	public <C extends Constituent> QueryResultArtData<C> searchConstituents(SearchHelper<C> arg0, ResultsPaginator arg1,
			FacetHelper arg2, ConstituentFactory<C> arg3, Enum<?>... arg4) 
	{
		return manager.searchConstituents(arg0, arg1, arg2, arg3, arg4);
	}

	@Override
	public <C extends Constituent> QueryResultArtData<C> searchConstituents(SearchHelper<C> arg0, ResultsPaginator arg1,
			FacetHelper arg2, SortHelper<C> arg3, ConstituentFactory<C> arg4)  
	{
		return manager.searchConstituents(arg0, arg1, arg2, arg3, arg4);
	}

	@Override
	public <C extends Constituent> QueryResultArtData<C> searchConstituents(SearchHelper<C> arg0, ResultsPaginator arg1,
			FacetHelper arg2, ConstituentFactory<C> arg3, FreeTextSearchable<C> arg4, Enum<?>... arg5)  
	{
		return manager.searchConstituents(arg0, arg1, arg2, arg3, arg4, arg5);
	}

	@Override
	public <C extends Constituent> QueryResultArtData<C> searchConstituents(SearchHelper<C> arg0, ResultsPaginator arg1,
			FacetHelper arg2, SortHelper<C> arg3, ConstituentFactory<C> arg4, FreeTextSearchable<C> arg5) 
	{
		return manager.searchConstituents(arg0, arg1, arg2, arg3, arg4, arg5);
	}

	@Override
	public QueryResultArtData<Exhibition> searchExhibitions(SearchHelper<Exhibition> arg0, ResultsPaginator arg1,
			SortHelper<Exhibition> arg2) throws DataNotReadyException 
	{
		return manager.searchExhibitions(arg0, arg1, arg2);
	}

	@Override
	public <T extends Exhibition> QueryResultArtData<T> searchExhibitions(SearchHelper<T> arg0, ResultsPaginator arg1,
			SortHelper<T> arg2, ExhibitionFactory<T> arg3) throws DataNotReadyException 
	{
		return manager.searchExhibitions(arg0, arg1, arg2, arg3);
	}

	@Override
	public QueryResultSuggestion<ArtDataSuggestion> suggest(SuggestType arg0, String arg1)
	{
		return manager.suggest(arg0, arg1);
	}

	@Override
	public QueryResultSuggestion<ArtDataSuggestion> suggestArtObjectFromArtist(String arg0, String arg1)
	{
		return manager.suggestArtObjectFromArtist(arg0, arg1);
	}

	@Override
	public QueryResultSuggestion<ArtDataSuggestion> suggestArtObjectTitles(String arg0, String arg1)
	{
		return manager.suggestArtObjectTitles(arg0, arg1);
	}

	@Override
	public QueryResultSuggestion<ArtDataSuggestion> suggestArtObjects(String arg0)
	{
		return manager.suggestArtObjects(arg0);
	}

	@Override
	public QueryResultSuggestion<ArtDataSuggestion> suggestArtistNames(String arg0)
	{
		return manager.suggestArtistNames(arg0);
	}

	@Override
	public QueryResultSuggestion<ArtDataSuggestion> suggestArtists(String arg0)
	{
		return manager.suggestArtists(arg0);
	}

	@Override
	public QueryResultSuggestion<ArtDataSuggestion> suggestExhibitions(String arg0)
	{
		return manager.suggestExhibitions(arg0);
	}

	@Override
	public QueryResultSuggestion<ArtDataSuggestion> suggestOwnerNames(String arg0)
	{
		return manager.suggestOwnerNames(arg0);
	}

	@Override
	public QueryResultSuggestion<ArtDataSuggestion> suggestOwners(String arg0) 
	{
		return manager.suggestOwners(arg0);
	}
}
