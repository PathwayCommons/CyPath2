package org.pathwaycommons.cypath2.internal;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.SwingUtilities;

import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.model.CyNetworkView;


/**
 * Listens for Network Events, and takes appropriate Actions.
 * May be subclassed.
 *
 * @author Ethan Cerami, Gary Bader, Chris Sander, Benjamin Gross
 * @author Igor Rodchenkov (re-factoring, porting).
 */
public class BioPaxTracker implements SetCurrentNetworkViewListener, RowsSetListener {

	private final BioPaxCytoPanelComponent cytoPanelComponent;
	private final CyServices cyServices;

	private static final int MAX_SELECTED_NODES_INFO = 5;
	
	/**
	 * Constructor.
	 *
	 * @param cytoPanelComponent
	 * @param cyServices
	 */
	public BioPaxTracker(BioPaxCytoPanelComponent cytoPanelComponent,
			CyServices cyServices) 
	{
		this.cytoPanelComponent = cytoPanelComponent;
		this.cyServices = cyServices;
	}

	/**
	 * Network Focus Event.
	 */
	@Override
	public void handleEvent(SetCurrentNetworkViewEvent e) {
		CyNetworkView view = e.getNetworkView();
		
		// update bpPanel accordingly
       	if (view != null && BioPaxUtil.isFromBiopax(view.getModel())) {
       		SwingUtilities.invokeLater(new Runnable() {
       			@Override
       			public void run() {
					cytoPanelComponent.resetText();
       	            CytoPanel cytoPanel = cyServices.cySwingApplication.getCytoPanel(CytoPanelName.EAST);
       	            cytoPanel.setState(CytoPanelState.DOCK);
       			}
       		});
        }
	}


	@Override
	public void handleEvent(RowsSetEvent e) {
		CyNetworkView view = cyServices.applicationManager.getCurrentNetworkView();
		if(view == null) return;
		
		final CyNetwork network = view.getModel();
		if (BioPaxUtil.isFromBiopax(network)) {

			if (!network.getDefaultNodeTable().equals(e.getSource()))
				return;

			//east panel will display info about several nodes selected (not all)
			final Collection<CyNode> selected = new ArrayList<CyNode>();			
			for (CyNode node : network.getNodeList()) {
				if (network.getRow(node).get(CyNetwork.SELECTED, Boolean.class)) {
					selected.add(node);
				}
			}

			if (!selected.isEmpty()) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						// Show the details
						cytoPanelComponent.updateNodeDetails(network, selected);
						// If legend is showing, show details
						cytoPanelComponent.showDetails();
					}
				});
			}
		}
	}

}
