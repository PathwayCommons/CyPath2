package org.pathwaycommons.cypath2.internal;

import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Map;

import javax.swing.JDialog;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.view.model.CyNetworkViewManager;

final class ShowTheDialogAction extends AbstractCyAction {
	
	private final JDialog dialog;
	private final Window parent;
	
	public ShowTheDialogAction(Map<String, String> configProps,
			Window parent, 
			Container gui,
			CyApplicationManager applicationManager,
			CyNetworkViewManager networkViewManager) 
	{
		super(configProps, applicationManager, networkViewManager);
		
		this.parent = parent;
		dialog = new JDialog(parent);
		dialog.add(gui);
	}

	private static final long serialVersionUID = -5069785324747282157L;

	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}

}
