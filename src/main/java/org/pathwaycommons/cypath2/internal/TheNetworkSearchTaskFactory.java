package org.pathwaycommons.cypath2.internal;

import cpath.service.GraphType;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class TheNetworkSearchTaskFactory extends AbstractNetworkSearchTaskFactory {

    private final CyAction action;
    private final App app;
    private boolean dialog = false;


    public TheNetworkSearchTaskFactory(CyAction showUI, App app) {
        super("pathwaycommons.cypath2", "Pathway Commons",
                "Search Pathway Commons, import a pathway or sub-network.",
                new ImageIcon(TheNetworkSearchTaskFactory.class.getResource("pc_logo.png")));

        this.action = showUI;
        this.app = app;
    }

    @Override
    public TaskIterator createTaskIterator() {
        if(dialog) {
            //show the dialog after performing the search for pathways
            app.bpTypeComboBox.setSelectedIndex(1); //pathways
            app.searchField.setText(getQuery());
            app.searchButton.doClick();
            action.actionPerformed(null);
            return new TaskIterator(new AbstractTask() {
                @Override
                public void run(TaskMonitor tm) throws Exception {
//                    tm.setStatusMessage(getName() + " dialog mode; query: " + getQuery());
                }
            });
        } else {
            String q = getQuery();
            Set<String> srcs = new HashSet<String>();
            for (String name : q.split("[\\s,]+"))
                srcs.add(name);
            return new TaskIterator(new NetworkAndViewTask(
                    App.client.createGraphQuery().kind(GraphType.PATHSBETWEEN).sources(srcs), q)
            );
        }
    }

    @Override
    public JComponent getOptionsComponent() {
        ButtonGroup bg = new ButtonGroup();
        JRadioButton cb1 = new JRadioButton("Quick (network by IDs)", true);
        cb1.setForeground(Color.WHITE);
        cb1.addActionListener(e -> dialog=false);
        bg.add(cb1);
        JRadioButton cb2 = new JRadioButton("Advanced (dialog)");
        cb2.setForeground(Color.WHITE);
        cb2.addActionListener(e -> dialog=true);
        bg.add(cb2);
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.DARK_GRAY);
        p.add(cb1, BorderLayout.NORTH);
        p.add(cb2, BorderLayout.SOUTH);
        return p;
    }
}
