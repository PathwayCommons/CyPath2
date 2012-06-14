package org.cytoscape.cpathsquared.internal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.cytoscape.cpathsquared.internal.HitsModel.SearchFor;
import org.cytoscape.util.swing.CheckBoxJList;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import cpath.client.CPath2Client;
import cpath.client.util.NoResultsFoundException;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;


final class SearchPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private final HitsModel hitsModel;
	private final JList resList; 
    private final JTextPane summaryTextPane;
    private final DetailsPanel detailsPanel;
    private final JScrollPane ppwListScrollPane;
    private static final String ENTER_TEXT = "Enter a keyword (e.g., gene/protein name or ID)";
    private final CheckBoxJList organismList;
    private final CheckBoxJList dataSourceList;
    private final JTextField searchField;
    private final JLabel info;
    private final JButton searchButton;

	public SearchPanel() 
    {	   	 	
		this.hitsModel = new HitsModel(true);
		
		setLayout(new BorderLayout());
		
		// Assembly the query panel
		JPanel searchQueryPanel = new JPanel();
		searchQueryPanel.setLayout(new BorderLayout());
		
    	organismList = new CheckBoxJList();
    	dataSourceList = new CheckBoxJList();
    	searchField = createSearchField();
        
        // create query field, examples/label, and button
        searchButton = new JButton("Search");
        searchButton.setToolTipText("Full-Text Search");
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
            	searchButton.setEnabled(false);
                executeSearch(searchField.getText(), 
                	organismList.getSelectedValues(), 
                	dataSourceList.getSelectedValues(),
                	hitsModel.searchFor.toString());
            }
        });
        searchButton.setAlignmentX(Component.LEFT_ALIGNMENT);  
                
        PulsatingBorder pulsatingBorder = new PulsatingBorder (searchField);
        searchField.setBorder (BorderFactory.createCompoundBorder(searchField.getBorder(),
                pulsatingBorder));
        searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchField.setMaximumSize(new Dimension(1000, 100));
        
        JEditorPane label = new JEditorPane (
        		"text/html", "Examples:  <a href='TP53'>TP53</a>, " +
                "<a href='BRCA*'>BRCA*</a>, or <a href='SRY'>SRY</a>.");
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
               
        info = new JLabel("", SwingConstants.CENTER);
        info.setFocusable(false);
        info.setFont(new Font(info.getFont().getFamily(), info.getFont().getStyle(), info.getFont().getSize()+1));
        info.setForeground(Color.BLUE);
        
        final JRadioButton button1 = new JRadioButton("pathways");
        button1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                hitsModel.searchFor = SearchFor.PATHWAY;
            }
        });
        //default option (2)
        final JRadioButton button2 = new JRadioButton("interactions");
        button2.setSelected(true);
        hitsModel.searchFor = SearchFor.INTERACTION;
        button2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
            	hitsModel.searchFor = SearchFor.INTERACTION;
            }
        });
        final JRadioButton button3 = new JRadioButton("physical entities");
        button3.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
            	hitsModel.searchFor = SearchFor.PHYSICALENTITY;
            }
        });

        ButtonGroup group = new ButtonGroup();
        group.add(button1);
        group.add(button2);
        group.add(button3);    
        
    	JPanel groupPanel = new JPanel();
        groupPanel.setBorder(new TitledBorder("Search for"));
        groupPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        groupPanel.add(button1, c);
        c.gridx = 0;
        c.gridy = 1;
        groupPanel.add(button2, c);
        c.gridx = 0;
        c.gridy = 2;
        groupPanel.add(button3, c);
        groupPanel.setMaximumSize(new Dimension(50, 100));
        
        searchQueryPanel.add(groupPanel, BorderLayout.LINE_START);
        searchQueryPanel.add(createOrganismFilterBox(), BorderLayout.CENTER);
        searchQueryPanel.add(createDataSourceFilterBox(), BorderLayout.LINE_END);
       
        JPanel keywordPane = new JPanel();
        keywordPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        keywordPane.add(searchButton);
        keywordPane.add(searchField);    
        keywordPane.add(label);
        keywordPane.setMaximumSize(new Dimension(1000, 15));
        
        searchQueryPanel.add(keywordPane, BorderLayout.PAGE_START);
        searchQueryPanel.add(info, BorderLayout.PAGE_END);
    	
        // Assembly the results panel
    	JPanel searchResultsPanel = new JPanel();
    	searchResultsPanel.setLayout(new BoxLayout(searchResultsPanel, BoxLayout.Y_AXIS));
    	
        detailsPanel = new DetailsPanel();
        summaryTextPane = detailsPanel.getTextPane();
    
        //create parent pathways panel (the second tab)
        final JList ppwList = new JList(new DefaultListModel());
        ppwList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        ppwList.setPrototypeCellValue("12345678901234567890");
        JPanel ppwListPane = new JPanel();
        ppwListPane.setLayout(new BorderLayout());
        ppwListScrollPane = new JScrollPane(ppwList);
        ppwListScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        ppwListPane.add(ppwListScrollPane, BorderLayout.CENTER);
                      
        // make (south) tabs
        JTabbedPane southPane = new JTabbedPane(); 
        southPane.add("Summary", detailsPanel);
        southPane.add("Parent Pathways", ppwListPane);
  
        // search hits list
        resList = new ToolTipsSearchHitsJList();
        resList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resList.setPrototypeCellValue("12345678901234567890");
        // define a list item selection listener which updates the details panel, etc..
        resList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
            	ToolTipsSearchHitsJList l = (ToolTipsSearchHitsJList) listSelectionEvent.getSource();
                int selectedIndex = l.getSelectedIndex();
                //  ignore the "unselect" event.
                if (!listSelectionEvent.getValueIsAdjusting()) {
                    if (selectedIndex >=0) {
                    	SearchHit item = (SearchHit) l.getModel().getElementAt(selectedIndex);
                		// get/create and show hit's summary
                		String summary = hitsModel.hitsSummaryMap.get(item.getUri());
                		summaryTextPane.setText(summary);
                		summaryTextPane.setCaretPosition(0);
                		
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
        // register the jlist as model's observer
        hitsModel.addObserver((Observer) resList);
        
        JPanel hitListPane = new JPanel();
        hitListPane.setLayout(new BorderLayout());
        JScrollPane hitListScrollPane = new JScrollPane(resList);
        hitListScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        hitListScrollPane.setBorder(new TitledBorder("Double-click to include/exclude an item to/from the network!"));
        // make (north) tabs       
        JSplitPane vSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, hitListScrollPane, southPane);
        vSplit.setDividerLocation(200);
        hitListPane.add(vSplit, BorderLayout.CENTER);
        
        //  Create search results extra filtering panel
        HitsFilterPanel filterPanel = new HitsFilterPanel(resList, hitsModel);
        
        //  Create the Split Pane
        JSplitPane hSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, filterPanel, hitListPane);
        hSplit.setDividerLocation(200);
        hSplit.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchResultsPanel.add(hSplit);
        
        // finish
        JSplitPane queryAndResults = new JSplitPane(JSplitPane.VERTICAL_SPLIT, searchQueryPanel, searchResultsPanel);
        queryAndResults.setDividerLocation(160);
        add(queryAndResults);         
    }

      
    private final JComponent createOrganismFilterBox() {	
    	
    	Task task = new Task() {	
			@Override
			public void run(TaskMonitor taskMonitor) throws Exception {
		    	Map<String,String> map = CpsFactory.getAvailableOrganisms();

		    	//make sorted by name list
		    	SortedSet<NvpListItem> items = new TreeSet<NvpListItem>();
		    	for(String o : map.keySet()) {
		    		items.add(new NvpListItem(map.get(o), o));
		    	}
		    	DefaultListModel model = new DefaultListModel();
		    	for(NvpListItem nvp : items) {
		    		model.addElement(nvp);
		    	}
		    	organismList.setModel(model);
		        organismList.setToolTipText("Select Organisms");
		        organismList.setAlignmentX(Component.LEFT_ALIGNMENT);
			}
			
			@Override
			public void cancel() {
			}
		};
        
        JScrollPane scroll = new JScrollPane(organismList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(new TitledBorder("Limit to organism(s):"));
        
        CpsFactory.execute(new TaskIterator(task));
        
        return scroll;
    }
    
    private final JComponent createDataSourceFilterBox() {
        
    	TaskIterator iterator = new TaskIterator(new Task() {
			@Override
			public void run(TaskMonitor taskMonitor) throws Exception {
		    	DefaultListModel dataSourceBoxModel = new DefaultListModel(); 
		        Map<String,String> map = CpsFactory.getLoadedDataSources();
		    	for(String d : map.keySet()) {
		    		dataSourceBoxModel.addElement(new NvpListItem(map.get(d), d));
		    	}
		        
		        dataSourceList.setModel(dataSourceBoxModel);
		        dataSourceList.setToolTipText("Select Datasources");
		        dataSourceList.setAlignmentX(Component.LEFT_ALIGNMENT);
			}

			@Override
			public void cancel() {
			}
    	});
        
        
        JScrollPane scroll = new JScrollPane(dataSourceList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(new TitledBorder("Limit to datasource(s):"));
        
        CpsFactory.execute(iterator);  
        
        return scroll;
    }
    
    
    /**
     * Creates the Search Field and associated listener(s)
     *
     * @return JTextField Object.
     */
    private final JTextField createSearchField() {
        final JTextField searchField = new JTextField(ENTER_TEXT.length());
        searchField.setText(ENTER_TEXT);
        searchField.setToolTipText(ENTER_TEXT);
        searchField.setMaximumSize(new Dimension(200, 9999));
        searchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent focusEvent) {
                if (searchField.getText() != null
                        && searchField.getText().startsWith("Enter")) {
                    searchField.setText("");
                }
            }
        });
        return searchField;
    }
    
    
    /**
     * Prepares and executes a search query.
     * 
     * @param keyword
     * @param organism
     * @param datasource
     * @param biopaxType
     */
    private void executeSearch(final String keyword, 
    		final Object[] organism, 
    		final Object[] datasource,
    		final String biopaxType) {
        
        final Set<String> organisms = new HashSet<String>();
        for(Object it : organism)
        	organisms.add(((NvpListItem) it).getValue());
        
    	final Set<String> datasources = new HashSet<String>();
        for(Object it : datasource)
        	datasources.add(((NvpListItem) it).getValue());
    	
        if (keyword == null || keyword.trim().length() == 0 || keyword.startsWith(ENTER_TEXT)) {
			info.setText("Error: Please enter a Gene Name or ID!");
			searchButton.setEnabled(true);
		} else {
			info.setText("");
			Task search = new Task() {
				@Override
				public void run(TaskMonitor taskMonitor) throws Exception {
					try {
						taskMonitor.setProgress(0);
						taskMonitor.setStatusMessage("Executing search for " + keyword);
						CPath2Client client = CpsFactory.newClient();
						client.setOrganisms(organisms);
						client.setType(biopaxType);
						if (datasource != null)
							client.setDataSources(datasources);
						
						SearchResponse searchResponse = (SearchResponse) client.search(keyword);
						// update hits model (also notifies observers!)
						hitsModel.update(searchResponse);
						info.setText("Hits found:  " + searchResponse.getNumHits() 
								+ "; retrieved: " + searchResponse.getSearchHit().size()
								+ " (page: " + searchResponse.getPageNo() + ")");
					} catch (NoResultsFoundException e) {
						info.setText("No match for:  " + keyword
							+ " and current filter values (try again)");
						hitsModel.update(new SearchResponse()); //clear
					} catch (Throwable e) { 
						// using Throwable helps catch unresolved runtime dependency issues!
						info.setText("Failed:  " + e);
						throw new RuntimeException(e);
					} finally {
						taskMonitor.setStatusMessage("Done");
						taskMonitor.setProgress(1);
						searchButton.setEnabled(true);
					}
				}

				@Override
				public void cancel() {
				}
			};

			CpsFactory.execute(new TaskIterator(search));
		}
    }    
 
    
	class ToolTipsSearchHitsJList extends JList implements Observer {

		public ToolTipsSearchHitsJList() {
			super(new DefaultListModel());
		}

		@Override
		public String getToolTipText(MouseEvent mouseEvent) {
			int index = locationToIndex(mouseEvent.getPoint());
			if (index >= 0 && getModel() != null) {
				SearchHit record = (SearchHit) getModel().getElementAt(index);
				StringBuilder html = new StringBuilder();
				html.append("<html><table cellpadding=10><tr><td>");
				html.append("<B>").append(record.getBiopaxClass());
				if (!record.getDataSource().isEmpty())
					html.append("&nbsp;").append(
							record.getDataSource().toString());
				if (!record.getOrganism().isEmpty())
					html.append("&nbsp;").append(
							record.getOrganism().toString());
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
			DefaultListModel lm = (DefaultListModel) this.getModel();
			lm.clear();
			for (SearchHit searchHit : resp.getSearchHit())
				lm.addElement(searchHit);
		}

		
		@Override
		public synchronized ListModel getModel() {
			return super.getModel();
		}
	}
}