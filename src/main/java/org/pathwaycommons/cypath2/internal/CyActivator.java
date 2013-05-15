package org.pathwaycommons.cypath2.internal;

import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.work.undo.UndoSupport;
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

import java.util.Properties;


public final class CyActivator extends AbstractCyActivator {
	private static final Logger LOGGER = LoggerFactory.getLogger(CyActivator.class);
	
	
	public CyActivator() {
		super();
	}


	public void start(BundleContext bc) {
		LOGGER.debug("starting...");
		
		CySwingApplication cySwingApplicationRef = getService(bc,CySwingApplication.class);
		DialogTaskManager taskManagerRef = getService(bc,DialogTaskManager.class);
		OpenBrowser openBrowserRef = getService(bc,OpenBrowser.class);
		CyNetworkManager cyNetworkManagerRef = getService(bc,CyNetworkManager.class);
		CyApplicationManager cyApplicationManagerRef = getService(bc,CyApplicationManager.class);
		CyNetworkViewManager cyNetworkViewManagerRef = getService(bc,CyNetworkViewManager.class);
		CyNetworkReaderManager cyNetworkViewReaderManagerRef = getService(bc,CyNetworkReaderManager.class);
		CyNetworkNaming cyNetworkNamingRef = getService(bc,CyNetworkNaming.class);
		CyNetworkFactory cyNetworkFactoryRef = getService(bc,CyNetworkFactory.class);
		CyLayoutAlgorithmManager cyLayoutsRef = getService(bc,CyLayoutAlgorithmManager.class);
		UndoSupport undoSupportRef = getService(bc,UndoSupport.class);
		VisualMappingManager visualMappingManagerRef = getService(bc,VisualMappingManager.class);
		VisualStyleFactory visualStyleFactoryRef = getService(bc,VisualStyleFactory.class);
		VisualMappingFunctionFactory discreteMappingFactoryRef = getService(bc,VisualMappingFunctionFactory.class,"(mapping.type=discrete)");
		VisualMappingFunctionFactory passthroughMappingFactoryRef = getService(bc,VisualMappingFunctionFactory.class,"(mapping.type=passthrough)");
		CyProperty cytoscapePropertiesServiceRef = getService(bc, CyProperty.class, "(cyPropertyName=cytoscape3.props)");
		
		BinarySifVisualStyleFactory binarySifVisualStyleUtil = new BinarySifVisualStyleFactory(
				visualStyleFactoryRef,
				visualMappingManagerRef,
				discreteMappingFactoryRef,
				passthroughMappingFactoryRef);
		
		// Create and register the OSGi service
		CyPath2 cPathSquaredWebServiceClient = new CyPath2(
				cySwingApplicationRef,
				taskManagerRef,
				openBrowserRef,
				cyNetworkManagerRef,
				cyApplicationManagerRef,
				cyNetworkViewManagerRef,
				cyNetworkViewReaderManagerRef,
				cyNetworkNamingRef,
				cyNetworkFactoryRef,
				cyLayoutsRef,
				undoSupportRef,
				binarySifVisualStyleUtil,
				visualMappingManagerRef,
				cytoscapePropertiesServiceRef);
		
		registerAllServices(bc, cPathSquaredWebServiceClient, new Properties());
	}
}

