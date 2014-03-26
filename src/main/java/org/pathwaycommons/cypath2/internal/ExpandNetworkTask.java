package org.pathwaycommons.cypath2.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import cpath.query.CPathGraphQuery;
import cpath.service.GraphType;

public class ExpandNetworkTask extends AbstractTask implements Task {

	@ProvidesTitle
	public String tunablesDlgTitle() {
		return "CyPathwayCommons: Expand Network (into a new network)";
	}
	
	@Tunable(description = "Use (an identifier) column:", gravity=701, groups = " ", 
			tooltip="Select a column (string type node attribute) where standard bio identifiers<br/> " +
				"or URIs are stored (e.g., HUGO symbols, UniProt, RefSeq, Entrez Gene, <br/>" +
				"ChEBI, PubChem Substance IDs, <br/>Pathway Commons or Identifiers.org URIs, " +
				"<br/>or all these kinds mixed, should be fine for the query)")
	public ListSingleSelection<String> columnSelection;
	
	@Tunable(description = "Graph query type:", gravity=705, groups = " ", 
			tooltip = "NOTE: filter values set in CyPathwayCommons 'Options' panel apply for this query as well.")
	public ListSingleSelection<String> querySelection;
	
	private final CyNetwork network;
		
	public ExpandNetworkTask(CyNetworkView cyNetworkView, View<CyNode> cyNodeView) 
	{
		network = cyNetworkView.getModel();
		columnSelection = getTargetColumns();
		querySelection = new ListSingleSelection<String>("NEIGHBORHOOD","PATHSBETWEEN");
		querySelection.setSelectedValue("NEIGHBORHOOD");
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("CyPathwayCommons: Expanding Selected Nodes to a New Network");
		if(cancelled) return;
		
		final String column = columnSelection.getSelectedValue();
		final String graphType = querySelection.getSelectedValue();
		taskMonitor.setStatusMessage("Getting the specified attribute values (IDs) from selected nodes");
		//collect the specified column values from the selected nodes
		Set<String> values = new HashSet<String>();
		for(CyNode node : network.getNodeList()) {
			if (!network.getRow(node).get(CyNetwork.SELECTED, Boolean.class))
				continue;			
			String val = network.getRow(node).get(column, String.class);
			if(val != null && !val.isEmpty())
				values.add(val);
		}
		
		if(values.isEmpty()) {
			taskMonitor.setStatusMessage("No values (IDs) " +
					"found in the attribute column of currently selected nodes.");
			cancel();
			return;
		}
		
		taskMonitor.setProgress(0.1);
		
		//execute, create a new network and view (if any data will be returned from the server)
		taskMonitor.setStatusMessage("Executing " + graphType + " query (in Pathway Commons)");		
		final CPathGraphQuery graphQ = CyPC.client.createGraphQuery()
    			.kind(("NEIGHBORHOOD".equals(graphType)) ? GraphType.NEIGHBORHOOD : GraphType.PATHSBETWEEN)
    			.sources(values)
    			.datasourceFilter(CyPC.options.selectedDatasources())
    			//.limit(1) TODO set limit via tunables (default is 1)
    			.organismFilter(CyPC.options.selectedOrganisms());
    	CyPC.cyServices.taskManager.execute(new TaskIterator(
    			new NetworkAndViewTask(CyPC.cyServices, graphQ, null)
        		));	
		
    	taskMonitor.setStatusMessage("Done");   	
		taskMonitor.setProgress(1.0);
	}

	
	private ListSingleSelection<String> getTargetColumns() 
	{
		final CyTable table = network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS);
		final List<String> colNames = new ArrayList<String>();
		
		for (final CyColumn col : table.getColumns()) {
			// Exclude list, numerical, boolean type columns
			if (String.class.isAssignableFrom(col.getType())) {
				colNames.add(col.getName());
			}
		}

		ListSingleSelection<String> toReturn = 
				new ListSingleSelection<String>(colNames);
		
		if(colNames.contains("UNIPROT"))
			toReturn.setSelectedValue("UNIPROT");
		else if(colNames.contains("GENE SYMBOL"))
			toReturn.setSelectedValue("GENE SYMBOL");
		else if(colNames.contains("URI"))
			toReturn.setSelectedValue("URI");
		else if(colNames.contains(CyRootNetwork.SHARED_NAME))
			toReturn.setSelectedValue(CyRootNetwork.SHARED_NAME); //less desired
		
		return toReturn;
	}
}
