package org.pathwaycommons.cypath2.internal;

import org.biopax.paxtools.trove.TProvider;
import org.biopax.paxtools.util.BPCollections;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.task.hide.UnHideAllEdgesTaskFactory;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.work.undo.UndoSupport;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.CyApplicationManager;
//import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
//import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.io.read.CyNetworkReaderManager;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.cytoscape.service.util.AbstractCyActivator;

import cpath.client.CPathClient;
import cpath.client.util.CPathException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.cytoscape.work.ServiceProperties.*;


public final class CyActivator extends AbstractCyActivator {
	private static final Logger LOGGER = LoggerFactory.getLogger(CyActivator.class);
	
	public CyActivator() {
		super();
		LOGGER.info("Creating CyPathwayCommons bundle activator...");
	}


	public void start(BundleContext bc) {
		LOGGER.info("Starting CyPathwayCommons app...");
		
		//set a system property for Paxtools to use memory-efficient collections
		try {
			Class.forName("org.biopax.paxtools.trove.TProvider");
			System.setProperty("paxtools.CollectionProvider", "org.biopax.paxtools.trove.TProvider");
			BPCollections.I.setProvider(new TProvider());
		} catch (ClassNotFoundException e1) {
			LOGGER.error("org.biopax.paxtools.trove.TProvider is not found on the app's classpath; " +
				"Paxtools will use default biopax collections (HashSet, HashMap based).");
		}		
		
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
		CyProperty<Properties> cyProperties = getService(bc, CyProperty.class, "(cyPropertyName=cytoscape3.props)");
		CyRootNetworkManager cyRootNetworkManager = getService(bc, CyRootNetworkManager.class);
		UnHideAllEdgesTaskFactory unHideAllEdgesTaskFactory = getService(bc, UnHideAllEdgesTaskFactory.class);
		
		// keep all the service references in one place -
		final CyServices cyServices = new CyServices(cySwingApplication, taskManager, openBrowser, 
				cyNetworkManager, cyApplicationManager, cyNetworkViewManager, cyNetworkReaderManager, 
				cyNetworkNaming, cyNetworkFactory, cyLayoutAlgorithmManager, undoSupport, visualMappingManager, 
				cyProperties, cyRootNetworkManager, unHideAllEdgesTaskFactory);
			    
	    // Create/init a cpath2 client instance
		String cPath2Url = cyProperties.getProperties().getProperty(CyPC.PROP_CPATH2_SERVER_URL);
		if(cPath2Url != null && !cPath2Url.isEmpty())
			CyPC.client = CPathClient.newInstance(cPath2Url); 
		else {
			//the default cpath2 URL unless -DcPath2Url=<someURL> jvm option used
			CyPC.client = CPathClient.newInstance(); 
		}
		cyProperties.getProperties().setProperty(CyPC.PROP_CPATH2_SERVER_URL, CyPC.client.getActualEndPointURL());	  	
		
		// set the other static field - cy3 services
		CyPC.cyServices = cyServices;
		
	    // new user-set global options (e.g., filters, query type)
		CyPC.options = new Options();
		
	    // get the app description from the resource file
	    Properties props = new Properties();
	    try {
	    	props.load(getClass().getResourceAsStream("/cypath2.properties"));
	    } catch (IOException e) { throw new RuntimeException(e);}
	    final String description = props.getProperty("cypath2.description");
	    		
	    // new app instance
		CyPC app = new CyPC("Pathway Commons 2 (BioPAX L3)", description);		
		// initialize (build the UI)
		try {
			app.init();
		} catch (CPathException e) {
			throw new RuntimeException(e);
		}
		
		// Register OSGi services: WebServiceClient, WebServiceGUIClient, SearchWebServiceClient,..
		registerAllServices(bc, app, new Properties());
		
		// Create a new menu/toolbar item (CyAction) that opens the CyPathwayCommons GUI 
		Map<String,String> showTheDialogActionProps = new HashMap<String, String>();
		showTheDialogActionProps.put(ID,"showCyPathwayCommonsDialogAction");
		showTheDialogActionProps.put(TITLE,"Search/Import Network...");		
		showTheDialogActionProps.put(PREFERRED_MENU, APPS_MENU + ".CyPathwayCommons");
		showTheDialogActionProps.put(MENU_GRAVITY,"2.0");
//		showTheDialogActionProps.put(TOOL_BAR_GRAVITY,"3.17");
//		showTheDialogActionProps.put(LARGE_ICON_URL,getClass().getResource("pc2.png").toString());
		showTheDialogActionProps.put(SMALL_ICON_URL,getClass().getResource("pc2_small.png").toString());
		showTheDialogActionProps.put(IN_TOOL_BAR,"false");
		showTheDialogActionProps.put(IN_MENU_BAR,"true");
		showTheDialogActionProps.put(TOOLTIP,"Networks From PC2");	
		ShowTheDialogAction showTheDialogAction = new ShowTheDialogAction(
				showTheDialogActionProps, cyServices, app.getQueryBuilderGUI());
		// register the service
		registerService(bc, showTheDialogAction, CyAction.class, new Properties());	
		
		// Create "About..." menu item and action
		Map<String,String> showAboutDialogActionProps = new HashMap<String, String>();
		showAboutDialogActionProps.put(ID,"showCyPathwayCommonsAboutDialogAction");
		showAboutDialogActionProps.put(TITLE,"About...");		
		showAboutDialogActionProps.put(PREFERRED_MENU, APPS_MENU + ".CyPathwayCommons");
		showAboutDialogActionProps.put(MENU_GRAVITY,"1.0");
		showAboutDialogActionProps.put(SMALL_ICON_URL,getClass().getResource("pc2_small.png").toString());
		showAboutDialogActionProps.put(IN_TOOL_BAR,"false");
		showAboutDialogActionProps.put(IN_MENU_BAR,"true");
		ShowAboutDialogAction showAboutDialogAction = new ShowAboutDialogAction(
				showAboutDialogActionProps, cyServices,
				"CyPathwayCommons", description
			);
		// register the service
		registerService(bc, showAboutDialogAction, CyAction.class, new Properties());
		
		// create a context menu (using a task factory, for this uses tunables and can be used by Cy3 scripts, headless too)
		final NodeViewTaskFactory expandNodeContextMenuFactory = new ExpandNetworkContextMenuFactory(cyServices);
		final Properties nodeProp = new Properties();
		nodeProp.setProperty("preferredTaskManager", "menu");
		nodeProp.setProperty(PREFERRED_MENU, NODE_APPS_MENU);
		nodeProp.setProperty(MENU_GRAVITY, "13.0");
		nodeProp.setProperty(TITLE, "CyPathwayCommons: Extend Network...");
		registerService(bc, expandNodeContextMenuFactory, NodeViewTaskFactory.class, nodeProp);	
		
			
		BioPaxDetailsPanel bioPaxDetailsPanel = new BioPaxDetailsPanel(openBrowser);
		BioPaxCytoPanelComponent cytoPanelComponent = new BioPaxCytoPanelComponent(bioPaxDetailsPanel, cyServices);
		registerService(bc, cytoPanelComponent, CytoPanelComponent.class, new Properties());
		
		// a biopax node selection listener and eastern cytopanel (results panel) feature
		BioPaxTracker bioPaxTracker = new BioPaxTracker(bioPaxDetailsPanel, cytoPanelComponent, cyServices);	
		registerAllServices(bc, bioPaxTracker, new Properties());
	}
}

