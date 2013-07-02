package org.pathwaycommons.cypath2.internal;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.cytoscape.work.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.client.CPath2Client;
import cpath.client.util.CPathException;
import cpath.service.GraphType;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.TraverseEntry;
import cpath.service.jaxb.TraverseResponse;

/**
 * Contains information regarding the currently selected set of interaction bundles.
 *
 */
final class HitsModel extends Observable {

    private SearchResponse response;
    
    final boolean parentPathwaysRequired;
    final String title;
    
    final Map<String, Integer> numHitsByTypeMap =  new ConcurrentSkipListMap<String, Integer>();
    final Map<String, Integer> numHitsByOrganismMap = new ConcurrentSkipListMap<String, Integer>();
    final Map<String, Integer> numHitsByDatasourceMap = new ConcurrentSkipListMap<String, Integer>();       
    final Map<String, String> hitsSummaryMap = new ConcurrentHashMap<String, String>();
    final Map<String, Collection<NvpListItem>> hitsPathwaysMap = new ConcurrentHashMap<String, Collection<NvpListItem>>();

    // full-text search query parameter
    volatile String searchFor = "Interaction"; 
    // advanced (graph or multiple items import) query parameter 
    volatile GraphType graphType = null;
    final CPath2Client graphQueryClient; 
    
	// dynamic map - one cpath-client instance per biopax property path (used by multiple thread)
	private static final Map<String, CPath2Client> proprtyPathToClientMap 
		= Collections.synchronizedMap(new HashMap<String, CPath2Client>());
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HitsModel.class);
    
    public HitsModel(String title, boolean parentPathwaysUsed, 
    		TaskManager taskManager, CPath2Client graphQueryClient) 
    {
		this.parentPathwaysRequired = parentPathwaysUsed;
		this.title = title;
		this.graphQueryClient = graphQueryClient;
	}
    
    public int getNumRecords() {
        if (response != null && !response.isEmpty()) {
            return response.getSearchHit().size();
        } else {
            return -1;
        }
    }

    
    /**
     * Refresh the model and notify all observers INFO_ABOUT it's changed.
     * 
     * @param response
     */
	public synchronized void update(final SearchResponse response) {
		this.response = response;
		
		numHitsByTypeMap.clear();
		numHitsByOrganismMap.clear();
		numHitsByDatasourceMap.clear();
		hitsSummaryMap.clear();
		hitsPathwaysMap.clear();
		
		// get/save info INFO_ABOUT components/participants/members;
		// run in a few separate threads
		ExecutorService exec = Executors.newFixedThreadPool(10);
		for (final SearchHit record : response.getSearchHit())
		{
			exec.submit(new Runnable() {		
				@Override
				public void run() {
					summarize(record);
				}
			});
		}

		exec.shutdown(); //prevents new jobs
		try {
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted unexpectedly!");
		}
		
		// notify observers (panels and jlists)
		HitsModel.this.setChanged();
		HitsModel.this.notifyObservers(response);
	}

    
    public SearchResponse getSearchResponse() {
        return response;
    }

    
	private void summarize(final SearchHit hit) {
        
		// do catalog -
		String type = hit.getBiopaxClass();
        Integer count = numHitsByTypeMap.get(type);
        if (count != null) {
        	numHitsByTypeMap.put(type, count + 1);
        } else {
        	numHitsByTypeMap.put(type, 1);
        }
        
        for(String org : hit.getOrganism()) {
        	Integer i = numHitsByOrganismMap.get(org);
            if (i != null) {
                numHitsByOrganismMap.put(org, i + 1);
            } else {
            	numHitsByOrganismMap.put(org, 1);
            }
        }
        
        for(String ds : hit.getDataSource()) {
        	Integer i = numHitsByDatasourceMap.get(ds);
            if (i != null) {
                numHitsByDatasourceMap.put(ds, i + 1);
            } else {
            	numHitsByDatasourceMap.put(ds, 1);
            }
        }
		
		
		// get/create and show hit's summary
		StringBuilder html = new StringBuilder();
		html.append("<html>")
			.append("<h2>").append(hit.getBiopaxClass()).append("</h2>");
		if (hit.getName() != null)
			html.append("<b>").append(hit.getName()).append("</b><br/>");
		
		final String uri = hit.getUri();
		html.append("<em>URI='").append(uri).append("'</em><br/>");
		
		//create a fake link to be intercepted when clicked
		String linkText = "Click to Import (a sub-network or neighborhood network).";
//		if(searchFor.equalsIgnoreCase("Pathway") || searchFor.equalsIgnoreCase("Interaction")) { //simply use 'get' query
//			linkText = "Click to Import.";
//		}
		html.append("<a href='#'>").append(linkText).append("</a>");

		//TODO may create other links (to e.g. provider's site)
		
		String primeExcerpt = hit.getExcerpt();
		if (primeExcerpt != null)
			html.append("<h3>Excerpt:</h3>")
				.append("<span class='excerpt'>")
					.append(primeExcerpt)
						.append("</span><br/>");
		//get organism URIs
		List<String> items = hit.getOrganism();
		if (items != null && !items.isEmpty()) {
			html.append("<h3>Organisms:</h3>").append("<ul>");
			for(String it : items) {
				String name = CyPath2.uriToOrganismNameMap.get(it);
				name = (name == null || name.isEmpty()) ? it : name;
				html.append("<li>")
					.append(name)
					.append("</li>");
			}
			html.append("</ul>");
		}
		//get datasource URIs
		items = hit.getDataSource();
		if (items != null && !items.isEmpty()) {
			html.append("<h3>Data sources:</h3>").append("<ul>");
			for(String it : items) {
				String name = CyPath2.uriToDatasourceNameMap.get(it);
				name = (name == null || name.isEmpty()) ? it : name;
				html.append("<li>")
					.append(name)
					.append("</li>");
			}
			html.append("</ul>");
		}

		String path = null;
		if("Pathway".equalsIgnoreCase(hit.getBiopaxClass()))
			path = "Pathway/pathwayComponent";
		else if("Complex".equalsIgnoreCase(hit.getBiopaxClass()))
				path = "Complex/component";
		else if(searchFor.equalsIgnoreCase("Interaction"))
			path = "Interaction/participant";
		else if(searchFor.equalsIgnoreCase("PhysicalEntity"))
				path = "PhysicalEntity/memberPhysicalEntity";
		else if(searchFor.equalsIgnoreCase("EntityReference"))
			path = "EntityReference/memberEntityReference";
		
		assert (path != null);
		
		// get names
		TraverseResponse members = traverse(path + ":Named/displayName", 
				Collections.singleton(uri));
		
		if (members == null)
		// no names? - get uris then
			members = traverse(path, Collections.singleton(uri));
			
		if (members != null) {
			List<String> values  = members.getTraverseEntry().get(0).getValue();
			if (!values.isEmpty())
				html.append("<h3>Contains ").append(values.size())
				.append(" (direct) members:</h3>")
				.append(StringUtils.join(values, "<br/>"));
		}

		html.append("</html>");		
		hitsSummaryMap.put(hit.getUri(), html.toString());
		
		if(this.parentPathwaysRequired && !hit.getPathway().isEmpty()) {
			// add parent pathways to the Map ("ppw" prefix means "parent pathway's" -)
			final Collection<NvpListItem> ppws = new TreeSet<NvpListItem>();	
			final Set<String> ppwUris = new HashSet<String>(hit.getPathway()); // a hack for not unique URIs (a cpath2 indexing bug...)
			TraverseResponse ppwNames = traverse("Named/displayName", ppwUris);
			if (ppwNames != null) {
				Map<String, String> ppwUriToNameMap = new HashMap<String, String>();			
				for (TraverseEntry e : ppwNames.getTraverseEntry()) {
					String name = (e.getValue().isEmpty()) ? e.getUri() : e.getValue().get(0);
					ppwUriToNameMap.put(e.getUri(), name);
				}
				
				// add ppw component counts to names and wrap/save as NVP, finally:
				TraverseResponse ppwComponents = traverse("Pathway/pathwayComponent", ppwUris); // this gets URIs of pathways
				for (TraverseEntry e : ppwComponents.getTraverseEntry()) {
					assert ppwUriToNameMap.containsKey(e.getUri());
					String name = ppwUriToNameMap.get(e.getUri());
					ppws.add(new NvpListItem(name + " (" + e.getValue().size() + " processes)", e.getUri()));
				}
			}
			hitsPathwaysMap.put(hit.getUri(), ppws);
		}
		
	}
	
	
	private TraverseResponse traverse(String path, Set<String> uris) {
		if(LOGGER.isDebugEnabled())
	   		LOGGER.debug("traverse: path=" + path);
	   		
	   	CPath2Client client = proprtyPathToClientMap.get(path);
	   	if(client == null) {
	   		client = CPath2Client.newInstance(graphQueryClient.getActualEndPointURL());
	   		client.setPath(path);
	   		proprtyPathToClientMap.put(path, client);
	   	}
	        
        TraverseResponse res = null;
		try {
			res = client.traverse(uris);
		} catch (CPathException e) {
			LOGGER.error("traverse: " + path + 
				" failed; uris:" + uris.toString(), e);
		}
				
       	return res;
	}

}