package org.cytoscape.cpathsquared.internal;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

/**
 * CPathSquared Web Service UI, integrated into the Cytoscape Web Services Framework.
 */
public final class CpsWebServiceGuiClient extends AbstractWebServiceGUIClient 
	implements NetworkImportWebServiceClient, SearchWebServiceClient
{
	// Display name of this client.
    private static final String DISPLAY_NAME = CpsFactory.SERVER_NAME + " Client";

    
    //TODO where it is used?..
    /**
     * 
     * Creates a new network and view using data returned 
     * from a 'get' or 'graph' web service query URL (with all parameters set)
     */
	@Override
	public TaskIterator createTaskIterator(final Object cpathSquaredQueryUrl) {

		TaskIterator taskIterator = new TaskIterator(new Task() {
			@Override
			public void run(TaskMonitor taskMonitor) throws Exception {
				CpsFactory.execute(new TaskIterator(
					new CreateNetworkAndViewTask((String) cpathSquaredQueryUrl,
						CpsFactory.downloadMode, CpsFactory.SERVER_NAME)), null);
			}

			@Override
			public void cancel() {
			}
		});

		return taskIterator;
	}
    
    /**
     * Creates a new Web Services client.
     */
    public CpsWebServiceGuiClient() {
    	super(CpsFactory.SERVER_URL, DISPLAY_NAME, makeDescription());
    	
        JTabbedPane tabbedPane = new JTabbedPane();
    	JPanel searchPanel = CpsFactory.createSearchPanel();
        tabbedPane.add("Search", searchPanel);
        tabbedPane.add("Top Pathways", CpsFactory.createTopPathwaysPanel());
        tabbedPane.add("Options", CpsFactory.createOptionsPane());
        
    	JPanel mainPanel = new JPanel();
        mainPanel.setPreferredSize(new Dimension (500,400));
        mainPanel.setLayout (new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);       
        searchPanel.requestFocusInWindow();

    	gui = mainPanel;
    }

    private static String makeDescription() {
        String desc = CpsFactory.INFO_ABOUT;
        desc = desc.replaceAll("<span class='bold'>", "<B>");
        desc = desc.replaceAll("</span>", "</B>");
        return "<html><body>" + desc + "</body></html>";
	}

}

