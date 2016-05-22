package org.pathwaycommons.cypath2.internal;

import cpath.client.CPathClient;
import cpath.client.util.CPathException;
import cpath.service.GraphType;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import org.biopax.paxtools.pattern.util.Blacklist;
import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;
import org.cytoscape.work.TaskIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * App: cPath2/PC Web Service client integrated
 * into the Cytoscape Web Services GUI Framework.
 */
final class App extends AbstractWebServiceGUIClient implements NetworkImportWebServiceClient, SearchWebServiceClient
{
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    
	static final String PROP_CPATH2_SERVER_URL = "cypath2.server.url";
    
	static CPathClient client; // shared stateless cPath2 client
	static Blacklist blacklist; // for the SIF converter, to avoid ubiquitous small molecules
	static CyServices cyServices; //Cy3 services
	static AppOptions options = new AppOptions(); //global query options/filters
	static BiopaxVisualStyleUtil visualStyleUtil;
    
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
    public App(String displayName, String description)
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

		visualStyleUtil = new BiopaxVisualStyleUtil(
				cyServices.visualStyleFactory, cyServices.mappingManager,
				cyServices.discreteMappingFunctionFactory, cyServices.passthroughMappingFunctionFactory);
		visualStyleUtil.init(); //important

		blacklist = new Blacklist(getClass().getResourceAsStream("/blacklist.txt"));

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
				tabbedPane.add("Find and Get", createSearchQueryPanel());
				tabbedPane.add("Graph Queries", new QueryPanel(advQueryPanelItemsList));
				gui.setPreferredSize(new Dimension(800, 600));
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
				client.createGraphQuery()
					.kind(GraphType.NEIGHBORHOOD)
					.direction(CPathClient.Direction.UNDIRECTED)
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
	private JComponent createOptionsPane() {
	    // create the filter-list of the data sources available on the PC server
	    DefaultListModel dataSourceBoxModel = new DefaultListModel(); 
	    for(String uri : uriToDatasourceNameMap.keySet()) {
	    	String name = uriToDatasourceNameMap.get(uri);
	    	dataSourceBoxModel.addElement(new NvpListItem(name, name));
	    }		        
	    options.dataSourceList.setModel(dataSourceBoxModel);	        
	    JScrollPane dataSourceFilterBox = new JScrollPane(options.dataSourceList, 
	       	JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	    dataSourceFilterBox.setBorder(new TitledBorder("Datasources:"));
		dataSourceFilterBox.setMaximumSize(new Dimension(300,300));

		// Init the organisms filter-list:
		// manually add items: only Homo sapiens, 9606 is currently supported
		//TODO add species as they become suppored by the PC web service
		SortedSet<NvpListItem> items = new TreeSet<NvpListItem>(); //sorted by name
		items.add(new NvpListItem("Homo sapiens", "9606"));
		DefaultListModel model = new DefaultListModel();
		for(NvpListItem nvp : items) {
			model.addElement(nvp);
		}
		options.organismList.setModel(model);
		JScrollPane organismFilterBox = new JScrollPane(options.organismList,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		organismFilterBox.setBorder(new TitledBorder("Organisms:"));
		organismFilterBox.setMaximumSize(new Dimension(150,300));
		organismFilterBox.setMinimumSize(new Dimension(100,50));

	    final JSplitPane filtersPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, dataSourceFilterBox, organismFilterBox);
	    filtersPane.setBorder(new TitledBorder(
			"Global filters for all queries, including node view context menu: Apps->Extend Network..."));
	    filtersPane.setDividerLocation(-1);
	    filtersPane.setResizeWeight(0.67f);
		filtersPane.setMaximumSize(new Dimension(450, 300));
		filtersPane.setMinimumSize(new Dimension(300, 200));
		filtersPane.setPreferredSize(new Dimension(300, 200));

		return filtersPane;
	}

	private Component createSearchQueryPanel() {

	  	// create the query field and examples label
	    final String ENTER_TEXT = "a keyword (e.g., gene name or identifier)";
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
	    searchField.setBorder (BorderFactory
				.createCompoundBorder(searchField.getBorder(), new PulsatingBorder(searchField)));
		searchField.setHorizontalAlignment(JTextField.LEFT);

	    JEditorPane label = new JEditorPane ("text/html", 
	    	"Example queries:  <a href='TP53'>TP53</a>, <a href='BRCA1'>BRCA1</a>, <a href='SRY'>SRY</a>, " +
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
	    Font font = label.getFont();
	    Font newFont = new Font (font.getFamily(), font.getStyle(), font.getSize()-2);
	    label.setFont(newFont);
	    label.setBorder(new EmptyBorder(5,3,3,3));

		final JLabel info = new JLabel(""); //, SwingConstants.LEFT);
	    info.setFocusable(false);
	    info.setFont(new Font(info.getFont().getFamily(), info.getFont().getStyle(), info.getFont().getSize()+1));
	    info.setForeground(Color.BLUE);

	    // create the search hits model
		final HitsModel hitsModel = new HitsModel("Search Hits", cyServices.taskManager);
		// search hits list
		final JList resList = new ToolTipsSearchHitsJList();
		// register the JList as model's observer
		hitsModel.addObserver((Observer) resList);

		final JScrollPane hitListScrollPane = new JScrollPane(resList);
		hitListScrollPane.setBorder(createTitledBorder("Result"));

		//BioPAX sub-class combo-box ('type' filter values)
		final JComboBox bpTypeComboBox = new JComboBox(
				new NvpListItem[] {
						new NvpListItem("Top Pathways",""), //this one is treated specially
						new NvpListItem("Pathways","Pathway"),
						new NvpListItem("Interactions (all types)", "Interaction"),
						new NvpListItem("Participants", "EntityReference")
				}
		);
		bpTypeComboBox.setSelectedIndex(0); //default value: Top Pathways
		bpTypeComboBox.setEditable(false);

		// Create the search button and action
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
	            	JOptionPane.showMessageDialog(gui, "Type something in the search box.");
	        		searchButton.setEnabled(true);
	        	} else {
	        		info.setText("");
					cachedThreadPool.execute(new Runnable() {
	        			@Override
	        			public void run() {
							SearchResponse searchResponse = null;
	        				try {
	        					LOGGER.info("Executing search for " + keyword);
	        					searchResponse = (selIndex == 0)
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

								// update hits model (make summaries, notify observers!)
								hitsModel.update(searchResponse);
								String numHitsMsg = "Total hits:  " + searchResponse.getNumHits();
								int n = searchResponse.getSearchHit().size();
								if(searchResponse.getNumHits() > n) {
									//TODO: pagination - 'prev','next' buttons; store current pageNo in hitsModel)
									int p = searchResponse.getPageNo();
									int npp = searchResponse.getMaxHitsPerPage();
									numHitsMsg += String.format(" (top %d..%d are shown)", p*npp, p*npp+n);
								}
								info.setText(numHitsMsg);

								//set the title for the results list pane
								if(hitsModel.searchFor.equalsIgnoreCase("EntityReference")) {
									hitListScrollPane.setBorder(createTitledBorder(
										"Matching participants (double-click to add to Graph Queries)"));
								}
								else if (hitsModel.searchFor.isEmpty()){
									hitListScrollPane.setBorder(createTitledBorder(
										"Matching top (root) pathways"));
								}
								else {
									hitListScrollPane.setBorder(createTitledBorder(
										"Matching " + hitsModel.searchFor.toLowerCase() + "s"));
								}
							} catch (Throwable e) {
								// can fail due to a proxy returned wrong response (500 instead of PC's 460)
								// (using Throwable helps catch unresolved transitive dependency, etc., exceptions)
								if(e instanceof CPathException) {
									SwingUtilities.invokeLater(new Runnable() {
										@Override
										public void run() {
											JOptionPane.showMessageDialog(gui, "No results (try again)");
										}
									});
								} else {
//									LOGGER.error("Search action failed; ", e);
									throw new RuntimeException("Search action failed; ", e);
								}
							} finally {
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										searchButton.setEnabled(true);
										gui.repaint();
										((Window) searchButton.getRootPane().getParent()).toFront();
									}
								});
							}
						}
	        		});
	        	}
	        }
	    });

		// create a tabs pane for the hit details
		final HitInfoJTabbedPane currentHitInfoPane = new HitInfoJTabbedPane(hitsModel);
		currentHitInfoPane.setPreferredSize(new Dimension(300, 200));

		//complete the results (hits) list initialization
        resList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resList.setPrototypeCellValue("12345678901234567890");
        // define a list item selection listener which updates the details panel, etc..
		resList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent listSelectionEvent) {
				int selectedIndex = resList.getSelectedIndex();
				if (selectedIndex >=0) {
					SearchHit item = (SearchHit)resList.getModel().getElementAt(selectedIndex);
					// show current hit's summary
					currentHitInfoPane.setCurrentItem(item);
				}
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

        // make (north) tabs
        final JSplitPane vSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, hitListScrollPane, currentHitInfoPane);
        vSplit.setDividerLocation(-1);
        vSplit.setResizeWeight(0.5f);

        // Create search results extra filtering panel
        HitsFilterPanel filterPanel = new HitsFilterPanel(resList);
		filterPanel.organismFilterEnabled = false; //TODO disabled as long as we have onlu human data
		filterPanel.datasourceFilterEnabled = true;
		filterPanel.typeFilterEnabled = true;

		// and add it as an Observer for the hits model (the Observable)
		hitsModel.addObserver(filterPanel);

        //  Create the search hits view split Pane
        JSplitPane hSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, filterPanel, vSplit);
        hSplit.setDividerLocation(-1);
        hSplit.setResizeWeight(0.33f);

		// Assembly and align everything...
		// query fields and examples
		Box queryBox = Box.createVerticalBox();
		bpTypeComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		bpTypeComboBox.setPreferredSize(new Dimension(250,30));
		bpTypeComboBox.setMaximumSize(new Dimension(400,30));
		queryBox.add(bpTypeComboBox);
		searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
		searchField.setMaximumSize(new Dimension(400,50));
		queryBox.add(searchField);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		queryBox.add(label);
		searchButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		queryBox.add(searchButton);
		queryBox.setPreferredSize(new Dimension(300,300));
		//query fields and global filters:
		JSplitPane queryAndFilters = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, queryBox, createOptionsPane());
		queryAndFilters.setAlignmentX(Component.LEFT_ALIGNMENT);
		queryAndFilters.setDividerLocation(-1);
		queryAndFilters.setResizeWeight(0.33f);

		//info and results parts:
		final Box results = Box.createVerticalBox();
		info.setAlignmentX(Component.LEFT_ALIGNMENT);
		results.add(info);
		hSplit.setAlignmentX(Component.LEFT_ALIGNMENT);
		results.add(hSplit);

		//finally
		JSplitPane searchPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, queryAndFilters, results);
		searchPanel.setPreferredSize(new Dimension(800, 600));
		searchPanel.setMinimumSize(new Dimension(600,400));
		return searchPanel;
    }

	@Override
	protected void finalize() throws Throwable {
		cachedThreadPool.shutdown();
		super.finalize();
	}
}
