/**
 * 
 */
package org.pathwaycommons.cypath2.internal;

import org.cytoscape.model.CyNode;
import org.cytoscape.task.AbstractNodeViewTaskFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.TaskIterator;

/**
 * @author rodche
 */
final class ExpandNodeContextMenuFactory extends AbstractNodeViewTaskFactory {

	public ExpandNodeContextMenuFactory(
			VisualMappingManager visualMappingManagerRef,
			CyLayoutAlgorithmManager cyLayoutsRef) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public TaskIterator createTaskIterator(View<CyNode> cyNodeView, CyNetworkView cyNetworkView) {
		// TODO Auto-generated method stub
		return null;
		//return new TaskIterator(new NeighborhoodQueryTask(cyNetworkView, cyNodeView, client));
	}

}
