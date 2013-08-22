package org.pathwaycommons.cypath2.internal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observer;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
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
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;
import org.cytoscape.util.swing.CheckBoxJList;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.client.CPathClient;
import cpath.client.CPathClient.Direction;
import cpath.client.util.CPathException;
import cpath.query.CPathGetQuery;
import cpath.query.CPathGraphQuery;
import cpath.service.GraphType;
import cpath.service.OutputFormat;
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
    
    static OutputFormat downloadMode = OutputFormat.BIOPAX; 
	static CPathClient client;
    
    static final Map<String,String> uriToOrganismNameMap = new HashMap<String, String>();
    static final Map<String,String> uriToDatasourceNameMap = new HashMap<String, String>();
    
    static final String CPATH_SERVER_NAME_ATTR = "CPATH_SERVER_NAME";
    static final String CPATH_SERVER_URL_ATTR = "CPATH_SERVER_URL";
	
	private final JPanel advQueryPanel;
	private final CheckBoxJList organismList;
	private final CheckBoxJList dataSourceList; 
	private final CyServices cyServices;
    
	/**
     * Creates a new Web Services client.
     * 
     * @param displayName
     * @param description
     * @param cyServices
     */
    public CyPath2(String displayName, String description, CyServices cyServices) 
    {    	   	
    	super(client.getEndPointURL(), displayName, description);
    	
    	this.cyServices = cyServices;
		
		//filter value lists
		organismList = new CheckBoxJList();
		dataSourceList = new CheckBoxJList(); 		
		advQueryPanel = new JPanel(new BorderLayout());

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
        tabbedPane.add("Search", createSearchPanel());
        tabbedPane.add("Top Pathways", createTopPathwaysPanel());
        tabbedPane.add("Advanced Query", advQueryPanel);
        tabbedPane.add("Options", createOptionsPane());        
    	JPanel mainPanel = (JPanel) gui;
        mainPanel.setPreferredSize(new Dimension (900,600));
        mainPanel.setLayout (new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
    }   
    

	/**
     * Creates a new network and view using data returned 
     * from the cpath2 '/get' by URI (or bio-identifier) query.
     */
	@Override
	public TaskIterator createTaskIterator(final Object uri) {

		TaskIterator taskIterator = new TaskIterator(new Task() {
			@Override
			public void run(TaskMonitor taskMonitor) throws Exception {
				cyServices.taskManager.setExecutionContext(null);
				final CPathGetQuery query = client.createGetQuery().sources(new String[] {(String) uri});
				cyServices.taskManager.execute(
					new TaskIterator(new NetworkAndViewTask(cyServices, query, ""))
				);
			}

			@Override
			public void cancel() {
			}
		});

		return taskIterator;
	}

	
	/**
	 * Creates a Titled Border with appropriate font settings.
	 * 
	 * @param title
	 *            Title.
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
	   	
	    // download oprions group
	    final JRadioButton button1 = new JRadioButton("Download BioPAX");
	    button1.setSelected(true);
	    button1.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	            downloadMode = OutputFormat.BIOPAX;
	        }
	    });
	    JTextArea textArea1 = new JTextArea(3, 20);
	    textArea1.setLineWrap(true);
	    textArea1.setWrapStyleWord(true);
	    textArea1.setEditable(false);
	    textArea1.setOpaque(false);
	    Font font = textArea1.getFont();
	    Font smallerFont = new Font(font.getFamily(), font.getStyle(), font.getSize() - 2);
	    textArea1.setFont(smallerFont);
	    textArea1.setText("Retrieve the full model, i.e., the BioPAX "
	        + "representation.  In this representation, nodes within a network can "
	        + "refer either to physical entities or processes.");
	    textArea1.setBorder(new EmptyBorder(5, 20, 0, 0));
	       
	    final JRadioButton button2 = new JRadioButton("Download SIF");
	    button2.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	            downloadMode = OutputFormat.BINARY_SIF;
	        }
	    });
	    JTextArea textArea2 = new JTextArea(3, 20);
	    textArea2.setLineWrap(true);
	    textArea2.setWrapStyleWord(true);
	    textArea2.setEditable(false);
	    textArea2.setOpaque(false);
	    textArea2.setFont(smallerFont);
	    textArea2.setText("Retrieve a simplified binary network, as inferred from the original "
	        + "BioPAX representation.  In this representation, nodes within a network refer "
	        + "to physical entities only, and edges refer to inferred interactions.");
	    textArea2.setBorder(new EmptyBorder(5, 20, 0, 0));

	    ButtonGroup group = new ButtonGroup();
	    group.add(button1);
	    group.add(button2);

	    JPanel configPanel = new JPanel();
	    configPanel.setBorder(new TitledBorder("Download Options"));
	    configPanel.setLayout(new GridBagLayout());
	    GridBagConstraints c = new GridBagConstraints();
	        
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = 1.0;
	    c.gridx = 0;
	    c.gridy = 0;
	    configPanel.add(button1, c);
	    c.gridy = 1;
	    configPanel.add(textArea1, c);
	    c.gridy = 2;
	    configPanel.add(button2, c);
	    c.gridy = 3;
	    configPanel.add(textArea2, c); 
	    //  Add invisible filler to take up all remaining space
	    c.gridy = 4;
	    c.weighty = 1.0;
	    JPanel filler = new JPanel();
	    configPanel.add(filler, c);
	    
	    panel.add(configPanel);	    
	    
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
	    organismList.setModel(model);
	    organismList.setToolTipText("Check to exclude entities not associated with at least one of selected organisms");
	    organismList.setAlignmentX(Component.LEFT_ALIGNMENT);
	        
	    JScrollPane organismFilterBox = new JScrollPane(organismList, 
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
	    dataSourceList.setModel(dataSourceBoxModel);
	    dataSourceList.setToolTipText("Check to exclude entities not associated with at least one of selected datasources");
	    dataSourceList.setAlignmentX(Component.LEFT_ALIGNMENT);
	        
	    JScrollPane dataSourceFilterBox = new JScrollPane(dataSourceList, 
	       	JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    dataSourceFilterBox.setBorder(new TitledBorder("Datasource(s):"));
	    dataSourceFilterBox.setPreferredSize(new Dimension(300, 200));
	    dataSourceFilterBox.setMinimumSize(new Dimension(200, 100));

	    JSplitPane filtersPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, dataSourceFilterBox, organismFilterBox);
	    filtersPane.setBorder(new TitledBorder("Global Filters (for all full-text search, top pathways, and graph queries)"));
	    filtersPane.setDividerLocation(400);
	    filtersPane.setResizeWeight(0.5f);
	    filtersPane.setAlignmentX(Component.LEFT_ALIGNMENT);
	    filtersPane.setPreferredSize(new Dimension(500, 250));
	    filtersPane.setMinimumSize(new Dimension(250, 200));
	    
	    panel.add(filtersPane);
	    
	    return panel; //new JScrollPane(panel);
	}

	    
	private Component createSearchPanel() 
	{	   	 		    	
	   	final HitsModel hitsModel = new HitsModel("Current Search Hits", cyServices.taskManager);
			
	    // create tabs pane for the hit details and parent pathways sun-panels
	    final CurrentHitInfoJTabbedPane currentHitInfoPane = new CurrentHitInfoJTabbedPane(hitsModel);
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
	    bpTypeComboBox.setSelectedIndex(1);
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
	        							.datasourceFilter(selectedDatasources())
	        							.organismFilter(selectedOrganisms())
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

        // picked by user items list (for adv. querying or downloading later)
        final JList userList = new JList(new DefaultListModel());
        userList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        userList.setPrototypeCellValue("123456789012345678901234567890123456789012345678901234567890"); 
        //double-click removes item from list
        userList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					if (userList.getSelectedIndex() >= 0) {
						DefaultListModel lm = (DefaultListModel) userList.getModel();
						lm.removeElementAt(userList.getSelectedIndex());
					}
				} 
			}
		});
	        
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
//                		// update pathways list
//                		DefaultListModel ppwListModel = (DefaultListModel) ppwList.getModel();
//						ppwListModel.clear();
//						Collection<NvpListItem> ppws = hitsModel.hitsPathwaysMap.get(item.getUri());
//						if (ppws != null && !ppws.isEmpty())
//							for (NvpListItem it : ppws)
//								ppwListModel.addElement(it);     
                    }
//                }
            }
        });
        //double-click adds item to the other list (user picked items)
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
						DefaultListModel lm = (DefaultListModel) userList.getModel();
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
	        
        //create adv. query panel with user picked items list 
        final JPanel advQueryCtrlPanel = new JPanel();
        advQueryCtrlPanel.setLayout(new BoxLayout(advQueryCtrlPanel, BoxLayout.Y_AXIS));
        advQueryCtrlPanel.setPreferredSize(new Dimension(400, 300));
        
        //add radio buttons for different query types
    	final JPanel queryTypePanel = new JPanel();
        queryTypePanel.setBorder(new TitledBorder("BioPAX Query Types"));
        queryTypePanel.setLayout(new GridBagLayout());   
        
        //create direction buttons in advance (to disable/enable)
        final JRadioButton both = new JRadioButton("Both directions");
        final JRadioButton down = new JRadioButton("Downstream"); 
        final JRadioButton up = new JRadioButton("Upstream"); 
        
	    ButtonGroup bg = new ButtonGroup();
	    JRadioButton b = new JRadioButton("Get (multiple sub-graphs as one 'network')");	    
        //default option (1)
	    b.setSelected(true);
	    hitsModel.graphType = null;
	    b.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	            hitsModel.graphType = null; //to use "get" command instead of "graph"
	        	both.setEnabled(false);
	        	up.setEnabled(false);
	        	down.setEnabled(false);
	        }
	    });
	    bg.add(b);

	    GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        queryTypePanel.add(b, c);
	    
	    b = new JRadioButton("Nearest Neighborhood");
	    b.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	        	hitsModel.graphType = GraphType.NEIGHBORHOOD;
	        	both.setEnabled(true);
	        	up.setEnabled(true);
	        	down.setEnabled(true);
	        	both.setSelected(true);
	        	hitsModel.direction = Direction.BOTHSTREAM;
	        }
	    });
	    bg.add(b);
        c.gridx = 0;
        c.gridy = 1;
        queryTypePanel.add(b, c);
	    
	    b = new JRadioButton("Common Stream");
	    b.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	           	hitsModel.graphType = GraphType.COMMONSTREAM;
	        	both.setEnabled(false);
	        	up.setEnabled(true);
	        	down.setEnabled(true);
	        	down.setSelected(true);
	        	hitsModel.direction = Direction.DOWNSTREAM;
	        }
	    });
	    bg.add(b);
        c.gridx = 0;
        c.gridy = 2;
        queryTypePanel.add(b, c);
	    
	    b = new JRadioButton("Paths Beetween");
	    b.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	        	hitsModel.graphType = GraphType.PATHSBETWEEN;
	        	both.setEnabled(false);
	        	up.setEnabled(false);
	        	down.setEnabled(false);
	        	hitsModel.direction = null;
	        }
	    });
	    bg.add(b);
        c.gridx = 0;
        c.gridy = 3;
        queryTypePanel.add(b, c);
	    
	    b = new JRadioButton("Paths From (selected) To (the rest)");
	    b.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	        	hitsModel.graphType = GraphType.PATHSFROMTO;
	        	both.setEnabled(false);
	        	up.setEnabled(false);
	        	down.setEnabled(false);
	        	hitsModel.direction = null;
	        }
	    });
	    bg.add(b);
        c.gridx = 0;
        c.gridy = 4;
        queryTypePanel.add(b, c);
        
        queryTypePanel.setMaximumSize(new Dimension(400, 150));           
        advQueryCtrlPanel.add(queryTypePanel);
        
        // add direction, limit options and the 'go' button to the panel	        
    	JPanel directionPanel = new JPanel();
    	directionPanel.setBorder(new TitledBorder("Direction"));
    	directionPanel.setLayout(new GridBagLayout());
    	bg = new ButtonGroup();	    
    	down.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	        	hitsModel.direction = Direction.DOWNSTREAM;
	        }
	    });
	    bg.add(down);
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        directionPanel.add(down, c);
	    
        up.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	        	hitsModel.direction = Direction.UPSTREAM;
	        }
	    });
	    bg.add(up);
        c.gridx = 0;
        c.gridy = 1;
        directionPanel.add(up, c);
	    
	    both.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	        	hitsModel.direction = Direction.BOTHSTREAM;
	        }
	    });
	    bg.add(both);
        c.gridx = 0;
        c.gridy = 2;
        directionPanel.add(both, c);
    		
        directionPanel.setMaximumSize(new Dimension(400, 200));
        advQueryCtrlPanel.add(directionPanel);
        
        
        // add "execute" (an advanced query) button
	    final JButton advQueryButton = new JButton("Execute");
	    advQueryButton.setToolTipText("Create a new network from a BioPAX graph query result");
	    advQueryButton.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	        	
	        	if(userList.getSelectedIndices().length == 0) {
	        		JOptionPane.showMessageDialog(advQueryCtrlPanel, "No items were selected from the list. " +
	        				"Please pick one or several to be used with the query.");
	        		return;
	        	}
	        		        	
	        	advQueryButton.setEnabled(false);
	           	
	        	//create source and target lists of URIs
	        	Set<String> srcs = new HashSet<String>();
	        	Set<String> tgts = new HashSet<String>();
	        	for(int i=0; i < userList.getModel().getSize(); i++) {
	        		String uri = ((NvpListItem) userList.getModel().getElementAt(i)).getValue();
	        		if(userList.isSelectedIndex(i))
	        			srcs.add(uri);
	        		else
	        			tgts.add(uri);
	        	}
	        	
	        	if(hitsModel.graphType == null) {
	        		final CPathGetQuery getQ = client.createGetQuery().sources(srcs);
	        		cyServices.taskManager.execute(new TaskIterator(
	        			new NetworkAndViewTask(cyServices, getQ, "Biopax sub-model")
	        			));
	        	} else {
	        		final CPathGraphQuery graphQ = client.createGraphQuery()
	        			.kind(hitsModel.graphType)
	        			.sources(srcs).targets(tgts)
	        			.datasourceFilter(selectedDatasources())
	        			.direction(hitsModel.direction)
	        			//.limit(?)
	        			.organismFilter(selectedOrganisms());
	        		cyServices.taskManager.execute(new TaskIterator(
	        			new NetworkAndViewTask(cyServices, graphQ, "Biopax " + hitsModel.graphType)
		        		));
	        	}
	        	
	        	advQueryButton.setEnabled(true);
	        }
	    });
	    
        advQueryCtrlPanel.add(advQueryButton);
    
        // add the picked items list
        JScrollPane advQueryListPane = new JScrollPane(userList);
        advQueryListPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        advQueryListPane.setBorder(createTitledBorder("Find/add items using Search page. Select items to use in a query. Double-click to remove."));
        advQueryPanel.add(advQueryCtrlPanel, BorderLayout.LINE_START);
        advQueryPanel.add(advQueryListPane, BorderLayout.CENTER);       
	        
        // final top-bottom panels arrange -
        JSplitPane queryAndResults = new JSplitPane(JSplitPane.VERTICAL_SPLIT, searchQueryPanel, searchResultsPanel);
        queryAndResults.setResizeWeight(0.25f);
        queryAndResults.setDividerLocation(150);
//        panel.add(queryAndResults);

        return queryAndResults; //panel;
    }
	    
	    
	private JPanel createTopPathwaysPanel() {
			
		final JPanel panel = new JPanel(); // to return       
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    	// hits model is used both by the filters panel pathways jlist
    	final HitsModel topPathwaysModel = new HitsModel("Top Pathways", cyServices.taskManager);        
        // make (south) tabs
        final CurrentHitInfoJTabbedPane southPane = new CurrentHitInfoJTabbedPane(topPathwaysModel);
        
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
						SearchHit item = (SearchHit) tpwJList.getModel()
								.getElementAt(selectedIndex);
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
	    					taskMonitor.setTitle("CyPath2 Task: Top Pathways");
	    					taskMonitor.setProgress(0.1);
	    					taskMonitor.setStatusMessage("Getting top pathways from the server...");
	    					final SearchResponse resp = client.createTopPathwaysQuery()
	    						.organismFilter(selectedOrganisms()).datasourceFilter(selectedDatasources())
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

	    
    /**
     * @return currently selected organisms
     */
    Set<String> selectedOrganisms() {
    	Set<String> values = new HashSet<String>();
    	for(Object it : organismList.getSelectedValues())
    		values.add(((NvpListItem) it).getValue()); 
    	return values;
	}
    
    /**
     * @return currently selected datasources
     */
    Set<String> selectedDatasources() {
    	Set<String> values = new HashSet<String>();
    	for(Object it : dataSourceList.getSelectedValues())
    		values.add(((NvpListItem) it).getValue()); 
    	return values;
	}


	/**
     * Hit Summary/Details Panel class.
     */
    final class CurrentHitInfoJTabbedPane extends JTabbedPane {
        private final JTextPane summaryTextPane;
        private final JTextPane detailsTextPane;
        private final HitsModel hitsModel;
        
        private SearchHit current;

        /**
         * Constructor.
         * @param browser 
         */
        public CurrentHitInfoJTabbedPane(HitsModel hitsModel) {          	
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
                   		cyServices.taskManager.execute(new TaskIterator(
                       		new NetworkAndViewTask(cyServices, query, hyperlinkEvent.getDescription())));
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
//    	    styleSheet.addRule("b {background-color: #FFFF00;}");
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
    						taskMonitor.setTitle("CyPath2 Task: Query for more details (of " + item + ")");
    						taskMonitor.setProgress(0.1);
    						taskMonitor.setStatusMessage("Getting pathway/component names and counts from the server...");
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
    			cyServices.taskManager.execute(taskIterator);
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
                   				.datasourceFilter(selectedDatasources())
                   				.organismFilter(selectedOrganisms())
                   				.sources(Collections.singleton(uri))
                   				.kind(GraphType.NEIGHBORHOOD);
                   			cyServices.taskManager.execute(new TaskIterator(
                       			new NetworkAndViewTask(cyServices, graphQuery, currentItem.getName() + " NEIGHBORHOOD")));
                   		} else { // use '/get' command
                   			final CPathGetQuery getQuery = CyPath2.client.createGetQuery()
                   				.sources(Collections.singleton(uri));
                   			cyServices.taskManager.execute(new TaskIterator(
                       			new NetworkAndViewTask(cyServices, getQuery, currentItem.getName())));
                   		}
                    }
                }
            });
            
            style(textPane);
            
            return textPane;
        }        

    }
    
}
