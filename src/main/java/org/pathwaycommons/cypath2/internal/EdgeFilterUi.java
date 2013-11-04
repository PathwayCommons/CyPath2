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

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.EDGE_VISIBLE;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

/**
 * Edge Filter Dialog.
 */
public class EdgeFilterUi extends JDialog {
	
	private static final long serialVersionUID = 1L;
	private HashSet<JCheckBox> checkBoxSet;
    private final CyNetwork cyNetwork;
    private final CyServices cyServices;

    /**
     * Constructor.
     * @param cyNetwork CyNetwork Object.
     */
    public EdgeFilterUi(CyNetwork cyNetwork, CyServices cyServices) 
    {
        this.cyNetwork = cyNetwork;
        this.cyServices = cyServices;

        initGui();
    }

	/**
     * Initializes GUI.
     */
    private void initGui() {
        this.setModal(true);
        this.setTitle("Edge Filter");
        checkBoxSet = new HashSet<JCheckBox>();
        
        //collect unique edge types (SIF interaction names)
        Set<String> interactionSet = new TreeSet<String>();
        for (CyEdge edge : cyNetwork.getEdgeList()) {
        	CyRow row = cyNetwork.getRow(edge);
            String interactionType = row.get("interaction", String.class);
            interactionSet.add(interactionType);
        }

        //create the checkbox panel (all types are selected by default)
        JPanel edgeSetPanel = new JPanel();
        Border emptyBorder = new EmptyBorder (10,10,10,100);
        Border titledBorder = new TitledBorder ("Edge Filter");
        CompoundBorder compoundBorder = new CompoundBorder (titledBorder, emptyBorder);
        edgeSetPanel.setBorder(compoundBorder);
        edgeSetPanel.setLayout(new BoxLayout(edgeSetPanel, BoxLayout.Y_AXIS));
        for(String interactionType : interactionSet) {
            JCheckBox checkBox = new JCheckBox(interactionType);
            checkBox.setActionCommand(interactionType);
            checkBox.addActionListener(new ApplyEdgeFilter());
            checkBox.setSelected(true);
            edgeSetPanel.add(checkBox);
            checkBoxSet.add(checkBox);
        }

        //  Select all edges (init)
        ApplyEdgeFilter apply = new ApplyEdgeFilter();
        apply.executeFilter();

        Container contentPane = this.getContentPane();
        contentPane.setLayout (new BorderLayout());
        contentPane.add(edgeSetPanel, BorderLayout.CENTER);

        JPanel panel = new JPanel();
        JButton closeButton = new JButton ("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                EdgeFilterUi.this.dispose();

            }
        });
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        panel.add(closeButton);
        contentPane.add(panel, BorderLayout.SOUTH);

        this.setLocationRelativeTo(cyServices.cySwingApplication.getJFrame());
        this.pack();
        this.setVisible(true);
    }
 
    
    
    /**
     * Apply Edge Filter (private class).
     */
    class ApplyEdgeFilter implements ActionListener {

        /**
         * Check Box selected or unselected.
         * @param actionEvent Action Event.
         */
        public void actionPerformed(ActionEvent actionEvent) {
            executeFilter();
        }

        /**
         * Executes the Edge Filter.
         */
        void executeFilter() {        	
            HashSet<String> selectedInteractionSet = new HashSet<String>();
            for (JCheckBox checkBox : checkBoxSet) {
                String action = checkBox.getActionCommand();
                if (checkBox.isSelected()) {
                    selectedInteractionSet.add(action);
                }
            }
            
            //collect edges with currently selected interaction types
            final ArrayList<CyEdge> edgeList = new ArrayList<CyEdge>();
            for (CyEdge edge: cyNetwork.getEdgeList()) {
                String interactionType = cyNetwork.getRow(edge).get("interaction", String.class);
                if (!selectedInteractionSet.contains(interactionType)) {
                    edgeList.add(edge);
                }
            }

            final CyNetworkView networkView = 
            		cyServices.applicationManager.getCurrentNetworkView();
            //Un-hide all edges
            setVisibleEdges(cyNetwork.getEdgeList(), true, networkView);
            
            //Hide unchecked type edges
            setVisibleEdges(edgeList, false, networkView);
            
            //update view
            cyServices.mappingManager.getVisualStyle(networkView).apply(networkView);
            networkView.updateView();
        }
                
    	void setVisibleEdges(Collection<CyEdge> edges, boolean visible, CyNetworkView view) {
    		for (CyEdge e : edges) {
    			final View<CyEdge> ev = view.getEdgeView(e);   					
    			if (visible)
    				ev.clearValueLock(EDGE_VISIBLE);
    			else
    				ev.setLockedValue(EDGE_VISIBLE, false);
    		}
    	}
    }    
}

