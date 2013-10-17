package org.pathwaycommons.cypath2.internal;

import java.io.File;
import java.io.FileWriter;

import javax.swing.JOptionPane;

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
class NetworkAndViewTask<T> extends AbstractTask {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(NetworkAndViewTask.class);
	
	private final String networkTitle;
	private final CPathQuery<T> cPathQuery;
	private final CyServices cyServices;
	
	/**
	 * Constructor 
	 * (advanced, for all get, traverse, and graph queries).
	 * 
	 * @param cyServices
	 * @param 
	 * @param networkTitle
	 */
	public NetworkAndViewTask(CyServices cyServices, CPathQuery<T> cPathQuery, String networkTitle) 
	{
		this.cyServices = cyServices;
		this.networkTitle = networkTitle;
		this.cPathQuery = cPathQuery;
	}

	public void run(TaskMonitor taskMonitor) throws Exception {
		String title = "CyPath2: Import a Network";
		taskMonitor.setTitle(title);
		try {
			taskMonitor.setProgress(0);
			taskMonitor.setStatusMessage("Retrieving a network " + 
					networkTitle + " from Pathway Commons 2...");
	    	
	    	String data = null;
			try {
				data = cPathQuery.stringResult(null); //default format is BioPAX
			} catch (CPathException e) {
				LOGGER.warn("cPath2 query failed", e);
			}
	    	
	    	if(data == null || data.isEmpty()) {
	    		JOptionPane.showMessageDialog(cyServices.cySwingApplication.getJFrame(), "No data returned from the server.");
	    		taskMonitor.setStatusMessage("No data returned from the server.");
	    		taskMonitor.setProgress(1.0);
	    		cancel();
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
			
			taskMonitor.setStatusMessage("Processing the BioPAX data: " +
					"looking for the BioPAX reader service...");    						
			// Import data via Cy3 I/O API
			final String inputName = cyServices.naming.getSuggestedNetworkTitle(networkTitle);
			final CyNetworkReader reader =  cyServices.networkViewReaderManager.getReader(tmpFile.toURI(), inputName);			
			//the first task (BioPAX reader) cretas the network; the second one registers it and adds the view:
			insertTasksAfterCurrentTask(reader, new AbstractTask() {			
				@Override
				public void run(TaskMonitor taskMonitor) throws Exception {
				  final CyNetwork cyNetwork = reader.getNetworks()[0];
		          cyServices.networkManager.addNetwork(cyNetwork);			
		          final CyNetworkView view = reader.buildCyNetworkView(cyNetwork);
		          cyServices.networkViewManager.addNetworkView(view);
				}
			});	

		} finally {
			taskMonitor.setStatusMessage("Done");
			taskMonitor.setProgress(1.0);
		}
	}
	
}