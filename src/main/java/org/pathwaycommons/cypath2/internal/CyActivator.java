package org.pathwaycommons.cypath2.internal;

import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.work.undo.UndoSupport;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.property.CyProperty;
import org.cytoscape.io.read.CyNetworkReaderManager;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.cytoscape.service.util.AbstractCyActivator;

import cpath.client.CPath2Client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.cytoscape.work.ServiceProperties.*;


public final class CyActivator extends AbstractCyActivator {
	private static final Logger LOGGER = LoggerFactory.getLogger(CyActivator.class);
	private static final String PROP_CPATH2_SERVER_URL = "cypath2.server.url";
	private static final String PROPERTIES_LOCATION = "/cypath2.properties"; //classpath
	
	
	public CyActivator() {
		super();
		LOGGER.debug("creating...");
	}


	public void start(BundleContext bc) {
		LOGGER.debug("starting...");
		
		CySwingApplication cySwingApplication = getService(bc,CySwingApplication.class);
		DialogTaskManager taskManager = getService(bc,DialogTaskManager.class);
		OpenBrowser openBrowser = getService(bc,OpenBrowser.class);
		CyNetworkManager cyNetworkManager = getService(bc,CyNetworkManager.class);
		CyApplicationManager cyApplicationManager = getService(bc,CyApplicationManager.class);
		CyNetworkViewManager cyNetworkViewManager = getService(bc,CyNetworkViewManager.class);
		CyNetworkReaderManager cyNetworkReaderManager = getService(bc,CyNetworkReaderManager.class);
		CyNetworkNaming cyNetworkNaming = getService(bc,CyNetworkNaming.class);
		CyNetworkFactory cyNetworkFactory = getService(bc,CyNetworkFactory.class);
		CyLayoutAlgorithmManager cyLayoutAlgorithmManager = getService(bc,CyLayoutAlgorithmManager.class);
		UndoSupport undoSupport = getService(bc,UndoSupport.class);
		VisualMappingManager visualMappingManager = getService(bc,VisualMappingManager.class);
		VisualStyleFactory visualStyleFactory = getService(bc,VisualStyleFactory.class);
		VisualMappingFunctionFactory discreteVisualMappingFunctionFactory = getService(bc,VisualMappingFunctionFactory.class,"(mapping.type=discrete)");
		VisualMappingFunctionFactory passthroughMappingFactoryRef = getService(bc,VisualMappingFunctionFactory.class,"(mapping.type=passthrough)");
		CyProperty<Properties> cyProperties = getService(bc, CyProperty.class, "(cyPropertyName=cytoscape3.props)");
		
		final BinarySifVisualStyleFactory binarySifVisualStyleUtil = new BinarySifVisualStyleFactory(
				visualStyleFactory,
				visualMappingManager,
				discreteVisualMappingFunctionFactory,
				passthroughMappingFactoryRef);
		
		// keep all the service references in one place -
		final CyServices cyServices = new CyServices(cySwingApplication, taskManager, openBrowser, 
				cyNetworkManager, cyApplicationManager, cyNetworkViewManager, cyNetworkReaderManager, 
				cyNetworkNaming, cyNetworkFactory, cyLayoutAlgorithmManager, undoSupport, binarySifVisualStyleUtil, 
				visualMappingManager, cyProperties);
		
		
		// read current configuration properties
		Properties props = new Properties();
		try {
			props.load(getClass().getResourceAsStream(PROPERTIES_LOCATION));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		// (normally not needed but) one can switch between cpath2 servers by updating the property
		// in the user's CytoscapeConfiguration directory. If it's set, this app will use it; 
		// othervise, the default will be used/set -
		String url = cyProperties.getProperties().getProperty(PROP_CPATH2_SERVER_URL);
		//to set a new or update the deprecated from v0.2.2 PURL...
		if(url == null || url.isEmpty() || url.contains("purl.org/pc2/current")) { 
			url = CPath2Client.newInstance().getActualEndPointURL();
		    cyProperties.getProperties().setProperty(PROP_CPATH2_SERVER_URL, url);
		}    
	    final String name = "Pathway Commons 2 (BioPAX L3)";
	    final String description = props.getProperty("cypath2.description");			
		
		// Create a new CyPath2 app instance
		CyPath2 app = new CyPath2(url, name, description, cyServices);
		
		// initialize (build the UI, etc. heavy calls)
		app.init();
		
		// Register OSGi services
		registerAllServices(bc, app, new Properties());
		
		// Create a new menu/toolbar item (CyAction) that opens the CyPath2 GUI 
		Map<String,String> showTheDialogActionProps = new HashMap<String, String>();
		showTheDialogActionProps.put(ID,"showCyPath2DialogAction");
		showTheDialogActionProps.put(TITLE,"Search/Import Network...");		
		showTheDialogActionProps.put(PREFERRED_MENU, APPS_MENU + ".CyPath2");
		showTheDialogActionProps.put(MENU_GRAVITY,"2.0");
//		showTheDialogActionProps.put(TOOL_BAR_GRAVITY,"3.17");
//		showTheDialogActionProps.put(LARGE_ICON_URL,getClass().getResource("pc2.png").toString());
		showTheDialogActionProps.put(SMALL_ICON_URL,getClass().getResource("pc2_small.png").toString());
		showTheDialogActionProps.put(IN_TOOL_BAR,"false");
		showTheDialogActionProps.put(IN_MENU_BAR,"true");
		showTheDialogActionProps.put(TOOLTIP,"Networks From PC2");
		
		ShowTheDialogAction showTheDialogAction = new ShowTheDialogAction(
				showTheDialogActionProps, 
				cySwingApplication.getJFrame(), 
				app.getQueryBuilderGUI(),
				cyApplicationManager, cyNetworkViewManager
				);
		// register the service
		registerService(bc, showTheDialogAction, CyAction.class, new Properties());	
		
		// Create "About..." menu item and action
		Map<String,String> showAboutDialogActionProps = new HashMap<String, String>();
		showAboutDialogActionProps.put(ID,"showCyPath2AboutDialogAction");
		showAboutDialogActionProps.put(TITLE,"About...");		
		showAboutDialogActionProps.put(PREFERRED_MENU, APPS_MENU + ".CyPath2");
		showAboutDialogActionProps.put(MENU_GRAVITY,"1.0");
		showAboutDialogActionProps.put(SMALL_ICON_URL,getClass().getResource("pc2_small.png").toString());
		showAboutDialogActionProps.put(IN_TOOL_BAR,"false");
		showAboutDialogActionProps.put(IN_MENU_BAR,"true");
 
		ShowAboutDialogAction showAboutDialogAction = new ShowAboutDialogAction(
				showAboutDialogActionProps, cySwingApplication.getJFrame(), 
				app.getDisplayName(), app.getDescription(),
				cyApplicationManager, cyNetworkViewManager, openBrowser
				);	
		// register the service
		registerService(bc, showAboutDialogAction, CyAction.class, new Properties());
		
		// create a context menu (using a task factory, for this uses tunables and can be used by Cy3 scripts, headless too)
		final NodeViewTaskFactory expandNodeContextMenuFactory = new ExpandNodeContextMenuFactory(visualMappingManager, cyLayoutAlgorithmManager);
		final Properties nodeProp = new Properties();
		nodeProp.setProperty("preferredTaskManager", "menu");
		nodeProp.setProperty(PREFERRED_MENU, NODE_APPS_MENU);
		nodeProp.setProperty(MENU_GRAVITY, "13.0");
		nodeProp.setProperty(TITLE, "CyPath2: Extend Network...");
		registerService(bc, expandNodeContextMenuFactory, NodeViewTaskFactory.class, nodeProp);
		
	}
}

