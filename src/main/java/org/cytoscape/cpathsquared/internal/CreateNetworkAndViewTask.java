package org.cytoscape.cpathsquared.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;

import javax.swing.SwingUtilities;


import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Complex;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.Named;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.cytoscape.cpathsquared.internal.util.Attributes;
import org.cytoscape.cpathsquared.internal.util.BioPaxUtil;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.client.CPath2Client;
import cpath.service.OutputFormat;

/**
 * Controller for Executing a Get Record(s) by CPath ID(s) command.
 * 
 */
public class CreateNetworkAndViewTask extends AbstractTask {
	private String queryUrl;
	private String networkTitle;
	private boolean haltFlag = false;
	private OutputFormat format;
	private final static String CPATH_SERVER_NAME_ATTRIBUTE = "CPATH_SERVER_NAME";
	private final static String CPATH_SERVER_DETAILS_URL = "CPATH_SERVER_DETAILS_URL";
	private static final Logger logger = LoggerFactory.getLogger(CreateNetworkAndViewTask.class);

	/**.
	 * Constructor.
	 * @param queryUrl cPath2 web service query ('get' or 'graph') URL with all parameters.
	 * @param format Output Format.
	 * @param networkTitle Tentative Network Title.
	 */
	public CreateNetworkAndViewTask(String queryUrl, OutputFormat format, String networkTitle) {
		this.queryUrl = queryUrl;
		this.format = format;
		this.networkTitle = networkTitle;
	}

	/**
	 * Our implementation of Task.abort()
	 */
	public void cancel() {
		haltFlag = true;
	}

	/**
	 * Our implementation of Task.getTitle.
	 * 
	 * @return Task Title.
	 */
	public String getTitle() {
		return "Retrieving " + networkTitle + " from " + CpsFactory.SERVER_NAME + "...";
	}

	/**
	 * Our implementation of Task.run().
	 * 
	 * @throws Exception
	 */
	public void run(TaskMonitor taskMonitor) throws Exception {
		String title = "Retrieving " + networkTitle + " from " 
				+ CpsFactory.SERVER_NAME + "...";
		taskMonitor.setTitle(title);
		try {
			taskMonitor.setProgress(0);
			taskMonitor.setStatusMessage("Retrieving BioPAX data...");
			
			// Get Data: BioPAX and the other format data (not BioPAX if required)
			CPath2Client cli = CpsFactory.newClient();
	    	final String biopaxData = cli.executeQuery(queryUrl, OutputFormat.BIOPAX);
	    	String data = (format == OutputFormat.BIOPAX) 
	    		? biopaxData : cli.executeQuery(queryUrl, format);
	    	
			taskMonitor.setProgress(0.4);
			if (haltFlag) return;
			
			// Store BioPAX to Temp File
			String tmpDir = System.getProperty("java.io.tmpdir");			
			// Branch based on download mode setting.
			File tmpFile;
			if (format == OutputFormat.BIOPAX) {
				tmpFile = File.createTempFile("temp", ".xml", new File(tmpDir));
			} else {
				tmpFile = File.createTempFile("temp", ".sif", new File(tmpDir));
			}
			tmpFile.deleteOnExit();
							
			FileWriter writer = new FileWriter(tmpFile);
			writer.write(data);
			writer.close();	
			
			taskMonitor.setProgress(0.5);
			if (haltFlag) return;
			taskMonitor.setStatusMessage("Creating Cytoscape Network from BioPAX Data...");
			
			// Import data via Cy3 I/O API
			
			CyNetworkReader reader = CpsFactory
				.context().networkViewReaderManager
					.getReader(tmpFile.toURI(), tmpFile.getName());	
			reader.run(taskMonitor);
			
			taskMonitor.setProgress(0.6);
			if (haltFlag) return;
			taskMonitor.setStatusMessage("Creating Network View...");

			final CyNetwork cyNetwork = reader.getNetworks()[0];
            final CyNetworkView view = reader.buildCyNetworkView(cyNetwork);

            CpsFactory.context().networkManager.addNetwork(cyNetwork);
            CpsFactory.context().networkViewManager.addNetworkView(view);

            CpsFactory.context().networkManager.addNetwork(cyNetwork);
            CpsFactory.context().networkViewManager.addNetworkView(view);

			taskMonitor.setProgress(0.7);
			if (haltFlag) return;
			
			if (format == OutputFormat.BINARY_SIF) {
				//fix the network name
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						String networkTitleWithUnderscores = CpsFactory.context().naming.getSuggestedNetworkTitle(networkTitle);
						Attributes.set(cyNetwork, cyNetwork, CyNetwork.NAME, networkTitleWithUnderscores, String.class);
					}
				});
				
				taskMonitor.setStatusMessage("Updating SIF network " +
					"attributes from corresonding BioPAX data...");				
				
				// Set the Quick Find Default Index
				Attributes.set(cyNetwork, cyNetwork, "quickfind.default_index", CyNetwork.NAME, String.class);
				// Specify that this is a BINARY_NETWORK
				Attributes.set(cyNetwork, cyNetwork, "BIOPAX_NETWORK", Boolean.TRUE, Boolean.class);
	
				// we gonna need the full (original biopax) model to create attributes
				final Model bpModel = BioPaxUtil.convertFromOwl(new ByteArrayInputStream(biopaxData.getBytes("UTF-8")));
				
				// Set node/edge attributes from the Biopax Model
				for (CyNode node : cyNetwork.getNodeList()) {
					CyRow row = cyNetwork.getRow(node);
					String uri = row.get(CyNetwork.NAME, String.class);
					BioPAXElement e = bpModel.getByID(uri);// can be null (for generic groups nodes)
					if(e instanceof EntityReference 
						|| e instanceof Complex 
						|| (e != null && e.getModelInterface().equals(PhysicalEntity.class))) 
						BioPaxUtil.createAttributesFromProperties(e, node, cyNetwork);
				}

				if (haltFlag) return;

				VisualStyle visualStyle = CpsFactory.context().binarySifVisualStyleUtil.getVisualStyle();
				CpsFactory.context().mappingManager.setVisualStyle(visualStyle, view);
				visualStyle.apply(view);
				view.updateView();
			} 

			taskMonitor.setProgress(0.8);
			if (haltFlag) return;
			taskMonitor.setStatusMessage("Generating html links...");
			
			// Add Links Back to cPath Instance
			addLinksToCPathInstance(cyNetwork);
			taskMonitor.setProgress(0.9);
			if (haltFlag) return;
			taskMonitor.setStatusMessage("Running the default layout algorithm...");

			// apply default layout
			CyLayoutAlgorithmManager layoutManager = CpsFactory.context().layoutManager;
			CyLayoutAlgorithm layout = layoutManager.getDefaultLayout();
			Object context = layout.getDefaultLayoutContext();
			insertTasksAfterCurrentTask(layout.createTaskIterator(view, context, CyLayoutAlgorithm.ALL_NODE_VIEWS,""));			
			
		} finally {
			taskMonitor.setStatusMessage("Done");
			taskMonitor.setProgress(1.0);
		}
	}

	/**
	 * Add Node Links Back to cPath Instance.
	 * 
	 * @param cyNetwork
	 *            CyNetwork.
	 */
	private void addLinksToCPathInstance(CyNetwork cyNetwork) {
		String serverName = CpsFactory.SERVER_NAME;
		String serverURL = CpsFactory.SERVER_URL;
		CyRow row = cyNetwork.getRow(cyNetwork);
		String cPathServerDetailsUrl = row.get(CreateNetworkAndViewTask.CPATH_SERVER_DETAILS_URL, String.class);
		if (cPathServerDetailsUrl == null) {
			Attributes.set(cyNetwork, cyNetwork, CreateNetworkAndViewTask.CPATH_SERVER_NAME_ATTRIBUTE,
					serverName, String.class);
			String url = serverURL.replaceFirst("webservice.do", "record2.do?id=");
			Attributes.set(cyNetwork, cyNetwork, CreateNetworkAndViewTask.CPATH_SERVER_DETAILS_URL, url, String.class);
		}
	}

	
	private void fixDisplayName(Model model) {
		if (logger.isInfoEnabled())
			logger.info("Trying to auto-fix 'null' displayName...");
		// where it's null, set to the shortest name if possible
		for (Named e : model.getObjects(Named.class)) {
			if (e.getDisplayName() == null) {
				if (e.getStandardName() != null) {
					e.setDisplayName(e.getStandardName());
				} else if (!e.getName().isEmpty()) {
					String dsp = e.getName().iterator().next();
					for (String name : e.getName()) {
						if (name.length() < dsp.length())
							dsp = name;
					}
					e.setDisplayName(dsp);
				}
			}
		}
		// if required, set PE name to (already fixed) ER's name...
		for(EntityReference er : model.getObjects(EntityReference.class)) {
			for(SimplePhysicalEntity spe : er.getEntityReferenceOf()) {
				if(spe.getDisplayName() == null || spe.getDisplayName().trim().length() == 0) {
					if(er.getDisplayName() != null && er.getDisplayName().trim().length() > 0) {
						spe.setDisplayName(er.getDisplayName());
					}
				}
			}
		}
	}
		
}	

