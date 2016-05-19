package org.pathwaycommons.cypath2.internal;

import java.awt.Component;
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
    private final JList hitsJList;
    private final CheckNode rootNode;
    private final JTreeWithCheckNodes tree;
    private final CollapsablePanel filterTreePanel;

	private CheckNode typeFilterNode;
	private CheckNode dataSourceFilterNode;
	private CheckNode organismFilterNode;

    boolean typeFilterEnabled;
    boolean organismFilterEnabled;
    boolean datasourceFilterEnabled;

    public HitsFilterPanel(final JList hitsJList,
						   boolean typeFilterEnabled,
						   boolean organismFilterEnabled,
						   boolean datasourceFilterEnabled)
	{
        this.hitsJList = hitsJList;
        this.typeFilterEnabled = typeFilterEnabled;
        this.organismFilterEnabled = organismFilterEnabled;
        this.datasourceFilterEnabled = datasourceFilterEnabled;
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        // create an empty filter tree (barebone)
        rootNode = new CheckNode("All");
        tree = new JTreeWithCheckNodes(rootNode);
        tree.setOpaque(false);
        filterTreePanel = new CollapsablePanel("Filter the hits");
        filterTreePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterTreePanel.getContentPane().add(tree);

        JScrollPane scrollPane = new JScrollPane(filterTreePanel);
        add(scrollPane);
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

    
	private void applyFilter(final HitsModel model) {
		
        ChainedFilter chainedFilter = new ChainedFilter();
        
        if(typeFilterEnabled) {
        	Set<String> entityTypeSet = new HashSet<String>();
        	for (int i = 0; i < typeFilterNode.getChildCount(); i++) {
        		CheckNode checkNode = (CheckNode) typeFilterNode.getChildAt(i);
        		CategoryCount categoryCount = (CategoryCount) checkNode.getUserObject();
        		String name = categoryCount.getCategoryName();
        		if (checkNode.isSelected()) { // - checked
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

   		DefaultListModel listModel = (DefaultListModel) hitsJList.getModel();
   		listModel.clear();

		List<SearchHit> passedRecordList = chainedFilter.filter(model.getHits());
		//TODO update/fix counts
		if (!passedRecordList.isEmpty()) {
			for (SearchHit searchHit : passedRecordList) {
				listModel.addElement(searchHit);
			}
		}

//		filterTreePanel.repaint();
	}

	
	/**
	 * Updates the filter tree view once the search result (hits) get updated.
	 */
	@Override
	public void update(Observable o, Object arg) {

		if (!(o instanceof HitsModel)) {
			return; //not applicable (or not implemented yet...)
		}

		final HitsModel model = (HitsModel) o;

		//cleanup, reset
		filterTreePanel.setVisible(false);
		tree.setModel(null);
		rootNode.removeAllChildren();
		organismFilterNode.removeAllChildren();
		dataSourceFilterNode.removeAllChildren();
		typeFilterNode.removeAllChildren();
		tree.setModel(new DefaultTreeModel(rootNode));

		if (model.getNumRecords() == 0) {
			return;
		}

		typeFilterEnabled = (
			model.searchFor == null || model.searchFor.isEmpty()
				|| model.searchFor.equalsIgnoreCase("Pathway")) ? false : true;

        if(typeFilterEnabled) {
			typeFilterNode = new CheckNode("BioPAX Type");
        	// Create BioPAX type filter nodes (leafs)
        	for (String key : model.numHitsByTypeMap.keySet()) {
        		CategoryCount categoryCount = new CategoryCount(key, model.numHitsByTypeMap.get(key));
        		CheckNode typeNode = new CheckNode(categoryCount, false, true);
        		typeFilterNode.add(typeNode);
        	}
			rootNode.add(typeFilterNode);
        } else typeFilterNode = null;
        
        if(organismFilterEnabled) {
			organismFilterNode = new CheckNode("Organism");
        	for (String key : model.numHitsByOrganismMap.keySet()) {
        		String name = App.uriToOrganismNameMap.get(key);
        		if(name == null) {name = key;}
        		CategoryCount categoryCount = new CategoryCount(name, model.numHitsByOrganismMap.get(key));
        		CheckNode organismNode = new CheckNode(categoryCount, false, true);
        		organismFilterNode.add(organismNode);
        	}
			rootNode.add(organismFilterNode);
        } else organismFilterNode = null;
        
        if(datasourceFilterEnabled) {
			dataSourceFilterNode = new CheckNode("Datasource");
			for (String key : model.numHitsByDatasourceMap.keySet()) {
        		String name = App.uriToDatasourceNameMap.get(key);
        		if(name == null) 
        			name = key; 
        		CategoryCount categoryCount = new CategoryCount(name, model.numHitsByDatasourceMap.get(key));
        		CheckNode dataSourceNode = new CheckNode(categoryCount, false, true);
        		dataSourceFilterNode.add(dataSourceNode);
        	}
			rootNode.add(dataSourceFilterNode);
        } else dataSourceFilterNode = null;

        expandAllNodes();

		tree.getModel().addTreeModelListener(new TreeModelListener() {
			// Respond to user check/unckeck nodes.
			public void treeNodesChanged(TreeModelEvent treeModelEvent) {
				applyFilter(model);
			}
			public void treeNodesInserted(TreeModelEvent treeModelEvent) {}
			public void treeNodesRemoved(TreeModelEvent treeModelEvent) {}
			public void treeStructureChanged(TreeModelEvent treeModelEvent) {}
		});

		filterTreePanel.repaint();
		filterTreePanel.setVisible(true);
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
						if (dataSourceSet.contains(App.uriToDatasourceNameMap.get(ds))
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
						String name = App.uriToOrganismNameMap.get(ds);
						if (organismsSet.contains(ds) || (name!=null && organismsSet.contains(name)))
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
