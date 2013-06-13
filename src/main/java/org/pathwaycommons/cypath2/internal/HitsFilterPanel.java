package org.pathwaycommons.cypath2.internal;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import cpath.service.jaxb.SearchHit;


final class HitsFilterPanel extends JPanel implements Observer {
	private static final long serialVersionUID = 1L;
    private final HitsModel hitsFilterModel;
    private final JList hitsJList;
    private final CheckNode rootNode;
    private final CheckNode typeFilterNode;
    private final CheckNode dataSourceFilterNode;
    private final CheckNode organismFilterNode;
    private final JTreeWithCheckNodes tree;
    private final CollapsablePanel filterTreePanel;
    private final boolean typeFilterEnabled;
    private final boolean organismFilterEnabled;
    private final boolean datasourceFilterEnabled;


    public HitsFilterPanel(final JList hitsJList, final HitsModel hitsModel, 
		boolean typeFilterEnabled, boolean organismFilterEnabled, boolean datasourceFilterEnabled) 
	{
        this.hitsFilterModel = hitsModel;
        this.hitsJList = hitsJList;
        this.typeFilterEnabled = typeFilterEnabled;
        this.organismFilterEnabled = organismFilterEnabled;
        this.datasourceFilterEnabled = datasourceFilterEnabled;
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        // create an empty filter tree (barebone)
        rootNode = new CheckNode("All");
        
        if(typeFilterEnabled) {
        	typeFilterNode = new CheckNode("BioPAX Type");
        	rootNode.add(typeFilterNode);
		} else typeFilterNode = null;
        
        if(organismFilterEnabled) {
        	organismFilterNode = new CheckNode("Organism");
        	rootNode.add(organismFilterNode);
        } else organismFilterNode = null;
        
        if(datasourceFilterEnabled) {
        	dataSourceFilterNode = new CheckNode("Datasource");
        	rootNode.add(dataSourceFilterNode);
        } else dataSourceFilterNode = null;
        
        tree = new JTreeWithCheckNodes(rootNode);
        tree.setOpaque(false);
        filterTreePanel = new CollapsablePanel("Filter");
        filterTreePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterTreePanel.getContentPane().add(tree);

        JScrollPane scrollPane = new JScrollPane(filterTreePanel);
        add(scrollPane);
        
        hitsFilterModel.addObserver(this);
    }

	
    /**
     * Expands all Nodes.
     */
    public void expandAllNodes() {
    	TreePath path;
    	filterTreePanel.setCollapsed(false);
        
    	if(typeFilterEnabled) {
    		typeFilterNode.setSelected(true);
    		path = new TreePath(typeFilterNode.getPath());
    		tree.expandPath(path);
    	}
        
    	if(datasourceFilterEnabled) {
    		dataSourceFilterNode.setSelected(true);
    		path = new TreePath(dataSourceFilterNode.getPath());
    		tree.expandPath(path);
    	}
        
    	if(organismFilterEnabled) {
    		organismFilterNode.setSelected(true);
    		path = new TreePath(organismFilterNode.getPath());
    		tree.expandPath(path);
    	}
    }

    
	private void applyFilter() {
		
        ChainedFilter chainedFilter = new ChainedFilter();
        
        if(typeFilterEnabled) {
        	Set<String> entityTypeSet = new HashSet<String>();
        	for (int i = 0; i < typeFilterNode.getChildCount(); i++) {
        		CheckNode checkNode = (CheckNode) typeFilterNode.getChildAt(i);
        		CategoryCount categoryCount = (CategoryCount) checkNode.getUserObject();
        		String name = categoryCount.getCategoryName();
        		if (checkNode.isSelected()) {
        			entityTypeSet.add(name);
        		}
			}
        	EntityTypeFilter entityTypeFilter = new EntityTypeFilter(entityTypeSet);
			chainedFilter.addFilter(entityTypeFilter);
        }
		
		if (organismFilterEnabled) {
			Set<String> entityOrganismSet = new HashSet<String>();
			for (int i = 0; i < organismFilterNode.getChildCount(); i++) {
				CheckNode checkNode = (CheckNode) organismFilterNode.getChildAt(i);
				CategoryCount categoryCount = (CategoryCount) checkNode.getUserObject();
				String name = categoryCount.getCategoryName();
				if (checkNode.isSelected()) {
					entityOrganismSet.add(name);
				}
			}
			OrganismFilter organismFilter = new OrganismFilter(entityOrganismSet);
			chainedFilter.addFilter(organismFilter);
		}
		
		if (datasourceFilterEnabled) {
			Set<String> entityDataSourceSet = new HashSet<String>();
			for (int i = 0; i < dataSourceFilterNode.getChildCount(); i++) {
				CheckNode checkNode = (CheckNode) dataSourceFilterNode.getChildAt(i);
				CategoryCount categoryCount = (CategoryCount) checkNode.getUserObject();
				String name = categoryCount.getCategoryName();
				if (checkNode.isSelected()) {
					entityDataSourceSet.add(name);
				}
			}
			DataSourceFilter dataSourceFilter = new DataSourceFilter(entityDataSourceSet);
			chainedFilter.addFilter(dataSourceFilter);
		}
		
		List<SearchHit> passedRecordList = chainedFilter
        	.filter(hitsFilterModel.getSearchResponse().getSearchHit());
			
   		DefaultListModel listModel = (DefaultListModel) hitsJList.getModel();
   		listModel.clear();
		if (passedRecordList.size() > 0) {
			for (SearchHit searchHit : passedRecordList) {
				listModel.addElement(searchHit);
			}
		}
	}

	
	/**
	 * Updates the filters tree from a new search results.
	 * 
	 * @param searchResponse
	 */
	@Override
	public void update(Observable o, Object arg) {
				
        if (hitsFilterModel.getNumRecords() == 0) {
            filterTreePanel.setVisible(false);
        } else {
            filterTreePanel.setVisible(true);
        }
        
        //  Remove all children
        
        if(typeFilterEnabled) {
        	typeFilterNode.removeAllChildren();
        	// Create HitsFilter Nodes
        	for (String key : hitsFilterModel.numHitsByTypeMap.keySet()) {
        		CategoryCount categoryCount = new CategoryCount(key, hitsFilterModel.numHitsByTypeMap.get(key));
        		CheckNode typeNode = new CheckNode(categoryCount, false, true);
        		typeFilterNode.add(typeNode);
        	}
        }
        
        if(organismFilterEnabled) {
        	organismFilterNode.removeAllChildren();
        	for (String key : hitsFilterModel.numHitsByOrganismMap.keySet()) {       	
        		String name = CyPath2.uriToOrganismNameMap.get(key);
        		if(name == null) 
        			name = key;        	
        		CategoryCount categoryCount = new CategoryCount(name, hitsFilterModel.numHitsByOrganismMap.get(key));
        		CheckNode organismNode = new CheckNode(categoryCount, false, true);
        		organismFilterNode.add(organismNode);
        	}
        }
        
        if(datasourceFilterEnabled) {
        	dataSourceFilterNode.removeAllChildren();
        	for (String key : hitsFilterModel.numHitsByDatasourceMap.keySet()) {
        		String name = CyPath2.uriToDatasourceNameMap.get(key);
        		if(name == null) 
        			name = key; 
        		CategoryCount categoryCount = new CategoryCount(name, hitsFilterModel.numHitsByDatasourceMap.get(key));
        		CheckNode dataSourceNode = new CheckNode(categoryCount, false, true);
        		dataSourceFilterNode.add(dataSourceNode);
        	}
        }
            
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        tree.setModel(treeModel);
        treeModel.addTreeModelListener(new TreeModelListener() {
            /**
             * Respond to user check node selections.
             *
             * @param treeModelEvent Tree Model Event Object.
             */
            public void treeNodesChanged(TreeModelEvent treeModelEvent) {
            	applyFilter();
            	filterTreePanel.repaint();
            }

            public void treeNodesInserted(TreeModelEvent treeModelEvent) {
                //  no-op
            }

            public void treeNodesRemoved(TreeModelEvent treeModelEvent) {
                //  no-op
            }

            public void treeStructureChanged(TreeModelEvent treeModelEvent) {
                //  no-op
            }
        });
        
        expandAllNodes();
	}
    
    
	class ToolTipsSearchHitsJList extends JList {

		public ToolTipsSearchHitsJList() {
			super(new DefaultListModel());
		}

		@Override
		public String getToolTipText(MouseEvent mouseEvent) {
			int index = locationToIndex(mouseEvent.getPoint());
			if (-1 < index) {
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
	}

	

	/**
	 * HitsFilter interface.
	 */
	interface HitsFilter {
	    /**
	     * Filters the record list.  Those items which pass the filter
	     * are included in the returned list.
	     *
	     * @param recordList List of SearchHit Objects.
	     * @return
	     */
	    List<SearchHit> filter (List<SearchHit> recordList);
	}
	
	
	class ChainedFilter implements HitsFilter {
	    private ArrayList<HitsFilter> filterList = new ArrayList<HitsFilter>();

	    /**
	     * Adds a new filter.
	     * @param filter HitsFilter Object.
	     */
	    public void addFilter (HitsFilter filter) {
	        filterList.add(filter);
	    }

	    /**
	     * Filters the record list.  Those items which pass the filter
	     * are included in the returned list.
	     *
	     * @param recordList
	     * @return
	     */    
	    public List<SearchHit> filter(List<SearchHit> recordList) {
	        for (HitsFilter filter:  filterList) {
	            recordList = filter.filter(recordList);
	        }
	        return recordList;
	    }
	}

	
	/**
	 * EntityType HitsFilter.
	 *
	 */
	class EntityTypeFilter implements HitsFilter {
	    Set<String> entityTypeSet;

	    /**
	     * Constructor.
	     *
	     * @param entityTypeSet Set of Entity Types we want to keep.
	     */
	    public EntityTypeFilter(Set<String> entityTypeSet) {
	        this.entityTypeSet = entityTypeSet;
	    }

	    /**
	     * Filters the record list.  Those items which pass the filter
	     * are included in the returned list.
	     *
	     * @param recordList
	     * @return
	     */
	    public List<SearchHit> filter(List<SearchHit> recordList) {
	        ArrayList<SearchHit> passedList = new ArrayList<SearchHit>();
	        for (SearchHit record : recordList) {
	            String type = record.getBiopaxClass();
	            if (type != null) {
	                if (entityTypeSet.contains(type)) {
	                    passedList.add(record);
	                }
	            }
	        }
	        return passedList;
	    }
	}
	
	
	class DataSourceFilter implements HitsFilter {
	    final Set<String> dataSourceSet;
	    
	    public DataSourceFilter(Set<String> dataSourceSet) {
	        this.dataSourceSet = dataSourceSet;
	    }

		public List<SearchHit> filter(List<SearchHit> recordList) {
			ArrayList<SearchHit> passedList = new ArrayList<SearchHit>();
			for (SearchHit record : recordList) {
				if (!record.getDataSource().isEmpty()) 
				{
					for(String ds: record.getDataSource()) {
						if (dataSourceSet.contains(CyPath2.uriToDatasourceNameMap.get(ds)) 
							|| dataSourceSet.contains(ds)) 
						{
							passedList.add(record);
							break;
						}
					}
				} else {
					passedList.add(record);
				}
			}
			return passedList;
		}
	}
	
	
	class OrganismFilter implements HitsFilter {
	    final Set<String> organismsSet;
	    
	    public OrganismFilter(Set<String> organismsSet) {
	        this.organismsSet = organismsSet;
	    }

		public List<SearchHit> filter(List<SearchHit> recordList) {
			ArrayList<SearchHit> passedList = new ArrayList<SearchHit>();
			for (SearchHit record : recordList) {
				if (!record.getOrganism().isEmpty()) 
				{
					for(String ds: record.getOrganism()) {
						if (organismsSet.contains(CyPath2.uriToOrganismNameMap.get(ds)) 
							|| organismsSet.contains(ds)) 
						{
							passedList.add(record);
							break;
						}
					}
				} else {
					passedList.add(record);
				}
			}
			return passedList;
		}
	}
	
	
	class CategoryCount {
	    private String categoryName;
	    private int count;

	    public CategoryCount (String categoryName, int count) {
	        this.categoryName = categoryName;
	        this.count = count;
	    }

	    public String getCategoryName() {
	        return categoryName;
	    }

	    public int getCount() {
	        return count;
	    }

	    public String toString() {
	        return categoryName + ":  " + count;
	    }
	}
}
