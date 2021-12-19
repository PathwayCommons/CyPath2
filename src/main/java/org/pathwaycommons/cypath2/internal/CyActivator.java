package org.pathwaycommons.cypath2.internal;

import cpath.client.CPathClient;
import org.biopax.paxtools.trove.TProvider;
import org.biopax.paxtools.util.BPCollections;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.search.NetworkSearchTaskFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.task.hide.UnHideAllEdgesTaskFactory;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.work.undo.UndoSupport;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.cytoscape.work.ServiceProperties.*;


public final class CyActivator extends AbstractCyActivator {
  private static final Logger LOGGER = LoggerFactory.getLogger(CyActivator.class);

  public CyActivator() {
    super();
    LOGGER.info("Creating PathwayCommons bundle activator...");
  }

  public void start(BundleContext bc) {
    LOGGER.info("Starting PathwayCommons app...");

    //set a system property for Paxtools to use memory-efficient collections
    try {
      Class.forName("org.biopax.paxtools.trove.TProvider");
      System.setProperty("paxtools.CollectionProvider", "org.biopax.paxtools.trove.TProvider");
      BPCollections.I.setProvider(new TProvider());
      LOGGER.info("Set: paxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider");
    } catch (ClassNotFoundException e1) {
      LOGGER.error("org.biopax.paxtools.trove.TProvider is not found on the classpath; so " +
        "Paxtools will use default biopax collections (HashSet, HashMap).");
    } catch (Throwable t) {
      LOGGER.error("static{} initializer failed; " + t);
    }

    CySwingApplication cySwingApplication = getService(bc, CySwingApplication.class);

    DialogTaskManager taskManager = getService(bc, DialogTaskManager.class);

    OpenBrowser openBrowser = getService(bc, OpenBrowser.class);

    CyNetworkManager cyNetworkManager = getService(bc, CyNetworkManager.class);

    CyApplicationManager cyApplicationManager = getService(bc, CyApplicationManager.class);

    CyNetworkViewManager cyNetworkViewManager = getService(bc, CyNetworkViewManager.class);

    CyNetworkNaming cyNetworkNaming = getService(bc, CyNetworkNaming.class);

    CyNetworkFactory cyNetworkFactory = getService(bc, CyNetworkFactory.class);

    CyLayoutAlgorithmManager cyLayoutAlgorithmManager = getService(bc, CyLayoutAlgorithmManager.class);

    UndoSupport undoSupport = getService(bc, UndoSupport.class);

    VisualMappingManager visualMappingManager = getService(bc, VisualMappingManager.class);

    VisualStyleFactory visualStyleFactory = getService(bc, VisualStyleFactory.class);

    VisualMappingFunctionFactory discreteMappingFunctionFactory =
      getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=discrete)");

    VisualMappingFunctionFactory passthroughMappingFunctionFactory =
      getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");

    CyProperty<Properties> cyProperties = getService(bc, CyProperty.class, "(cyPropertyName=cytoscape3.props)");

    CyRootNetworkManager cyRootNetworkManager = getService(bc, CyRootNetworkManager.class);

    UnHideAllEdgesTaskFactory unHideAllEdgesTaskFactory = getService(bc, UnHideAllEdgesTaskFactory.class);

    CyNetworkViewFactory cyNetworkViewFactory = getService(bc, CyNetworkViewFactory.class);

    StreamUtil streamUtil = getService(bc, StreamUtil.class);

    // keep all the service references in one place -
    App.cyServices = new CyServices(
      cySwingApplication,
      taskManager,
      openBrowser,
      cyNetworkManager,
      cyApplicationManager,
      cyNetworkViewManager,
      cyNetworkNaming,
      cyNetworkFactory,
      cyLayoutAlgorithmManager,
      undoSupport,
      visualMappingManager,
      cyProperties,
      cyRootNetworkManager,
      unHideAllEdgesTaskFactory,
      cyNetworkViewFactory,
      visualStyleFactory,
      discreteMappingFunctionFactory,
      passthroughMappingFunctionFactory,
      streamUtil);

    // Create/init a cpath2 client instance
    String cPath2Url = cyProperties.getProperties().getProperty(App.PROP_CPATH2_SERVER_URL);
    if (cPath2Url != null && !cPath2Url.isEmpty())
      App.client = CPathClient.newInstance(cPath2Url);
    else {
      //the default cpath2 URL unless -DcPath2Url=<someURL> jvm option used
      App.client = CPathClient.newInstance();
    }
    cyProperties.getProperties().setProperty(App.PROP_CPATH2_SERVER_URL, App.client.getEndPointURL());

    // get the app description from the resource file
    final Properties props = new Properties();
    try {
      props.load(getClass().getResourceAsStream("/cypath2.properties"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    final String description = props.getProperty("cypath2.description");

    // Create and initialize (build the GUI) new App instance
    App app = new App("Pathway Commons", description);
    app.init();

    // Create a new menu/toolbar item (CyAction) that opens the PathwayCommons GUI
    Map<String, String> showTheDialogActionProps = new HashMap<String, String>();
    showTheDialogActionProps.put(ID, "showCyPathwayCommonsDialogAction");
    showTheDialogActionProps.put(TITLE, "Search...");
    showTheDialogActionProps.put(PREFERRED_MENU, APPS_MENU + ".PathwayCommons");
    showTheDialogActionProps.put(MENU_GRAVITY, "2.0");
    showTheDialogActionProps.put(SMALL_ICON_URL, getClass().getResource("pc_logo.png").toString());
    showTheDialogActionProps.put(IN_TOOL_BAR, "false");
    showTheDialogActionProps.put(IN_MENU_BAR, "true");
    showTheDialogActionProps.put(TOOLTIP, "Networks From PC2");
    ShowTheDialogAction showTheDialogAction =
      new ShowTheDialogAction(showTheDialogActionProps, app.getQueryBuilderGUI());
    // register the service
    registerService(bc, showTheDialogAction, CyAction.class, new Properties());

    // Create "About..." menu item and action
    Map<String, String> showAboutDialogActionProps = new HashMap<String, String>();
    showAboutDialogActionProps.put(ID, "showCyPathwayCommonsAboutDialogAction");
    showAboutDialogActionProps.put(TITLE, "About...");
    showAboutDialogActionProps.put(PREFERRED_MENU, APPS_MENU + ".PathwayCommons");
    showAboutDialogActionProps.put(MENU_GRAVITY, "1.0");
    showAboutDialogActionProps.put(SMALL_ICON_URL, getClass().getResource("pc_logo.png").toString());
    showAboutDialogActionProps.put(IN_TOOL_BAR, "false");
    showAboutDialogActionProps.put(IN_MENU_BAR, "true");
    ShowAboutDialogAction showAboutDialogAction =
      new ShowAboutDialogAction(showAboutDialogActionProps, "Pathway Commons App", description);
    // register the service
    registerService(bc, showAboutDialogAction, CyAction.class, new Properties());

    // create a context menu (using a task factory, for this uses tunables and can be used by Cy3 scripts, headless too)
    final NodeViewTaskFactory expandNodeContextMenuFactory = new ExpandNetworkContextMenuFactory();
    final Properties nodeProp = new Properties();
    nodeProp.setProperty("preferredTaskManager", "menu");
    nodeProp.setProperty(PREFERRED_MENU, NODE_APPS_MENU);
    nodeProp.setProperty(MENU_GRAVITY, "13.0");
    nodeProp.setProperty(TITLE, "PathwayCommons: Extend Network...");
    registerService(bc, expandNodeContextMenuFactory, NodeViewTaskFactory.class, nodeProp);

    // Node selection listener (only for networks imported from BioPAX) and eastern cytopanel (results panel).
    final EastCytoPanelComponent cytoPanelComponent = new EastCytoPanelComponent();
    registerAllServices(bc, cytoPanelComponent);

    // Register: WebServiceClient, WebServiceGUIClient, SearchWebServiceClient,..
    registerAllServices(bc, app);

    // Register the NetworkSearchTaskFactory
    final NetworkSearchTaskFactory networkSearchTaskFactory =
      new TheNetworkSearchTaskFactory(showTheDialogAction, app);
    registerAllServices(bc, networkSearchTaskFactory);
  }

}

