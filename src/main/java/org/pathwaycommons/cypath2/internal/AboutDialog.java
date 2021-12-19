package org.pathwaycommons.cypath2.internal;

import org.cytoscape.util.swing.OpenBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;


final class AboutDialog extends JDialog implements HyperlinkListener {

  private static final long serialVersionUID = 5333460301722177689L;

  private static final Logger logger = LoggerFactory.getLogger(AboutDialog.class);
  private static final Dimension WINDOW_SIZE = new Dimension(500, 400);

  private final OpenBrowser openBrowser;

  private JEditorPane mainEditorPane;
  private JScrollPane mainScrollPane;
  private JLabel titleLabel;
  private JPanel titlePanel;
  private JPanel mainPanel;


  public AboutDialog(Window parent, final OpenBrowser openBrowser) {
    super(parent, Dialog.ModalityType.APPLICATION_MODAL);
    this.openBrowser = openBrowser;
    initComponents();
    mainEditorPane.setEditable(false);
    mainEditorPane.addHyperlinkListener(this);
    setLocationRelativeTo(parent);
    setModalityType(DEFAULT_MODALITY_TYPE);
    this.setPreferredSize(WINDOW_SIZE);
    this.setSize(WINDOW_SIZE);
  }


  public void showDialog(String title, Icon icon, String description) {
    titleLabel.setText(title);
    titleLabel.setIcon(icon);

    URL target = null;
    mainEditorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    try {
      target = new URL(description);
    } catch (MalformedURLException e) {
      mainEditorPane.setContentType("text/html");
      mainEditorPane.setText(description);
      repaint();
      setVisible(true);
      return;
    }

    try {
      mainEditorPane.setPage(target);
      pack();
      repaint();
      setVisible(true);
    } catch (IOException e) {
      mainEditorPane.setText("Could not connect to " + target.toString());
      pack();
      repaint();
      setVisible(true);
    }
  }

  private void initComponents() {
    titlePanel = new JPanel();
    titleLabel = new JLabel();
    mainPanel = new JPanel();
    mainScrollPane = new JScrollPane();
    mainEditorPane = new JEditorPane();

    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setTitle("About");

    titlePanel.setBackground(new java.awt.Color(255, 255, 255));

    titleLabel.setFont(new java.awt.Font("SansSerif", 0, 18));
    titleLabel.setText("Client Name Here");

    GroupLayout titlePanelLayout = new GroupLayout(titlePanel);
    titlePanel.setLayout(titlePanelLayout);
    titlePanelLayout.setHorizontalGroup(titlePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(
        titlePanelLayout.createSequentialGroup().addContainerGap()
          .addComponent(titleLabel, GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE)
          .addContainerGap()));
    titlePanelLayout.setVerticalGroup(titlePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
      titlePanelLayout.createSequentialGroup().addContainerGap()
        .addComponent(titleLabel, GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE).addContainerGap()));

    mainPanel.setBackground(new java.awt.Color(255, 255, 255));

    mainScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    mainScrollPane.setFont(new java.awt.Font("SansSerif", 0, 12));

    mainEditorPane.setEditable(false);
    mainScrollPane.setViewportView(mainEditorPane);

    final GroupLayout mainPanelLayout = new GroupLayout(mainPanel);
    mainPanel.setLayout(mainPanelLayout);
    mainPanelLayout.setHorizontalGroup(mainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
      mainPanelLayout.createSequentialGroup().addContainerGap().addComponent(mainScrollPane)
        .addContainerGap()));
    mainPanelLayout.setVerticalGroup(mainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addGroup(
        mainPanelLayout.createSequentialGroup().addContainerGap()
          .addComponent(mainScrollPane, GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE)
          .addContainerGap()));

    GroupLayout layout = new GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      .addComponent(titlePanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
      .addComponent(mainPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
    layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
      layout.createSequentialGroup()
        .addComponent(titlePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
          GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(mainPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

    pack();
  }

  public void hyperlinkUpdate(HyperlinkEvent e) {
    if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED)
      return;

    final String url = e.getURL().toString();
    try {
      openBrowser.openURL(url);
    } catch (Exception err) {
      logger.warn("Unable to open browser for " + url.toString(), err);
    }
  }
}