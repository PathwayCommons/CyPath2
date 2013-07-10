package org.pathwaycommons.cypath2.internal;

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.io.read.CyNetworkReaderManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.undo.UndoSupport;

final class CyServices {
	
	final CySwingApplication cySwingApplication;
	final TaskManager taskManager;
	final OpenBrowser openBrowser;
	final CyNetworkManager networkManager;
	final CyApplicationManager applicationManager;
	final CyNetworkViewManager networkViewManager;
	final CyNetworkReaderManager networkViewReaderManager;
	final CyNetworkNaming naming;
	final CyNetworkFactory networkFactory;
	final CyLayoutAlgorithmManager layoutManager;
	final UndoSupport undoSupport;
	final BinarySifVisualStyleFactory binarySifVisualStyleUtil;
	final VisualMappingManager mappingManager;
	final CyProperty<Properties> cyProperty;
	public CyServices(CySwingApplication cySwingApplication,
			TaskManager taskManager, OpenBrowser openBrowser,
			CyNetworkManager networkManager,
			CyApplicationManager applicationManager,
			CyNetworkViewManager networkViewManager,
			CyNetworkReaderManager networkViewReaderManager,
			CyNetworkNaming naming, CyNetworkFactory networkFactory,
			CyLayoutAlgorithmManager layoutManager, UndoSupport undoSupport,
			BinarySifVisualStyleFactory binarySifVisualStyleUtil,
			VisualMappingManager mappingManager,
			CyProperty<Properties> cyProperty) 
	{
		this.cySwingApplication = cySwingApplication;
		this.taskManager = taskManager;
		this.openBrowser = openBrowser;
		this.networkManager = networkManager;
		this.applicationManager = applicationManager;
		this.networkViewManager = networkViewManager;
		this.networkViewReaderManager = networkViewReaderManager;
		this.naming = naming;
		this.networkFactory = networkFactory;
		this.layoutManager = layoutManager;
		this.undoSupport = undoSupport;
		this.binarySifVisualStyleUtil = binarySifVisualStyleUtil;
		this.mappingManager = mappingManager;
		this.cyProperty = cyProperty;
	}
	
}
