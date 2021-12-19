package org.pathwaycommons.cypath2.internal;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.EDGE_VISIBLE;

/**
 * Edge Filter Dialog.
 */
public class EdgeFilterUi extends JDialog {

  private static final long serialVersionUID = 1L;
  private final CyNetwork cyNetwork;
  private HashSet<JCheckBox> checkBoxSet;

  /**
   * Constructor.
   *
   * @param cyNetwork CyNetwork Object.
   */
  public EdgeFilterUi(CyNetwork cyNetwork) {
    this.cyNetwork = cyNetwork;
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
    Border emptyBorder = new EmptyBorder(10, 10, 10, 100);
    Border titledBorder = new TitledBorder("Edge Filter");
    CompoundBorder compoundBorder = new CompoundBorder(titledBorder, emptyBorder);
    edgeSetPanel.setBorder(compoundBorder);
    edgeSetPanel.setLayout(new BoxLayout(edgeSetPanel, BoxLayout.Y_AXIS));
    for (String interactionType : interactionSet) {
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
    contentPane.setLayout(new BorderLayout());
    contentPane.add(edgeSetPanel, BorderLayout.CENTER);

    JPanel panel = new JPanel();
    JButton closeButton = new JButton("Close");
    closeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent actionEvent) {
        EdgeFilterUi.this.dispose();

      }
    });
    panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
    panel.add(closeButton);
    contentPane.add(panel, BorderLayout.SOUTH);

    this.setLocationRelativeTo(App.cyServices.cySwingApplication.getJFrame());
    this.pack();
    this.setVisible(true);
  }


  /**
   * Apply Edge Filter (private class).
   */
  class ApplyEdgeFilter implements ActionListener {

    /**
     * Check Box selected or unselected.
     *
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
      for (CyEdge edge : cyNetwork.getEdgeList()) {
        String interactionType = cyNetwork.getRow(edge).get("interaction", String.class);
        if (!selectedInteractionSet.contains(interactionType)) {
          edgeList.add(edge);
        }
      }

      final CyNetworkView networkView =
        App.cyServices.applicationManager.getCurrentNetworkView();
      //Un-hide all edges
      setVisibleEdges(cyNetwork.getEdgeList(), true, networkView);

      //Hide unchecked type edges
      setVisibleEdges(edgeList, false, networkView);

      //update view
      App.cyServices.mappingManager.getVisualStyle(networkView).apply(networkView);
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


