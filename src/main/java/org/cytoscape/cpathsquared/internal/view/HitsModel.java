package org.cytoscape.cpathsquared.internal.view;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.cytoscape.cpathsquared.internal.CPath2Factory;
import org.cytoscape.cpathsquared.internal.CPath2Factory.SearchFor;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

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
    
    final Map<String, Integer> numHitsByTypeMap = new TreeMap<String, Integer>();
    final Map<String, Integer> numHitsByOrganismMap = new TreeMap<String, Integer>();
    final Map<String, Integer> numHitsByDatasourceMap = new TreeMap<String, Integer>();
    
    final Map<String, String> hitsSummaryMap = new HashMap<String, String>();
    final Map<String, Collection<NameValuePairListItem>> hitsPathwaysMap 
    		= new HashMap<String, Collection<NameValuePairListItem>>();

    
    public HitsModel(boolean parentPathwaysUsed) {
		this.parentPathwaysRequired = parentPathwaysUsed;
	}
    
    public int getNumRecords() {
        if (response != null && !response.isEmpty()) {
            return response.getSearchHit().size();
        } else {
            return -1;
        }
    }
  

    private void catalog(final SearchHit record) {
        String type = record.getBiopaxClass();
        Integer count = numHitsByTypeMap.get(type);
        if (count != null) {
        	numHitsByTypeMap.put(type, count + 1);
        } else {
        	numHitsByTypeMap.put(type, 1);
        }
        
        for(String org : record.getOrganism()) {
        	Integer i = numHitsByOrganismMap.get(org);
            if (i != null) {
                numHitsByOrganismMap.put(org, i + 1);
            } else {
            	numHitsByOrganismMap.put(org, 1);
            }
        }
        
        for(String ds : record.getDataSource()) {
        	Integer i = numHitsByDatasourceMap.get(ds);
            if (i != null) {
                numHitsByDatasourceMap.put(ds, i + 1);
            } else {
            	numHitsByDatasourceMap.put(ds, 1);
            }
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
	
		// get/save info INFO_ABOUT components/participants/members
		TaskIterator taskIterator = new TaskIterator(new Task() {
			@Override
			public void run(TaskMonitor taskMonitor) throws Exception {
				try {
					taskMonitor.setTitle("cPathSquared Task");
					taskMonitor.setProgress(0.1);
					taskMonitor.setStatusMessage("Summarizing search hits...");
					float i = 0;
					final int sz = response.getSearchHit().size();
					for (SearchHit record : response.getSearchHit()) {
						catalog(record);
						summarize(record);
						taskMonitor.setProgress(++i/sz);
					}	
					
					//notify observers (panels and jlists)
					HitsModel.this.setChanged();
					HitsModel.this.notifyObservers(response);
				} catch (Throwable e) { 
					//fail on both when there is no data (server error) and runtime/osgi errors
					throw new RuntimeException(e);
				} finally {
					taskMonitor.setStatusMessage("Done");
					taskMonitor.setProgress(1.0);
				}
			}
			@Override
			public void cancel() {
			}
		});
		
		// kick task execution
		CPath2Factory.getTaskManager().execute(taskIterator);
	}

    
    public SearchResponse getSearchResponse() {
        return CPath2Factory.unmodifiableSearchResponce(response);
    }

    
    //to be run within a Task (does WS queries)!
	private void summarize(final SearchHit item) {
		// get/create and show hit's summary
		StringBuilder html = new StringBuilder();
		html.append("<html>")
			.append("<h2>").append(item.getBiopaxClass()).append("</h2>");
		if (item.getName() != null)
			html.append("<b>").append(item.getName()).append("</b><br/>");
		
		html.append("<em>URI='").append(item.getUri()).append("'</em><br/>");
		
		// TODO can generate links (to be either opened in system's browser or intercepted/converted to a Task!)
//		html.append("<a href='").append("Foo").append("'>").append("Bar").append("</a>");

		String primeExcerpt = item.getExcerpt();
		if (primeExcerpt != null)
			html.append("<h3>Excerpt:</h3>")
				.append("<span class='excerpt'>")
					.append(primeExcerpt)
						.append("</span><br/>");
		
		List<String> items = item.getOrganism();
		if (items != null && !items.isEmpty())
			html.append("<h3>Organisms:</h3>")
				.append(StringUtils.join(items, "<br/>"));
		
		items = item.getDataSource();
		if (items != null && !items.isEmpty())
			html.append("<h3>Data sources:</h3>")
				.append(StringUtils.join(items, "<br/>"));
		
		String path = null;
		if("Pathway".equalsIgnoreCase(item.getBiopaxClass()))
			path = "Pathway/pathwayComponent";
		else if("Complex".equalsIgnoreCase(item.getBiopaxClass()))
				path = "Complex/component";
		else if(CPath2Factory.searchFor == SearchFor.INTERACTION)
			path = "Interaction/participant";
		else if(CPath2Factory.searchFor == SearchFor.PHYSICALENTITY)
				path = "PhysicalEntity/memberPhysicalEntity";		
		TraverseResponse members = CPath2Factory
			.traverse(path + ":Named/displayName", Collections.singleton(item.getUri()));
		
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
			final Collection<NameValuePairListItem> ppws = new TreeSet<NameValuePairListItem>();	
			final Set<String> ppwUris = new HashSet<String>(item.getPathway()); // a hack for not unique URIs (a cpath2 indexing bug...)
			TraverseResponse ppwNames = CPath2Factory.traverse("Named/displayName", ppwUris);
			if (ppwNames != null) {
				Map<String, String> ppwUriToNameMap = new HashMap<String, String>();			
				for (TraverseEntry e : ppwNames.getTraverseEntry()) {
					String name = (e.getValue().isEmpty()) ? e.getUri() : e.getValue().get(0);
					ppwUriToNameMap.put(e.getUri(), name);
				}
				
				// add ppw component counts to names and wrap/save as NVP, finally:
				TraverseResponse ppwComponents = CPath2Factory.traverse(
						"Pathway/pathwayComponent", ppwUris); // this gets URIs of pathways
				for (TraverseEntry e : ppwComponents.getTraverseEntry()) {
					assert ppwUriToNameMap.containsKey(e.getUri());
					String name = ppwUriToNameMap.get(e.getUri());
					ppws.add(new NameValuePairListItem(name + " (" + e.getValue().size() + " processes)", e.getUri()));
				}
			}
			hitsPathwaysMap.put(item.getUri(), ppws);
		}
		
	}
}