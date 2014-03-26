package org.pathwaycommons.cypath2.internal;

import java.io.File;
import java.io.FileWriter;

import org.biopax.paxtools.model.Model;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.client.util.CPathException;
import cpath.query.CPathQuery;

/**
 * A Task that gets data from the cPath2 server and 
 * creates a new Cytoscape network and view.
 * 
 * @author rodche
 */
class NetworkAndViewTask extends AbstractTask {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(NetworkAndViewTask.class);
	
	private final CPathQuery<Model> cPathQuery;
	private final CyServices cyServices;
	private final String networkName;
	private final String biopaxData;
	
	/**
	 * Constructor 
	 * (advanced, for all get and graph queries).
	 * 
	 * @param cyServices
	 * @param networkName
	 * @param 
	 */
	public NetworkAndViewTask(CyServices cyServices, CPathQuery<Model> cPathQuery, String networkName) 
	{
		this.cyServices = cyServices;
		this.cPathQuery = cPathQuery;
		this.networkName = networkName;
		this.biopaxData = null;
	}

	/**
	 * Constructor 
	 * when you already have biopax data.
	 * 
	 * @param cyServices
	 * @param networkName
	 * @param 
	 */
	public NetworkAndViewTask(CyServices cyServices, String biopaxData, String networkName) 
	{
		this.cyServices = cyServices;
		this.biopaxData = biopaxData;
		this.networkName = networkName;
		this.cPathQuery = null;
	}
	
	
	public void run(TaskMonitor taskMonitor) throws Exception {
		
		taskMonitor.setTitle("CyPathwayCommons Query");
		
		if(cancelled) return;
		
		try {
			taskMonitor.setProgress(0);
			taskMonitor.setStatusMessage("Getting the network data from server...");
	    	
	    	String data = biopaxData; //may be null
	    	
	    	if(data == null && cPathQuery != null) {   	
	    		try {
	    			data = cPathQuery.stringResult(null); //default format is BioPAX
	    		} catch (CPathException e) {
	    			LOGGER.warn("cPath2 query failed", e);
	    		}
	    	}
	    	
	    	if(data == null || data.isEmpty()) {
//	    		JOptionPane.showMessageDialog(cyServices.cySwingApplication.getJFrame(), "No data returned from the server.");
	    		taskMonitor.setStatusMessage("No data returned from the server.");
//	    		taskMonitor.setProgress(1.0);
	    		cancel();
	    		return;
//	    		throw new RuntimeException("No data returned from the server.");
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
			
			taskMonitor.setStatusMessage("Processing the BioPAX data: " +
					"looking for the BioPAX reader service...");					
			// Import data via Cy3 I/O API
			final CyNetworkReader reader =  cyServices.networkViewReaderManager.getReader(tmpFile.toURI(), null);			
			//the first task (the BioPAX reader) creates a network; the second one registers it and adds the view:
			insertTasksAfterCurrentTask(reader, new AbstractTask() {			
				@Override
				public void run(TaskMonitor taskMonitor) throws Exception {
					taskMonitor.setTitle("CyPathwayCommons after BioPAX read");
					final CyNetwork cyNetwork = reader.getNetworks()[0];
					
					//check / set the network name attr. (otherwise, it won't be shown in the panel - a bug?..)
					String name = cyNetwork.getRow(cyNetwork).get(CyNetwork.NAME, String.class);
					if (name == null || name.trim().length() == 0) {
						name = networkName;
						if (name == null)
							name = "Network by CyPathwayCommons (name is missing)";
						Attributes.set(cyNetwork, cyNetwork, CyNetwork.NAME, name, String.class);
					}				
					cyServices.networkManager.addNetwork(cyNetwork);
					//if a new root network was created, register that one as well
//					CyRootNetwork cyRootNetwork = cyServices.rootNetworkManager.getRootNetwork(cyNetwork);
//					if(cyRootNetwork != null) {
//						cyServices.networkManager.addNetwork(cyRootNetwork);
//						taskMonitor.setStatusMessage("Registered the root network");
//					}
					taskMonitor.setStatusMessage("Registered the network");
					// create and register the view
					final CyNetworkView view = reader.buildCyNetworkView(cyNetwork);
					cyServices.networkViewManager.addNetworkView(view);
					taskMonitor.setStatusMessage("Created and registered the view");
				}
			});	

		} finally {
			taskMonitor.setStatusMessage("Done");
			taskMonitor.setProgress(1.0);
		}
	}
	
}