package llc.alersconsulting.core.client.testers;

import java.util.List;
import java.util.Map;

import gov.nga.common.entities.art.ArtDataSuggestion;
import gov.nga.common.entities.art.ArtObject;
import gov.nga.common.entities.art.QueryResultSuggestion;
import gov.nga.common.entities.art.SuggestType;
import gov.nga.common.search.FreeTextSearchable;
import gov.nga.common.utils.CollectionUtils;
import llc.alersconsulting.core.client.CoreDataService;

public class MockArtObjectFreeTextSearcher implements FreeTextSearchable<ArtObject>
{
    public static enum MOCKFILTER {MOCKTITLE};
    
    private final CoreDataService manager;
    
    protected MockArtObjectFreeTextSearcher(final CoreDataService manager)
    {
        this.manager = manager;
    }
    
    private Map<Long, ArtObject> createMapFromList(List<ArtObject> baseList)
    {
        final Map<Long, ArtObject> baseMap = CollectionUtils.newHashMap();
        if (baseList != null)
        {
            for (ArtObject obj: baseList)
            {
                baseMap.put(obj.getObjectID(), obj);
            }
        }
        return baseMap;
    }

    @Override
    public List<ArtObject> freeTextSearch(List<Enum<?>> fields,
            List<String> searchTerms, List<ArtObject> baseList) 
    {
        final List<ArtObject> newResults = CollectionUtils.newArrayList();
        boolean performSearch = false;
        for (Enum<?> fld: fields)
        {
            if (fld.equals(MOCKFILTER.MOCKTITLE))
            {
                performSearch = true;
            }
        }
        if (performSearch)
        {
            final Map<Long, ArtObject> curRsltMap = createMapFromList(baseList);
            //we're just going to mock this using the suggest feature. Each term is an OR statement
            for (String term: searchTerms)
            {
                QueryResultSuggestion<? extends ArtDataSuggestion> rslt = manager.suggest(SuggestType.ARTOBJECT_TITLE_ID, term);
                if (rslt.getResultCount() > 0)
                {
                    for (ArtDataSuggestion hit: rslt.getResults())
                    {
                        if (curRsltMap.containsKey(hit.getEntityID()))
                        {
                            newResults.add(curRsltMap.get(hit.getEntityID()));
                            curRsltMap.remove(hit.getEntityID());
                        }
                    }
                }
            }
        }
        else
        {
            newResults.addAll(baseList);
        }
        return newResults;
    }


}
