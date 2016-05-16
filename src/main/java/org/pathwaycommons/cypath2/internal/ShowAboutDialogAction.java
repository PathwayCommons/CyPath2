package org.pathwaycommons.cypath2.internal;

import java.awt.event.ActionEvent;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.cytoscape.application.swing.AbstractCyAction;

final class ShowAboutDialogAction extends AbstractCyAction {
	
	private static final long serialVersionUID = 3111419593280157192L;
	
	private final AboutDialog dialog;
	private final Icon icon;
	private final String title;
	private final String description;
	
	public ShowAboutDialogAction(
			Map<String, String> configProps, String title, String description)
	{
		super(configProps, App.cyServices.applicationManager, App.cyServices.networkViewManager);
		
		icon = new ImageIcon(getClass().getResource("pc2.png"), "PC2 icon");
		this.title = title;
		this.description = description;
		dialog = new AboutDialog(App.cyServices.cySwingApplication.getJFrame(), App.cyServices.openBrowser);
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		dialog.showDialog("About " + title, icon, description);
	}

}
