package org.pathwaycommons.cypath2.internal;

import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Map;

import javax.swing.JDialog;

import org.cytoscape.application.swing.AbstractCyAction;

final class ShowTheDialogAction extends AbstractCyAction {
	
	private static final long serialVersionUID = -5069785324747282157L;
	
	private final JDialog dialog;
	private final Window parent;

	
	public ShowTheDialogAction(Map<String, String> configProps,CyServices cyServices, Container gui) 
	{
		super(configProps, cyServices.applicationManager, cyServices.networkViewManager);
		
		this.parent = cyServices.cySwingApplication.getJFrame();
		dialog = new JDialog(parent);
		dialog.add(gui);
	}

	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}

}
