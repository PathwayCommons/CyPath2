package org.pathwaycommons.cypath2.internal;

import org.cytoscape.util.swing.CheckBoxJList;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Global filters and options.
 *
 * @author rodche
 */
final class AppOptions {
  final CheckBoxJList organismList;
  final CheckBoxJList dataSourceList;

  public AppOptions() {
    organismList = new CheckBoxJList();
    organismList.setToolTipText("Check to exclude entities not associated with at least one of selected organisms");
    organismList.setAlignmentX(Component.LEFT_ALIGNMENT);

    dataSourceList = new CheckBoxJList();
    dataSourceList.setToolTipText("Check to exclude entities not associated with at least one of selected datasources");
    dataSourceList.setAlignmentX(Component.LEFT_ALIGNMENT);
  }

  /**
   * @return currently selected organisms
   */
  Set<String> selectedOrganisms() {
    Set<String> values = new HashSet<String>();
    for (Object it : organismList.getSelectedValues())
      values.add(((NvpListItem) it).getValue());
    return values;
  }

  /**
   * @return currently selected datasources
   */
  Set<String> selectedDatasources() {
    Set<String> values = new HashSet<String>();
    for (Object it : dataSourceList.getSelectedValues())
      values.add(((NvpListItem) it).getValue());
    return values;
  }
}
