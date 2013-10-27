package org.pathwaycommons.cypath2.internal;

/*
 * #%L
 * Cytoscape BioPAX Impl (biopax-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */


import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
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
 */
public class LegendPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/**
     * BioPAX Legend.
     */
    public static int BIOPAX_LEGEND = 0;

    /**
     * Binary Legend.
     */
    public static int BINARY_LEGEND = 1;

    /**
	 * Constructor.
	 */
	public LegendPanel(int mode, final CyServices cyServices) {
		this.setLayout(new BorderLayout());

		JTextPane textPane = new JTextPane();
		textPane.setEditable(false);
		textPane.setContentType("text/html");
        textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        URL legendUrl;
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
                        new EdgeFilterUi(cyServices.applicationManager.getCurrentNetwork(), cyServices);
                    }
                }
            }
        });
        
        BioPaxDetailsPanel.modifyStyleSheetForSingleDocument(textPane);

        JScrollPane scrollPane = new JScrollPane(textPane);
		this.add(scrollPane, BorderLayout.CENTER);
	}
	
	
	private String retrieveDocument(String urlStr) throws MalformedURLException, IOException {
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
