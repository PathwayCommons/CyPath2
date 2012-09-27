package org.cytoscape.cpathsquared.internal;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.cytoscape.work.TaskManager;

import cpath.client.CPath2Client;
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
    @Deprecated
    private final TaskManager taskManager;
    
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
    
    public HitsModel(String title, boolean parentPathwaysUsed, TaskManager taskManager) {
		this.parentPathwaysRequired = parentPathwaysUsed;
		this.title = title;
		this.taskManager = taskManager;
		this.graphQueryClient = CyPath2.newClient();
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

		ExecutorService exec = Executors.newFixedThreadPool(10);
		
		// get/save info INFO_ABOUT components/participants/members
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
        return unmodifiableSearchResponce(response);
    }

    
	private void summarize(final SearchHit item) {
        
		// do catalog -
		String type = item.getBiopaxClass();
        Integer count = numHitsByTypeMap.get(type);
        if (count != null) {
        	numHitsByTypeMap.put(type, count + 1);
        } else {
        	numHitsByTypeMap.put(type, 1);
        }
        
        for(String org : item.getOrganism()) {
        	Integer i = numHitsByOrganismMap.get(org);
            if (i != null) {
                numHitsByOrganismMap.put(org, i + 1);
            } else {
            	numHitsByOrganismMap.put(org, 1);
            }
        }
        
        for(String ds : item.getDataSource()) {
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
			.append("<h2>").append(item.getBiopaxClass()).append("</h2>");
		if (item.getName() != null)
			html.append("<b>").append(item.getName()).append("</b><br/>");
		
		html.append("<em>URI='").append(item.getUri()).append("'</em><br/>");
		
		//create a link to be intercepted/converted to a (import a sub-model) Task!
		String linkUrl = CyPath2.newClient().queryNeighborhood(Collections.singleton(item.getUri()));
		String linkText = "Click to import (nearest neighborhood network).";
		//TODO in the future, consider subclasses of Interaction as well or limit possible values entered by users -
		if(searchFor.equalsIgnoreCase("Pathway") || searchFor.equalsIgnoreCase("Interaction")) { //simply use 'get' query
			linkUrl = CyPath2.newClient().queryGet(Collections.singleton(item.getUri()));
			linkText = "Click to import (new network).";
		}
		html.append("<a href='")
			.append(linkUrl).append("'>")
			.append(linkText).append("</a>");

		//TODO may create other links (to e.g. provider's site)
		
		String primeExcerpt = item.getExcerpt();
		if (primeExcerpt != null)
			html.append("<h3>Excerpt:</h3>")
				.append("<span class='excerpt'>")
					.append(primeExcerpt)
						.append("</span><br/>");
		
		List<String> items = item.getOrganism();
		if (items != null && !items.isEmpty()) {
			html.append("<h3>Organisms:</h3>").append("<ul>");
			for(String uri : items) {
				html.append("<li>")
					.append(CyPath2.uriToOrganismNameMap.get(uri))
					.append("</li>");
			}
			html.append("</ul>");
		}
		
		items = item.getDataSource();
		if (items != null && !items.isEmpty()) {
			html.append("<h3>Data sources:</h3>").append("<ul>");
			for(String uri : items) {
				html.append("<li>")
					.append(CyPath2.uriToDatasourceNameMap.get(uri))
					.append("</li>");
			}
			html.append("</ul>");
		}

		String path = null;
		if("Pathway".equalsIgnoreCase(item.getBiopaxClass()))
			path = "Pathway/pathwayComponent";
		else if("Complex".equalsIgnoreCase(item.getBiopaxClass()))
				path = "Complex/component";
		else if(searchFor.equalsIgnoreCase("Interaction"))
			path = "Interaction/participant";
		else if(searchFor.equalsIgnoreCase("PhysicalEntity"))
				path = "PhysicalEntity/memberPhysicalEntity";
		else if(searchFor.equalsIgnoreCase("EntityReference"))
			path = "EntityReference/memberEntityReference";
		
		assert (path != null);
		TraverseResponse members = CyPath2
			.traverse(path + ":Named/displayName", 
				Collections.singleton(item.getUri()));
		
		if (members != null) {
			List<String> values  = members.getTraverseEntry().get(0).getValue();
			if (!values.isEmpty())
				html.append("<h3>Contains ").append(values.size())
				.append(" (direct) members:</h3>")
				.append(StringUtils.join(values, "<br/>"));
		}

		html.append("</html>");		
		hitsSummaryMap.put(item.getUri(), html.toString());
		
		if(this.parentPathwaysRequired && !item.getPathway().isEmpty()) {
			// add parent pathways to the Map ("ppw" prefix means "parent pathway's" -)
			final Collection<NvpListItem> ppws = new TreeSet<NvpListItem>();	
			final Set<String> ppwUris = new HashSet<String>(item.getPathway()); // a hack for not unique URIs (a cpath2 indexing bug...)
			TraverseResponse ppwNames =CyPath2.traverse("Named/displayName", ppwUris);
			if (ppwNames != null) {
				Map<String, String> ppwUriToNameMap = new HashMap<String, String>();			
				for (TraverseEntry e : ppwNames.getTraverseEntry()) {
					String name = (e.getValue().isEmpty()) ? e.getUri() : e.getValue().get(0);
					ppwUriToNameMap.put(e.getUri(), name);
				}
				
				// add ppw component counts to names and wrap/save as NVP, finally:
				TraverseResponse ppwComponents = CyPath2.traverse(
						"Pathway/pathwayComponent", ppwUris); // this gets URIs of pathways
				for (TraverseEntry e : ppwComponents.getTraverseEntry()) {
					assert ppwUriToNameMap.containsKey(e.getUri());
					String name = ppwUriToNameMap.get(e.getUri());
					ppws.add(new NvpListItem(name + " (" + e.getValue().size() + " processes)", e.getUri()));
				}
			}
			hitsPathwaysMap.put(item.getUri(), ppws);
		}
		
	}
	
	
	private SearchResponse unmodifiableSearchResponce(final SearchResponse resp) {
		if (resp == null)
			return null;

		// create a read-only proxy/view
		SearchResponse imm = new SearchResponse() {
			@Override
			public String getComment() {
				return resp.getComment();
			}

			@Override
			public Integer getMaxHitsPerPage() {
				return resp.getMaxHitsPerPage();
			}

			@Override
			public Integer getNumHits() {
				return resp.getNumHits();
			}

			@Override
			public Integer getPageNo() {
				return resp.getPageNo();
			}

			@Override
			public List<SearchHit> getSearchHit() {
				return Collections.unmodifiableList(resp.getSearchHit());
			}

			@Override
			public boolean isEmpty() {
				return resp.isEmpty();
			}

			@Override
			public void setComment(String comment) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setMaxHitsPerPage(Integer maxHitsPerPage) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setNumHits(Integer numHits) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setPageNo(Integer pageNo) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setSearchHit(List<SearchHit> searchHit) {
				throw new UnsupportedOperationException();
			}
		};

		return imm;
	}
}