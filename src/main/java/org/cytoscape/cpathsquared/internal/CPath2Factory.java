package org.cytoscape.cpathsquared.internal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.io.read.CyNetworkReaderManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.undo.UndoSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import cpath.client.CPath2Client;
import cpath.client.util.CPathException;
import cpath.service.OutputFormat;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.TraverseResponse;

/** A "God" singleton object, which, once initialized, provides 
 *  access to injected Cy3 OSGi services and static constants 
 *  throughout all the implementation classes within this app.
 */
public final class CPath2Factory {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CPath2Factory.class);
	
	private static CySwingApplication application;
	private static TaskManager taskManager;
	private static OpenBrowser openBrowser;
	private static CyNetworkManager networkManager;
	private static CyApplicationManager applicationManager;
	private static CyNetworkViewManager networkViewManager;
	private static CyNetworkReaderManager networkViewReaderManager;
	private static CyNetworkNaming naming;
	private static CyNetworkFactory networkFactory;
	private static CyLayoutAlgorithmManager layoutManager;
	private static UndoSupport undoSupport;
	private static BinarySifVisualStyleFactory binarySifVisualStyleUtil;
	private static VisualMappingManager mappingManager;
	private static CyProperty cyProperty;
	
	public static final String JVM_PROPERTY_CPATH2_URL = "cPath2Url";
	public static final String DEFAULT_CPATH2_URL = "http://www.pathwaycommons.org/pc2/";	
    public static final String cPathUrl = System.getProperty(JVM_PROPERTY_CPATH2_URL, DEFAULT_CPATH2_URL);   
    public static final String serverName = "Pathway Commons (BioPAX L3)";
    public static final String INFO_ABOUT = 
    	"<span class='bold'>Pathway Commons 2</span> is a warehouse of " +
    	"biological pathway information integrated from public databases and " +
    	"persisted in BioPAX Level3 format, which one can search, traverse, download.";
    
    public static String iconToolTip  = "Import Pathway Data from Pathway Commons (cPathSquared web services, BioPAX L3)";
    
    public static String iconFileName = "pc.png";
    
    public static OutputFormat downloadMode = OutputFormat.BIOPAX;
        
	// this model is used by the filters panel (left) and hits jlist
	private static final HitsModel topPathwaysModel = new HitsModel(false);
    
	//create top pathways panel (north)
	private static final TopPathwaysJList tpwJList = new TopPathwaysJList();
	
    
	// non-instantiable static factory class
	private CPath2Factory() {
		throw new AssertionError();
	}
	
	public static void init(CySwingApplication app, TaskManager tm, OpenBrowser ob, 
			CyNetworkManager nm, CyApplicationManager am, CyNetworkViewManager nvm, 
			CyNetworkReaderManager nvrm, CyNetworkNaming nn, CyNetworkFactory nf, 
			CyLayoutAlgorithmManager lam, UndoSupport us, 
			BinarySifVisualStyleFactory bsvsf, VisualMappingManager mm,
			CyProperty prop) 
	{
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
	}

	public static OpenBrowser getOpenBrowser() {
		return openBrowser;
	}

	public static CySwingApplication getCySwingApplication() {
		return application;
	}

	public static TaskManager getTaskManager() {
		return taskManager;
	}

	public static CyNetworkManager getNetworkManager() {
		return networkManager;
	}

	public static CyApplicationManager getCyApplicationManager() {
		return applicationManager;
	}

	public static CyNetworkViewManager getCyNetworkViewManager() {
		return networkViewManager;
	}

	public static CyNetworkReaderManager getCyNetworkViewReaderManager() {
		return networkViewReaderManager;
	}

	public static CyNetworkNaming getCyNetworkNaming() {
		return naming;
	}

	public static CyNetworkFactory getCyNetworkFactory() {
		return networkFactory;
	}

	public static UndoSupport getUndoSupport() {
		return undoSupport;
	}

	public static CyNetworkManager getCyNetworkManager() {
		return networkManager;
	}

	public static CyLayoutAlgorithmManager getCyLayoutAlgorithmManager() {
		return layoutManager;
	}
	
	public static BinarySifVisualStyleFactory getBinarySifVisualStyleUtil() {
		return binarySifVisualStyleUtil;
	}

	public static CyNetworkViewManager getNetworkViewManager() {
		return networkViewManager;
	}

	public static CyNetworkReaderManager getNetworkViewReaderManager() {
		return networkViewReaderManager;
	}

	public static CyNetworkNaming getNaming() {
		return naming;
	}

	public static CyNetworkFactory getNetworkFactory() {
		return networkFactory;
	}

	public static CyLayoutAlgorithmManager getLayoutManager() {
		return layoutManager;
	}

	public static VisualMappingManager getMappingManager() {
		return mappingManager;
	}
    
    public static CPath2Client newClient() {
        CPath2Client client = CPath2Client.newInstance();
        client.setEndPointURL(cPathUrl);
		return client;
	}

	/**
     * Gets One or more records by Primary ID.
     * @param ids               Array of URIs.
     * @param format            Output format.
     * @return data string.
     * @throws EmptySetException    Empty Set Error.
     */
    public static String getRecordsByIds(String[] ids, OutputFormat format) 
    {
    	//TODO client to return other formats as well
    	CPath2Client cli = newClient();
//    	Model res = cli.get(Arrays.asList(ids));  
//    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        BioPaxUtil.getBiopaxIO().convertToOWL(res, baos);
//        
//        return baos.toString();
    	
    	String queryUrl = cli.queryGet(Arrays.asList(ids));
    	RestTemplate template = new RestTemplate();
    	
    	return template.getForObject(queryUrl, String.class);
    }


    public static Map<String, String> getAvailableOrganisms() {
        return newClient().getValidOrganisms();
    }

    
    public static Map<String, String> getLoadedDataSources() {
        return newClient().getValidDataSources();
    }


	public static SearchResponse topPathways(String keyword, Set<String> organism,
			Set<String> datasource) {
		return newClient().getTopPathways();
	}
	
	
    public static TraverseResponse traverse(String path, Collection<String> uris) 
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

    
    public static SearchResponse unmodifiableSearchResponce(final SearchResponse resp) {
    	if(resp == null)
    		return null;
    	
    	// create a read-only proxy/view
    	SearchResponse imm = new SearchResponse() {
			@Override
			public String getComment() {
				return resp.getComment();
			}

			@Override
			public Integer getMaxHitsPerPage() {
				return resp.getMaxHitsPerPage();
			}

			@Override
			public Integer getNumHits() {
				return resp.getNumHits();
			}

			@Override
			public Integer getPageNo() {
				return resp.getPageNo();
			}

			@Override
			public List<SearchHit> getSearchHit() {
				return Collections.unmodifiableList(resp.getSearchHit());
			}

			@Override
			public boolean isEmpty() {
				return resp.isEmpty();
			}

			@Override
			public void setComment(String comment) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setMaxHitsPerPage(Integer maxHitsPerPage) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setNumHits(Integer numHits) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setPageNo(Integer pageNo) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setSearchHit(List<SearchHit> searchHit) {
				throw new UnsupportedOperationException();
			}
    	};  	
    	
    	return imm;
    }
    
    
    /**
     * Gets a global Cytoscape property value. 
     * 
     * @param key
     * @return
     */
    public static Object getCyProperty(String key) {
		return ((Properties)cyProperty.getProperties()).getProperty(key);
	}
    
    
    
	/**
     * Creates a Titled Border with appropriate font settings.
     * @param title Title.
     * @return TitledBorder Object.
     */
    public static TitledBorder createTitledBorder (String title) {
        TitledBorder border = new TitledBorder(title);
        Font font = border.getTitleFont();
        Font newFont = new Font (font.getFamily(), Font.BOLD, font.getSize()+2);
        border.setTitleFont(newFont);
        border.setTitleColor(new Color(102,51,51));
        return border;
    }
    
    public static JScrollPane createOptionsPane() {
    	JPanel panel = new JPanel();
    	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    	
        // download oprions group
        final JRadioButton button1 = new JRadioButton("Download BioPAX");
        button1.setSelected(true);
        button1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                CPath2Factory.downloadMode = OutputFormat.BIOPAX;
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
                CPath2Factory.downloadMode = OutputFormat.BINARY_SIF;
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


	public synchronized static JPanel createTopPathwaysPanel() {
		
		final JPanel panel = new JPanel(); // to return       
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        //  Create Info Panel (the first tab)
        final DetailsPanel detailsPanel = new DetailsPanel();
        final JTextPane summaryTextPane = detailsPanel.getTextPane();
        
        // make (south) tabs
        JTabbedPane southPane = new JTabbedPane(); 
        southPane.add("Summary", detailsPanel);

		//  Create the 'off-line' filtering panel
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
						//TODO execute download network task

				        CPath2Factory.getTaskManager().execute(new TaskIterator(
				        	new ExecuteGetByUriTask(new String[]{uri}, 
				        		CPath2Factory.downloadMode, "Downloading " +
				        			item.getName())));	
					}
				}
			}
		});
		// add the list to model's observers
        topPathwaysModel.addObserver(tpwJList);
        
        JPanel tpwFilterListPanel = new JPanel();
        tpwFilterListPanel.setBorder(new TitledBorder("Type in the text field " +
        		"to filter; double-click to download (creates a new network)!"));
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
        panel.repaint();
            
        return panel;
	}

	
	/**
	 * Loads the list of top pathways from the server
	 * if it hasn't been done already.
	 * 
	 * @param topPathwaysModel
	 * @param tpwJList
	 */
	public static synchronized void loadTopPathwaysOnce() {
		if(topPathwaysModel.getSearchResponse() != null) 
			return; // already done!
		
		TaskIterator taskIterator = new TaskIterator(new Task() {
			@Override
			public void run(TaskMonitor taskMonitor) throws Exception {
				try {
					taskMonitor.setTitle("cPathSquared Task: Top Pathways");
					taskMonitor.setProgress(0.1);
					taskMonitor.setStatusMessage("Retrieving top pathways...");
					SearchResponse resp = CPath2Factory.newClient().getTopPathways();
					// reset the model and kick off observers (list and filter panel)
			        topPathwaysModel.update(resp);
				} catch (Throwable e) { 
					//fail on both when there is no data (server error) and runtime/osgi errors
					throw new RuntimeException(e);
				} finally {
					taskMonitor.setStatusMessage("Done");
					taskMonitor.setProgress(1.0);
				}
			}
			@Override
			public void cancel() {
			}
		});
		
		// kick off the task execution
		CPath2Factory.getTaskManager().execute(taskIterator);
	}

	
    static class TopPathwaysJList extends FilterdJList<SearchHit> implements Observer {
    	
    	@Override
    	public String getToolTipText(MouseEvent mouseEvent) {
    		int index = locationToIndex(mouseEvent.getPoint());
    		if (-1 < index) {
    			SearchHit record = (SearchHit) getModel().getElementAt(index);
    			StringBuilder html = new StringBuilder();
    			html.append("<html><table cellpadding=10><tr><td>");
    			html.append("<B>");
    			if(!record.getDataSource().isEmpty())
    				html.append("&nbsp;").append(record.getDataSource().toString());
    			if(!record.getOrganism().isEmpty())
    				html.append("&nbsp;").append(record.getOrganism().toString());
    			html.append("</B>&nbsp;");
    			html.append("</td></tr></table></html>");
    			return html.toString();
    		} else {
    			return null;
    		}
    	}

		@Override
		public void update(Observable o, Object arg) {
			SearchResponse resp = (SearchResponse) arg;
			FilterListModel<SearchHit> lm = (FilterListModel<SearchHit>) this.getModel();
			lm.clear();
			List<SearchHit> searchHits = resp.getSearchHit();
			if (!searchHits.isEmpty())
				for (SearchHit searchHit : searchHits)
					lm.addElement(searchHit);	
		}
		
		@Override
		public synchronized ListModel getModel() {
			return super.getModel();
		}
    }

	
    static class FilterdJList<T> extends JList {
    	private final FilterField<T> filterField;
    	private final int DEFAULT_FIELD_WIDTH = 20;
    	
    	public FilterdJList() {
    		setModel(new FilterListModel<T>());
			filterField = new FilterField<T>(DEFAULT_FIELD_WIDTH);
		}
    	   	
    	public void setModel(ListModel m) {
    		if(m instanceof FilterListModel)
    			super.setModel(m);
    		else 
    			throw new IllegalArgumentException("is not a FilterListMode!");
		}  	
    	
    	public JTextField getFilterField() {
			return filterField;
		}
    	
    	class FilterField<E> extends JTextField implements DocumentListener {

    		public FilterField(int width) {
				super(width);
				getDocument().addDocumentListener(this);
			}
    		
			@Override
			public void changedUpdate(DocumentEvent arg0) {
				((FilterListModel<E>)getModel()).refilter();
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				((FilterListModel<E>)getModel()).refilter();
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				((FilterListModel<E>)getModel()).refilter();
			}	
    	}
    	    	
		class FilterListModel<E> extends DefaultListModel {
			ArrayList<E> items;
			ArrayList<E> filterItems;

			private synchronized List<E> items() {
				return items;
			}
			
			private synchronized List<E> filterItems() {
				return filterItems;
			}
			
			public FilterListModel() {
				items = new ArrayList<E>();
				filterItems = new ArrayList<E>();
			}

			@Override
			public Object getElementAt(int index) {
				if (index < filterItems().size())
					return filterItems().get(index);
				else
					return null;
			}

			@Override
			public int getSize() {
				return filterItems().size();
			}
			
			@Override
			public void addElement(Object o) {
				items().add((E) o);
				refilter();
			}

			@Override
			public void setElementAt(Object obj, int index) {
				throw new UnsupportedOperationException();
			}
			
			private void refilter() {
				filterItems().clear();
				String term = getFilterField().getText();
				for (E it : items())
					if (it.toString().indexOf(term, 0) != -1)
						filterItems().add(it);
				fireContentsChanged(this, 0, getSize());
			}
			
			@Override
			public void clear() {
				super.clear();
				items().clear();
				refilter();
			}
		}
    }

	public static JPanel createSearchPanel() {
		return new SearchPanel();
	}
}
