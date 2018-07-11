package org.pathwaycommons.cypath2.internal;

import cpath.service.jaxb.SearchHit;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;
import java.util.List;


final class HitsFilterPanel extends JPanel implements Observer {
  private static final long serialVersionUID = 1L;
  private final JList hitsJList;
  private final CheckNode rootNode;
  private final JTreeWithCheckNodes tree;
  private final CollapsablePanel filterTreePanel;
  private final Map<String, Integer> numHitsByTypeMap = new HashMap<String, Integer>();
  private final Map<String, Integer> numHitsByOrganismMap = new HashMap<String, Integer>();
  private final Map<String, Integer> numHitsByDatasourceMap = new HashMap<String, Integer>();
  boolean typeFilterEnabled;
  boolean organismFilterEnabled;
  boolean datasourceFilterEnabled;
  private CheckNode typeFilterNode;
  private CheckNode dataSourceFilterNode;
  private CheckNode organismFilterNode;

  public HitsFilterPanel(final JList hitsJList) {
    this.hitsJList = hitsJList;

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    // create an empty filter tree (barebone)
    rootNode = new CheckNode("All");
    tree = new JTreeWithCheckNodes(rootNode);
    tree.setOpaque(false);
    filterTreePanel = new CollapsablePanel("Show/hide hits (filter)");
    filterTreePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    filterTreePanel.getContentPane().add(tree);

    JScrollPane scrollPane = new JScrollPane(filterTreePanel);
    add(scrollPane);
  }


  private void expandAllNodes() {
    TreePath path;
    filterTreePanel.setCollapsed(false);

    if (typeFilterEnabled) {
      typeFilterNode.setSelected(true);
      path = new TreePath(typeFilterNode.getPath());
      tree.expandPath(path);
    }

    if (datasourceFilterEnabled) {
      dataSourceFilterNode.setSelected(true);
      path = new TreePath(dataSourceFilterNode.getPath());
      tree.expandPath(path);
    }

    if (organismFilterEnabled) {
      organismFilterNode.setSelected(true);
      path = new TreePath(organismFilterNode.getPath());
      tree.expandPath(path);
    }
  }


  private void applyFilter(final HitsModel model) {

    ChainedFilter chainedFilter = new ChainedFilter();

    if (typeFilterEnabled) {
      Set<String> entityTypeSet = new HashSet<String>();
      for (int i = 0; i < typeFilterNode.getChildCount(); i++) {
        CheckNode checkNode = (CheckNode) typeFilterNode.getChildAt(i);
        CategoryCount categoryCount = (CategoryCount) checkNode.getUserObject();
        if (checkNode.isSelected()) { // - checked
          entityTypeSet.add(categoryCount.id);
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
        if (checkNode.isSelected()) {
          entityOrganismSet.add(categoryCount.id);
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
        if (checkNode.isSelected()) {
          entityDataSourceSet.add(categoryCount.id);
        }
      }
      DataSourceFilter dataSourceFilter = new DataSourceFilter(entityDataSourceSet);
      chainedFilter.addFilter(dataSourceFilter);
    }

    //apply filters and update the counts next to checked tree nodes
    final List<SearchHit> passedRecordList = chainedFilter.filter(model.getHits());

    //update the hits list (clear, add only hits that pass current filters)
    final DefaultListModel listModel = (DefaultListModel) hitsJList.getModel();
    listModel.clear();
    if (!passedRecordList.isEmpty()) {
      for (SearchHit searchHit : passedRecordList) {
        listModel.addElement(searchHit);
      }
    }

    updateTree(passedRecordList);
  }

  private void updateTree(List<SearchHit> hits) {
    //update counts in the filter tree view/pane;
    //but don't remove unchecked nodes and corresp. map keys:
    numHitsByTypeMap.values().clear();
    numHitsByOrganismMap.values().clear();
    numHitsByDatasourceMap.values().clear();
    for (SearchHit hit : hits) {
      updateCategoryToCountMaps(hit);
    }

    //update counts (in CategoryCount) in each tree node
    if (typeFilterEnabled) {
      for (int i = 0; i < typeFilterNode.getChildCount(); i++) {
        CheckNode checkNode = (CheckNode) typeFilterNode.getChildAt(i);
        CategoryCount categoryCount = (CategoryCount) checkNode.getUserObject();
        categoryCount.count = numHitsByTypeMap.get(categoryCount.id);
      }
    }
    if (organismFilterEnabled) {
      for (int i = 0; i < organismFilterNode.getChildCount(); i++) {
        CheckNode checkNode = (CheckNode) organismFilterNode.getChildAt(i);
        CategoryCount categoryCount = (CategoryCount) checkNode.getUserObject();
        categoryCount.count = numHitsByOrganismMap.get(categoryCount.id);
      }
    }
    if (datasourceFilterEnabled) {
      for (int i = 0; i < dataSourceFilterNode.getChildCount(); i++) {
        CheckNode checkNode = (CheckNode) dataSourceFilterNode.getChildAt(i);
        CategoryCount categoryCount = (CategoryCount) checkNode.getUserObject();
        categoryCount.count = numHitsByDatasourceMap.get(categoryCount.id);
      }
    }

    filterTreePanel.repaint();
  }


  /**
   * Resets the tree view once the search result (hits) get updated
   * (after a new search query executed).
   */
  @Override
  public void update(Observable o, Object arg) {

    if (!(o instanceof HitsModel)) {
      return; //not applicable (or not implemented yet...)
    }

    final HitsModel model = (HitsModel) o;
    if (model.getNumRecords() == 0) {
      return; //do nothing
    }

    filterTreePanel.setVisible(false); //hide

    //cleanup, reset
    tree.setModel(null);
    rootNode.removeAllChildren();
    organismFilterNode = null;
    dataSourceFilterNode = null;
    typeFilterNode = null;
    tree.setModel(new DefaultTreeModel(rootNode));

    //initialize the numHitsBy* maps (counts):
    numHitsByTypeMap.clear();
    numHitsByOrganismMap.clear();
    numHitsByDatasourceMap.clear();
    for (SearchHit hit : model.getHits()) {
      updateCategoryToCountMaps(hit);
    }

    //create filter tree nodes (CheckNode)
    typeFilterEnabled = (
      model.searchFor == null || model.searchFor.isEmpty()
        || model.searchFor.equalsIgnoreCase("Pathway")) ? false : true;

    if (typeFilterEnabled) {
      typeFilterNode = new CheckNode("BioPAX Type");
      rootNode.add(typeFilterNode);
      // Create BioPAX type filter nodes (leafs)
      for (String key : numHitsByTypeMap.keySet()) {
        CategoryCount categoryCount = new CategoryCount(key, key, numHitsByTypeMap.get(key));
        CheckNode typeNode = new CheckNode(categoryCount, false, true);
        typeFilterNode.add(typeNode);
      }
    } else typeFilterNode = null;

    if (organismFilterEnabled) {
      organismFilterNode = new CheckNode("Organism");
      rootNode.add(organismFilterNode);
      for (String key : numHitsByOrganismMap.keySet()) {
        String name = App.uriToOrganismNameMap.get(key);
        if (name == null)
          name = key;
        CategoryCount categoryCount = new CategoryCount(key, name, numHitsByOrganismMap.get(key));
        CheckNode organismNode = new CheckNode(categoryCount, false, true);
        organismFilterNode.add(organismNode);
      }
    } else organismFilterNode = null;

    if (datasourceFilterEnabled) {
      dataSourceFilterNode = new CheckNode("Datasource");
      rootNode.add(dataSourceFilterNode);
      for (String key : numHitsByDatasourceMap.keySet()) {
        String name = App.uriToDatasourceNameMap.get(key);
        if (name == null)
          name = key;
        CategoryCount categoryCount = new CategoryCount(key, name, numHitsByDatasourceMap.get(key));
        CheckNode dataSourceNode = new CheckNode(categoryCount, false, true);
        dataSourceFilterNode.add(dataSourceNode);
      }
    } else dataSourceFilterNode = null;


    // set to apply new filters once user checked/unchecked a node
    tree.getModel().addTreeModelListener(new TreeModelListener() {
      // Respond to user check/unckeck nodes.
      public void treeNodesChanged(TreeModelEvent treeModelEvent) {
        applyFilter(model);
      }

      public void treeNodesInserted(TreeModelEvent treeModelEvent) {
      }

      public void treeNodesRemoved(TreeModelEvent treeModelEvent) {
      }

      public void treeStructureChanged(TreeModelEvent treeModelEvent) {
      }
    });

    expandAllNodes();

    filterTreePanel.repaint();
    filterTreePanel.setVisible(true);
  }

  private void updateCategoryToCountMaps(final SearchHit hit) {
    // catalog/organize hit counts by type, organism, source -
    String type = hit.getBiopaxClass();
    Integer count = numHitsByTypeMap.get(type);
    if (count != null) {
      numHitsByTypeMap.put(type, count + 1);
    } else {
      numHitsByTypeMap.put(type, 1);
    }

    for (String org : hit.getOrganism()) {
      Integer i = numHitsByOrganismMap.get(org);
      if (i != null) {
        numHitsByOrganismMap.put(org, i + 1);
      } else {
        numHitsByOrganismMap.put(org, 1);
      }
    }

    for (String ds : hit.getDataSource()) {
      Integer i = numHitsByDatasourceMap.get(ds);
      if (i != null) {
        numHitsByDatasourceMap.put(ds, i + 1);
      } else {
        numHitsByDatasourceMap.put(ds, 1);
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
    List<SearchHit> filter(List<SearchHit> recordList);
  }


  class ChainedFilter implements HitsFilter {
    private ArrayList<HitsFilter> filterList = new ArrayList<HitsFilter>();

    /**
     * Adds a new filter.
     *
     * @param filter HitsFilter Object.
     */
    public void addFilter(HitsFilter filter) {
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
      for (HitsFilter filter : filterList) {
        recordList = filter.filter(recordList);
      }
      return recordList;
    }
  }


  /**
   * EntityType HitsFilter.
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
        if (!record.getDataSource().isEmpty()) {
          for (String ds : record.getDataSource()) {
            if (dataSourceSet.contains(App.uriToDatasourceNameMap.get(ds))
              || dataSourceSet.contains(ds)) {
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
        if (!record.getOrganism().isEmpty()) {
          for (String ds : record.getOrganism()) {
            String name = App.uriToOrganismNameMap.get(ds);
            if (organismsSet.contains(ds) || (name != null && organismsSet.contains(name))) {
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
    private final String name;
    private final String id;
    Integer count; //modifiable, null-able

    public CategoryCount(String id, String name, int count) {
      this.id = id;
      this.name = name;
      this.count = count;
    }

    public String toString() {
      return name + ":  " + ((count != null) ? count.intValue() : 0);
    }
  }
}
