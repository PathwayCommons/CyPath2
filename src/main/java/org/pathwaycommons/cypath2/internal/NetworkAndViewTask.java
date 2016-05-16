package org.pathwaycommons.cypath2.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;

import org.biopax.paxtools.model.Model;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.client.util.CPathException;
import cpath.query.CPathQuery;

import javax.swing.*;

/**
 * A Task that gets data from the cPath2 server and 
 * creates a new Cytoscape network and view.
 * 
 * @author rodche
 */
class NetworkAndViewTask extends AbstractTask {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(NetworkAndViewTask.class);
	
	private final CPathQuery<Model> cPathQuery;
	private final String networkName;

	/**
	 * Constructor 
	 * (advanced, for all get and graph queries).
	 *  @param cPathQuery query
	 * @param networkName network name
	 */
	public NetworkAndViewTask(CPathQuery<Model> cPathQuery, String networkName)
	{
		this.cPathQuery = cPathQuery;
		this.networkName = networkName;
	}

	public void run(TaskMonitor taskMonitor) throws Exception {
		
		taskMonitor.setTitle("Pathway Commons Query");
		
		if(cancelled) return;
		
		try {
			taskMonitor.setProgress(0);
			taskMonitor.setStatusMessage("Getting the network data from server...");
	    	
	    	String data = null;
	    	if(cPathQuery != null) {
	    		try {
	    			data = cPathQuery.stringResult(null); //default format is BioPAX
	    		} catch (CPathException e) {
	    			LOGGER.warn("cPath2 query failed", e);
	    		}
	    	}
	    	
	    	if(data == null || data.isEmpty()) {
	    		taskMonitor.setStatusMessage("No data returned from the server.");
	    		return;
	    	}
	    	
	    	// done.
			taskMonitor.setProgress(0.4);    			
			if (cancelled) {
				return;
			}
    						
			// Branch based on download mode setting.
			File tmpFile;
			//always BioPAX (no need in getting SIF from the server anymore due to new BioPAX plugin)!
			tmpFile = File.createTempFile("tmp_cypath2", ".owl");
			tmpFile.deleteOnExit();   							
			FileWriter writer = new FileWriter(tmpFile);
			writer.write(data);
			writer.close();	
    			
			taskMonitor.setProgress(0.5);
			if (cancelled) return;
			
			taskMonitor.setStatusMessage("Processing the BioPAX data...");
			final BioPaxReaderTask reader =  new BioPaxReaderTask(new FileInputStream(tmpFile), null);
			//the first task (the BioPAX reader) creates a network; the second one registers it and adds the view:
			insertTasksAfterCurrentTask(reader, new AbstractTask() {
				@Override
				public void run(TaskMonitor taskMonitor) throws Exception {
					taskMonitor.setTitle("PathwayCommons, after BioPAX read");
					final CyNetwork cyNetwork =  reader.getNetworks()[0];
					
					//check / set the network name attr. (otherwise, it won't be shown in the panel - a bug?..)
					String name = cyNetwork.getRow(cyNetwork).get(CyNetwork.NAME, String.class);
					if (name == null || name.trim().length() == 0) {
						name = networkName;
						if (name == null)
							name = "Network from PathwayCommons (name is missing)";
						Attributes.set(cyNetwork, cyNetwork, CyNetwork.NAME, name, String.class);
					}				
					App.cyServices.networkManager.addNetwork(cyNetwork);

					//if a new root network was created, register that one as well
//					CyRootNetwork cyRootNetwork = cyServices.rootNetworkManager.getRootNetwork(cyNetwork);
//					if(cyRootNetwork != null) {
//						cyServices.networkManager.addNetwork(cyRootNetwork);
//						taskMonitor.setStatusMessage("Registered the root network");
//					}
					taskMonitor.setStatusMessage("Registered the network");

					// create and register the view
					final CyNetworkView view =  reader.buildCyNetworkView(cyNetwork);
					applyStyleAndLayout(view);

					taskMonitor.setStatusMessage("Created and registered the view");
				}
			});
		} finally {
			taskMonitor.setStatusMessage("Done");
			taskMonitor.setProgress(1.0);
		}
	}


	//sets a custom style and layout for just created view
	private void applyStyleAndLayout(final CyNetworkView view) {
		// apply the PC style and layout to a BioPAX-origin view;
		final CyNetwork cyNetwork = view.getModel();

		VisualStyle style = null;
		String kind = cyNetwork.getRow(cyNetwork).get(BioPaxMapper.BIOPAX_NETWORK, String.class);
		if (BiopaxVisualStyleUtil.BIO_PAX_VISUAL_STYLE.equals(kind))
			style = App.visualStyleUtil.getBioPaxVisualStyle();
		else if (BiopaxVisualStyleUtil.BINARY_SIF_VISUAL_STYLE.equals(kind))
			style = App.visualStyleUtil.getBinarySifVisualStyle();

		//apply style and layout
		if(style != null) {
			final VisualStyle vs = style;
			//apply style and layout
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					// do layout
					CyLayoutAlgorithm layout = App.cyServices.layoutManager.getLayout("force-directed");
					if (layout == null) {
						layout = App.cyServices.layoutManager.getDefaultLayout();
						LOGGER.warn("'force-directed' layout not found; will use the default one.");
					}
					App.cyServices.taskManager.execute(layout.createTaskIterator(view,
							layout.getDefaultLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS,""));

					App.cyServices.mappingManager.setVisualStyle(vs, view);
					vs.apply(view);
					view.updateView();
				}
			});
		}
	}
}