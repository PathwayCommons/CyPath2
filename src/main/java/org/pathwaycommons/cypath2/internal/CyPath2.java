package org.pathwaycommons.cypath2.internal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Observer;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.client.CPathClient;
import cpath.client.util.CPathException;
import cpath.query.CPathGetQuery;
import cpath.service.GraphType;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;

/**
 * CyPath2: CPathSquared Web Service client integrated into the Cytoscape Web Services GUI Framework.
 */
final class CyPath2 extends AbstractWebServiceGUIClient 
	implements NetworkImportWebServiceClient, SearchWebServiceClient
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CyPath2.class);
    
	static final String PROP_CPATH2_SERVER_URL = "cypath2.server.url";
    
	static CPathClient client; // shared stateless cPath2 client
	static CyServices cyServices; //Cy3 services
	static Options options; //global query options/filters
    
    static final Map<String,String> uriToOrganismNameMap = new HashMap<String, String>();
    static final Map<String,String> uriToDatasourceNameMap = new HashMap<String, String>();
    
    static final String CPATH_SERVER_NAME_ATTR = "CPATH_SERVER_NAME";
    static final String CPATH_SERVER_URL_ATTR = "CPATH_SERVER_URL";
	
	final JList advQueryPanelItemsList;
    
	/**
     * Creates a new Web Services client.
     * 
     * @param displayName
     * @param description
     * @param cyServices
     */
    public CyPath2(String displayName, String description) 
    {    	   	
    	super(client.getEndPointURL(), displayName, description);
		
        // user items list for the adv. query panel
        advQueryPanelItemsList = new JList(new DefaultListModel());
        advQueryPanelItemsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        advQueryPanelItemsList.setPrototypeCellValue("123456789012345678901234567890123456789012345678901234567890"); 
        //double-click removes item from list
        advQueryPanelItemsList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					if (advQueryPanelItemsList.getSelectedIndex() >= 0) {
						DefaultListModel lm = (DefaultListModel) advQueryPanelItemsList.getModel();
						lm.removeElementAt(advQueryPanelItemsList.getSelectedIndex());
					}
				} 
			}
		});

		gui = new JPanel();
    }
 
    /**
     * Creates the UI and loads 
     * some initial data from the server.
     * @throws CPathException 
     * 
     */
    public void init() throws CPathException {   	
    	// init datasources and org. maps (in a separate thread)
//    	ClassLoaderHack.runWithHack(new Runnable() {			
//			@Override
//			public void run() {
				SearchResponse res;
				try {
					res = client.createSearchQuery()
							.typeFilter("Provenance")
							.allPages() //sets .queryString("*") automatically
							.result();
					for(SearchHit bs : res.getSearchHit()) {
						uriToDatasourceNameMap.put(bs.getUri(), bs.getName());
					}
			        res = client.createSearchQuery()
			        	.typeFilter("BioSource")
			        	.allPages() //sets .queryString("*") automatically
			        	.result();
			        for(SearchHit bs : res.getSearchHit()) {
			        	uriToOrganismNameMap.put(bs.getUri(), bs.getName());
			        }  	
				} catch (CPathException e) {
					throw new RuntimeException(e);
				}
//			}
//		}, com.sun.xml.bind.v2.ContextFactory.class);
    	
        // create the UI
		final JTabbedPane  tabbedPane = new JTabbedPane();
        tabbedPane.add("Search", createSearchQueryPanel());
        tabbedPane.add("Top Pathways", createTopPathwaysPanel());
        tabbedPane.add("Advanced Query", new AdvancedQueryPanel(advQueryPanelItemsList));
        tabbedPane.add("Options", createOptionsPane());        
    	JPanel mainPanel = (JPanel) gui;
        mainPanel.setPreferredSize(new Dimension (900,600));
        mainPanel.setLayout (new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
    }   
    

	/**
     * Execute a neighborhood graph query and 
     * create a new network and view. 
     * 
     * @param query - only string is accepted - separated by spaces IDs or URIs of bioentities.
     */
	public TaskIterator createTaskIterator(Object query) {
		if(!(query instanceof String))
			throw new IllegalArgumentException("Unsupported query: " + query
				+ " (a string containing a list of ID/URI separated by spaces is required)");
		
		final String[] ids = ((String)query).split("\\s+");
		
		return new TaskIterator(new NetworkAndViewTask(cyServices, 
				client.createGraphQuery().kind(GraphType.NEIGHBORHOOD).sources(ids), 
				null));
	}

	
	/**
	 * Creates a Titled Border with appropriate font settings.
	 * 
	 * @param title
	 * @return
	 */
	static Border createTitledBorder(String title) {
		TitledBorder border = BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.RAISED), title);
		Font font = border.getTitleFont();
		if(font != null) { //weird null on a Win7 system
			Font newFont = new Font(font.getFamily(), Font.BOLD, font.getSize() + 1);
			border.setTitleFont(newFont);
		}
		
		border.setTitleColor(new Color(102, 51, 51));
		return border;
	}

	
    /**
	 * Creates the app's options panel.
	 * 
	 * @return
	 */
	Component createOptionsPane() {
	   	JPanel panel = new JPanel();
	   	panel.setLayout(new GridLayout(2, 1));    
	    
		// Initialize the organisms filter-list:
	    // manually add choice(s): only human is currently supported
	    // (other are disease/experimental organisms data)
	    //TODO add more org. as they become suppored/available (from web service, uriToOrganismNameMap) 
	    SortedSet<NvpListItem> items = new TreeSet<NvpListItem>(); //sorted by name
	    items.add(new NvpListItem("Human", "9606"));   
	    DefaultListModel model = new DefaultListModel();
	    for(NvpListItem nvp : items) {
	    	model.addElement(nvp);
	    }
	    options.organismList.setModel(model);	        
	    JScrollPane organismFilterBox = new JScrollPane(options.organismList, 
	    	JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    organismFilterBox.setBorder(new TitledBorder("Organism(s):"));
	    organismFilterBox.setPreferredSize(new Dimension(300, 200));
	    organismFilterBox.setMinimumSize(new Dimension(200, 100));
	        
	    // create the filter-list of datasources available on the server  
	    DefaultListModel dataSourceBoxModel = new DefaultListModel(); 
	    for(String uri : uriToDatasourceNameMap.keySet()) {
	    	String name = uriToDatasourceNameMap.get(uri);
	    	dataSourceBoxModel.addElement(new NvpListItem(name, name));
	    }		        
	    options.dataSourceList.setModel(dataSourceBoxModel);	        
	    JScrollPane dataSourceFilterBox = new JScrollPane(options.dataSourceList, 
	       	JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    dataSourceFilterBox.setBorder(new TitledBorder("Datasource(s):"));
	    dataSourceFilterBox.setPreferredSize(new Dimension(300, 200));
	    dataSourceFilterBox.setMinimumSize(new Dimension(200, 100));

	    JSplitPane filtersPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, dataSourceFilterBox, organismFilterBox);
	    filtersPane.setBorder(new TitledBorder("Global Filters - for all queries, including Top Pathways and " +
	    		"Apps->Extend Network... node view context menu."));
	    filtersPane.setDividerLocation(400);
	    filtersPane.setResizeWeight(0.5f);
	    filtersPane.setAlignmentX(Component.LEFT_ALIGNMENT);
	    filtersPane.setPreferredSize(new Dimension(500, 250));
	    filtersPane.setMinimumSize(new Dimension(250, 200));
	    
	    panel.add(filtersPane);
	    
	    return panel;
	}


	private Component createSearchQueryPanel() {
		
	   	final HitsModel hitsModel = new HitsModel("Current Search Hits", cyServices.taskManager);

	    // create tabs pane for the hit details and parent pathways sun-panels
	    final HitInfoJTabbedPane currentHitInfoPane = new HitInfoJTabbedPane(hitsModel);
	    currentHitInfoPane.setPreferredSize(new Dimension(300, 250));
	    currentHitInfoPane.setMinimumSize(new Dimension(200, 150));
	        
	    final JPanel searchQueryPanel = new JPanel();
	    searchQueryPanel.setMinimumSize(new Dimension(400, 100));
	        
	  	// create the query field and examples label
	    final String ENTER_TEXT = "Enter a keyword (e.g., gene/protein name or ID)";
	  	final JTextField searchField = new JTextField(ENTER_TEXT.length());
	    searchField.setText(ENTER_TEXT);
	    searchField.setToolTipText(ENTER_TEXT);
	    searchField.addFocusListener(new FocusAdapter() {
	        public void focusGained(FocusEvent focusEvent) {
	            if (searchField.getText() != null
	                    && searchField.getText().startsWith("Enter")) {
	                searchField.setText("");
	            }
	        }
	    });   	
	    	
	    searchField.setBorder (BorderFactory.createCompoundBorder(searchField.getBorder(),
	    		new PulsatingBorder(searchField)));
	    searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
	    searchField.setMaximumSize(new Dimension(300, 100));
	        
	    JEditorPane label = new JEditorPane ("text/html", 
	    	"Examples:  <a href='TP53'>TP53</a>, <a href='BRCA1'>BRCA1</a>, <a href='SRY'>SRY</a>, " +
	        "<a href='name:kinase AND pathway:signal*'>name:kinase AND pathway:signal*</a> <br/>" +
	        "search fields: <em>comment, ecnumber, keyword, name, pathway, term, xrefdb, xrefid, dataSource, organism </em>");
	    label.setEditable(false);
	    label.setOpaque(false);
	    label.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
	    label.addHyperlinkListener(new HyperlinkListener() {
	        // Update search box with activated example.
	        public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
	            if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	                searchField.setText(hyperlinkEvent.getDescription());
	            }
	        }
	    });
	    label.setAlignmentX(Component.LEFT_ALIGNMENT);
	    Font font = label.getFont();
	    Font newFont = new Font (font.getFamily(), font.getStyle(), font.getSize()-2);
	    label.setFont(newFont);
	    label.setBorder(new EmptyBorder(5,3,3,3));
	    label.setAlignmentX(Component.LEFT_ALIGNMENT);
	     
	    final JLabel info = new JLabel("", SwingConstants.LEFT);
	    info.setFocusable(false);
	    info.setFont(new Font(info.getFont().getFamily(), info.getFont().getStyle(), info.getFont().getSize()+1));
	    info.setForeground(Color.BLUE);
	    info.setMaximumSize(new Dimension(400, 50));        

	    //BioPAX sub-class combo-box ('type' filter values)
	    final JComboBox bpTypeComboBox = new JComboBox(
	    	new NvpListItem[] {
	    		new NvpListItem("Pathways","Pathway"),
	    		new NvpListItem("Interactions (all types)", "Interaction"),
	    		new NvpListItem("Entity states (form, location)", "PhysicalEntity"),
	    		new NvpListItem("Entity references (mol. classes)", "EntityReference")
	    	}
	    );
	    bpTypeComboBox.setSelectedIndex(0); //default value: Pathway
	    bpTypeComboBox.setEditable(false);
	        
	    // create the search button and action
	    final JButton searchButton = new JButton("Search");
	    searchButton.setToolTipText("Full-Text Search");
	    searchButton.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	        	searchButton.setEnabled(false);
	        	hitsModel.searchFor = ((NvpListItem)bpTypeComboBox.getSelectedItem()).getValue();
	           	final String keyword = searchField.getText();           	
	            if (keyword == null || keyword.trim().length() == 0 || keyword.startsWith(ENTER_TEXT)) {
	            	JOptionPane.showMessageDialog(searchQueryPanel, "Please enter something into the search box.");
	        		searchButton.setEnabled(true);
	        	} else {
	        		info.setText("");
	        		Task search = new Task() {
	        			@Override
	        			public void run(TaskMonitor taskMonitor) throws Exception {
	        				try {
	        					taskMonitor.setProgress(0);
	        					taskMonitor.setStatusMessage("Executing search for " + keyword);        					
	        					final SearchResponse searchResponse = client.createSearchQuery()
	        							.typeFilter(hitsModel.searchFor)
	        							.datasourceFilter(options.selectedDatasources())
	        							.organismFilter(options.selectedOrganisms())
	        							.queryString(keyword)
	        							.result();
	        					if(searchResponse != null) {
	        						// update hits model (make summaries, notify observers!)
	        						hitsModel.update(searchResponse);
	        						info.setText("Matches:  " + searchResponse.getNumHits() 
	        							+ "; retrieved: " + searchResponse.getSearchHit().size()
	        							+ " (page #" + searchResponse.getPageNo() + ")");
	        					} else {
	        						info.setText("No Matches Found");
	        						JOptionPane.showMessageDialog(searchQueryPanel, "No Matches Found");
	        					}
	        				} catch (CPathException e) {
	        					JOptionPane.showMessageDialog(searchQueryPanel, "Error: " + e);
								hitsModel.update(new SearchResponse()); //clear
	        				} catch (Throwable e) { 
	        					// using Throwable helps catch unresolved runtime dependency issues!
	        					JOptionPane.showMessageDialog(searchQueryPanel, "Error: " + e);
	        					throw new RuntimeException(e);
	        				} finally {
	        					taskMonitor.setStatusMessage("Done");
	        					taskMonitor.setProgress(1);
	        					searchButton.setEnabled(true);
	        					Window parentWindow = ((Window) searchQueryPanel.getRootPane().getParent());
	        					searchQueryPanel.repaint();
	        					parentWindow.toFront();
	        				}
	        			}

	        			@Override
	        			public void cancel() {
	        			}
	        		};

	        		cyServices.taskManager.execute(new TaskIterator(search));
	        	}	             	
	        }
	    });
	    searchButton.setAlignmentX(Component.LEFT_ALIGNMENT); 
        
	    final JPanel keywordPane = new JPanel();
        keywordPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        keywordPane.add(label);
        keywordPane.add(searchField);
        keywordPane.add(bpTypeComboBox);
        keywordPane.add(searchButton);
        keywordPane.setPreferredSize(new Dimension(400, 100));
        keywordPane.setMinimumSize(new Dimension(400, 100));
    
		searchQueryPanel.setLayout(new BoxLayout(searchQueryPanel, BoxLayout.X_AXIS));
        searchQueryPanel.add(keywordPane);
        
        // Assembly the results panel
    	final JPanel searchResultsPanel = new JPanel();
    	searchResultsPanel.setMinimumSize(new Dimension(400, 350));
    	searchResultsPanel.setLayout(new BoxLayout(searchResultsPanel, BoxLayout.Y_AXIS));
    	searchResultsPanel.add(info); 
        
        // search hits list
        final JList resList = new ToolTipsSearchHitsJList();
        resList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resList.setPrototypeCellValue("12345678901234567890");
        // define a list item selection listener which updates the details panel, etc..
        resList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                int selectedIndex = resList.getSelectedIndex();
                //  ignore the "unselect" event.
//                if (!listSelectionEvent.getValueIsAdjusting()) {
                    if (selectedIndex >=0) {
                    	SearchHit item = (SearchHit)resList.getModel().getElementAt(selectedIndex);
                		// show current hit's summary
                    	currentHitInfoPane.setCurrentItem(item);   
                    }
//                }
            }
        });
        //double-click adds the item to the list for a adv. query
        resList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int selectedIndex = resList.getSelectedIndex();
					if (selectedIndex >= 0) {
						SearchHit hit = (SearchHit) resList.getModel().getElementAt(selectedIndex);
						//using hit.toString instead of getName works when there is no name or there are XML-encoded symbols
						StringBuilder sb = new StringBuilder(hit.getBiopaxClass());
						sb.append(": ").append(hit.toString());
						if(hit.getName() != null) //otherwise, toString already shows URI instead of name
							sb.append(" (uri: " + hit.getUri() + ")");
						NvpListItem nvp = new NvpListItem(sb.toString(), hit.getUri());
						DefaultListModel lm = (DefaultListModel) advQueryPanelItemsList.getModel();
						if(!lm.contains(nvp))
							lm.addElement(nvp);
					}
				}
			}
		});  
	        
        // register the JList as model's observer
        hitsModel.addObserver((Observer) resList);
        
        JPanel hitListPane = new JPanel();
        hitListPane.setMinimumSize(new Dimension(300, 150));
        hitListPane.setLayout(new BorderLayout());
        JScrollPane hitListScrollPane = new JScrollPane(resList);
        hitListScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        hitListScrollPane.setBorder(createTitledBorder("Double-click adds it to Advanced Query page."));
        hitListScrollPane.setPreferredSize(new Dimension(300, 150));
        // make (north) tabs       
        JSplitPane vSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, hitListScrollPane, currentHitInfoPane);
        vSplit.setDividerLocation(150);
        vSplit.setResizeWeight(0.5f);
        hitListPane.add(vSplit, BorderLayout.CENTER);
	        
        //  Create search results extra filtering panel
        HitsFilterPanel filterPanel = new HitsFilterPanel(resList, hitsModel, true, false, true);
        filterPanel.setMinimumSize(new Dimension(250, 300));
	    filterPanel.setPreferredSize(new Dimension(300, 400));
        //  Create the Split Pane
        JSplitPane hSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, filterPanel, hitListPane);
        hSplit.setDividerLocation(250);
        hSplit.setResizeWeight(0.33f);
        hSplit.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchResultsPanel.add(hSplit);
    	        
        // final top-bottom panels arrange -
        JSplitPane queryAndResults = new JSplitPane(JSplitPane.VERTICAL_SPLIT, searchQueryPanel, searchResultsPanel);
        queryAndResults.setResizeWeight(0.25f);
        queryAndResults.setDividerLocation(150);

        return queryAndResults; //panel;
    }
	
	
	private JPanel createTopPathwaysPanel() {
			
		final JPanel panel = new JPanel(); // to return       
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    	// hits model is used both by the filters panel pathways jlist
    	final HitsModel topPathwaysModel = new HitsModel("Top Pathways", cyServices.taskManager);        
        // make (south) tabs
        final HitInfoJTabbedPane southPane = new HitInfoJTabbedPane(topPathwaysModel);
        
    	// create top pathways list
    	final TopPathwaysJList tpwJList = new TopPathwaysJList();
    	// as for current version, enable only the filter by data source (type is always Pathway, organisms - human + unspecified)
    	final HitsFilterPanel filterPanel = new HitsFilterPanel(tpwJList, topPathwaysModel, false, false, true);
        tpwJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tpwJList.setPrototypeCellValue("12345678901234567890");
        // define a list item selection listener which updates the details panel, etc..
        tpwJList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
            	TopPathwaysJList l = (TopPathwaysJList) listSelectionEvent.getSource();
                int selectedIndex = l.getSelectedIndex();
                //  ignore the "unselect" event.
//                if (!listSelectionEvent.getValueIsAdjusting()) {
                    if (selectedIndex >=0) {
                    	SearchHit item = (SearchHit) l.getModel().getElementAt(selectedIndex);
                		// update hit's summary/details pane
                		southPane.setCurrentItem(item);				
                    }
//                }
            }
        });
		tpwJList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int selectedIndex = tpwJList.getSelectedIndex();
					// ignore the "unselect" event.
					if (selectedIndex >= 0) {
						SearchHit item = (SearchHit) tpwJList.getModel().getElementAt(selectedIndex);
						String uri = item.getUri();
						final CPathGetQuery getQuery = client.createGetQuery().sources(new String[]{uri});
						cyServices.taskManager.execute(new TaskIterator(
				        	new NetworkAndViewTask(cyServices, getQuery, item.toString())));	
					}
				}
			}
		});
		// add the list to model's observers
        topPathwaysModel.addObserver(tpwJList);
        
        JPanel tpwFilterListPanel = new JPanel();
        tpwFilterListPanel.setBorder(createTitledBorder("Type to quickly find a pathway. " +
       		"Double-click to download (create a network)."));
        tpwFilterListPanel.setLayout(new BorderLayout());
        JScrollPane tpwListScrollPane = new JScrollPane(tpwJList);
        tpwListScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        //add the filter text field and scroll pane
        tpwFilterListPanel.add(tpwJList.getFilterField(), BorderLayout.NORTH);
        tpwFilterListPanel.add(tpwListScrollPane, BorderLayout.CENTER);
	        
        JSplitPane vSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tpwFilterListPanel, southPane);
        vSplit.setDividerLocation(300);
        vSplit.setResizeWeight(0.5f);
	        
        //  Create the Split Pane
        JSplitPane hSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, filterPanel, vSplit);
        hSplit.setDividerLocation(300);
        hSplit.setAlignmentX(Component.LEFT_ALIGNMENT);
        hSplit.setResizeWeight(0.33f);
        
	    // create the update button and action
	    final JButton updateButton = new JButton("Update Top Pathways");
	    updateButton.setToolTipText("Get/Update Top Pathways (From the Server)");
	    updateButton.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	        	updateButton.setEnabled(false);	        			
	            // load pathways from server
	    		TaskIterator taskIterator = new TaskIterator(new AbstractTask() {
	    			@Override
	    			public void run(TaskMonitor taskMonitor) throws Exception {
	    				try {
	    					taskMonitor.setTitle("CyPath2 Top Pathways");
	    					taskMonitor.setProgress(0.1);
	    					taskMonitor.setStatusMessage("Getting top pathways from the server...");
	    					final SearchResponse resp = client.createTopPathwaysQuery()
	    						.organismFilter(options.selectedOrganisms())
	    							.datasourceFilter(options.selectedDatasources())
	    								.result();
	    					// reset the model and kick off observers (list and filter panel)
	    					if(resp != null)
	    						topPathwaysModel.update(resp);	
	    					else {
	    						taskMonitor.setStatusMessage("Not Found");
	    						JOptionPane.showMessageDialog(panel, "No Matches Found");
	    					}
	    				} catch (Throwable e) { 
	    					//fail on both when there is no data (server error) and runtime/osgi errors
	    					throw new RuntimeException(e);
	    				} finally {
	    					taskMonitor.setStatusMessage("Done");
	    					taskMonitor.setProgress(1.0);
	    					Window parentWindow = ((Window) panel.getRootPane().getParent());
	    					panel.repaint();
	    					parentWindow.toFront();
	    					updateButton.setEnabled(true);
	    				}
	    			}
	    		});			
	    		// kick off the task execution
	    		cyServices.taskManager.execute(taskIterator);
	        }
	    });
	    updateButton.setAlignmentX(Component.LEFT_ALIGNMENT); 
               
        panel.add(updateButton);
        panel.add(hSplit);	        
        panel.repaint();
        
        return panel;
	}
    
}
