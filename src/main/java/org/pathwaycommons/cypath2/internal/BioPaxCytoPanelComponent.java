package org.pathwaycommons.cypath2.internal;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.model.CyNetworkView;

/**
 * Cytoscape 3 Results Panel implementation.
 * Listens for BioPAX-originated network's events and displays
 * node details, etc.
 *
 * @author Ethan Cerami
 * @author Igor Rodchenkov
 */
public class BioPaxCytoPanelComponent implements CytoPanelComponent, RowsSetListener {

	private final static String DETAILS_CARD = "DETAILS";
	private final static String LEGEND_CARD = "LEGEND";

	private final JEditorPane label;
	private final JPanel cards;
	private final Icon icon;
	private final BioPaxDetailsPanel bpDetailsPanel;
	private final JPanel component;


	public BioPaxCytoPanelComponent() {
		this.bpDetailsPanel = new BioPaxDetailsPanel(CyPC.cyServices.openBrowser);
		this.icon = new ImageIcon(getClass().getResource("read_obj.gif"));
		this.component = new JPanel(new BorderLayout());
		this.cards = new JPanel(new CardLayout());

		component.add(cards, BorderLayout.CENTER);
		label = new JEditorPane("text/html", "<a href='LEGEND'>Legend</a>");
		component.add(label, BorderLayout.SOUTH);

		cards.add(bpDetailsPanel, DETAILS_CARD);
		cards.add(new LegendPanel(), LEGEND_CARD);
		label.setEditable(false);
		label.setOpaque(false);
		label.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		final Font font = label.getFont();
		label.setFont(new Font(font.getFamily(), font.getStyle(), font.getSize() + 1));
		label.setBorder(new EmptyBorder(5, 3, 3, 3));
		label.addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
				if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					String name = hyperlinkEvent.getDescription();
					if (name.equalsIgnoreCase("LEGEND")) {
						showLegend();
					} else {
						showDetails();
					}
				}
			}
		});
	}

	@Override
	public Component getComponent() {
		return component;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.EAST;
	}

	@Override
	public String getTitle() {
		return "Node Details";
	}

	@Override
	public Icon getIcon() {
		return icon;
	}

	public void showDetails() {
		if (BioPaxUtil.isFromBiopax(CyPC.cyServices.applicationManager.getCurrentNetwork())) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					CardLayout cl = (CardLayout) (cards.getLayout());
					cl.show(cards, DETAILS_CARD);
					label.setText("<a href='LEGEND'>Legend</a>");
				}
			});
		}
	}

	public void showLegend() {
		final CyNetwork network = CyPC.cyServices.applicationManager.getCurrentNetwork();
		if (network != null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					CardLayout cl = (CardLayout) (cards.getLayout());
					if (BioPaxUtil.isSifFromBiopax(network)) {
						cl.show(cards, LEGEND_CARD);
						label.setText("<a href='DETAILS'>Details</a>");
					} else if (BioPaxUtil.isFromBiopax(network)) {
						cl.show(cards, DETAILS_CARD);
						label.setText("<a href='LEGEND'>Legend</a>");
					}
				}
			});
		}
	}

	public void updateNodeDetails(final CyNetwork network, final Collection<CyNode> selected) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				bpDetailsPanel.updateNodeDetails(network, selected);
			}
		});
	}

	// RowsSetListener interface impl.
	@Override
	public void handleEvent(RowsSetEvent e) {
		CyNetworkView view = CyPC.cyServices.applicationManager.getCurrentNetworkView();
		if(view == null) return;

		final CyNetwork network = view.getModel();
		if (BioPaxUtil.isFromBiopax(network)) {

			if (!network.getDefaultNodeTable().equals(e.getSource()))
				return;

			CytoPanel eastCytoPanel = CyPC.cyServices.cySwingApplication.getCytoPanel(CytoPanelName.EAST);
			if(eastCytoPanel.getState() != CytoPanelState.DOCK)
				CyPC.cyServices.cySwingApplication.getCytoPanel(CytoPanelName.EAST).setState(CytoPanelState.DOCK);

			//east panel will display info about several nodes selected (not all)
			final Collection<CyNode> selected = new ArrayList<CyNode>();
			for (CyNode node : network.getNodeList()) {
				if (network.getRow(node).get(CyNetwork.SELECTED, Boolean.class)) {
					selected.add(node);
				}
			}

			if (!selected.isEmpty()) {
				// Show the details
				updateNodeDetails(network, selected);
				// If legend is showing, show details
				showDetails();
			}
		}
	}

}