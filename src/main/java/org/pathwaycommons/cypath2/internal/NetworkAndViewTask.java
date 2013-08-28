package org.pathwaycommons.cypath2.internal;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.EntityReference;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.client.util.CPathException;
import cpath.query.CPathQuery;
import cpath.service.OutputFormat;

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
				data = cPathQuery.stringResult(CyPath2.downloadMode);
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
    			
			// Save the BioPAX or SIF data to a temporary local file
			String tmpDir = System.getProperty("java.io.tmpdir");			
			// Branch based on download mode setting.
			File tmpFile;
			if (CyPath2.downloadMode == OutputFormat.BIOPAX) {
				tmpFile = File.createTempFile("temp", ".xml", new File(tmpDir));
			} else {
				tmpFile = File.createTempFile("temp", ".sif", new File(tmpDir));
			}
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
			reader.run(taskMonitor);
    			
			taskMonitor.setProgress(0.6);
			if (cancelled) return;
			taskMonitor.setStatusMessage("Creating Network View...");

			final CyNetwork cyNetwork = reader.getNetworks()[0];
            final CyNetworkView view = reader.buildCyNetworkView(cyNetwork);

            cyServices.networkManager.addNetwork(cyNetwork);
            cyServices.networkViewManager.addNetworkView(view);

			taskMonitor.setProgress(0.7);
			if (cancelled) return;
    			
			//post-process SIF network (retrieve biopax attributes from the server)
			if (CyPath2.downloadMode == OutputFormat.BINARY_SIF) {
				//fix the network name
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						String networkTitleWithUnderscores = cyServices.naming.getSuggestedNetworkTitle(networkTitle);
						Attributes.set(cyNetwork, cyNetwork, CyNetwork.NAME, networkTitleWithUnderscores, String.class);
						Attributes.set(cyNetwork, cyNetwork, CyRootNetwork.SHARED_NAME, networkTitleWithUnderscores, String.class);
					}
				});
    				
				taskMonitor.setStatusMessage("Updating SIF network " +
					"attributes from corresonding BioPAX data...");				
    				
				// Set the Quick Find Default Index
				Attributes.set(cyNetwork, cyNetwork, "quickfind.default_index", CyNetwork.NAME, String.class);
				// Set attribute: BIOPAX_NETWORK = FALSE (but not empty/null) 
				// (hack: the biopax core plugin displays SIF visual legend and node's info properly if "BIOPAX_NETWORK" attr. is present)
				Attributes.set(cyNetwork, cyNetwork, "BIOPAX_NETWORK", Boolean.FALSE, Boolean.class);
	
				// we need the biopax sub-model to create node/edge attributes
				final Set<String> uris = new HashSet<String>();
				for (CyNode node : cyNetwork.getNodeList()) {
					//hack: we know that the built-in Cy3 SIF reader uses URIs 
					// from the Pathway Commons SIF data to fill the NAME column by default...
					String uri = cyNetwork.getRow(node).get(CyNetwork.NAME, String.class);
					if(uri != null && !uri.contains("/group/")) {
						uris.add(uri);
					} 
				}
				if (cancelled) return;
				
				//retrieve the model (using a STAX hack)
				final Model[] callback = new Model[1];
				ClassLoaderHack.runWithHack(new Runnable() {
					@Override
					public void run() {
						try {
							callback[0] = CyPath2.client.createGetQuery()
								.sources(uris)
									.result();
						} catch (Throwable e) {
							LOGGER.warn("Import failed: " + e);
						}
					}
				}, com.ctc.wstx.stax.WstxInputFactory.class);
				final Model bpModel = callback[0];
				
				// Set node/edge attributes from the Biopax Model
				for (CyNode node : cyNetwork.getNodeList()) {
					String uri = cyNetwork.getRow(node).get(CyNetwork.NAME, String.class);
					BioPAXElement e = bpModel.getByID(uri);// can be null (for generic/group nodes)
					if(e instanceof EntityReference || e instanceof Entity) 
					{
						//note: in fact, SIF formatted data contains only ERs, PEs (no sub-classes), and Complexes.
						BioPaxUtil.createAttributesFromProperties(e, node, cyNetwork);
					} else if (e != null){
						LOGGER.warn("SIF network has an unexpected node: " + uri 
								+ " of type " + e.getModelInterface());
						BioPaxUtil.createAttributesFromProperties(e, node, cyNetwork);
					} else { //e == null						
						if(uri.contains("/group/")) {
							Attributes.set(cyNetwork, node, BioPaxUtil.BIOPAX_ENTITY_TYPE, "(Generic/Group)", String.class);
							Attributes.set(cyNetwork, node, BioPaxUtil.BIOPAX_RDF_ID, uri, String.class);
							Attributes.set(cyNetwork, node, CyRootNetwork.SHARED_NAME, "(Group)", String.class);
							Attributes.set(cyNetwork, node, CyNetwork.NAME, "(Group)", String.class);
						} else {
							LOGGER.warn("URI, which is not a generated " +
									"generic/group's one, is not found on the server: " + uri);
						}

					}
				}
			} 
    			
			taskMonitor.setProgress(0.8);
			if (cancelled) return;
			
			taskMonitor.setStatusMessage("Running the default layout algorithm...");

			view.updateView();
    			
		} finally {
			taskMonitor.setStatusMessage("Done");
			taskMonitor.setProgress(1.0);
		}
	}

}