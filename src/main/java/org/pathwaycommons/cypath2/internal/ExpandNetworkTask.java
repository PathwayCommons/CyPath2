package org.pathwaycommons.cypath2.internal;

import javax.swing.JOptionPane;

import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

class ExpandNetworkTask extends AbstractTask implements Task {

	
	public ExpandNetworkTask(CyNetworkView cyNetworkView,
			View<CyNode> cyNodeView) {
		// TODO ExpandNetworkTask - implement the constructor
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		// TODO ExpandNetworkTask - implement run method
		taskMonitor.setStatusMessage("TODO: This task is not implemented yet.");
		JOptionPane.showMessageDialog(null, "TODO: This task is not implemented yet.");
		taskMonitor.setProgress(1.0);
	}

}