package org.pathwaycommons.cypath2.internal;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * JTree with selectable leaves.
 * <p>
 * Code was originally obtained from:
 * http://www.javaresearch.org/source/javaresearch/jrlib0.6/org/jr/swing/tree/
 * <p>
 * and, has since been modified.
 */
final class JTreeWithCheckNodes extends JTree {

  /**
   * Constructor.
   *
   * @param rootNode Root Node.
   */
  public JTreeWithCheckNodes(TreeNode rootNode) {
    super(rootNode);
    setCellRenderer(new CheckNodeRenderer());
    getSelectionModel().setSelectionMode(
      TreeSelectionModel.SINGLE_TREE_SELECTION
    );
    putClientProperty("JTree.lineStyle", "Angled");
    addMouseListener(new NodeSelectionListener(this));
  }
}

/**
 * Listens for node selection events.
 */
class NodeSelectionListener extends MouseAdapter {
  JTree tree;

  /**
   * Constructor.
   *
   * @param tree JTree Object.
   */
  NodeSelectionListener(JTree tree) {
    this.tree = tree;
  }

  /**
   * Mouse Click Event.
   *
   * @param e MouseEvent Object.
   */
  public void mouseClicked(MouseEvent e) {
    int x = e.getX();
    int y = e.getY();
    int row = tree.getRowForLocation(x, y);
    TreePath path = tree.getPathForRow(row);
    if (path != null) {
      CheckNode node = (CheckNode) path.getLastPathComponent();
      boolean isSelected = !(node.isSelected());
      node.setSelected(isSelected);
      if (node.getSelectionMode() == CheckNode.DIG_IN_SELECTION) {
        if (isSelected) {
          tree.expandPath(path);
        } else {
          tree.collapsePath(path);
        }
      }
      ((DefaultTreeModel) tree.getModel()).nodeChanged(node);
      if (row == 0) {
        tree.revalidate();
        tree.repaint();
      }
    }
  }
}
