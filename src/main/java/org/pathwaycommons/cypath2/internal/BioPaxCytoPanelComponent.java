package org.pathwaycommons.cypath2.internal;

/*
 * #%L
 * Cytoscape BioPAX Impl (biopax-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;

/**
 * Cytoscape Results Panel (east) implementation.
 * 
 *
 * @author Ethan Cerami
 * @author Igor Rodchenkov (refactoring)
 */
public class BioPaxCytoPanelComponent extends JPanel implements CytoPanelComponent {

	private final Icon icon;
	
	private static final long serialVersionUID = 1L;

	private JEditorPane label;
    private JPanel cards;
	private final CyServices cyServices;

    private final static String DETAILS_CARD = "DETAILS";
    private final static String LEGEND_BIOPAX_CARD = "LEGEND_BIOPAX";
    private final static String LEGEND_BINARY_CARD = "LEGEND_BINARY";
    
	/**
	 * Constructor.
	 * 
	 * @param bpDetailsPanel
     * @param cyServices
	 */
	public BioPaxCytoPanelComponent(BioPaxDetailsPanel bpDetailsPanel, CyServices cyServices) 
	{
		this.cyServices = cyServices;
		
        cards = new JPanel(new CardLayout());
        LegendPanel bioPaxLegendPanel = new LegendPanel(LegendPanel.BIOPAX_LEGEND, 
        		cyServices.applicationManager, cyServices.cySwingApplication);
        LegendPanel binaryLegendPanel = new LegendPanel(LegendPanel.BINARY_LEGEND, 
        		cyServices.applicationManager, cyServices.cySwingApplication);

        cards.add (bpDetailsPanel, DETAILS_CARD);
        cards.add (bioPaxLegendPanel, LEGEND_BIOPAX_CARD);
        cards.add (binaryLegendPanel, LEGEND_BINARY_CARD);
        
        this.setLayout(new BorderLayout());
		this.add(cards, BorderLayout.CENTER);

        label = new JEditorPane ("text/html", "<a href='LEGEND'>Visual Legend</a>");
        label.setEditable(false);
        label.setOpaque(false);
        label.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
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

        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        Font font = label.getFont();
        Font newFont = new Font (font.getFamily(), font.getStyle(), font.getSize()-2);
        label.setFont(newFont);
        label.setBorder(new EmptyBorder(5,3,3,3));
        this.add(label, BorderLayout.SOUTH);
		
		URL url = getClass().getResource("read_obj.gif");
		icon = new ImageIcon(url);
	}
	
	@Override
	public Component getComponent() {
		return this;
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

	/**
     * Show Details Panel.
     */
    public void showDetails() {
    	if(BioPaxUtil.isFromBiopax(cyServices.applicationManager.getCurrentNetwork())) {
    		CardLayout cl = (CardLayout)(cards.getLayout());
    		cl.show(cards, DETAILS_CARD);
    		label.setText("<a href='LEGEND'>Visual Legend</a>");
    	}
    }

    /**
     * Show Legend Panel.
     */
    public void showLegend() {
        CyNetwork network = cyServices.applicationManager.getCurrentNetwork();
        
        if(network == null)
        	return;

        CardLayout cl = (CardLayout)(cards.getLayout());       

        if (BioPaxUtil.isSifFromBiopax(network)) {
        	cl.show(cards, LEGEND_BINARY_CARD);
        	label.setText("<a href='DETAILS'>View Details</a>");
        } else if(BioPaxUtil.isDefaultBiopax(network)) { 
        	cl.show(cards, LEGEND_BIOPAX_CARD);
        	label.setText("<a href='DETAILS'>View Details</a>");
        } else if (BioPaxUtil.isFromBiopax(network)){
        	cl.show(cards, DETAILS_CARD);
        	label.setText("<a href='LEGEND'>Visual Legend</a>");
        	//TODO add for SBGN later; ignore other...
        }
        
    }	
	
}
