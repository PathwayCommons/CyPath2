package org.pathwaycommons.cypath2.internal;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.Collection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

/**
 * Cytoscape 3 Results Panel (east) implementation.
 *
 * @author Ethan Cerami
 * @author Igor Rodchenkov
 */
public class BioPaxCytoPanelComponent implements CytoPanelComponent {

	private final static String DETAILS_CARD = "DETAILS";
	private final static String LEGEND_BIOPAX_CARD = "LEGEND_BIOPAX";
	private final static String LEGEND_BINARY_CARD = "LEGEND_BINARY";

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
		cards.add(bpDetailsPanel, DETAILS_CARD);
		cards.add(new LegendPanel(LegendPanel.BIOPAX_LEGEND), LEGEND_BIOPAX_CARD);
		cards.add(new LegendPanel(LegendPanel.BINARY_LEGEND), LEGEND_BINARY_CARD);
		label = new JEditorPane("text/html", "<a href='LEGEND'>Legend</a>");
		component.add(label, BorderLayout.SOUTH);
		label.setEditable(false);
		label.setOpaque(false);
		label.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		Font font = label.getFont();
		Font newFont = new Font(font.getFamily(), font.getStyle(), font.getSize() + 1);
		label.setFont(newFont);
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
						cl.show(cards, LEGEND_BINARY_CARD);
						label.setText("<a href='DETAILS'>Details</a>");
					} else if (BioPaxUtil.isDefaultBiopax(network)) {
						cl.show(cards, LEGEND_BIOPAX_CARD);
						label.setText("<a href='DETAILS'>Details</a>");
					} else if (BioPaxUtil.isFromBiopax(network)) {
						cl.show(cards, DETAILS_CARD);
						label.setText("<a href='LEGEND'>Legend</a>");
						//TODO add for SBGN later; ignore other...
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

	public void resetText() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				bpDetailsPanel.resetText();
			}
		});
	}
}