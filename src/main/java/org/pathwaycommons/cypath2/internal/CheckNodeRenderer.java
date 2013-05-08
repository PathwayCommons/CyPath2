package org.pathwaycommons.cypath2.internal;


import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.TreeCellRenderer;

/**
 * Node with CheckBox Renderer.
 *
 * Code was originally obtained from:
 * http://www.javaresearch.org/source/javaresearch/jrlib0.6/org/jr/swing/tree/
 *
 * and, has since been modified.
*/
final class CheckNodeRenderer implements TreeCellRenderer {

    /**
     * Gets the Tree Cell Renderer.
     * @param tree          JTree Object.
     * @param value         Object value.
     * @param isSelected    Node is selected.
     * @param expanded      Node is expanded.
     * @param leaf          Node is a leaf.
     * @param row           Row number.
     * @param hasFocus      Node has focus.
     * @return Custom Component.
     */
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean isSelected, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {
        String stringValue = tree.convertValueToText(value, isSelected,
                expanded, leaf, row, hasFocus);
        CustomNodePanel customNodePanel = new CustomNodePanel(tree, value,
                expanded, leaf, stringValue);
        customNodePanel.setEnabled(tree.isEnabled());
        return customNodePanel;
    }
}

/**
 * Custom Node Panel.
 *
 * Code was originally obtained from:
 * http://www.javaresearch.org/source/javaresearch/jrlib0.6/org/jr/swing/tree/
 *
 */
class CustomNodePanel extends JPanel {
    private JCheckBox check;
    private JLabel label;
    
    // use the "lazy initialization holder class idiom" to sync. shared (by threads) static fields
    private static class FieldHolder {
        private static final ImageIcon filterIcon = 
        	new ImageIcon(GradientHeader.class.getResource("stock_autofilter.png"));
    }
    static ImageIcon filterIcon() { return FieldHolder.filterIcon;}

    /**
     * Constructor.
     * @param tree      JTree Object.
     * @param value     Object value.
     * @param expanded  Node is expanded.
     * @param leaf      Node is a leaf.
     * @param stringValue String value.
     */
    public CustomNodePanel(JTree tree, Object value,
            boolean expanded, boolean leaf, String stringValue) {
        setLayout(new BorderLayout());
        if (leaf) {
            add(check = new JCheckBox(), BorderLayout.WEST);
            check.setBackground(UIManager.getColor("Tree.textBackground"));
            check.setSelected(((CheckNode) value).isSelected());
            check.setOpaque(false);
        }
        add(label = new JLabel(), BorderLayout.EAST);
        label.setOpaque(false);
        label.setFont(tree.getFont());
        label.setText(stringValue);
        if (leaf) {
            //label.setIcon(UIManager.getIcon("Tree.leafIcon"));
        } else if (expanded) {
            label.setIcon(filterIcon());
            //label.setIcon(UIManager.getIcon("Tree.openIcon"));
        } else {
            label.setIcon(filterIcon());
            //label.setIcon(UIManager.getIcon("Tree.closedIcon"));
        }
        setOpaque(false);
    }
}
