package org.pathwaycommons.cypath2.internal;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.model.CyNetworkViewManager;

final class ShowAboutDialogAction extends AbstractCyAction {
	
	private static final long serialVersionUID = 3111419593280157192L;
	
	private final AboutDialog dialog;
	private final Icon icon;
	private final String title;
	private final String description;
	
	public ShowAboutDialogAction(Map<String, String> configProps,
			Window parent, 
			CyPath2 app,
			CyApplicationManager applicationManager,
			CyNetworkViewManager networkViewManager,
			OpenBrowser openBrowser) 
	{
		super(configProps, applicationManager, networkViewManager);
		
		icon = new ImageIcon(getClass().getResource("pc2.png"), "PC2 icon");
		title = app.getDisplayName();
		description = app.getDescription();
		dialog = new AboutDialog(parent, openBrowser);
	}

	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		dialog.showDialog("About " + title, icon, description);
	}

}
