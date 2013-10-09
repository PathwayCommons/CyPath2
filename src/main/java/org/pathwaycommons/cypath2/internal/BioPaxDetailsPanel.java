package org.pathwaycommons.cypath2.internal;

/*
 * #%L
 * Cytoscape BioPAX Impl (biopax-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013
 *   Memorial Sloan-Kettering Cancer Center
 *   The Cytoscape Consortium
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
import java.awt.Color;
import java.awt.Dimension;
import java.util.Collection;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SizeRequirements;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ParagraphView;
import javax.swing.text.html.StyleSheet;

import org.cytoscape.application.CyApplicationManager;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.util.swing.OpenBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * BioPAX Details Panel.
 *
 * @author Ethan Cerami, rodche
 */
public class BioPaxDetailsPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(BioPaxDetailsPanel.class);
	
    static final String BIOPAX_CHEMICAL_MODIFICATIONS_LIST = "chemical_modifications";
    static final String BIOPAX_UNIFICATION_REFERENCES = "unification_references";
    static final String BIOPAX_RELATIONSHIP_REFERENCES = "relationship_references";
    static final String BIOPAX_PUBLICATION_REFERENCES = "publication_references";
    static final String BIOPAX_IHOP_LINKS = "ihop_links";	
	
	CyApplicationManager applicationManager;
	
	/**
	 * Foreground Color.
	 */
	static final Color FG_COLOR = new Color(75, 75, 75);
	
	private JScrollPane scrollPane;
	private JTextPane textPane;

	
	/**
	 * Constructor.
	 */
	public BioPaxDetailsPanel(OpenBrowser browser) {
		textPane = new JTextPane();

		//  Set Editor Kit that is capable of handling long words
		MyEditorKit kit = new MyEditorKit();
		textPane.setEditorKit(kit);
        modifyStyleSheetForSingleDocument(textPane);

        textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        textPane.setBorder(new EmptyBorder (5,5,5,5));
        textPane.setContentType("text/html");
		textPane.setEditable(false);
		textPane.addHyperlinkListener(new LaunchExternalBrowser(browser));
		resetText();

		scrollPane = new JScrollPane(textPane);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		this.setLayout(new BorderLayout());
		this.add(scrollPane, BorderLayout.CENTER);
		this.setPreferredSize(new Dimension(300, 300));
		this.setMaximumSize(new Dimension(300, 300));
	}

    public static void modifyStyleSheetForSingleDocument(JTextPane textPane) {
        HTMLDocument htmlDoc = (HTMLDocument) textPane.getDocument();
        StyleSheet styleSheet = htmlDoc.getStyleSheet();
        styleSheet.addRule("h2 {color: #663333; font-weight: bold; "
                + "margin-bottom:3px}");
        styleSheet.addRule("h3 {color: #663333; font-weight: bold;"
                + "margin-bottom:7px}");
        styleSheet.addRule("ul { list-style-type: none; margin-left: 5px; "
                + "padding-left: 1em;	text-indent: -1em;}");
        styleSheet.addRule("h4 {color: #66333; font-weight: bold; margin-bottom:3px;}");
        styleSheet.addRule(".link {color:blue; text-decoration: underline;}");
        styleSheet.addRule(".description {font-size: 85%;}");
        styleSheet.addRule(".rule {font-size: 90%; font-weight:bold}");
        styleSheet.addRule(".excerpt {font-size: 90%;}");
    }

    /**
	 * Resets the Text to "Select a node to view details...";
	 */
	public void resetText() {
		StringBuffer temp = new StringBuffer();
		temp.append("<HTML><BODY>");
		temp.append("Select a node to view details...");
		temp.append("</BODY></HTML>");
		textPane.setText(temp.toString());
	}

	/**
	 * Resets the Text to the specified Text String.
	 *
	 * @param text Text String.
	 */
	public void resetText(String text) {
		StringBuffer temp = new StringBuffer();
		temp.append("<html><body>");
		temp.append(text);
		temp.append("</body></html>");
		textPane.setText(temp.toString());
	}

	/**
	 * Shows details about BioPAX Entities 
	 * that were mapped to the selected nodes.
	 * 
	 * @param network
	 * @param nodes
	 */
	public void updateNodeDetails(CyNetwork network, Collection<CyNode> nodes) {
        String s;

		StringBuffer buf = new StringBuffer("<html><body>");
		buf.append("<h2>Details</h2>");
		
		for (CyNode selected : nodes) {			
			CyRow row = network.getRow(selected);
			// name
			s = row.get(CyNetwork.NAME, String.class);
			buf.append("<h3>" + s + "</h3>");

			// type (to the text buffer)
			String type = row.get(BioPaxUtil.BIOPAX_ENTITY_TYPE, String.class);
			if (type != null) {
				buf.append("<h4>");
				buf.append("BioPAX Class: " + type);
				buf.append("</h4>");
			}
			// organism
			s = null;
			s = row.get("entityReference/organism/displayName", String.class);
			if(s == null)
				s = row.get("organism/displayName", String.class);
			if (s != null) {
				buf.append("<h4>");
				buf.append("Organism: " + s);
				buf.append("</h4>");
			}        
			// cellular location
			s = null;
			s = row.get("cellularLocation", String.class);
			if (s != null) {
				buf.append("<h4>");
				buf.append("Cellular Location: " + s);
				buf.append("</h4>");
			}		
			// chemical modification
			addAttributeList(network, selected, null,
					BIOPAX_CHEMICAL_MODIFICATIONS_LIST, "Chemical Modifications:", buf);
			// data source
			addAttributeList(network, selected, null, "dataSource", "Data sources:", buf);        		
			// links
			addLinks(network, selected, buf);
		}
		
		buf.append("</body></html>");
		
		textPane.setText(buf.toString());
		textPane.setCaretPosition(0);
    }


    private void addLinks(CyNetwork network, CyNode node, StringBuffer buf) {
    	CyRow row = network.getRow(node);

        addAttributeList(network, node, CyNetwork.HIDDEN_ATTRS,
                BIOPAX_UNIFICATION_REFERENCES, "Links:", buf);
        addAttributeList(network, node, CyNetwork.HIDDEN_ATTRS,
                BIOPAX_RELATIONSHIP_REFERENCES, null, buf);
        addAttributeList(network, node, CyNetwork.HIDDEN_ATTRS,
                BIOPAX_PUBLICATION_REFERENCES, "Publications:", buf);
         
        addIHOPLinks(network, node, buf);
	}

	private void addAttributeList(CyNetwork network, CyNode node, String tableName, 
			String attribute, String label, StringBuffer buf) 
	{
		StringBuffer displayString = new StringBuffer();
		// use private or default table
		CyRow row = (tableName == null) ? network.getRow(node) : network.getRow(node,tableName);
		if (row.getTable().getColumn(attribute) == null) {
			return;
		}
		
		List<String> list = row.getList(attribute, String.class);
        if (list == null) {
        	return;
        }
        
        int len = list.size();
        boolean tooMany = false;
        if (len > 7) {
            len = 7;
            tooMany = true;
        }
        for (int lc = 0; lc < len; lc++) {
			String listItem = list.get(lc);

			if ((listItem != null) && (listItem.length() > 0)) {
                displayString.append("<li> - " + listItem);
                displayString.append("</li>"); 
			}
		}
        if (tooMany) {
            displayString.append("<li>  ...</li>");
        }

        // do we have a string to display ?
		if (displayString.length() > 0) {
			if(label != null) {
				buf.append("<h4>");
				buf.append(label);
				buf.append("</h4>");
			}
            buf.append ("<ul>");
            buf.append(displayString.toString());
            buf.append ("</ul>");
        }
	}


	private void addIHOPLinks(CyNetwork network, CyNode node, StringBuffer buf) {
		CyRow row = network.getRow(node,CyNetwork.HIDDEN_ATTRS);
		String ihopLinks = row.get(BIOPAX_IHOP_LINKS, String.class);

		if (ihopLinks != null) {
			buf.append("<ul>");
			buf.append(ihopLinks);
			buf.append("</ul>");
		}
	}
}


/**
 * Editor Kit which is capable of handling long words.
 * <p/>
 * Here is a description of the problem:
 * By default, JTextPane uses an InlineView. It was designed to avoid
 * wrapping.  Text can't be broken if it doesn't contain spaces.
 * <p/>
 * This is a real problem with the BioPaxDetailsPanel, as BioPax Unique
 * Identifiers can get really long, and this prevents the user from
 * resizing the CytoPanel to any arbitrary size.
 * <p/>
 * The solution below comes from:
 * http://joust.kano.net/weblog/archives/000074.html
 * <p/>
 * (The following code is released in the public domain.)
 *
 * @author Joust Team.
 */
class MyEditorKit extends HTMLEditorKit {
	private static final long serialVersionUID = 1L;

	/**
	 * Gets the ViewFactor Object.
	 *
	 * @return View Factor Object.
	 */
	public ViewFactory getViewFactory() {
		return new MyViewFactory(super.getViewFactory());
	}

	/**
	 * Word Splitting Paragraph View.
	 */
	private static class WordSplittingParagraphView extends ParagraphView {
		public WordSplittingParagraphView(javax.swing.text.Element elem) {
			super(elem);
		}

		protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
			SizeRequirements sup = super.calculateMinorAxisRequirements(axis, r);
			sup.minimum = 1;

			return sup;
		}
	}

	/**
	 * View Factory.
	 */
	private static class MyViewFactory implements ViewFactory {
		private final ViewFactory parent;

		/**
		 * Constructor.
		 *
		 * @param parent ViewFactory Object.
		 */
		public MyViewFactory(ViewFactory parent) {
			this.parent = parent;
		}

		/**
		 * Creates a Text Element View.
		 *
		 * @param elem Element Object.
		 * @return View Object.
		 */
		public View create(javax.swing.text.Element elem) {
			AttributeSet attr = elem.getAttributes();
			Object name = attr.getAttribute(StyleConstants.NameAttribute);

			if ((name == HTML.Tag.P) || (name == HTML.Tag.IMPLIED)) {
				return new WordSplittingParagraphView(elem);
			}

			return parent.create(elem);
		}
	}
}
