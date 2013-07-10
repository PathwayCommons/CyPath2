package org.pathwaycommons.cypath2.internal;

import java.io.File;
import java.io.FileWriter;
import java.util.Collections;
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
import org.cytoscape.model.CyRow;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.pathwaycommons.cypath2.internal.BioPaxUtil.StaxHack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.client.CPath2Client;
import cpath.service.Cmd;
import cpath.service.GraphType;
import cpath.service.OutputFormat;

/**
 * A Task that gets data from the cPath2 server and 
 * creates a new Cytoscape network and view.
 * 
 * @author rodche
 */
class NetworkAndViewTask extends AbstractTask {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(NetworkAndViewTask.class);
	
	private final String networkTitle;
	private final GraphType graphType;
	private final Set<String> sources;
	private final Set<String> targets;
	private final Cmd command;
	private final CPath2Client client;
	private final CyServices cyServices;

	/**
	 * Constructor 
	 * (for a simple get-by-URI query).
	 * 
	 * @param cyServices
	 * @param client 
	 * @param uri of a pathway or interaction
	 * @param networkTitle optional name for the new network
	 */
	public NetworkAndViewTask(CyServices cyServices, CPath2Client client, String uri, String networkTitle) {
		this.cyServices = cyServices;
		this.networkTitle = networkTitle;
		this.graphType = null; //  if null, will use the cpath2 '/get' (by URIs) command
		this.sources = Collections.singleton(uri);
		this.targets = null;
		this.command = Cmd.GET;
		this.client = client;
	}
	
	/**
	 * Constructor 
	 * (advanced, for all get, traverse, and graph queries).
	 * 
	 * @param cyServices
	 * @param client
	 * @param command
	 * @param graphType
	 * @param srcs
	 * @param tgts
	 * @param networkTitle
	 */
	public NetworkAndViewTask(CyServices cyServices, CPath2Client client, Cmd command, GraphType graphType, 
			Set<String> srcs, Set<String> tgts, String networkTitle) 
	{
		this.cyServices = cyServices;
		this.networkTitle = networkTitle;
		this.sources = srcs;
		this.targets = tgts;
		this.graphType = graphType; 
		this.command = command;
		this.client = client;
	}

	public void run(TaskMonitor taskMonitor) throws Exception {
		String title = "Retrieving a network " + networkTitle 
			+ " from Pathway Commons 2...";
		taskMonitor.setTitle(title);
		try {
			taskMonitor.setProgress(0);
			taskMonitor.setStatusMessage("Retrieving data...");
	    	
	    	// do query, get data as string
	    	final String data = client.doPost(command, String.class, 
	    	    client.buildRequest(command, graphType, sources, targets, CyPath2.downloadMode));
	    	
	    	if(data == null || data.isEmpty()) {
	    		JOptionPane.showMessageDialog(cyServices.cySwingApplication.getJFrame(), "No data returned from the server.");
	    		return;
	    	}
	    		
	    	
	    	// done.
			taskMonitor.setProgress(0.4);    			
			if (cancelled) 
				return;
    			
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
				// Specify that this is a BINARY_NETWORK
				Attributes.set(cyNetwork, cyNetwork, "BIOPAX_NETWORK", Boolean.TRUE, Boolean.class);
	
				// we need the biopax sub-model to create node/edge attributes
				final Set<String> uris = new HashSet<String>();
				// Set node/edge attributes from the Biopax Model
				for (CyNode node : cyNetwork.getNodeList()) {
					String uri = cyNetwork.getRow(node).get(CyNetwork.NAME, String.class);
					if(!uri.contains("/group/")) {
						uris.add(uri);
					} else {
						Attributes.set(cyNetwork, node, BioPaxUtil.BIOPAX_ENTITY_TYPE, "(Generic/Group)", String.class);
						Attributes.set(cyNetwork, node, BioPaxUtil.BIOPAX_RDF_ID, uri, String.class);
						Attributes.set(cyNetwork, node, CyRootNetwork.SHARED_NAME, "(Group)", String.class);
						Attributes.set(cyNetwork, node, CyNetwork.NAME, "(Group)", String.class);
					}
				}
				if (cancelled) return;
				
				//retrieve the model (using a STAX hack)
				final Model[] callback = new Model[1];
				StaxHack.runWithHack(new Runnable() {
					@Override
					public void run() {
						try {
							callback[0] = client.get(uris);
						} catch (Throwable e) {
							LOGGER.warn("Import failed: " + e);
						}
					}
				});
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
					} else { //e == null
						assert uri.contains("/group/") : "URI, which is not a generated " +
							"generic/group's one, is not found on the server: " + uri;
						
						if(!uri.contains("/group/")) {
							LOGGER.warn("URI, which is not a generated " +
								"generic/group's one, is not found on the server: " + uri);
						}
					}
				}

				if (cancelled) return;

				VisualStyle visualStyle = cyServices.binarySifVisualStyleUtil.getVisualStyle();
				cyServices.mappingManager.setVisualStyle(visualStyle, view);
				visualStyle.apply(view);
				view.updateView();
			} 

			taskMonitor.setProgress(0.8);
			if (cancelled) return;
			taskMonitor.setStatusMessage("Generating html links...");
    			
			// Add Links Back to cPath2 Instance
			CyRow row = cyNetwork.getRow(cyNetwork);
			String cPathServerDetailsUrl = row.get(CyPath2.CPATH_SERVER_URL_ATTR, String.class);
			if (cPathServerDetailsUrl == null) {
				Attributes.set(cyNetwork, cyNetwork, CyPath2.CPATH_SERVER_URL_ATTR, client.getEndPointURL(), String.class);
			}
    			
			taskMonitor.setProgress(0.9);
			if (cancelled) return;
			taskMonitor.setStatusMessage("Running the default layout algorithm...");

			view.updateView();
    			
		} finally {
			taskMonitor.setStatusMessage("Done");
			taskMonitor.setProgress(1.0);
		}
	}

}