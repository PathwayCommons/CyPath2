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
	private final Container gui;
	
	public ShowTheDialogAction(Map<String, String> configProps, Container gui)
	{
		super(configProps, CyPC.cyServices.applicationManager, CyPC.cyServices.networkViewManager);
		
		this.parent = CyPC.cyServices.cySwingApplication.getJFrame();
		this.gui = gui;
		
		dialog = new JDialog(parent);
		dialog.setModal(true);
	}

	
	@Override
	public void actionPerformed(ActionEvent ae) {
		dialog.remove(gui);
		dialog.add(gui);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}

}
