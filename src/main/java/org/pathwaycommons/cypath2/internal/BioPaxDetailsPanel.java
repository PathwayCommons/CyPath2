package org.pathwaycommons.cypath2.internal;

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
	
    static final String BIOPAX_CHEMICAL_MODIFICATIONS_LIST = "CHEMICAL_MODIFICATIONS";
    static final String BIOPAX_UNIFICATION_REFERENCES = "UNIFICATION_REFERENCES";
    static final String BIOPAX_RELATIONSHIP_REFERENCES = "RELATIONSHIP_REFERENCES";
    static final String BIOPAX_PUBLICATION_REFERENCES = "PUBLICATION_REFERENCES";
    static final String BIOPAX_IHOP_LINKS = "IHOP_LINKS";	
	
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
		StringBuffer buf = new StringBuffer("<html><body>");
		buf.append("<dl>");
		for (CyNode selected : nodes) {			
			CyRow row = network.getRow(selected);
			// name
			String s = row.get(CyNetwork.NAME, String.class);
			buf.append("<dt><strong>").append(s).append("</strong>");
			// type (to the text buffer)
			String type = row.get(BioPaxUtil.BIOPAX_ENTITY_TYPE, String.class);
			if (type != null) {
				buf.append(" (BioPAX: <em>").append(type).append("</em>)");
			}
			buf.append("</dt>").append("<dd>").append("<dl>");
			// organism
			s = null;
			s = row.get("entityReference/organism/displayName", String.class);
			if(s == null)
				s = row.get("organism/displayName", String.class);
			if (s != null) {
				buf.append("<dt>").append("Organism").append("</dt>")
				.append("<dd>").append(s).append("</dd>");
			}        
			// cellular location
			s = null;
			s = row.get("cellularLocation", String.class);
			if (s != null) {
				buf.append("<dt>").append("Cellular Location").append("</dt>")
				.append("<dd>").append(s).append("</dd>");
			}
			
			// chemical modification
			addAttributeList(network, selected, null,
					BIOPAX_CHEMICAL_MODIFICATIONS_LIST, "Chemical Modifications:", buf);
			// data source
			addAttributeList(network, selected, null, "dataSource", "Data sources:", buf);        		
			// links
			addLinks(network, selected, buf);
			buf.append("</dl>");
			buf.append("</dd><hr/>");			
		}
		
		buf.append("</dl>");
		buf.append("</body></html>");
		
		textPane.setText(buf.toString());
		textPane.setCaretPosition(0);
    }


    private void addLinks(CyNetwork network, CyNode node, StringBuffer buf) {
        addAttributeList(network, node, CyNetwork.HIDDEN_ATTRS,
                BIOPAX_UNIFICATION_REFERENCES, "Unification Xrefs:", buf);
        addAttributeList(network, node, CyNetwork.HIDDEN_ATTRS,
                BIOPAX_RELATIONSHIP_REFERENCES, "Relationship Xrefs:", buf);
        addAttributeList(network, node, CyNetwork.HIDDEN_ATTRS,
                BIOPAX_PUBLICATION_REFERENCES, "Publications:", buf);
         
        addIHOPLinks(network, node, buf);
	}

    /*
     * 
     * @param network
     * @param node
     * @param tableName attributes table name, e.g., CyNetwork.HIDDEN_ATTRS; if null - the default is used
     * @param attribute
     * @param label
     * @param buf
     */
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
			buf.append("<dt>").append(label).append("</dt>");
           	buf.append("<dd><ul>").append(displayString.toString()).append("</ul></dd>");
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
 *
 * Here is a description of the problem:
 * By default, JTextPane uses an InlineView. It was designed to avoid
 * wrapping.  Text can't be broken if it doesn't contain spaces.
 *
 * This is a real problem with the BioPaxDetailsPanel, as BioPax Unique
 * Identifiers can get really long, and this prevents the user from
 * resizing the CytoPanel to any arbitrary size.
 *
 * The solution below comes from:
 * http://joust.kano.net/weblog/archives/000074.html
 *
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
