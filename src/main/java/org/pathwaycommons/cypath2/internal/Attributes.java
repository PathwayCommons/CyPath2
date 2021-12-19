package org.pathwaycommons.cypath2.internal;

import org.cytoscape.model.*;

import java.util.Collection;
import java.util.List;

final class Attributes {

  public static void set(CyNetwork network, CyIdentifiable entry, String name, Object value, Class<?> type) {
    set(network, entry, CyNetwork.DEFAULT_ATTRS, name, value, type);
  }

  public static void set(CyNetwork network, CyIdentifiable entry, String tableName, String name, Object value, Class<?> type) {
    CyRow row = network.getRow(entry, tableName);
    CyTable table = row.getTable();
    CyColumn column = table.getColumn(name);
    if (value != null) {
      if (column == null) {
        if (value instanceof List) {
          table.createListColumn(name, type, false);
        } else if (value instanceof Collection) {
          throw new IllegalArgumentException("Attribute value is a Collection and not List: "
            + value.getClass().getSimpleName());
        } else {
          table.createColumn(name, type, false);
        }
      }
      row.set(name, value);
    }
  }
}
