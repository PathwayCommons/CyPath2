package org.pathwaycommons.cypath2.internal;

import org.cytoscape.application.swing.AbstractCyAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;

final class ShowTheDialogAction extends AbstractCyAction {

  private static final long serialVersionUID = -5069785324747282157L;

  private final JDialog dialog;
  private final Window parent;
  private final Container gui;

  public ShowTheDialogAction(Map<String, String> configProps, Container gui) {
    super(configProps, App.cyServices.applicationManager, App.cyServices.networkViewManager);

    this.parent = App.cyServices.cySwingApplication.getJFrame();
    this.gui = gui;

    dialog = new JDialog(parent);
    dialog.setModal(true);
  }


  @Override
  public void actionPerformed(ActionEvent ae) {
    dialog.remove(gui);
    dialog.add(gui);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);
  }

}
