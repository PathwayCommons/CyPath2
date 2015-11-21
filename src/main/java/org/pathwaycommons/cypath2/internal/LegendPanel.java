package org.pathwaycommons.cypath2.internal;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * Displays the Default Visual Style Legend for the BioPAX Mapper.
 *
 * @author Ethan Cerami
 * @author Igor Rodchenkov
 */
public class LegendPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	static final int BIOPAX_LEGEND = 0;
	static final int BINARY_LEGEND = 1;


	public LegendPanel(int mode) {
		this.setLayout(new BorderLayout());

		JTextPane textPane = new JTextPane();
		textPane.setEditable(false);
		textPane.setContentType("text/html");
        textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        URL legendUrl; //TODO update legend html templates to reflect new SIF rules/patterns
        if (mode == BIOPAX_LEGEND) {
            legendUrl = LegendPanel.class.getResource("legend.html");
        } else {
            legendUrl = LegendPanel.class.getResource("binary_legend.html");
        }
        StringBuffer temp = new StringBuffer();
		temp.append("<html><body>");

		try {
			String legendHtml = retrieveDocument(legendUrl.toString());
			temp.append(legendHtml);
		} catch (Exception e) {
			temp.append("Could not load legend... " + e.toString());
		}

		temp.append("</body></html>");
		textPane.setText(temp.toString());

		textPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
                if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    String name = hyperlinkEvent.getDescription();
                    if (name.equalsIgnoreCase("filter")) {
                        new EdgeFilterUi(CyPC.cyServices.applicationManager.getCurrentNetwork());
                    }
					else if(name.equalsIgnoreCase("sif_relations")) {
						CyPC.cyServices.openBrowser.openURL("http://www.pathwaycommons.org/pc2/formats#sif_relations");
					} else {
						CyPC.cyServices.openBrowser.openURL(hyperlinkEvent.getURL().toString());
					}
                }
            }
        });
        
        BioPaxDetailsPanel.modifyStyleSheetForSingleDocument(textPane);

        JScrollPane scrollPane = new JScrollPane(textPane);
		this.add(scrollPane, BorderLayout.CENTER);
	}

	private String retrieveDocument(String urlStr) throws IOException {
		URL url = new URL(urlStr);
		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		return readFile(in);
	}
	
	private String readFile(BufferedReader in) throws IOException {
		StringBuffer buf = new StringBuffer();
		String str;
		while ((str = in.readLine()) != null) {
			buf.append(str + "\n");
		}
		in.close();

		return buf.toString();
	}
}
