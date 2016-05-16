package org.pathwaycommons.cypath2.internal;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;

/**
 * BioPAX network related utilities
 */
final class BioPaxUtil {
    
	static final String BIOPAX_URI = "URI";
	static final String BIOPAX_ENTITY_TYPE = "BIOPAX_TYPE";
	static final String BIOPAX_NETWORK = "BIOPAX_NETWORK";	
    
	// private Constructor
	private BioPaxUtil() {}

	/*
	 * Detects whether a network was generated from BioPAX data.
	 * 
	 * This is mainly to decide whether to watch for node selection
	 * events or not, to update the Results Panel (eastern) with the node's 
	 * info and visual legend. 
	 * 
	 * @param cyNetwork
	 */
	static boolean isFromBiopax(CyNetwork cyNetwork) {
		//true if the attribute column exists
		CyTable cyTable = cyNetwork.getDefaultNodeTable();
		return cyTable.getColumn(BIOPAX_ENTITY_TYPE) != null
				&& cyTable.getColumn(BIOPAX_URI) != null;
	}
	
	
	/*
	 * Detects whether a network was generated from BioPAX data
	 * using the default mapping method, where physical entities 
	 * and interactions make nodes, and BioPAX Entity class
	 * (both for the domain and range) properties make edges.
	 * 
	 * This is mainly to decide whether to watch for node selection
	 * events or not, to update the Results Panel (eastern) with the node's 
	 * info and visual legend. 
	 * 
	 * @param cyNetwork
	 */
	static boolean isDefaultBiopax(CyNetwork cyNetwork) {
        boolean biopax = false;
		String type = cyNetwork.getRow(cyNetwork).get(BIOPAX_NETWORK, String.class);
		if("DEFAULT".equalsIgnoreCase(type) || BiopaxVisualStyleUtil.BIO_PAX_VISUAL_STYLE.equalsIgnoreCase(type))
            biopax = true;		
		return isFromBiopax(cyNetwork) && biopax; 
	}
	
	
	/*
	 * Detects whether it's a SIF network converted from BioPAX.
	 * 
	 * @param cyNetwork
	 */
	static boolean isSifFromBiopax(CyNetwork cyNetwork) {
		//using BIOPAX_NETWORK network attribute (used to be set by the BioPAX reader)
        boolean sif = false;
		String type = cyNetwork.getRow(cyNetwork).get(BIOPAX_NETWORK, String.class);
		if("SIF".equalsIgnoreCase(type) || BiopaxVisualStyleUtil.BINARY_SIF_VISUAL_STYLE.equalsIgnoreCase(type))
            sif = true;		
		return isFromBiopax(cyNetwork) && sif; 
	}
}
