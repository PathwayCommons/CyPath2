package org.pathwaycommons.cypath2.internal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Properties;
import java.util.Set;

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
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.EntityReference;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.io.read.CyNetworkReaderManager;
import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.property.CyProperty;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.util.swing.CheckBoxJList;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.undo.UndoSupport;
import org.pathwaycommons.cypath2.internal.BioPaxUtil.StaxHack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.client.CPath2Client;
import cpath.client.CPath2Client.Direction;
import cpath.client.util.CPathException;
import cpath.service.Cmd;
import cpath.service.GraphType;
import cpath.service.OutputFormat;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.TraverseResponse;

/**
 * CyPath2: CPathSquared Web Service client integrated into the Cytoscape Web Services GUI Framework.
 */
public final class CyPath2 extends AbstractWebServiceGUIClient 
	implements NetworkImportWebServiceClient, SearchWebServiceClient
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CyPath2.class);
    
    private static OutputFormat downloadMode = OutputFormat.BIOPAX;  
    
    public static final Map<String,String> uriToOrganismNameMap = new HashMap<String, String>();
    public static final Map<String,String> uriToDatasourceNameMap = new HashMap<String, String>();
    
    private static final String CPATH_SERVER_NAME_ATTR = "CPATH_SERVER_NAME";
    private static final String CPATH_SERVER_URL_ATTR = "CPATH_SERVER_URL";
	
	// dynamic map - one cpath-client instance per biopax property path (used by multiple thread)
	private static final Map<String, CPath2Client> proprtyPathToClientMap 
		= Collections.synchronizedMap(new HashMap<String, CPath2Client>());

	private final CySwingApplication application;
	private final TaskManager taskManager;
	private final OpenBrowser openBrowser;
	private final CyNetworkManager networkManager;
	private final CyApplicationManager applicationManager;
	private final CyNetworkViewManager networkViewManager;
	private final CyNetworkReaderManager networkViewReaderManager;
	private final CyNetworkNaming naming;
	private final CyNetworkFactory networkFactory;
	private final CyLayoutAlgorithmManager layoutManager;
	private final UndoSupport undoSupport;
	private final BinarySifVisualStyleFactory binarySifVisualStyleUtil;
	private final VisualMappingManager mappingManager;
	private final CyProperty<Properties> cyProperty;
	
	private JPanel advQueryPanel;
	
	private final CheckBoxJList organismList;
	private final CheckBoxJList dataSourceList; 

    
	/**
     * Creates a new Web Services client.
     */
    public CyPath2(String uri, String displayName, String description, 
    		CySwingApplication app, TaskManager tm, OpenBrowser ob, 
			CyNetworkManager nm, CyApplicationManager am, CyNetworkViewManager nvm, 
			CyNetworkReaderManager nvrm, CyNetworkNaming nn, CyNetworkFactory nf, 
			CyLayoutAlgorithmManager lam, UndoSupport us, 
			BinarySifVisualStyleFactory bsvsf, VisualMappingManager mm,
			CyProperty<Properties> prop) 
    {    	   	
    	super(uri, displayName, description);
    	
		application = app;
		taskManager = tm;
		openBrowser = ob;
		networkManager = nm;
		applicationManager = am;
		networkViewManager = nvm;
		networkViewReaderManager = nvrm;
		naming = nn;
		layoutManager = lam;
		networkFactory = nf;
		undoSupport = us;
		binarySifVisualStyleUtil = bsvsf;
		mappingManager = mm;
		cyProperty = prop;
		
		//filter value lists
		organismList = new CheckBoxJList();
		dataSourceList = new CheckBoxJList(); 
		
		JPanel mainPanel = new JPanel();
		gui = mainPanel; 
		
    }
 
    /**
     * Creates the UI and loads 
     * some initial data from the server.
     * 
     */
    public void init() {   	
    	// init datasources and org. maps (in a separate thread)
		CPath2Client cPath2Client = newClient();
		cPath2Client.setType("Provenance");
		List<SearchHit> hits = cPath2Client.findAll();
		for(SearchHit bs : hits) {
			uriToDatasourceNameMap.put(bs.getUri(), bs.getName());
		}
        cPath2Client.setType("BioSource");
        hits = cPath2Client.findAll();
        for(SearchHit bs : hits) {
        	uriToOrganismNameMap.put(bs.getUri(), bs.getName());
        }    	
    	
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
				taskManager.setExecutionContext(null);
				taskManager.execute(new TaskIterator(
					new CpsNetworkAndViewTask(newClient(), (String) uri, "")));
			}

			@Override
			public void cancel() {
			}
		});

		return taskIterator;
	}
	
		
    static CPath2Client newClient() {
        CPath2Client client = CPath2Client.newInstance();
		return client;
	}

    
    static TraverseResponse traverse(String path, Collection<String> uris) 
	   {
	   	if(LOGGER.isDebugEnabled())
	   		LOGGER.debug("traverse: path=" + path);
	   		
	   	CPath2Client client = proprtyPathToClientMap.get(path);
	   	if(client == null) {
	   		client = newClient();
	   		client.setPath(path);
	   		proprtyPathToClientMap.put(path, client);
	   	}
	        
        TraverseResponse res = null;
		try {
			res = client.traverse(uris);
		} catch (CPathException e) {
			LOGGER.error("traverse: " + path + 
				" failed; uris:" + uris.toString(), e);
		}
				
       	return res;
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
		Font newFont = new Font(font.getFamily(), Font.BOLD, font.getSize() + 1);
		border.setTitleFont(newFont);
		border.setTitleColor(new Color(102, 51, 51));
		return border;
	}

	
    /**
	 * Creates the app's options panel.
	 * 
	 * @return
	 */
	JScrollPane createOptionsPane() {
	   	JPanel panel = new JPanel();
	   	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
	    	
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
//	    SortedSet<NvpListItem> items = new TreeSet<NvpListItem>(); //sorted by name
//	    items.add(new NvpListItem("Human", "9606"));	    
	    DefaultListModel model = new DefaultListModel();
//	    for(NvpListItem nvp : items) {
//	    	model.addElement(nvp);
//	    }
	    model.addElement(new NvpListItem("Human", "9606"));
	    organismList.setModel(model);
	    organismList.setToolTipText("Select Organisms");
	    organismList.setAlignmentX(Component.LEFT_ALIGNMENT);
	    organismList.setSelectedIndex(0); //always selected as long as there is only one organism (human)
	        
	    JScrollPane organismFilterBox = new JScrollPane(organismList, 
	    	JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    organismFilterBox.setBorder(new TitledBorder("Organism(s) in:"));
//	    organismFilterBox.setMinimumSize(new Dimension(200, 200));
	        
	    // create the filter-list of datasources available on the server  
	    DefaultListModel dataSourceBoxModel = new DefaultListModel(); 
	    for(String uri : uriToDatasourceNameMap.keySet()) {
	    	dataSourceBoxModel.addElement(new NvpListItem(uriToDatasourceNameMap.get(uri), uri));
	    }		        
	    dataSourceList.setModel(dataSourceBoxModel);
	    dataSourceList.setToolTipText("Select Datasources");
	    dataSourceList.setAlignmentX(Component.LEFT_ALIGNMENT);
	        
	    JScrollPane dataSourceFilterBox = new JScrollPane(dataSourceList, 
	       	JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    dataSourceFilterBox.setBorder(new TitledBorder("Datasource(s) in:"));
//	    dataSourceFilterBox.setMinimumSize(new Dimension(200, 200)); 

	    JPanel filtersPane = new JPanel();
	    filtersPane.setBorder(new TitledBorder("Filter Options"));
	    filtersPane.setLayout(new FlowLayout(FlowLayout.LEFT));
	 // this filter is temporarily DISABLED (Human is the only supported and always selected)
//	    filtersPane.add(organismFilterBox);
	    filtersPane.add(dataSourceFilterBox);
	    filtersPane.setMinimumSize(new Dimension(400, 200));
	    panel.add(filtersPane);
	    
	    return new JScrollPane(panel);
	}

	    
	private JPanel createSearchPanel() 
	{	   	 	
		final JPanel panel = new JPanel();
	    	
	   	final HitsModel hitsModel = new HitsModel("Current Search Hits", true, taskManager);
	   	panel.setLayout(new BorderLayout());
			
	    // create tabs pane for the hit details and parent pathways sun-panels
	    final JTabbedPane detailsTabbedPane = new JTabbedPane();
	    final DetailsPanel detailsPanel = new DetailsPanel(openBrowser);
	    detailsTabbedPane.add("Summary", detailsPanel);
	    //parent pathways list pane (its content to be added below)
	    JPanel ppwListPane = new JPanel();
	    detailsTabbedPane.add("Parent Pathways", ppwListPane);
	        
	    final JPanel searchQueryPanel = new JPanel();
	        
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

	    //TODO add a Search for a BioPAX Type combo-box
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
	            final Set<String> organisms = new HashSet<String>();
	            for(Object it : organismList.getSelectedValues())
	               	organisms.add(((NvpListItem) it).getValue());                
	            final Set<String> datasources = new HashSet<String>();
	            for(Object it : dataSourceList.getSelectedValues())
	            	datasources.add(((NvpListItem) it).getValue());
	            	
	            if (keyword == null || keyword.trim().length() == 0 || keyword.startsWith(ENTER_TEXT)) {
	            	JOptionPane.showMessageDialog(gui, "Please enter something into the search box.");
	        		searchButton.setEnabled(true);
	        	} else {
	        		info.setText("");
	        		Task search = new Task() {
	        			@Override
	        			public void run(TaskMonitor taskMonitor) throws Exception {
	        				try {
	        					taskMonitor.setProgress(0);
	        					taskMonitor.setStatusMessage("Executing search for " + keyword);
	        					CPath2Client client = newClient();
	        					client.setOrganisms(organisms);
	        					client.setType(hitsModel.searchFor.toString());
	        					if (!datasources.isEmpty())
	        						client.setDataSources(datasources);       						
	        					final SearchResponse searchResponse = (SearchResponse) client.search(keyword);
	        					// update hits model (make summaries, notify observers!)
								hitsModel.update(searchResponse);
	        					info.setText("Matches:  " + searchResponse.getNumHits() 
	        						+ "; retrieved: " + searchResponse.getSearchHit().size()
	        							+ " (page #" + searchResponse.getPageNo() + ")");
	        				} catch (CPathException e) {
	        					JOptionPane.showMessageDialog(gui, "Error: " + e + 
	        						" (using query '" + keyword + "' and current filter values)");
								hitsModel.update(new SearchResponse()); //clear
	        				} catch (Throwable e) { 
	        					// using Throwable helps catch unresolved runtime dependency issues!
	        					JOptionPane.showMessageDialog(gui, "Error: " + e);
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

	        		taskManager.execute(new TaskIterator(search));
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
        keywordPane.setMinimumSize(new Dimension(400, 200));
    
		searchQueryPanel.setLayout(new BoxLayout(searchQueryPanel, BoxLayout.X_AXIS));
        searchQueryPanel.add(keywordPane);
        
        // Assembly the results panel
    	final JPanel searchResultsPanel = new JPanel();
    	searchResultsPanel.setLayout(new BoxLayout(searchResultsPanel, BoxLayout.Y_AXIS));
    	searchResultsPanel.add(info);
        //create parent pathways panel (the second tab)
        final JList ppwList = new JList(new DefaultListModel());
        ppwList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ppwList.setPrototypeCellValue("12345678901234567890");   
        ppwList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int selectedIndex = ppwList.getSelectedIndex();
					// ignore the "unselect" event.
					if (selectedIndex >= 0) {
						NvpListItem item = (NvpListItem) ppwList.getModel()
								.getElementAt(selectedIndex);
						String uri = item.getValue();
				        taskManager.execute(new TaskIterator(
				        	new CpsNetworkAndViewTask(hitsModel.graphQueryClient, uri, item.toString())));	
					}
				}
			}
		});  
	        
        ppwListPane.setLayout(new BorderLayout());
        final JScrollPane ppwListScrollPane = new JScrollPane(ppwList);
        ppwListScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        ppwListScrollPane.setBorder(createTitledBorder("Double-click to import (a new network)."));
        ppwListPane.add(ppwListScrollPane, BorderLayout.CENTER);

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
                if (!listSelectionEvent.getValueIsAdjusting()) {
                    if (selectedIndex >=0) {
                    	SearchHit item = (SearchHit)resList.getModel().getElementAt(selectedIndex);
                		// show current hit's summary
                		String summary = hitsModel.hitsSummaryMap.get(item.getUri());
                    	detailsPanel.setCurrentItem(item, summary);
                		// update pathways list
                		DefaultListModel ppwListModel = (DefaultListModel) ppwList.getModel();
						ppwListModel.clear();
						Collection<NvpListItem> ppws = hitsModel.hitsPathwaysMap.get(item.getUri());
						if (ppws != null && !ppws.isEmpty())
							for (NvpListItem it : ppws)
								ppwListModel.addElement(it);           			
                    }
                }
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
        hitListPane.setLayout(new BorderLayout());
        JScrollPane hitListScrollPane = new JScrollPane(resList);
        hitListScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        hitListScrollPane.setBorder(createTitledBorder("Double-click adds it to Advanced Query page."));
        // make (north) tabs       
        JSplitPane vSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, hitListScrollPane, detailsTabbedPane);
        vSplit.setDividerLocation(250);
        hitListPane.add(vSplit, BorderLayout.CENTER);
	        
        //  Create search results extra filtering panel
        HitsFilterPanel filterPanel = new HitsFilterPanel(resList, hitsModel, true, false, false);
	        
        //  Create the Split Pane
        JSplitPane hSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, filterPanel, hitListPane);
        hSplit.setDividerLocation(250);
        hSplit.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchResultsPanel.add(hSplit);
	        
        //create adv. query panel with user picked items list 
        advQueryPanel = new JPanel(new BorderLayout());
        JPanel advQueryCtrlPanel = new JPanel();
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
	            hitsModel.graphType = null;
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
	        	hitsModel.graphQueryClient.setDirection(Direction.BOTHSTREAM);
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
	        	hitsModel.graphQueryClient.setDirection(Direction.DOWNSTREAM);
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
	        	hitsModel.graphQueryClient.setDirection(null);
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
	        	hitsModel.graphQueryClient.setDirection(null);
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
	        	hitsModel.graphQueryClient.setDirection(Direction.DOWNSTREAM);
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
	        	hitsModel.graphQueryClient.setDirection(Direction.UPSTREAM);
	        }
	    });
	    bg.add(up);
        c.gridx = 0;
        c.gridy = 1;
        directionPanel.add(up, c);
	    
	    both.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	        	hitsModel.graphQueryClient.setDirection(Direction.BOTHSTREAM);
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
	        		JOptionPane.showMessageDialog(gui, "No items were selected from the list. " +
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
	        	
	        	//use same organism and datasource filters for the GRAPH query
	        	final Set<String> organisms = new HashSet<String>();
	        	for(Object it : organismList.getSelectedValues())
	               	organisms.add(((NvpListItem) it).getValue());                
	            final Set<String> datasources = new HashSet<String>();
	            for(Object it : dataSourceList.getSelectedValues())
	            	datasources.add(((NvpListItem) it).getValue());
	        	hitsModel.graphQueryClient.setOrganisms(organisms);
	        	hitsModel.graphQueryClient.setDataSources(datasources);
	        	        	
	        	if(hitsModel.graphType == null)
	        		taskManager.execute(new TaskIterator(
	        			new CpsNetworkAndViewTask(hitsModel.graphQueryClient, 
	        					Cmd.GET, null, srcs, null, "Biopax sub-model")
	        			));
	        	else
	        		taskManager.execute(new TaskIterator(
	        			new CpsNetworkAndViewTask(hitsModel.graphQueryClient, 
	        					Cmd.GRAPH, hitsModel.graphType, srcs, tgts, "Biopax " + hitsModel.graphType)
		        		));
	        	
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
        queryAndResults.setDividerLocation(180);
        panel.add(queryAndResults);
	        
        return panel;
    }
	    
	    
	private JPanel createTopPathwaysPanel() {
			
		final JPanel panel = new JPanel(); // to return       
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        //  Create Info Panel (the first tab)
        final DetailsPanel detailsPanel = new DetailsPanel(openBrowser);
        final JTextPane summaryTextPane = detailsPanel.getTextPane();
	        
        // make (south) tabs
        JTabbedPane southPane = new JTabbedPane(); 
        southPane.add("Summary", detailsPanel);
	        
    	// hits model is used both by the filters panel pathways jlist
    	final HitsModel topPathwaysModel = new HitsModel("Top Pathways", false, taskManager);
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
                if (!listSelectionEvent.getValueIsAdjusting()) {
                    if (selectedIndex >=0) {
                    	SearchHit item = (SearchHit) l.getModel().getElementAt(selectedIndex);
                		// get/create and show hit's summary
                		String summary = topPathwaysModel.hitsSummaryMap.get(item.getUri());
                		detailsPanel.setCurrentItem(item, summary);
                		summaryTextPane.setText(summary);
                		summaryTextPane.setCaretPosition(0);					
                    }
                }
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
				        taskManager.execute(new TaskIterator(
				        	new CpsNetworkAndViewTask(topPathwaysModel.graphQueryClient, uri, item.toString())));	
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
	        
        //  Create the Split Pane
        JSplitPane hSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, filterPanel, vSplit);
        hSplit.setDividerLocation(300);
        hSplit.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        
	    // create the update button and action
	    final JButton updateButton = new JButton("Update Top Pathways");
	    updateButton.setToolTipText("Get/Update Top Pathways (From the Server)");
	    updateButton.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	        	updateButton.setEnabled(false);	        			
	            // load pathways from server
	    		TaskIterator taskIterator = new TaskIterator(new Task() {
	    			@Override
	    			public void run(TaskMonitor taskMonitor) throws Exception {
	    				try {
	    					taskMonitor.setTitle("cPathSquared Task: Top Pathways");
	    					taskMonitor.setProgress(0.1);
	    					taskMonitor.setStatusMessage("Retrieving top pathways...");
	    					final SearchResponse resp = topPathwaysModel.graphQueryClient.getTopPathways();
	    					// reset the model and kick off observers (list and filter panel)
							topPathwaysModel.update(resp);		    			        
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
	    			@Override
	    			public void cancel() {
	    			}
	    		});			
	    		// kick off the task execution
	    		taskManager.execute(taskIterator);
	        }
	    });
	    updateButton.setAlignmentX(Component.LEFT_ALIGNMENT); 
               
        panel.add(updateButton);
        panel.add(hSplit);	        
        panel.repaint();
        
        return panel;
	}
	
		
    /**
     * Creates a JTextPane with correct line wrap settings.
     *
     * @return JTextPane Object.
     */
    private JTextPane createHtmlTextPane(final DetailsPanel detailsPanel) {
        final JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBorder(new EmptyBorder(7,7,7,7));
        textPane.setContentType("text/html");
        textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        final CPath2Client client = newClient(); //handles user's clicks on biopax URIs
        textPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
                if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                	//import/create a network if the special (fake) link is clicked
               		SearchHit currentItem = detailsPanel.getCurrentItem();
               		String uri = currentItem.getUri();
               		if(!currentItem.getBiopaxClass().equalsIgnoreCase("Pathway")) {
               			taskManager.execute(new TaskIterator(
                   			new CpsNetworkAndViewTask(client, Cmd.GRAPH, 
                   				GraphType.NEIGHBORHOOD, Collections.singleton(uri), null, 
                   					currentItem.getName() + " NEIGHBORHOOD")));
               		} else { // use '/get' command
               			taskManager.execute(new TaskIterator(
                   			new CpsNetworkAndViewTask(client, uri, currentItem.getName())));
               		}
                }
            }
        });

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
        
        return textPane;
    }

	    
    /**
     * Summary Panel.
     *
     */
    final class DetailsPanel extends JPanel {
        private final Document doc;
        private final JTextPane textPane;
        
        private SearchHit current;

        /**
         * Constructor.
         * @param browser 
         */
        public DetailsPanel(OpenBrowser openBrowser) {
            this.setLayout(new BorderLayout());
            textPane = createHtmlTextPane(this);
            doc = textPane.getDocument();
            JScrollPane scrollPane = encloseInJScrollPane (textPane);
            add(scrollPane, BorderLayout.CENTER);
        }

        /**
         * Sets the current item and HTML to display
         * 
         * @param item
         * @param summary
         */
        public synchronized void setCurrentItem(SearchHit item, String summary) {
			current = item;
    		getTextPane().setText(summary);
    		getTextPane().setCaretPosition(0); 
		}

        public synchronized SearchHit getCurrentItem() {
			return current;
		}
        
		/**
         * Gets the summary document model.
         * @return Document object.
         */
        public Document getDocument() {
            return doc;
        }

        /**
         * Gets the summary text pane object.
         * @return JTextPane Object.
         */
        public JTextPane getTextPane() {
            return textPane;
        }

        /**
         * Encloses the specified JTextPane in a JScrollPane.
         *
         * @param textPane JTextPane Object.
         * @return JScrollPane Object.
         */
        private JScrollPane encloseInJScrollPane(JTextPane textPane) {
            JScrollPane scrollPane = new JScrollPane(textPane);
            return scrollPane;
        }

    }
	    
	    
    /**
     * A Task that gets data from the cPath2 server and 
     * creates a new Cytoscape network and view.
     * 
     * @author rodche
     */
    class CpsNetworkAndViewTask extends AbstractTask {
    	
    	private final String networkTitle;
    	private final GraphType graphType;
    	private final Set<String> sources;
    	private final Set<String> targets;
    	private final Cmd command;
    	private final CPath2Client client;

    	/**
    	 * Constructor 
    	 * (for a simple get-by-URI query).
    	 * 
    	 * @param uri of a pathway or interaction
    	 * @param networkTitle optional name for the new network
    	 */
    	public CpsNetworkAndViewTask(CPath2Client client, String uri, String networkTitle) {
    		this.networkTitle = networkTitle;
    		this.graphType = null; //  if null, will use the cpath2 '/get' (by URIs) command
    		this.sources = Collections.singleton(uri);
    		this.targets = null;
    		this.command = Cmd.GET;
    		this.client = client;
    	}
    	
    	/**
    	 * Constructor 
    	 * (advanced, for all get, traverse, and graph queries).
    	 * 
    	 * @param command
    	 * @param graphType
    	 * @param srcs
    	 * @param tgts
    	 * @param networkTitle
    	 */
    	public CpsNetworkAndViewTask(CPath2Client client, Cmd command, GraphType graphType, 
    			Set<String> srcs, Set<String> tgts, String networkTitle) 
    	{
    		this.networkTitle = networkTitle;
    		this.sources = srcs;
    		this.targets = tgts;
    		this.graphType = graphType; 
    		this.command = command;
    		this.client = client;
    	}

    	public void run(TaskMonitor taskMonitor) throws Exception {
    		String title = "Retrieving a network " + networkTitle + " from " 
   				+ getDisplayName() + "...";
    		taskMonitor.setTitle(title);
    		try {
    			taskMonitor.setProgress(0);
    			taskMonitor.setStatusMessage("Retrieving data...");
    	    	
    	    	// do query, get data as string
    	    	final String data = client.doPost(command, String.class, 
    	    	    client.buildRequest(command, graphType, sources, targets, downloadMode));
    	    	
    	    	if(data == null || data.isEmpty()) {
    	    		JOptionPane.showMessageDialog(gui, "No data returned from the server.");
    	    		return;
    	    	}
    	    		
    	    	
    	    	// done.
    			taskMonitor.setProgress(0.4);    			
    			if (cancelled) 
    				return;
	    			
    			// Save the BioPAX or SIF data to a temporary local file
    			String tmpDir = System.getProperty("java.io.tmpdir");			
    			// Branch based on download mode setting.
    			File tmpFile;
    			if (downloadMode == OutputFormat.BIOPAX) {
    				tmpFile = File.createTempFile("temp", ".xml", new File(tmpDir));
    			} else {
    				tmpFile = File.createTempFile("temp", ".sif", new File(tmpDir));
    			}
    			tmpFile.deleteOnExit();
	    							
    			FileWriter writer = new FileWriter(tmpFile);
    			writer.write(data);
    			writer.close();	
	    			
    			taskMonitor.setProgress(0.5);
    			if (cancelled) return;
    			taskMonitor.setStatusMessage("Creating Cytoscape Network from BioPAX Data...");
	    			
    			// Import data via Cy3 I/O API	
    			String inputName = naming.getSuggestedNetworkTitle(networkTitle);
    			CyNetworkReader reader =  networkViewReaderManager
   					.getReader(tmpFile.toURI(), inputName);	
    			reader.run(taskMonitor);
	    			
    			taskMonitor.setProgress(0.6);
    			if (cancelled) return;
    			taskMonitor.setStatusMessage("Creating Network View...");

    			final CyNetwork cyNetwork = reader.getNetworks()[0];
                final CyNetworkView view = reader.buildCyNetworkView(cyNetwork);

                networkManager.addNetwork(cyNetwork);
                networkViewManager.addNetworkView(view);

    			taskMonitor.setProgress(0.7);
    			if (cancelled) return;
	    			
    			//post-process SIF network (retrieve biopax attributes from the server)
    			if (downloadMode == OutputFormat.BINARY_SIF) {
    				//fix the network name
    				SwingUtilities.invokeLater(new Runnable() {
    					public void run() {
    						String networkTitleWithUnderscores = naming.getSuggestedNetworkTitle(networkTitle);
    						Attributes.set(cyNetwork, cyNetwork, CyNetwork.NAME, networkTitleWithUnderscores, String.class);
    						Attributes.set(cyNetwork, cyNetwork, CyRootNetwork.SHARED_NAME, networkTitleWithUnderscores, String.class);
    					}
    				});
	    				
    				taskMonitor.setStatusMessage("Updating SIF network " +
    					"attributes from corresonding BioPAX data...");				
	    				
    				// Set the Quick Find Default Index
    				Attributes.set(cyNetwork, cyNetwork, "quickfind.default_index", CyNetwork.NAME, String.class);
    				// Specify that this is a BINARY_NETWORK
    				Attributes.set(cyNetwork, cyNetwork, "BIOPAX_NETWORK", Boolean.TRUE, Boolean.class);
    	
    				// we need the biopax sub-model to create node/edge attributes
    				final Set<String> uris = new HashSet<String>();
    				// Set node/edge attributes from the Biopax Model
    				for (CyNode node : cyNetwork.getNodeList()) {
    					String uri = cyNetwork.getRow(node).get(CyNetwork.NAME, String.class);
    					if(!uri.contains("/group/")) {
    						uris.add(uri);
    					} else {
							Attributes.set(cyNetwork, node, BioPaxUtil.BIOPAX_ENTITY_TYPE, "(Generic/Group)", String.class);
							Attributes.set(cyNetwork, node, BioPaxUtil.BIOPAX_RDF_ID, uri, String.class);
							Attributes.set(cyNetwork, node, CyRootNetwork.SHARED_NAME, "(Group)", String.class);
							Attributes.set(cyNetwork, node, CyNetwork.NAME, "(Group)", String.class);
    					}
    				}
    				if (cancelled) return;
    				
    				//retrieve the model (using a STAX hack)
    				final Model[] callback = new Model[1];
    				StaxHack.runWithHack(new Runnable() {
    					@Override
    					public void run() {
    						try {
    							callback[0] = client.get(uris);
    						} catch (Throwable e) {
    							LOGGER.warn("Import failed: " + e);
    						}
    					}
    				});
    				final Model bpModel = callback[0];
    				
    				// Set node/edge attributes from the Biopax Model
    				for (CyNode node : cyNetwork.getNodeList()) {
    					String uri = cyNetwork.getRow(node).get(CyNetwork.NAME, String.class);
    					BioPAXElement e = bpModel.getByID(uri);// can be null (for generic/group nodes)
    					if(e instanceof EntityReference || e instanceof Entity) 
    					{
    						//note: in fact, SIF formatted data contains only ERs, PEs (no sub-classes), and Complexes.
    						BioPaxUtil.createAttributesFromProperties(e, node, cyNetwork);
    					} else if (e != null){
   							LOGGER.warn("SIF network has an unexpected node: " + uri 
   								+ " of type " + e.getModelInterface());
    					} else { //e == null
    						assert uri.contains("/group/") : "URI, which is not a generated " +
    							"generic/group's one, is not found on the server: " + uri;
    						
    						if(!uri.contains("/group/")) {
    							LOGGER.warn("URI, which is not a generated " +
    								"generic/group's one, is not found on the server: " + uri);
    						}
    					}
    				}

    				if (cancelled) return;

    				VisualStyle visualStyle = binarySifVisualStyleUtil.getVisualStyle();
    				mappingManager.setVisualStyle(visualStyle, view);
    				visualStyle.apply(view);
    				view.updateView();
    			} 

    			taskMonitor.setProgress(0.8);
    			if (cancelled) return;
    			taskMonitor.setStatusMessage("Generating html links...");
	    			
    			// Add Links Back to cPath2 Instance
    			CyRow row = cyNetwork.getRow(cyNetwork);
    			String cPathServerDetailsUrl = row.get(CPATH_SERVER_URL_ATTR, String.class);
    			if (cPathServerDetailsUrl == null) {
    				Attributes.set(cyNetwork, cyNetwork, CPATH_SERVER_NAME_ATTR, getDisplayName(), String.class);
    				Attributes.set(cyNetwork, cyNetwork, CPATH_SERVER_URL_ATTR, client.getEndPointURL(), String.class);
    			}
	    			
    			taskMonitor.setProgress(0.9);
    			if (cancelled) return;
    			taskMonitor.setStatusMessage("Running the default layout algorithm...");

    			view.updateView();
	    			
    		} finally {
    			taskMonitor.setStatusMessage("Done");
    			taskMonitor.setProgress(1.0);
    		}
    	}
    
    }
    
}
