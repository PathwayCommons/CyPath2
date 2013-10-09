package org.pathwaycommons.cypath2.internal;

import java.io.File;
import java.io.FileWriter;

import javax.swing.JOptionPane;

import org.cytoscape.io.read.CyNetworkReader;
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
		String title = "Retrieving a network " + networkTitle 
			+ " from Pathway Commons 2...";
		taskMonitor.setTitle(title);
		try {
			taskMonitor.setProgress(0);
			taskMonitor.setStatusMessage("Retrieving data...");
	    	
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
			taskMonitor.setStatusMessage("Creating Cytoscape Network from BioPAX Data...");
    			
			// Import data via Cy3 I/O API	
			String inputName = cyServices.naming.getSuggestedNetworkTitle(networkTitle);
			CyNetworkReader reader =  cyServices.networkViewReaderManager.getReader(tmpFile.toURI(), inputName);
			//TODO WRAP in taskManager.execute(iterator) for biopax app's TUNABLES there to show up (allows to choose SIF mode)
			reader.run(taskMonitor);
    			
			taskMonitor.setProgress(0.6);
			if (cancelled) return;
			taskMonitor.setStatusMessage("Creating Network View...");

			//TODO can remove commented out 4 lines below (used to add network and view using corresp. Cy managers)?
//			final CyNetwork cyNetwork = reader.getNetworks()[0];
//          final CyNetworkView view = reader.buildCyNetworkView(cyNetwork);
//          cyServices.networkManager.addNetwork(cyNetwork);
//          cyServices.networkViewManager.addNetworkView(view);
    			
			taskMonitor.setProgress(0.9);
			if (cancelled) return;

    			
		} finally {
			taskMonitor.setStatusMessage("Done");
			taskMonitor.setProgress(1.0);
		}
	}
	
}