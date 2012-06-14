package org.cytoscape.cpathsquared.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

import cpath.service.OutputFormat;

/**
 * Controller for Executing a Get Record(s) by CPath ID(s) command.
 * 
 */
public class GetByUriTask extends AbstractTask {
	private String ids[];
	private String networkTitle;
	private boolean haltFlag = false;
	private OutputFormat format;
	private final static String CPATH_SERVER_NAME_ATTRIBUTE = "CPATH_SERVER_NAME";
	private final static String CPATH_SERVER_DETAILS_URL = "CPATH_SERVER_DETAILS_URL";
	private static final Logger logger = LoggerFactory.getLogger(GetByUriTask.class);

	/**.
	 * Constructor.
	 * @param ids Array of cPath IDs.
	 * @param format Output Format.
	 * @param networkTitle Tentative Network Title.
	 */
	public GetByUriTask(String[] ids, OutputFormat format, String networkTitle) {
		this.ids = ids;
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
			
			// Get Data: BioPAX and the other format (if required)
			final String biopaxData = CpsFactory.getRecordsByIds(ids, OutputFormat.BIOPAX);
			// we gonna need the model to create attributes for non-biopax CyNetworks (e.g., created from SIF) 
			final Model bpModel = BioPaxUtil.convertFromOwl(new ByteArrayInputStream(biopaxData.getBytes("UTF-8")));
			
			final String data = (format == OutputFormat.BIOPAX) 
				? biopaxData : CpsFactory.getRecordsByIds(ids, format);
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
			
//			//the biopax graph reader used to need the following property to be set...
//			if (networkTitle != null && networkTitle.length() > 0) {
//				System.setProperty("biopax.network_view_title", networkTitle);
//			}
			
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
				taskMonitor.setStatusMessage("Updating SIF network " +
					"attributes from corresonding BioPAX data...");				
				
				// Set the Quick Find Default Index
				Attributes.set(cyNetwork, cyNetwork, "quickfind.default_index", CyNetwork.NAME, String.class);
				// Specify that this is a BINARY_NETWORK
				Attributes.set(cyNetwork, cyNetwork, "BIOPAX_NETWORK", Boolean.TRUE, Boolean.class);

				// Get all node details.
				createSinNetworkNodeAttributes(cyNetwork, bpModel);

				if (haltFlag) return; //TODO not sure whether it's the best or only place for this check...

				VisualStyle visualStyle = CpsFactory.context().binarySifVisualStyleUtil.getVisualStyle();
				CpsFactory.context().mappingManager.setVisualStyle(visualStyle, view);

//				//fix the network name
//				SwingUtilities.invokeLater(new Runnable() {
//					public void run() {
//						String networkTitleWithUnderscores = networkTitle.replaceAll(": ", "");
//						networkTitleWithUnderscores = networkTitleWithUnderscores.replaceAll(" ", "_");
//						CyNetworkNaming naming = CpsFactory.getCyNetworkNaming();
//						networkTitleWithUnderscores = naming.getSuggestedNetworkTitle(networkTitleWithUnderscores);
//						Attributes.set(cyNetwork, cyNetwork, CyNetwork.NAME, networkTitleWithUnderscores, String.class);
//					}
//				});
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
		String cPathServerDetailsUrl = row.get(GetByUriTask.CPATH_SERVER_DETAILS_URL, String.class);
		if (cPathServerDetailsUrl == null) {
			Attributes.set(cyNetwork, cyNetwork, GetByUriTask.CPATH_SERVER_NAME_ATTRIBUTE,
					serverName, String.class);
			String url = serverURL.replaceFirst("webservice.do", "record2.do?id=");
			Attributes.set(cyNetwork, cyNetwork, GetByUriTask.CPATH_SERVER_DETAILS_URL, url, String.class);
		}
	}

	
	/**
	 * Create BioPAX attributes for each node in the SIF network
	 */
	private void createSinNetworkNodeAttributes(CyNetwork cyNetwork, Model model) {
		List<List<CyNode>> batchList = createBatchArray(cyNetwork);
		if (batchList.size() == 0) {
			logger.info("Skipping node details.  Already have all the details new need.");
		}
		
		for (int i = 0; i < batchList.size(); i++) {
			if (haltFlag == true)
				break;
			
			List<CyNode> currentList = batchList.get(i);
			logger.debug("Getting node details, batch:  " + i);
			String ids[] = new String[currentList.size()];
			Map<String, CyNode> nodes = new HashMap<String, CyNode>();
			for (int j = 0; j < currentList.size(); j++) {
				CyNode node = currentList.get(j);
				String name = cyNetwork.getRow(node).get(CyNetwork.NAME, String.class);
				nodes.put(name, node);
				ids[j] = name;
			}
			try {		
				//map biopax properties to Cy attributes for SIF nodes
				for (BioPAXElement e : model.getObjects()) {
					if(e instanceof EntityReference 
						|| e instanceof Complex 
						|| e.getModelInterface().equals(PhysicalEntity.class)) 
					{
						CyNode node = nodes.get(e.getRDFId());
						if(node != null)
							BioPaxUtil.createAttributesFromProperties(e, node, cyNetwork);
						// - this will also update the 'name' attribute (to a biol. label)
						else {
							logger.debug("Oops: no node for " + e.getRDFId());
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private List<List<CyNode>> createBatchArray(CyNetwork cyNetwork) {
		int max_ids_per_request = 50;
		List<List<CyNode>> masterList = new ArrayList<List<CyNode>>();
		List<CyNode> currentList = new ArrayList<CyNode>();
		int counter = 0;
		for (CyNode node : cyNetwork.getNodeList()) {
			CyRow row = cyNetwork.getRow(node);
			String label = row.get(CyNetwork.NAME, String.class);

			// If we already have details on this node, skip it.
			if (label == null) {
				currentList.add(node);
				counter++;
			}
			if (counter > max_ids_per_request) {
				masterList.add(currentList);
				currentList = new ArrayList<CyNode>();
				counter = 0;
			}
		}
		if (currentList.size() > 0) {
			masterList.add(currentList);
		}
		return masterList;
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

