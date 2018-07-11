package org.pathwaycommons.cypath2.internal;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;


/**
 * Model for selectable nodes.
 * <p>
 * Code was originally obtained from:
 * http://www.javaresearch.org/source/javaresearch/jrlib0.6/org/jr/swing/tree/
 * <p>
 * and, has since been modified.
 */
final class CheckNode extends DefaultMutableTreeNode {
  public final static int SINGLE_SELECTION = 0;
  public final static int DIG_IN_SELECTION = 4;
  protected int selectionMode;
  protected boolean isSelected;

  /**
   * Constructor.
   *
   * @param userObject User Object.
   */
  public CheckNode(Object userObject) {
    this(userObject, true, false);
  }

  /**
   * Constructor.
   *
   * @param userObject     User Object.
   * @param allowsChildren Node allows children.
   * @param isSelected     Node is currently selected.
   */
  public CheckNode(Object userObject, boolean allowsChildren, boolean isSelected) {
    super(userObject, allowsChildren);
    this.isSelected = isSelected;
    setSelectionMode(DIG_IN_SELECTION);
  }

  /**
   * Gets the selection mode.
   *
   * @return selection mode.
   */
  public int getSelectionMode() {
    return selectionMode;
  }

  /**
   * Sets the selection mode.
   *
   * @param mode Selection mode.
   */
  public void setSelectionMode(int mode) {
    selectionMode = mode;
  }

  /**
   * Node is selected.
   *
   * @return true or false.
   */
  public boolean isSelected() {
    return isSelected;
  }

  /**
   * Sets selected / unselected.
   *
   * @param isSelected selected/uncselected.
   */
  public void setSelected(boolean isSelected) {
    this.isSelected = isSelected;

    //  DIG_IN Option
    if ((selectionMode == DIG_IN_SELECTION)
      && (children != null)) {
      Enumeration enum1 = children.elements();
      while (enum1.hasMoreElements()) {
        CheckNode node = (CheckNode) enum1.nextElement();
        node.setSelected(isSelected);
      }
    }
  }
}