package org.pathwaycommons.cypath2.internal;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import org.cytoscape.work.TaskIterator;

import cpath.query.CPathGetQuery;
import cpath.query.CPathGraphQuery;
import cpath.service.GraphType;
import cpath.service.jaxb.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hit Summary/Details Panel class.
 */
final class HitInfoJTabbedPane extends JTabbedPane {
	private static final long serialVersionUID = 1L;
	private final JTextPane summaryTextPane;
    private final JTextPane detailsTextPane;
    private final HitsModel hitsModel;        
    private SearchHit current; //selected one

	private static final Logger LOG = LoggerFactory.getLogger(HitInfoJTabbedPane.class);


    public HitInfoJTabbedPane(HitsModel hitsModel) {
    	this.hitsModel = hitsModel;
    	
    	//build 'summary' tab
    	JPanel summaryPanel = new JPanel();
    	summaryPanel.setLayout(new BorderLayout());
        summaryTextPane = createSummaryHtmlTextPane();
        JScrollPane scrollPane = new JScrollPane(summaryTextPane);
        summaryPanel.add(scrollPane, BorderLayout.CENTER);         
	    add("Summary", summaryPanel);
	    
	    //build 'details' tab (parent pathways, etc.)
	    JPanel detailsPane = new JPanel();
        detailsPane.setLayout(new BorderLayout());                       
        detailsTextPane = createDetailsHtmlTextPane();
        scrollPane = new JScrollPane(detailsTextPane);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPane.add(scrollPane, BorderLayout.CENTER);
        add("Details", detailsPane);
        
        repaint();
    }


    private JTextPane createDetailsHtmlTextPane() {
        final JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBorder(new EmptyBorder(7,7,7,7));
        textPane.setContentType("text/html");
        textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        textPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
                if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                	//import/create a network (parent pathways) if the link is clicked
               		String uri = hyperlinkEvent.getURL().toString();
               		final CPathGetQuery query = CyPC.client.createGetQuery().sources(Collections.singleton(uri));
               		CyPC.cyServices.taskManager.execute(new TaskIterator(
                   		new NetworkAndViewTask(CyPC.cyServices, query, current.toString())));
                }
            }
        });

        style(textPane);
        
        return textPane;
	}


    private void style(JTextPane textPane) {
        StyleSheet styleSheet = ((HTMLDocument) textPane.getDocument()).getStyleSheet();
        styleSheet.addRule("h2 {color:  #663333; font-size: 102%; font-weight: bold; "
            + "margin-bottom:3px}");
        styleSheet.addRule("h3 {color: #663333; font-size: 95%; font-weight: bold;"
        	+ "margin-bottom:7px}");
        styleSheet.addRule("ul { list-style-type: none; margin-left: 5px; "
            + "padding-left: 1em;	text-indent: -1em;}");
	    styleSheet.addRule("h4 {color: #66333; font-weight: bold; margin-bottom:3px;}");
//	    styleSheet.addRule("b {background-color: #FFFF00;}");
	    styleSheet.addRule(".bold {font-weight:bold;}");
        styleSheet.addRule(".link {color:blue; text-decoration: underline;}");
        styleSheet.addRule(".excerpt {font-size: 90%;}");
        // highlight matching fragments
        styleSheet.addRule(".hitHL {background-color: #FFFF00;}");		
	}

	/**
     * Sets the current item and HTML to display
     *
	 * @param item
	 */
    public synchronized void setCurrentItem(final SearchHit item) {
		String summaryHtml = hitsModel.hitsSummaryMap.get(item.getUri());
		summaryTextPane.setText(summaryHtml);
		summaryTextPane.setCaretPosition(0);

		//get or build the second (details) tab content    		
		String detailsHtml = hitsModel.hitsDetailsMap.get(item.getUri());
		if (detailsHtml != null && !detailsHtml.isEmpty()) {
			detailsTextPane.setText(detailsHtml);
			current = item;
			detailsTextPane.setCaretPosition(0);
			repaint();
		} else {
			detailsTextPane.setText("");
			//get/update info in another thread...
			CyPC.cachedThreadPool.execute(new Runnable() {
				@Override
				public void run() {
					try {
						LOG.debug("CyPathwayCommons, getting current hit's (" + item + ") info from the server...");
						final String html = hitsModel.fetchDetails(item); //runs several web queries
						detailsTextPane.setText(html);
						current = item;
						detailsTextPane.setCaretPosition(0);
						repaint();
					} catch (Throwable e) {
						throw new RuntimeException(e);
					}
				}
			});
		}
	}
    
    /*
     * Creates a JTextPane with correct line wrap settings
     * and hyperlink action.
     */
    private JTextPane createSummaryHtmlTextPane() {
        final JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBorder(new EmptyBorder(7,7,7,7));
        textPane.setContentType("text/html");
        textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        textPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
                if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                	//import/create a network if the link is clicked
               		SearchHit currentItem = current;
               		String uri = hyperlinkEvent.getURL().toString();
               		if(!currentItem.getBiopaxClass().equalsIgnoreCase("Pathway")) {
        	        	//create new 'neighborhood' query; use global organism and datasource filters	
               			final CPathGraphQuery graphQuery = CyPC.client.createGraphQuery()
               				.datasourceFilter(CyPC.options.selectedDatasources())
               				.organismFilter(CyPC.options.selectedOrganisms())
               				.sources(Collections.singleton(uri))
               				.kind(GraphType.NEIGHBORHOOD);
               			CyPC.cyServices.taskManager.execute(new TaskIterator(
                   			new NetworkAndViewTask(CyPC.cyServices, graphQuery, currentItem.toString())));
               		} else { // use '/get' command
               			final CPathGetQuery getQuery = CyPC.client.createGetQuery()
               				.sources(Collections.singleton(uri));
               			CyPC.cyServices.taskManager.execute(new TaskIterator(
                   			new NetworkAndViewTask(CyPC.cyServices, getQuery, currentItem.toString())));
               		}
                }
            }
        });
        
        style(textPane);
        
        return textPane;
    }        

}
