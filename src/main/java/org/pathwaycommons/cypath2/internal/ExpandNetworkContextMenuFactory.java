package org.pathwaycommons.cypath2.internal;

import org.cytoscape.model.CyNode;
import org.cytoscape.task.AbstractNodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskIterator;

/**
 * @author rodche
 */
final class ExpandNetworkContextMenuFactory extends AbstractNodeViewTaskFactory {

	private final CyServices cyServices;	
	
	public ExpandNetworkContextMenuFactory(CyServices cyServices) {
		this.cyServices = cyServices;
	}

	@Override
	public TaskIterator createTaskIterator(View<CyNode> cyNodeView, CyNetworkView cyNetworkView) {
		return new TaskIterator(new ExpandNetworkTask(cyNetworkView, cyNodeView));
	}

}
