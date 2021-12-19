package org.pathwaycommons.cypath2.internal;

import org.apache.commons.lang3.StringEscapeUtils;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.pattern.miner.SIFEnum;
import org.biopax.paxtools.pattern.miner.SIFType;
import org.cytoscape.application.NetworkViewRenderer;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListMultipleSelection;
import org.cytoscape.work.util.ListSingleSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.*;


/**
 * BioPAX reader and network/view builder.
 */
public class BioPaxReaderTask extends AbstractTask implements CyNetworkReader {

  private static final Logger log = LoggerFactory.getLogger(BioPaxReaderTask.class);

  private static final String CREATE_NEW_COLLECTION = "A new network collection";

  private final HashMap<String, CyRootNetwork> nameToRootNetworkMap;
  private final Collection<CyNetwork> networks;
  @Tunable(description = "BioPAX Mapping:", groups = {"Options"}, tooltip = "<html>How to process the BioPAX result:" +
    "<ul><li><strong>Hypergraph</strong>: map biopax entities and interactions to nodes; properties " +
    "- to edges and table attributes;</li>" +
    "<li><strong>Binary (SIF)</strong>: convert the BioPAX to SIF model and table attributes;</li>" +
    "</ul></html>", gravity = 500)
  public ListSingleSelection<ReaderMode> readerMode;
  @Tunable(description = "Parent Network:", groups = {"Options"},
    tooltip = "Select a network collection", gravity = 701)
  public ListSingleSelection<String> rootNetworkSelection;
  //this one is normally invisible due to there is only one renderer
  @Tunable(description = "Network View Renderer:", groups = {"Options"}, gravity = 702)
  public ListSingleSelection<NetworkViewRenderer> rendererList;
  //select inference rules (multi-selection) for the SIF converter
  @Tunable(description = "BioPAX Patterns:", groups = {"Options"},
    tooltip = "Select interaction types to infer from the BioPAX result",
    gravity = 703, dependsOn = "readerMode=Binary")
  public ListMultipleSelection<SIFType> sifSelection;
  private InputStream stream;
  private String inputName;
  private CyRootNetwork rootNetwork;

  /**
   * Constructor
   *
   * @param stream    input biopax stream
   * @param inputName a file or pathway name (can be later updated using actual data)
   */
  public BioPaxReaderTask(InputStream stream, String inputName) {
    this.networks = new HashSet<>();
    this.stream = stream;
    this.inputName = inputName;

    // initialize the root networks Collection
    nameToRootNetworkMap = new HashMap<>();
    for (CyNetwork net : App.cyServices.networkManager.getNetworkSet()) {
      final CyRootNetwork rootNet = App.cyServices.rootNetworkManager.getRootNetwork(net);
      if (!nameToRootNetworkMap.containsValue(rootNet))
        nameToRootNetworkMap.put(rootNet.getRow(rootNet).get(CyRootNetwork.NAME, String.class),
          rootNet);
    }
    List<String> rootNames = new ArrayList<>();
    rootNames.add(CREATE_NEW_COLLECTION);
    rootNames.addAll(nameToRootNetworkMap.keySet());
    rootNetworkSelection = new ListSingleSelection<>(rootNames);
    rootNetworkSelection.setSelectedValue(CREATE_NEW_COLLECTION);

    // initialize the list of data processing modes
    readerMode = new ListSingleSelection<>(ReaderMode.values());
    readerMode.setSelectedValue(ReaderMode.HYPERGRAPH);

    // init the BioPAX patterns list
    sifSelection = new ListMultipleSelection<>(SIFEnum.values());
    // select several interesting patterns by default:
    List<SIFType> values = Arrays.asList(
      SIFEnum.CONTROLS_EXPRESSION_OF,
      SIFEnum.CONTROLS_STATE_CHANGE_OF,
      SIFEnum.IN_COMPLEX_WITH);
    sifSelection.setSelectedValues(values);

    // initialize renderer list
    final List<NetworkViewRenderer> renderers = new ArrayList<>();
    final Set<NetworkViewRenderer> rendererSet = App.cyServices.applicationManager.getNetworkViewRendererSet();
    // If there is only one registered renderer, we don't want to add it to the List Selection,
    // so the combo-box does not appear to the user, since there is nothing to select anyway.
    if (rendererSet.size() > 1) {
      renderers.addAll(rendererSet);
      Collections.sort(renderers, (r1, r2) -> r1.toString().compareToIgnoreCase(r2.toString()));
    }
    rendererList = new ListSingleSelection<>(renderers);
  }

  @ProvidesTitle()
  public String tunableDialogTitle() {
    return "Cy Network and View from Pathway Commons query";
  }

  @Override
  public void run(TaskMonitor taskMonitor) throws Exception {
    taskMonitor.setTitle("BioPAX reader");
    taskMonitor.setProgress(0.0);

    if (cancelled) return;

    // import BioPAX data into a new in-memory model
    Model model = null;
    try {
      model = BioPaxMapper.read(stream);
    } catch (Throwable e) {
      throw new RuntimeException("BioPAX reader failed to build a BioPAX model " +
        "(check the data for syntax errors) - " + e);
    }

    if (model == null) {
      throw new RuntimeException("BioPAX reader did not find any BioPAX data there.");
    }

    final String networkName = getNetworkName(model);
    String msg = "New model contains " + model.getObjects().size() + " BioPAX elements";
    taskMonitor.setStatusMessage(msg);

    //set parent/root network (can be null - add a new networks group)
    rootNetwork = nameToRootNetworkMap.get(rootNetworkSelection.getSelectedValue());

    final BioPaxMapper mapper = new BioPaxMapper(model, App.cyServices.networkFactory);

    ReaderMode selectedMode = readerMode.getSelectedValue();
    switch (selectedMode) {
      case HYPERGRAPH:
        // Map BioPAX Data to Cytoscape Nodes/Edges (run as task)
        taskMonitor.setStatusMessage("Mapping BioPAX model to CyNetwork...");
        CyNetwork network = mapper.createCyNetwork(networkName, rootNetwork);
        if (network.getNodeCount() == 0)
          throw new RuntimeException("Pathway is empty. Please check the BioPAX source file.");
        // set the biopax network mapping type for other plugins
        Attributes.set(network, network, BioPaxMapper.BIOPAX_NETWORK,
          BiopaxVisualStyleUtil.BIO_PAX_VISUAL_STYLE, String.class);
        //(the network name attr. was already set by the biopax mapper)
        //register the network
        networks.add(network);
        break;

      case BINARY:
        //convert BioPAX to the custom binary SIF format (using a tmp file)
        taskMonitor.setStatusMessage("Mapping BioPAX model to SIF, then to CyNetwork...");
        final File tmpSifFile = File.createTempFile("tmp_biopax2sif", ".sif");
        tmpSifFile.deleteOnExit();
        BioPaxMapper.convertToCustomSIF(model,
          sifSelection.getSelectedValues().toArray(new SIFType[]{}),
          new FileOutputStream(tmpSifFile));

        // create a new CyNetwork
        CyNetwork net = (rootNetwork == null)
          ? App.cyServices.networkFactory.createNetwork()
          : rootNetwork.addSubNetwork();

        // line-by-line parse the custom SIF file (- create nodes, edges and edge attributes)
        CustomSifParser customSifParser = new CustomSifParser(net, App.cyServices);
        BufferedReader reader = Files.newBufferedReader(tmpSifFile.toPath());
        String line = null;
        while ((line = reader.readLine()) != null) {
          customSifParser.parse(line);
        }
        reader.close();

        // create node attributes from the BioPAX properties
        createSifNodeAttr(model, net, taskMonitor);

        // final touches -
        // set the biopax network mapping type for other plugins to use/consider
        Attributes.set(net, net, BioPaxMapper.BIOPAX_NETWORK,
          BiopaxVisualStyleUtil.BINARY_SIF_VISUAL_STYLE, String.class);
        //set the network name (very important!)
        Attributes.set(net, net, CyNetwork.NAME, networkName, String.class);
        //register the network
        networks.add(net);
        taskMonitor.setStatusMessage("SIF network updated...");
        break;
      default:
        break;
    }
  }

  private void createSifNodeAttr(Model model, CyNetwork cyNetwork,
                                 TaskMonitor taskMonitor) throws IOException {
    taskMonitor.setStatusMessage("Updating SIF network node attributes from the BioPAX model...");

    // Set the Quick Find Default Index
    Attributes.set(cyNetwork, cyNetwork, "quickfind.default_index", CyNetwork.NAME, String.class);
    if (cancelled) return;

    // Set node attributes from the Biopax Model
    for (CyNode node : cyNetwork.getNodeList()) {
      String uri = cyNetwork.getRow(node).get(CyNetwork.NAME, String.class);
      BioPAXElement e = model.getByID(uri);
      if (e instanceof EntityReference || e instanceof Entity) {
        BioPaxMapper.createAttributesFromProperties(e, model, node, cyNetwork);
      } else if (e != null) {
        log.warn("SIF network has an unexpected node: " + uri + " of type " + e.getModelInterface());
        BioPaxMapper.createAttributesFromProperties(e, model, node, cyNetwork);
      } else { //should never happen anymore...
        log.error("(BUG) the biopax model does not have an object with URI=" + uri);
      }
    }
  }

  private String getNetworkName(Model model) {
    // make a network name from pathway name(s) or the file name
    String name = BioPaxMapper.getName(model);

    if (name == null || name.trim().isEmpty()) {
      name = (inputName == null || inputName.trim().isEmpty())
        ? "BioPAX_Network" : inputName;
    } else {
      int l = (name.length() < 100) ? name.length() : 100;
      name = (inputName == null || inputName.trim().isEmpty())
        ? name.substring(0, l)
        : inputName; //preferred
    }

    // Take appropriate adjustments, if name already exists
    name = App.cyServices.naming.getSuggestedNetworkTitle(
      StringEscapeUtils.unescapeHtml4(name) +
        " (" + readerMode.getSelectedValue() + ")");

    log.info("New BioPAX network name is: " + name);

    return name;
  }

  @Override
  public CyNetwork[] getNetworks() {
    return networks.toArray(new CyNetwork[]{});
  }

  @Override
  public CyNetworkView buildCyNetworkView(final CyNetwork network) {
    CyNetworkView view = getNetworkViewFactory().createNetworkView(network);

    if (!App.cyServices.networkViewManager.getNetworkViews(network).contains(view))
      App.cyServices.networkViewManager.addNetworkView(view);

    return view;
  }

  private CyNetworkViewFactory getNetworkViewFactory() {
    if (rendererList != null && rendererList.getSelectedValue() != null)
      return rendererList.getSelectedValue().getNetworkViewFactory();
    else
      return App.cyServices.networkViewFactory;
  }

  /**
   * BioPAX parsing/converting options.
   *
   * @author rodche
   */
  private enum ReaderMode {
    /**
     * Default BioPAX to Cytoscape network/view mapping:
     * entity objects (sub-classes, including interactions too)
     * will be CyNodes interconnected by edges that
     * correspond to biopax properties with Entity type domain and range;
     * some of dependent utility class objects and simple properties are used to
     * generate node attributes.
     */
    HYPERGRAPH("Hypergraph"),

    /**
     * BioPAX to SIF, and then to Cytoscape mapping:
     * first, it converts BioPAX to SIF (using Paxtools library); next,
     * delegates network/view creation to the first available SIF anotherReader.
     */
    BINARY("Binary");

    private final String name;

    ReaderMode(String name) {
      this.name = name;
    }

    static String[] names() {
      ReaderMode vals[] = ReaderMode.values();
      String names[] = new String[vals.length];
      for (int i = 0; i < vals.length; i++)
        names[i] = vals[i].toString();
      return names;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
