package org.cytoscape.cpathsquared.internal;

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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Observer;
import java.util.Properties;
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
import org.biopax.paxtools.model.level3.Complex;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.PhysicalEntity;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.client.CPath2Client;
import cpath.client.CPath2Client.Direction;
import cpath.client.util.CPathException;
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
	
	static final String JVM_PROPERTY_CPATH2_URL = "cPath2Url";
	static final String DEFAULT_SERVER_URL = "http://www.pathwaycommons.org/pc2/";	
    static final String SERVER_URL = System.getProperty(JVM_PROPERTY_CPATH2_URL, DEFAULT_SERVER_URL);   
    static final String SERVER_NAME = "Pathway Commons (BioPAX L3)";
    static final String INFO_ABOUT = 
    	"<b>cPath2 (Pathway Commons)</b> is a warehouse of " +
    	"biological pathway information integrated from public databases and " +
    	"persisted in BioPAX Level3 format, which one can search, traverse, download.";
    
    static String iconToolTip  = "Import Networks from Pathway Commons Web Services (BioPAX L3)";    
    static String iconFileName = "pc.png";
    static OutputFormat downloadMode = OutputFormat.BIOPAX;
    
    static Map<String,String> uriToOrganismNameMap = 
    		Collections.unmodifiableMap(newClient().getValidOrganisms());
    static Map<String,String> uriToDatasourceNameMap = 
    		Collections.unmodifiableMap(newClient().getValidDataSources());
    
	// Display name of this client.
    private static final String DISPLAY_NAME = SERVER_NAME + " Client";
	private static final String CPATH_SERVER_NAME_ATTRIBUTE = "CPATH_SERVER_NAME";
	private static final String CPATH_SERVER_DETAILS_URL = "CPATH_SERVER_DETAILS_URL";

	final CySwingApplication application;
	final TaskManager taskManager;
	final OpenBrowser openBrowser;
	final CyNetworkManager networkManager;
	final CyApplicationManager applicationManager;
	final CyNetworkViewManager networkViewManager;
	final CyNetworkReaderManager networkViewReaderManager;
	final CyNetworkNaming naming;
	final CyNetworkFactory networkFactory;
	final CyLayoutAlgorithmManager layoutManager;
	final UndoSupport undoSupport;
	final BinarySifVisualStyleFactory binarySifVisualStyleUtil;
	final VisualMappingManager mappingManager;
	final CyProperty<Properties> cyProperty;
	
	private JPanel advQueryPanel;
    
    /**
     * Creates a new Web Services client.
     */
    public CyPath2(CySwingApplication app, TaskManager tm, OpenBrowser ob, 
			CyNetworkManager nm, CyApplicationManager am, CyNetworkViewManager nvm, 
			CyNetworkReaderManager nvrm, CyNetworkNaming nn, CyNetworkFactory nf, 
			CyLayoutAlgorithmManager lam, UndoSupport us, 
			BinarySifVisualStyleFactory bsvsf, VisualMappingManager mm,
			CyProperty<Properties> prop) {
    	super(SERVER_URL, DISPLAY_NAME, "<html><body>" + INFO_ABOUT + "</body></html>");
    	
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
    	
		final JTabbedPane  tabbedPane = new JTabbedPane();
        tabbedPane.add("Search", createSearchPanel()); //also init. the advQueryPanel
        tabbedPane.add("Top Pathways", createTopPathwaysPanel());
        tabbedPane.add("Advanced Query", advQueryPanel);
        tabbedPane.add("Options", createOptionsPane());
        
    	JPanel mainPanel = new JPanel();
        mainPanel.setPreferredSize(new Dimension (900,600));
        mainPanel.setLayout (new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);       

    	gui = mainPanel;
    }
    
    
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
				taskManager.setExecutionContext(null);
				taskManager.execute(new TaskIterator(
					new CpsNetworkAndViewTask((String) cpathSquaredQueryUrl, "")));
			}

			@Override
			public void cancel() {
			}
		});

		return taskIterator;
	}
	
		
    static CPath2Client newClient() {
        CPath2Client client = CPath2Client.newInstance();
        client.setEndPointURL(SERVER_URL);
		return client;
	}

    
    static TraverseResponse traverse(String path, Collection<String> uris) 
	   {
	   	if(LOGGER.isDebugEnabled())
	   		LOGGER.debug("traverse: path=" + path);
        CPath2Client client = newClient();
        client.setPath(path);
	        
        TraverseResponse res = null;
		try {
			res = client.traverse(uris);;
		} catch (CPathException e) {
			LOGGER.error("getting " + path + 
				" failed; uri:" + uris.toString(), e);
		}
				
       	return res;
    }

	
	/**
	 * Gets a global Cytoscape property value.
	 * 
	 * @param key
	 * @return
	 */
	private Object getCyProperty(String key) {
		return ((Properties) cyProperty.getProperties()).getProperty(key);
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
	static JScrollPane createOptionsPane() {
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

		// create the filter-list of organisms available on the server
	    final CheckBoxJList organismList = new CheckBoxJList();
	    //make sorted by name list
	    SortedSet<NvpListItem> items = new TreeSet<NvpListItem>();
//	    for(String o : uriToOrganismNameMap.keySet()) {
//	    	items.add(new NvpListItem(uriToOrganismNameMap.get(o), o));
//	    }
	    //manually add several popular organisms ()
	    items.add(new NvpListItem("Human", "urn:miriam:taxonomy:9606"));
	    items.add(new NvpListItem("Mouse", "urn:miriam:taxonomy:10090"));
	    items.add(new NvpListItem("Yeast", "urn:miriam:taxonomy:4923"));
	    
	    DefaultListModel model = new DefaultListModel();
	    for(NvpListItem nvp : items) {
	    	model.addElement(nvp);
	    }
	    organismList.setModel(model);
	    organismList.setToolTipText("Select Organisms");
	    organismList.setAlignmentX(Component.LEFT_ALIGNMENT);
	        
	    JScrollPane organismFilterBox = new JScrollPane(organismList, 
	    	JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	    organismFilterBox.setBorder(new TitledBorder("Organism(s) in:"));
	    organismFilterBox.setMinimumSize(new Dimension(200, 200));
	        
	    // create the filter-list of datasources available on the server
	    final CheckBoxJList dataSourceList = new CheckBoxJList();   
	    DefaultListModel dataSourceBoxModel = new DefaultListModel(); 
	    for(String d : uriToDatasourceNameMap.keySet()) {
	    	dataSourceBoxModel.addElement(new NvpListItem(uriToDatasourceNameMap.get(d), d));
	    }		        
	    dataSourceList.setModel(dataSourceBoxModel);
	    dataSourceList.setToolTipText("Select Datasources");
	    dataSourceList.setAlignmentX(Component.LEFT_ALIGNMENT);
	        
	    JScrollPane dataSourceFilterBox = new JScrollPane(dataSourceList, 
	       	JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	    dataSourceFilterBox.setBorder(new TitledBorder("Datasource(s) in:"));
	    dataSourceFilterBox.setMinimumSize(new Dimension(200, 200));  

	    //TODO add a Search for a BioPAX Type combo-box
	    final JComboBox bpTypeComboBox = new JComboBox(
	    	new NvpListItem[] {
	    		new NvpListItem("Pathways","Pathway"),
	    		new NvpListItem("Interactions (all types)", "Interaction"),
	    		new NvpListItem("Entity states (mol./complex form, location)", "PhysicalEntity"),
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
	            	info.setText("Error: Please enter a Gene Name or ID.");
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
	        					SearchResponse searchResponse = (SearchResponse) client.search(keyword);
	        					// update hits model (also notifies observers!)
	        					hitsModel.update(searchResponse, panel);
	        					info.setText("Matches:  " + searchResponse.getNumHits() 
	        						+ "; retrieved: " + searchResponse.getSearchHit().size()
	        							+ " (page #" + searchResponse.getPageNo() + ")");
	        				} catch (CPathException e) {
	        					info.setText(e.getError().getErrorMsg()
	        						+ " (using query '" + keyword + "' and current filter values)");
	        					hitsModel.update(new SearchResponse(), searchQueryPanel); //clear
	        				} catch (Throwable e) { 
	        					// using Throwable helps catch unresolved runtime dependency issues!
	        					info.setText("Unknown Error.");
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
        searchQueryPanel.add(organismFilterBox);
        searchQueryPanel.add(dataSourceFilterBox);

        
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
						String queryUrl = newClient()
							.queryGet(Collections.singleton(uri));
				        taskManager.execute(new TaskIterator(
				        	new CpsNetworkAndViewTask(queryUrl, item.toString())));	
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
                		// get/create and show hit's summary
                		String summary = hitsModel.hitsSummaryMap.get(item.getUri());
                		detailsPanel.getTextPane().setText(summary);
                		detailsPanel.getTextPane().setCaretPosition(0);               		
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
	        
        // register the jlist as model's observer
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
        HitsFilterPanel filterPanel = new HitsFilterPanel(resList, hitsModel);
	        
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
        
        // add direction, limit options and the 'go' button
	        
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
        
        
        // add "execute" button
	    final JButton advQueryButton = new JButton("Execute");
	    advQueryButton.setToolTipText("Create a new network from a BioPAX graph query result");
	    advQueryButton.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	        	advQueryButton.setEnabled(false);
	           	
	        	//create souce and target lists of URIs
	        	Set<String> srcs = new HashSet<String>();
	        	Set<String> tgts = new HashSet<String>();
	        	for(int i=0; i < userList.getModel().getSize(); i++) {
	        		String uri = ((NvpListItem) userList.getModel().getElementAt(i)).getValue();
	        		if(userList.isSelectedIndex(i))
	        			srcs.add(uri);
	        		else
	        			tgts.add(uri);
	        	}
	        	
	        	//hitsModel.graphQueryClient uses direction and limit set by user
	        	String queryUrl = hitsModel.graphQueryClient.queryGet(srcs);
	        	
	        	if(hitsModel.graphType != null)
	        	switch (hitsModel.graphType) {
				case NEIGHBORHOOD:
					queryUrl = hitsModel.graphQueryClient.queryNeighborhood(srcs);
					break;
				case COMMONSTREAM:
					queryUrl = hitsModel.graphQueryClient.queryCommonStream(srcs);
					break;
				case PATHSBETWEEN:
					queryUrl = hitsModel.graphQueryClient.queryPathsBetween(srcs);
					break;
				case PATHSFROMTO:
					queryUrl = hitsModel.graphQueryClient.queryPathsFromTo(srcs, tgts);
					break;
				default:
					break;
				}

	        	taskManager.execute(new TaskIterator(new CpsNetworkAndViewTask(queryUrl, null)));
	        	
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
    	final HitsFilterPanel filterPanel = new HitsFilterPanel(tpwJList, topPathwaysModel);
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
						String queryUrl = newClient()
							.queryGet(Collections.singleton(uri));
				        taskManager.execute(new TaskIterator(
				        	new CpsNetworkAndViewTask(queryUrl, item.toString())));	
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
        panel.add(hSplit);        
	        
        // load pathways from server
		TaskIterator taskIterator = new TaskIterator(new Task() {
			@Override
			public void run(TaskMonitor taskMonitor) throws Exception {
				try {
					taskMonitor.setTitle("cPathSquared Task: Top Pathways");
					taskMonitor.setProgress(0.1);
					taskMonitor.setStatusMessage("Retrieving top pathways...");
					SearchResponse resp = newClient().getTopPathways();
					// reset the model and kick off observers (list and filter panel)
			        topPathwaysModel.update(resp, panel);
				} catch (Throwable e) { 
					//fail on both when there is no data (server error) and runtime/osgi errors
					throw new RuntimeException(e);
				} finally {
					taskMonitor.setStatusMessage("Done");
					taskMonitor.setProgress(1.0);
					Window parentWindow = ((Window) panel.getRootPane().getParent());
					panel.repaint();
					parentWindow.toFront();
				}
			}
			@Override
			public void cancel() {
			}
		});
			
		// kick off the task execution
		taskManager.execute(taskIterator);
	        
        panel.repaint();
        return panel;
	}
	
		
    /**
     * Creates a JTextPane with correct line wrap settings.
     *
     * @return JTextPane Object.
     */
    private JTextPane createHtmlTextPane(final JPanel context) {
        final JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBorder(new EmptyBorder(7,7,7,7));
        textPane.setContentType("text/html");
        textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        textPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
                if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
//                	browser.openURL(hyperlinkEvent.getURL().toString());
                	//import data and create network only if a special (name) link clicked
                	String queryUrl = hyperlinkEvent.getURL().toString();
                    if(queryUrl.startsWith(CyPath2.SERVER_URL)) {
    				        taskManager.execute(new TaskIterator(
    				        	new CpsNetworkAndViewTask(queryUrl, "")));// TODO a better network title (it'll be changed anyway...)?
                    }
                }
            }
        });

        HTMLDocument htmlDoc = (HTMLDocument) textPane.getDocument();
        StyleSheet styleSheet = htmlDoc.getStyleSheet();
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
     *
     */
    class CpsNetworkAndViewTask extends AbstractTask {
    	private String queryUrl;
    	private String networkTitle;

    	/**
    	 * Constructor.
    	 * 
    	 * @param queryUrl cPath2 URL (HTTP GET) query
    	 * @param networkTitle optional name for the new network
    	 */
    	public CpsNetworkAndViewTask(String queryUrl, String networkTitle) {
    		this.queryUrl = queryUrl;
    		this.networkTitle = networkTitle;
    	}

    	public void run(TaskMonitor taskMonitor) throws Exception {
    		String title = "Retrieving " + networkTitle + " from " 
   				+ CyPath2.SERVER_NAME + "...";
    		taskMonitor.setTitle(title);
    		try {
    			taskMonitor.setProgress(0);
    			taskMonitor.setStatusMessage("Retrieving BioPAX data...");
	    			
    			// Get Data: BioPAX and the other format data (not BioPAX if required)
    			CPath2Client cli = CyPath2.newClient();
    			//get data (throws exception if no results or internal error!)
    	    	final String biopaxData = cli.executeQuery(queryUrl, OutputFormat.BIOPAX);
    	    	
    	    	String data = (downloadMode == OutputFormat.BIOPAX) 
    	    		? biopaxData : cli.executeQuery(queryUrl, downloadMode);
	    	    	
    			taskMonitor.setProgress(0.4);
    			if (cancelled) return;
	    			
    			// Store BioPAX to Temp File
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
	    			
    			CyNetworkReader reader =  networkViewReaderManager
   					.getReader(tmpFile.toURI(), tmpFile.getName());	
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
	    			
    			if (downloadMode == OutputFormat.BINARY_SIF) {
    				//fix the network name
    				SwingUtilities.invokeLater(new Runnable() {
    					public void run() {
    						String networkTitleWithUnderscores = naming.getSuggestedNetworkTitle(networkTitle);
    						Attributes.set(cyNetwork, cyNetwork, CyNetwork.NAME, networkTitleWithUnderscores, String.class);
    					}
    				});
	    				
    				taskMonitor.setStatusMessage("Updating SIF network " +
    					"attributes from corresonding BioPAX data...");				
	    				
    				// Set the Quick Find Default Index
    				Attributes.set(cyNetwork, cyNetwork, "quickfind.default_index", CyNetwork.NAME, String.class);
    				// Specify that this is a BINARY_NETWORK
    				Attributes.set(cyNetwork, cyNetwork, "BIOPAX_NETWORK", Boolean.TRUE, Boolean.class);
    	
    				// we gonna need the full (original biopax) model to create attributes
    				final Model bpModel = BioPaxUtil.convertFromOwl(new ByteArrayInputStream(biopaxData.getBytes("UTF-8")));
    				
    				// Set node/edge attributes from the Biopax Model
    				for (CyNode node : cyNetwork.getNodeList()) {
    					CyRow row = cyNetwork.getRow(node);
    					String uri = row.get(CyNetwork.NAME, String.class);
    					BioPAXElement e = bpModel.getByID(uri);// can be null (for generic groups nodes)
    					if(e instanceof EntityReference 
    						|| e instanceof Complex 
    						|| (e != null && e.getModelInterface().equals(PhysicalEntity.class))) 
    						BioPaxUtil.createAttributesFromProperties(e, node, cyNetwork);
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
    			String cPathServerDetailsUrl = row.get(CPATH_SERVER_DETAILS_URL, String.class);
    			if (cPathServerDetailsUrl == null) {
    				Attributes.set(cyNetwork, cyNetwork, CPATH_SERVER_NAME_ATTRIBUTE, SERVER_NAME, String.class);
    				Attributes.set(cyNetwork, cyNetwork, CPATH_SERVER_DETAILS_URL, SERVER_URL, String.class);
    			}
	    			
    			taskMonitor.setProgress(0.9);
    			if (cancelled) return;
    			taskMonitor.setStatusMessage("Running the default layout algorithm...");

// biopax/sif app do this!
//    			// apply default layout
//    			CyLayoutAlgorithm layout = layoutManager.getDefaultLayout();
//    			Object context = layout.getDefaultLayoutContext();
//    			insertTasksAfterCurrentTask(layout.createTaskIterator(view, context, CyLayoutAlgorithm.ALL_NODE_VIEWS,""));	
    			view.updateView();
	    			
    		} finally {
    			taskMonitor.setStatusMessage("Done");
    			taskMonitor.setProgress(1.0);
    		}
    	}
	    		
    }	

}
