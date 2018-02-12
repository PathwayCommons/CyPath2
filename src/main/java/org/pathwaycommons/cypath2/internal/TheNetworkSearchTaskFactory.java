package org.pathwaycommons.cypath2.internal;

import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import javax.swing.*;
import java.awt.*;

public class TheNetworkSearchTaskFactory extends AbstractNetworkSearchTaskFactory {

    private final CyAction action;
    private final App app;


    public TheNetworkSearchTaskFactory(CyAction showUI, App app) {
        super("pathwaycommons.cypath2", "Pathway Commons",
                "Search Pathway Commons, import a pathway or sub-network.",
                new ImageIcon(TheNetworkSearchTaskFactory.class.getResource("pc_logo.png")));

        this.action = showUI;
        this.app = app;
    }

    @Override
    public TaskIterator createTaskIterator() {
        //show the dialog after performing the search for pathways
        app.bpTypeComboBox.setSelectedIndex(1); //pathways
        app.searchField.setText(getQuery());
        app.searchButton.doClick();
        action.actionPerformed(null);

        return new TaskIterator(new AbstractTask() {
            @Override
            public void run(TaskMonitor tm) throws Exception {
                tm.setStatusMessage("Search in " + getName() + ": " + getQuery());
            }
        });
    }

    @Override
    public JComponent getOptionsComponent() {
        JRadioButton cb1 = new JRadioButton("Quick network", true);
        JRadioButton cb2 = new JRadioButton("Dialog");
        cb1.setForeground(Color.WHITE);
        cb2.setForeground(Color.WHITE);

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.DARK_GRAY);
        p.add(cb1, BorderLayout.NORTH);
        p.add(cb2, BorderLayout.SOUTH);

        return p;
    }
}
