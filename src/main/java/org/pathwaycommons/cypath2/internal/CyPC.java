package org.pathwaycommons.cypath2.internal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;
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
import org.cytoscape.work.TaskIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.client.CPathClient;
import cpath.client.util.CPathException;
import cpath.service.GraphType;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;

/**
 * CyPC: cPath2/PC Web Service client integrated 
 * into the Cytoscape Web Services GUI Framework.
 */
final class CyPC extends AbstractWebServiceGUIClient implements NetworkImportWebServiceClient, SearchWebServiceClient
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CyPC.class);
    
	static final String PROP_CPATH2_SERVER_URL = "cypath2.server.url";
    
	static CPathClient client; // shared stateless cPath2 client
	static CyServices cyServices; //Cy3 services
	static Options options = new Options(); //global query options/filters
    
    static final Map<String,String> uriToOrganismNameMap = new HashMap<String, String>();
    static final Map<String,String> uriToDatasourceNameMap = new HashMap<String, String>();

	static final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
	
	final JList advQueryPanelItemsList;
    
	/**
     * Constructor.
	 * Creates a new Web Services client.
     * 
     * @param displayName app display name
     * @param description app description
     */
    public CyPC(String displayName, String description) 
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
     * Creates the GUI and loads
     * some initial data from the server.
     */
    public void init()  {
    	// init datasources and organisms maps (in a separate thread)
		cachedThreadPool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					SearchResponse res = client.createSearchQuery()
							.typeFilter("Provenance")
							.allPages() //sets .queryString("*") automatically
							.result();
					for (SearchHit bs : res.getSearchHit()) {
						uriToDatasourceNameMap.put(bs.getUri(), bs.getName());
					}
					uriToOrganismNameMap.put("http://identifiers.org/taxonomy/9606", "Homo sapiens");
				} catch (CPathException e) {
					throw new RuntimeException(e);
				}

				// create the UI
				final JTabbedPane tabbedPane = new JTabbedPane();
				tabbedPane.add("Search", createSearchQueryPanel());
				tabbedPane.add("Advanced Query", new AdvancedQueryPanel(advQueryPanelItemsList));
				gui.setPreferredSize(new Dimension(900, 600));
				gui.setLayout(new BorderLayout());
				gui.add(tabbedPane, BorderLayout.CENTER);
			}
		});
    }

	/**
     * Execute a neighborhood graph query and 
     * create a new network and view. 
     * 
     * @param query - only string is accepted - separated by spaces IDs or URIs of bio entities.
     */
	public TaskIterator createTaskIterator(Object query) {
		if(!(query instanceof String))
			throw new IllegalArgumentException("Unsupported query: " + query
				+ " (a string containing a list of ID/URI separated by spaces is required)");
		
		final String[] ids = ((String)query).split("\\s+");
		
		return new TaskIterator(new NetworkAndViewTask(
				cyServices,
				client.createGraphQuery()
					.kind(GraphType.NEIGHBORHOOD)
					.sources(ids)
					.organismFilter(options.selectedOrganisms())
					.datasourceFilter(options.selectedDatasources())
				, null));
	}
	
	/*
	 * Creates a Titled Border with appropriate font settings.
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
	
    /*
	 * Global options panel (search/graph query filters).
	 */
	private Component createOptionsPane() {
		// Initialize the organisms filter-list:
	    // manually add choice(s): only human is currently supported
	    //TODO add more org. as they become suppored/available (from web service)
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

		return filtersPane;
	}

	private Component createSearchQueryPanel() {
		
	   	final HitsModel hitsModel = new HitsModel("Current Search Hits", cyServices.taskManager);

	    // create tabs pane for the hit details and parent pathways sun-panels
	    final HitInfoJTabbedPane currentHitInfoPane = new HitInfoJTabbedPane(hitsModel);
	    currentHitInfoPane.setPreferredSize(new Dimension(300, 250));
	    currentHitInfoPane.setMinimumSize(new Dimension(200, 150));
	        
	  	// create the query field and examples label
	    final String ENTER_TEXT = "Enter a keyword (e.g., gene/protein name or ID)";
	  	final JTextField searchField = new JTextField(ENTER_TEXT.length());
	    searchField.setText(ENTER_TEXT);
	    searchField.setToolTipText(ENTER_TEXT);
	    searchField.addFocusListener(new FocusAdapter() {
	        public void focusGained(FocusEvent focusEvent) {
	            if (searchField.getText() != null && searchField.getText().startsWith("Enter")) {
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
	        "search fields: <em>comment, ecnumber, keyword, name, pathway, term, xrefdb, xrefid, " +
			"dataSource, organism </em>");
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
				new NvpListItem("Top Pathways (have no parents)","Pathway"), //this one is treated specially
	    		new NvpListItem("Pathways","Pathway"),
	    		new NvpListItem("Interactions (all types)", "Interaction"),
	    		new NvpListItem("Participants", "EntityReference")
	    	}
	    );
	    bpTypeComboBox.setSelectedIndex(0); //default value: Top Pathways
	    bpTypeComboBox.setEditable(false);

		final Box searchQueryPanel = Box.createVerticalBox();
		searchQueryPanel.setMinimumSize(new Dimension(400, 100));

	    // create the search button and action
	    final JButton searchButton = new JButton("Search");
	    searchButton.setToolTipText("Full-Text Search");
	    searchButton.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	        	searchButton.setEnabled(false);
				final int selIndex = bpTypeComboBox.getSelectedIndex();
				final NvpListItem selItem = (NvpListItem) bpTypeComboBox.getItemAt(selIndex);
	        	hitsModel.searchFor = selItem.getValue();
	           	final String keyword = searchField.getText();           	
	            if (keyword == null || keyword.trim().length() == 0 || keyword.startsWith(ENTER_TEXT)) {
	            	JOptionPane.showMessageDialog(searchQueryPanel, "Please enter something into the search box.");
	        		searchButton.setEnabled(true);
	        	} else {
	        		info.setText("");
					cachedThreadPool.execute(new Runnable() {
	        			@Override
	        			public void run() {
	        				try {
	        					LOGGER.info("Executing search for " + keyword);
	        					final SearchResponse searchResponse = (selIndex == 0)
									? client.createTopPathwaysQuery() //search query for top pathways, with filters
										.datasourceFilter(options.selectedDatasources())
										.organismFilter(options.selectedOrganisms())
										.queryString(keyword)
										.result()
									: client.createSearchQuery() //other search query with filters
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
		    						SwingUtilities.invokeLater(new Runnable() {
										@Override
										public void run() {
											JOptionPane.showMessageDialog(searchQueryPanel, "No Matches Found");
										}
									});
	        					}
	        				} catch (final Throwable e) {
	        					// using Throwable helps catch unresolved runtime dependency issues!
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										JOptionPane.showMessageDialog(searchQueryPanel, "Error: " + e);
									}
								});
								if(!(e instanceof CPathException)) throw new RuntimeException(e);
								hitsModel.update(new SearchResponse()); //clear
	        				} finally {
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										searchButton.setEnabled(true);
										searchQueryPanel.repaint();
										((Window) searchQueryPanel.getRootPane().getParent()).toFront();
									}
								});
	        				}
	        			}
	        		});
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

		searchQueryPanel.add(createOptionsPane());
		searchQueryPanel.add(Box.createVerticalGlue());
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

	@Override
	protected void finalize() throws Throwable {
		cachedThreadPool.shutdown();
		super.finalize();
	}
}
