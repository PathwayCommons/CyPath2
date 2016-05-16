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

	public LegendPanel() {
		setLayout(new BorderLayout());
		JTextPane textPane = new JTextPane();
		JScrollPane scrollPane = new JScrollPane(textPane);
		add(scrollPane, BorderLayout.CENTER);

		textPane.setEditable(false);
		textPane.setContentType("text/html");
        textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

		BioPaxDetailsPanel.modifyStyleSheetForSingleDocument(textPane);

        StringBuffer temp = new StringBuffer();
		temp.append("<html><body>");
		try {
			String legendHtml = retrieveDocument(LegendPanel.class.getResource("visual_legend.html").toString());
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
                        new EdgeFilterUi(App.cyServices.applicationManager.getCurrentNetwork());
                    }
					else {
						App.cyServices.openBrowser.openURL(hyperlinkEvent.getURL().toString());
					}
                }
            }
        });
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
