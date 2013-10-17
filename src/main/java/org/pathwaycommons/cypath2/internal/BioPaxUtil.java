/** Copyright (c) 2010 University of Toronto (UofT)
 ** and Memorial Sloan-Kettering Cancer Center (MSKCC).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/
package org.pathwaycommons.cypath2.internal;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;

/**
 * BioPAX network related utilities
 * 
 * @CyAPI.Final.Class
 */
final class BioPaxUtil {
    
	static final String BIOPAX_URI = "URI";
	static final String BIOPAX_ENTITY_TYPE = "BIOPAX_TYPE";
	static final String BIOPAX_NETWORK = "BIOPAX_NETWORK";	
    
	// private Constructor
	private BioPaxUtil() {}
			
	
	/**
	 * Detects whether a network was generated from BioPAX data.
	 * 
	 * This is mainly to decide whether to watch for node selection
	 * events or not, to update the Results Panel (eastern) with the node's 
	 * info and visual legend. 
	 * 
	 * @param cyNetwork
	 */
	public static boolean isFromBiopax(CyNetwork cyNetwork) {
		//true if the attribute column exists
		CyTable cyTable = cyNetwork.getDefaultNodeTable();
		return cyTable.getColumn(BIOPAX_ENTITY_TYPE) != null
				&& cyTable.getColumn(BIOPAX_URI) != null;
	}
	
	
	/**
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
	public static boolean isDefaultBiopax(CyNetwork cyNetwork) {
        boolean biopax = false;
		String type = cyNetwork.getRow(cyNetwork).get(BIOPAX_NETWORK, String.class);
		if("DEFAULT".equalsIgnoreCase(type)) 
            biopax = true;		
		return isFromBiopax(cyNetwork) && biopax; 
	}
	
	
	/**
	 * Detects whether it's a SIF network converted from BioPAX.
	 * 
	 * @param cyNetwork
	 */
	public static boolean isSifFromBiopax(CyNetwork cyNetwork) {
		//using BIOPAX_NETWORK network attribute (used to be set by the BioPAX reader)
        boolean sif = false;
		String type = cyNetwork.getRow(cyNetwork).get(BIOPAX_NETWORK, String.class);
		if("SIF".equalsIgnoreCase(type)) 
            sif = true;		
		return isFromBiopax(cyNetwork) && sif; 
	}
	
	
	/**
	 * Tells whether it's a SBGN network converted from BioPAX.
	 * 
	 * @param cyNetwork
	 */
	public static boolean isSbgnFromBiopax(CyNetwork cyNetwork) {
        boolean sbgn = false;
		String type = cyNetwork.getRow(cyNetwork).get(BIOPAX_NETWORK, String.class);
		if("SBGN".equalsIgnoreCase(type)) 
            sbgn = true;		
		return isFromBiopax(cyNetwork) && sbgn; 
	}
}
