package org.cytoscape.cpathsquared.internal;

import java.awt.BorderLayout;
import java.util.Collections;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.work.TaskIterator;

/**
 * Summary Panel.
 *
 */
final class DetailsPanel extends JPanel {
    private Document doc;
    private JTextPane textPane;

    /**
     * Constructor.
     * @param browser 
     */
    public DetailsPanel() {
        this.setLayout(new BorderLayout());
        textPane = createHtmlTextPane(CpsFactory.context().openBrowser);
        doc = textPane.getDocument();
        JScrollPane scrollPane = encloseInJScrollPane (textPane);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Gets the summary document model.
     * @return Document object.
     */
    public Document getDocument() {
        return doc;
    }

    /**
     * Gets the summary text pane object.
     * @return JTextPane Object.
     */
    public JTextPane getTextPane() {
        return textPane;
    }

    /**
     * Encloses the specified JTextPane in a JScrollPane.
     *
     * @param textPane JTextPane Object.
     * @return JScrollPane Object.
     */
    private JScrollPane encloseInJScrollPane(JTextPane textPane) {
        JScrollPane scrollPane = new JScrollPane(textPane);
        return scrollPane;
    }

    /**
     * Creates a JTextPane with correct line wrap settings.
     *
     * @return JTextPane Object.
     */
    public static JTextPane createHtmlTextPane(final OpenBrowser browser) {
        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBorder(new EmptyBorder(7,7,7,7));
        textPane.setContentType("text/html");
        textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        textPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
                if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
//                    browser.openURL(hyperlinkEvent.getURL().toString());
                	//import data and create network only if a special (name) link clicked
                	String queryUrl = hyperlinkEvent.getURL().toString();
                    if(queryUrl.startsWith(CpsFactory.SERVER_URL)) {
    				        CpsFactory.context().taskManager.execute(new TaskIterator(
    				        	new CreateNetworkAndViewTask(queryUrl, 
    				        		CpsFactory.downloadMode, 
    				        		"tmp")));
                    }
                }
            }
        });

        HTMLDocument htmlDoc = (HTMLDocument) textPane.getDocument();
        StyleSheet styleSheet = htmlDoc.getStyleSheet();
        styleSheet.addRule("h2 {color:  #663333; font-size: 102%; font-weight: bold; "
                + "margin-bottom:3px}");
        styleSheet.addRule("h3 {color: #663333; font-size: 95%; font-weight: bold;"
                + "margin-bottom:7px}");
        styleSheet.addRule("ul { list-style-type: none; margin-left: 5px; "
                + "padding-left: 1em;	text-indent: -1em;}");
        styleSheet.addRule("h4 {color: #66333; font-weight: bold; margin-bottom:3px;}");
//        styleSheet.addRule("b {background-color: #FFFF00;}");
        styleSheet.addRule(".bold {font-weight:bold;}");
        styleSheet.addRule(".link {color:blue; text-decoration: underline;}");
        styleSheet.addRule(".excerpt {font-size: 90%;}");
        // highlight matching fragments
        styleSheet.addRule(".hitHL {background-color: #FFFF00;}");
        return textPane;
    }
}