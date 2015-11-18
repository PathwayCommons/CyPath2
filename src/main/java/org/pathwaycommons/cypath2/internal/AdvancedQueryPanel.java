package org.pathwaycommons.cypath2.internal;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.cytoscape.work.TaskIterator;

import cpath.client.CPathClient;
import cpath.client.CPathClient.Direction;
import cpath.query.CPathGetQuery;
import cpath.query.CPathGraphQuery;
import cpath.service.GraphType;

final class AdvancedQueryPanel extends JPanel {
	
	GraphType graphType;
	CPathClient.Direction direction;
	
	final JList list;
	
	
	public AdvancedQueryPanel(JList list) {
		this.list = list;
		graphType = GraphType.NEIGHBORHOOD;
		direction = Direction.DOWNSTREAM;
		create();
	}
	
	
	private void create() 
	{	   	 		    			
	  	setLayout(new BorderLayout());
	  	setPreferredSize(new Dimension(800, 600));
           
        //create adv. query panel with user picked items list 
        final JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setPreferredSize(new Dimension(400, 300));
        
        //add radio buttons for different query types
    	final JPanel queryTypePanel = new JPanel();
        queryTypePanel.setBorder(CyPC.createTitledBorder("Graph Query Type"));
        queryTypePanel.setLayout(new GridBagLayout());   
        
        //create direction buttons in advance (to disable/enable)
        final JRadioButton both = new JRadioButton("Both directions");
        final JRadioButton down = new JRadioButton("Downstream"); 
        final JRadioButton up = new JRadioButton("Upstream"); 
        
	    ButtonGroup bg = new ButtonGroup();
	    JRadioButton b = new JRadioButton("Get (interactions/pathways by URIs)");    
        //default option (1)
	    b.setSelected(true);
	    graphType = null;
	    b.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	            graphType = null; //to use "get" command instead of "graph"
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
	        	graphType = GraphType.NEIGHBORHOOD;
	        	both.setEnabled(true);
	        	up.setEnabled(true);
	        	down.setEnabled(true);
	        	both.setSelected(true);
	        	direction = Direction.BOTHSTREAM;
	        }
	    });
	    bg.add(b);
        c.gridx = 0;
        c.gridy = 1;
        queryTypePanel.add(b, c);
	    
	    b = new JRadioButton("Common Stream");
	    b.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	           	graphType = GraphType.COMMONSTREAM;
	        	both.setEnabled(false);
	        	up.setEnabled(true);
	        	down.setEnabled(true);
	        	down.setSelected(true);
	        	direction = Direction.DOWNSTREAM;
	        }
	    });
	    bg.add(b);
        c.gridx = 0;
        c.gridy = 2;
        queryTypePanel.add(b, c);
	    
	    b = new JRadioButton("Paths Beetween");
	    b.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	        	graphType = GraphType.PATHSBETWEEN;
	        	both.setEnabled(false);
	        	up.setEnabled(false);
	        	down.setEnabled(false);
	        	direction = null;
	        }
	    });
	    bg.add(b);
        c.gridx = 0;
        c.gridy = 3;
        queryTypePanel.add(b, c);
	    
	    b = new JRadioButton("Paths From (selected) To (the rest)");
	    b.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	        	graphType = GraphType.PATHSFROMTO;
	        	both.setEnabled(false);
	        	up.setEnabled(false);
	        	down.setEnabled(false);
	        	direction = null;
	        }
	    });
	    bg.add(b);
        c.gridx = 0;
        c.gridy = 4;
        queryTypePanel.add(b, c);
        
        queryTypePanel.setMaximumSize(new Dimension(400, 150));           
        controlPanel.add(queryTypePanel);
        
        // add direction, limit options and the 'go' button to the panel	        
    	JPanel directionPanel = new JPanel();
    	directionPanel.setBorder(CyPC.createTitledBorder("Direction"));
    	directionPanel.setLayout(new GridBagLayout());
    	bg = new ButtonGroup();	    
    	down.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	        	direction = Direction.DOWNSTREAM;
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
	        	direction = Direction.UPSTREAM;
	        }
	    });
	    bg.add(up);
        c.gridx = 0;
        c.gridy = 1;
        directionPanel.add(up, c);
	    
	    both.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	        	direction = Direction.BOTHSTREAM;
	        }
	    });
	    bg.add(both);
        c.gridx = 0;
        c.gridy = 2;
        directionPanel.add(both, c);
    		
        directionPanel.setMaximumSize(new Dimension(400, 200));
        controlPanel.add(directionPanel);
               
        // add "execute" (an advanced query) button
	    final JButton execQueryButton = new JButton("Execute Query and Create Network");
	    execQueryButton.setToolTipText("Runs a BioPAX graph query, " +
	    		"downloads results, builds a new network and view");
	    execQueryButton.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {	        	
	        	if(list.getSelectedIndices().length == 0) {
	        		JOptionPane.showMessageDialog(controlPanel, 
	        			"No items were selected from the list. " +
	        			"Please pick one or several to be used with the query.");
	        		return;
	        	}
	        		        	
	        	execQueryButton.setEnabled(false);
	           	
	        	//create source and target lists of URIs
	        	Set<String> srcs = new HashSet<String>();
	        	Set<String> tgts = new HashSet<String>();
	        	for(int i=0; i < list.getModel().getSize(); i++) {
	        		String uri = ((NvpListItem) list.getModel()
	        				.getElementAt(i)).getValue();
	        		if(list.isSelectedIndex(i))
	        			srcs.add(uri);
	        		else
	        			tgts.add(uri);
	        	}
	        	
	        	if(graphType == null) {
	        		final CPathGetQuery getQ = CyPC.client
	        				.createGetQuery().sources(srcs);
	        		CyPC.cyServices.taskManager.execute(new TaskIterator(
	        			new NetworkAndViewTask(CyPC.cyServices, getQ, null)
	        			));
	        	} else {
	        		final CPathGraphQuery graphQ = CyPC.client
	        			.createGraphQuery()
	        			.kind(graphType)
	        			.sources(srcs).targets(tgts)
	        			.datasourceFilter(CyPC.options.selectedDatasources())
	        			.direction(direction)
	        			//.limit(1) TODO set limit (optional; default is 1)
	        			.organismFilter(CyPC.options.selectedOrganisms());
	        		CyPC.cyServices.taskManager.execute(new TaskIterator(
	        			new NetworkAndViewTask(CyPC.cyServices, graphQ, null)
		        		));
	        	}
	        	
	        	execQueryButton.setEnabled(true);
	        }
	    });	
	    
	    final JPanel execQueryButtonPanel = new JPanel();
	    execQueryButtonPanel.setMaximumSize(new Dimension(400, 100));
	    execQueryButtonPanel.add(execQueryButton);
	    controlPanel.add(execQueryButtonPanel);
    
        // add the sources/targets list and the label
	    JEditorPane infoLabel = new JEditorPane ("text/html", 
	    	"<ol><li>Use bio entities from search results (in Search tab, double-click on a hit) " +
	    	"or enter standard gene, protein, metabolite identifiers into the input field below.</li>" +
	    	"<li>Review and refine (double-click to remove an item).</li>" +
	    	"<li>Select one or multiple items (sources).</li>" +
	    	"<li>Select query type and direction in the right panel.</li>" +
	    	"<li>Click 'Execute Query' (gets BioPAX data and creates new network and view).</li></ol><br/>");
	    infoLabel.setEditable(false);
	    infoLabel.setOpaque(false);
	    infoLabel.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
	    infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		infoLabel.setBorder(new EmptyBorder(5,3,5,3));
	    final Font font = infoLabel.getFont();
	    Font newFont = new Font (font.getFamily(), font.getStyle(), font.getSize()-2);
	    infoLabel.setFont(newFont);
		infoLabel.setPreferredSize(new Dimension(800, 120));
		
		JScrollPane listPane = new JScrollPane(list);
		listPane.setToolTipText("The list of URIs (of BioPAX elements) from search results " +
	    	"or standard identifiers from the input field below to be used with a BioPAX graph query.");
        listPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        listPane.setBorder(CyPC.createTitledBorder("Select sources/targets from:"));
        listPane.setPreferredSize(new Dimension(400, 400));
        listPane.setMinimumSize(new Dimension(400, 200));
        
		// create the custom query field and examples label
	  	final JTextField inputField = new JTextField(0);
	    inputField.setBorder(BorderFactory.createCompoundBorder(
	    		CyPC.createTitledBorder("Add more items by ID:"),//inputField.getBorder(),
	    		new PulsatingBorder(inputField)));
	    inputField.setAlignmentX(Component.LEFT_ALIGNMENT);
	    inputField.setPreferredSize(new Dimension(400, 50));
	    
	    JEditorPane inputFieldLabel = new JEditorPane ("text/html", 
	    		"Example: <a href='BRCA1, MDM2'>BRCA1, MDM2</a> (space/comma separated IDs).");
	    inputFieldLabel.setEditable(false);
	    inputFieldLabel.setOpaque(false);
	    inputFieldLabel.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
	    inputFieldLabel.addHyperlinkListener(new HyperlinkListener() {
	        // Update input field with the example.
	        public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
	            if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	                inputField.setText(hyperlinkEvent.getDescription());
	            }
	        }
	    });
	    inputFieldLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    inputFieldLabel.setFont(newFont);
	    inputFieldLabel.setBorder(new EmptyBorder(5,3,1,3));
	    inputFieldLabel.setPreferredSize(new Dimension(400, 25)); 
	    
	    final JCheckBox checkInputValuesBox = new JCheckBox("Check availability before adding"); 	    
	    
        // process the input IDs button
	    final JButton inputFieldButton = new JButton("Add to List");
	    inputFieldButton.setToolTipText("Adds the identifiers " +
	    		"from the input box to the sources/targets list " +
	    		"(optionally, checks availability first)");
	    inputFieldButton.addActionListener(new ActionListener() {	    	
			public void actionPerformed(ActionEvent actionEvent) {
	        	final String inputFieldValue = inputField.getText();
	        	//exit (do nothing) when no input provided
	        	if(inputFieldValue.isEmpty()) 
	        		return;
	        	//split
	        	final String[] inputIds = inputFieldValue.split("[,\\s]+");	        	
	        	// to report IDs for which there are no Xrefs in the database
	        	final Set<String> notFound = new HashSet<String>();

//				Adding new items to the list
				CyPC.cachedThreadPool.execute(new Runnable() {
					@Override
					public void run() {
						try {
							inputFieldButton.setEnabled(false);
							notFound.clear();
							//re-usable query object ('sources' parameter is to be set every time it's used)
							final CPathGetQuery query = CyPC.client.createGetQuery();
							int i = 0;
							for (String id : inputIds) {
								i++;
								//skip empty
								if (id.trim().isEmpty()) continue;

								// Here we exploit "Get by ID" cpath2 web service feature,
								// which can return a single BioPAX Xref object if a single ID was
								// used (instead of URI) in the query, and the Xref it maps to
								// actually exists in the Pathway Commons database.
								if (checkInputValuesBox.isSelected()) {
									String result = query.sources(Collections.singleton(id)).stringResult(null); //gets BioPAX
									if (result != null) {
										NvpListItem newItem = new NvpListItem(id, id); //id can be used as URI in (less specific) graph queries
										if (!((DefaultListModel) list.getModel()).contains(newItem))
											((DefaultListModel) list.getModel()).addElement(newItem);
									} else {
										notFound.add(id);
									}
								} else {
									NvpListItem newItem = new NvpListItem(id, id);
									if (!((DefaultListModel) list.getModel()).contains(newItem))
										((DefaultListModel) list.getModel()).addElement(newItem);
								}
							}

							if (!notFound.isEmpty()) {
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										JOptionPane.showMessageDialog(inputField, "The following IDs were not added " +
												"(no xrefs found in the database): " + notFound);
									}
								});
							}
						} catch (final Exception e) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									JOptionPane.showMessageDialog(inputField,
											"Failed to check the input and update the list: " + e);
								}
							});
						} finally {
							inputFieldButton.setEnabled(true);
						}
					}
				});
	        }
	    }); 
        
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));       
        inputPanel.add(listPane);
        inputPanel.add(inputField); 
        inputPanel.add(inputFieldLabel);
        inputPanel.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputFieldButton, checkInputValuesBox));
        add(infoLabel, BorderLayout.NORTH);
		add(inputPanel, BorderLayout.CENTER);
		add(controlPanel, BorderLayout.EAST);
    }	
}
