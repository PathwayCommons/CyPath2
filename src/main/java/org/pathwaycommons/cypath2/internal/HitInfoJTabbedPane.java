package org.pathwaycommons.cypath2.internal;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Collections;

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

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import cpath.query.CPathGetQuery;
import cpath.query.CPathGraphQuery;
import cpath.service.GraphType;
import cpath.service.jaxb.SearchHit;

/**
 * Hit Summary/Details Panel class.
 */
final class HitInfoJTabbedPane extends JTabbedPane {
	private static final long serialVersionUID = 1L;
	private final JTextPane summaryTextPane;
    private final JTextPane detailsTextPane;
    private final HitsModel hitsModel;        
    private SearchHit current; //selected one

    /**
     * Constructor.
     * @param browser 
     */
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
               		final CPathGetQuery query = CyPath2.client.createGetQuery().sources(Collections.singleton(uri));
               		CyPath2.cyServices.taskManager.execute(new TaskIterator(
                   		new NetworkAndViewTask(CyPath2.cyServices, query, current.toString())));
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
		if(detailsHtml != null && !detailsHtml.isEmpty())   		
			detailsTextPane.setText(detailsHtml);
		else { //if(detailsTextPane.isVisible()) {
			detailsTextPane.setText("");
			TaskIterator taskIterator = new TaskIterator(new AbstractTask() {
				@Override
				public void run(TaskMonitor taskMonitor) throws Exception {
					try {
						taskMonitor.setTitle("CyPath2 auto-query");
						taskMonitor.setProgress(0.1);
						taskMonitor.setStatusMessage("Getting current hit's (" + item 
								+ ") info from the server...");
						final String html = hitsModel.fetchDetails(item);
						detailsTextPane.setText(html);
					} catch (Throwable e) { 
						//fail on server error and runtime/osgi error
						throw new RuntimeException(e);
					} finally {
						taskMonitor.setStatusMessage("Done");
						taskMonitor.setProgress(1.0);
					}
				}
			});			
			// kick off the task execution
			CyPath2.cyServices.taskManager.execute(taskIterator);
		}
		
		current = item;
		detailsTextPane.setCaretPosition(0);
		repaint();
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
               			final CPathGraphQuery graphQuery = CyPath2.client.createGraphQuery()
               				.datasourceFilter(CyPath2.options.selectedDatasources())
               				.organismFilter(CyPath2.options.selectedOrganisms())
               				.sources(Collections.singleton(uri))
               				.kind(GraphType.NEIGHBORHOOD);
               			CyPath2.cyServices.taskManager.execute(new TaskIterator(
                   			new NetworkAndViewTask(CyPath2.cyServices, graphQuery, currentItem.toString())));
               		} else { // use '/get' command
               			final CPathGetQuery getQuery = CyPath2.client.createGetQuery()
               				.sources(Collections.singleton(uri));
               			CyPath2.cyServices.taskManager.execute(new TaskIterator(
                   			new NetworkAndViewTask(CyPath2.cyServices, getQuery, currentItem.toString())));
               		}
                }
            }
        });
        
        style(textPane);
        
        return textPane;
    }        

}
