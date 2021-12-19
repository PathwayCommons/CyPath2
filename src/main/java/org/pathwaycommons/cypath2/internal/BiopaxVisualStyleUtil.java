package org.pathwaycommons.cypath2.internal;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.Control;
import org.biopax.paxtools.model.level3.ControlType;
import org.biopax.paxtools.model.level3.Interaction;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.LineTypeVisualProperty;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.ArrowShape;
import org.cytoscape.view.presentation.property.values.LineType;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.view.vizmap.*;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;
import static org.pathwaycommons.cypath2.internal.BioPaxMapper.BIOPAX_ENTITY_TYPE;

/**
 * Creates an "out-of-the-box" default Visual Mapper for rendering BioPAX
 * networks.
 *
 * @author Ethan Cerami
 * @author Igor Rodchenkov (re-factoring using PaxTools API, migrating to Cytoscape 3.x)
 */
public class BiopaxVisualStyleUtil {

  /**
   * size of physical entity node (default node size width)
   */
  public static final double BIO_PAX_VISUAL_STYLE_PHYSICAL_ENTITY_NODE_WIDTH = 20;
  /**
   * size of physical entity node (default node size height)
   */
  public static final double BIO_PAX_VISUAL_STYLE_PHYSICAL_ENTITY_NODE_HEIGHT = 20;
  /**
   * size of physical entity node scale - (used to scale post tranlational
   * modification nodes)
   */
  public static final double BIO_PAX_VISUAL_STYLE_PHYSICAL_ENTITY_NODE_SIZE_SCALE = 3;
  static final String BIO_PAX_VISUAL_STYLE = "Hypergraph (PathwayCommons)"; //BioPAX
  static final String BINARY_SIF_VISUAL_STYLE = "Binary SIF (PathwayCommons)";
  private static final Logger LOG = LoggerFactory.getLogger(BiopaxVisualStyleUtil.class);
  private static final double BIO_PAX_VISUAL_STYLE_INTERACTION_NODE_SIZE_SCALE = 0.67;
  private static final double BIO_PAX_VISUAL_STYLE_COMPLEX_NODE_SIZE_SCALE = 0.67;
  private static final Color DEFAULT_NODE_COLOR = new Color(255, 255, 255);
  private static final Color DEFAULT_NODE_BORDER_COLOR = new Color(0, 102, 102);
  private static final Color COMPLEX_NODE_COLOR = DEFAULT_NODE_COLOR; //new Color(0, 0, 0);
  private static final Color COMPLEX_NODE_BORDER_COLOR = DEFAULT_NODE_BORDER_COLOR; //COMPLEX_NODE_COLOR;

  //edge attr. name created by the core SIF reader
  private static final String INTERACTION = "interaction";

  private final VisualStyleFactory styleFactory;
  private final VisualMappingManager mappingManager;
  private final VisualMappingFunctionFactory discreteFactory;
  private final VisualMappingFunctionFactory passthroughFactory;

  private VisualStyle simpleBiopaxStyle;
  private VisualStyle binarySifStyle;

  /**
   * Constructor
   *
   * @param styleFactory
   * @param mappingManager
   * @param discreteMappingFactory
   * @param passthroughFactory
   */
  public BiopaxVisualStyleUtil(VisualStyleFactory styleFactory,
                               VisualMappingManager mappingManager,
                               VisualMappingFunctionFactory discreteMappingFactory,
                               VisualMappingFunctionFactory passthroughFactory) {
    this.styleFactory = styleFactory;
    this.mappingManager = mappingManager;
    this.discreteFactory = discreteMappingFactory;
    this.passthroughFactory = passthroughFactory;
  }

  /**
   * Based on given arguments, determines proper rectangle coordinates
   * used to render custom node shape.
   */
  private static Rectangle2D getCustomShapeRect(BufferedImage image, int modificationCount) {
    // our scale factor
    double scale = .1;
    final double[] startX = {
      0, (BIO_PAX_VISUAL_STYLE_PHYSICAL_ENTITY_NODE_WIDTH * BIO_PAX_VISUAL_STYLE_PHYSICAL_ENTITY_NODE_SIZE_SCALE) / 2,
      0, (-1 * BIO_PAX_VISUAL_STYLE_PHYSICAL_ENTITY_NODE_WIDTH * BIO_PAX_VISUAL_STYLE_PHYSICAL_ENTITY_NODE_SIZE_SCALE) / 2
    };

    final double[] startY = {
      (-1 * BIO_PAX_VISUAL_STYLE_PHYSICAL_ENTITY_NODE_HEIGHT * BIO_PAX_VISUAL_STYLE_PHYSICAL_ENTITY_NODE_SIZE_SCALE) / 2, 0,
      (BIO_PAX_VISUAL_STYLE_PHYSICAL_ENTITY_NODE_HEIGHT * BIO_PAX_VISUAL_STYLE_PHYSICAL_ENTITY_NODE_SIZE_SCALE) / 2, 0
    };

    // create and return rect
    return new Rectangle2D
      .Double(startX[modificationCount] + ((-1 * (image.getWidth() / 2)) * scale),
      startY[modificationCount] + ((-1 * (image.getHeight() / 2)) * scale),
      (double) image.getWidth() * scale,
      (double) image.getHeight() * scale
    );
  }

  public synchronized void init() {
    initBioPaxVisualStyle();
    initBinarySifVisualStyle();
  }

  /**
   * Gets the BioPAX style.
   *
   * @return VisualStyle Object.
   */
  public VisualStyle getBioPaxVisualStyle() {
    return simpleBiopaxStyle;
  }

  /**
   * Gets the 'BioPAX_SIF' style.
   *
   * @return VisualStyle Object.
   */
  public VisualStyle getBinarySifVisualStyle() {
    return binarySifStyle;
  }

  private void initBioPaxVisualStyle() {
    if (simpleBiopaxStyle == null) {
      simpleBiopaxStyle = findBiopaxVisualStyle(BIO_PAX_VISUAL_STYLE);
    }

    //removing the style is required mostly when installing a new version of this app
    if (simpleBiopaxStyle != null)
      mappingManager.removeVisualStyle(simpleBiopaxStyle);

    // create and add new empty (for now) 'BioPAX' style
    simpleBiopaxStyle = styleFactory.createVisualStyle(BIO_PAX_VISUAL_STYLE);
    mappingManager.addVisualStyle(simpleBiopaxStyle);

    // unlock node size, color
    for (VisualPropertyDependency<?> vpd : simpleBiopaxStyle.getAllVisualPropertyDependencies()) {
      if (vpd.getIdString().equals("nodeSizeLocked")) {
        vpd.setDependency(false);
      } else if (vpd.getIdString().equals("arrowColorMatchesEdge")) {
        vpd.setDependency(true);
      }
    }

    // Node size
    // create a discrete mapper, for mapping biopax node type
    // to a particular node size.
    DiscreteMapping<String, Double> width = (DiscreteMapping<String, Double>) discreteFactory
      .createVisualMappingFunction(BIOPAX_ENTITY_TYPE, String.class, NODE_WIDTH);
    DiscreteMapping<String, Double> height = (DiscreteMapping<String, Double>) discreteFactory
      .createVisualMappingFunction(BIOPAX_ENTITY_TYPE, String.class, NODE_HEIGHT);
    // map all interactions to required size
    for (Class c : BioPaxMapper.getSubclassNames(Interaction.class)) {
      String entityName = c.getSimpleName();
      width.putMapValue(entityName,
        new Double(BIO_PAX_VISUAL_STYLE_PHYSICAL_ENTITY_NODE_WIDTH
          * BIO_PAX_VISUAL_STYLE_INTERACTION_NODE_SIZE_SCALE));
      height.putMapValue(entityName,
        new Double(BIO_PAX_VISUAL_STYLE_PHYSICAL_ENTITY_NODE_HEIGHT
          * BIO_PAX_VISUAL_STYLE_INTERACTION_NODE_SIZE_SCALE));
    }

    // map all complex to required size
    String entityName = "Complex";//c.getSimpleName();
    width.putMapValue(entityName,
      new Double(BIO_PAX_VISUAL_STYLE_PHYSICAL_ENTITY_NODE_WIDTH
        * BIO_PAX_VISUAL_STYLE_COMPLEX_NODE_SIZE_SCALE));
    height.putMapValue(entityName,
      new Double(BIO_PAX_VISUAL_STYLE_PHYSICAL_ENTITY_NODE_HEIGHT
        * BIO_PAX_VISUAL_STYLE_COMPLEX_NODE_SIZE_SCALE));
    // create and set node height calculator in node appearance calculator
    simpleBiopaxStyle.setDefaultValue(NODE_WIDTH,
      BIO_PAX_VISUAL_STYLE_PHYSICAL_ENTITY_NODE_WIDTH);
    simpleBiopaxStyle.setDefaultValue(NODE_HEIGHT,
      BIO_PAX_VISUAL_STYLE_PHYSICAL_ENTITY_NODE_HEIGHT);
    simpleBiopaxStyle.addVisualMappingFunction(width);
    simpleBiopaxStyle.addVisualMappingFunction(height);

    // Node label: pass through mapper for node labels
    simpleBiopaxStyle.addVisualMappingFunction(passthroughFactory
      .createVisualMappingFunction(CyNetwork.NAME, String.class, NODE_LABEL));
    // Tooltip
    simpleBiopaxStyle.addVisualMappingFunction(passthroughFactory
      .createVisualMappingFunction(BIOPAX_ENTITY_TYPE, String.class, NODE_TOOLTIP));
    simpleBiopaxStyle.addVisualMappingFunction(passthroughFactory
      .createVisualMappingFunction(INTERACTION, String.class, EDGE_TOOLTIP));

    // Node color
    simpleBiopaxStyle.setDefaultValue(NODE_FILL_COLOR, DEFAULT_NODE_COLOR);
    // create a discrete mapper, for mapping biopax node type
    // to a particular node color
    DiscreteMapping<String, Paint> color = (DiscreteMapping<String, Paint>) discreteFactory
      .createVisualMappingFunction(BIOPAX_ENTITY_TYPE, String.class, NODE_FILL_COLOR);
    // map all complex to black
    color.putMapValue("Complex", COMPLEX_NODE_COLOR);
    simpleBiopaxStyle.addVisualMappingFunction(color);

    // Node border color
    simpleBiopaxStyle.setDefaultValue(NODE_BORDER_PAINT, DEFAULT_NODE_BORDER_COLOR);
    // create a discrete mapper, for mapping biopax node type
    // to a particular node color
    DiscreteMapping<String, Paint> paintFunction = (DiscreteMapping<String, Paint>) discreteFactory
      .createVisualMappingFunction(BIOPAX_ENTITY_TYPE, String.class, NODE_BORDER_PAINT);
    // map all complex to black
    paintFunction.putMapValue("Complex", COMPLEX_NODE_BORDER_COLOR);
    simpleBiopaxStyle.addVisualMappingFunction(paintFunction);

    // Target arrows
    DiscreteMapping<String, ArrowShape> tgtArrowShape = (DiscreteMapping<String, ArrowShape>) discreteFactory
      .createVisualMappingFunction(INTERACTION, String.class, EDGE_TARGET_ARROW_SHAPE);
    //'controller' and 'left' will have default arrow shape (none)
    tgtArrowShape.putMapValue("right", ArrowShapeVisualProperty.ARROW);
    tgtArrowShape.putMapValue("product", ArrowShapeVisualProperty.ARROW);
    tgtArrowShape.putMapValue("controlled", ArrowShapeVisualProperty.DELTA_SHORT_1);//rare (usually 'ACTIVATION', 'INHIBITION')
    tgtArrowShape.putMapValue("cofactor", ArrowShapeVisualProperty.OPEN_DIAMOND);
    tgtArrowShape.putMapValue("member", ArrowShapeVisualProperty.ARROW_SHORT);
    tgtArrowShape.putMapValue("controller", ArrowShapeVisualProperty.CIRCLE);

    // Inhibition Edges
    for (ControlType controlType : ControlType.values()) {
      if (controlType.toString().startsWith("I"))
        tgtArrowShape.putMapValue(controlType.toString(),
          ArrowShapeVisualProperty.T);
    }
    // Activation Edges
    for (ControlType controlType : ControlType.values()) {
      if (controlType.toString().startsWith("A"))
        tgtArrowShape.putMapValue(controlType.toString(),
          ArrowShapeVisualProperty.DELTA);
    }
    simpleBiopaxStyle.addVisualMappingFunction(tgtArrowShape);

    // Line types
    DiscreteMapping<String, LineType> edgeLineType = (DiscreteMapping<String, LineType>) discreteFactory
      .createVisualMappingFunction(INTERACTION, String.class, EDGE_LINE_TYPE);
    edgeLineType.putMapValue("member", LineTypeVisualProperty.DOT);
    edgeLineType.putMapValue("contains", LineTypeVisualProperty.LONG_DASH);
    simpleBiopaxStyle.addVisualMappingFunction(edgeLineType);

    DiscreteMapping<String, ArrowShape> srcArrowShape = (DiscreteMapping<String, ArrowShape>) discreteFactory
      .createVisualMappingFunction(INTERACTION, String.class, EDGE_SOURCE_ARROW_SHAPE);
    srcArrowShape.putMapValue("contains", ArrowShapeVisualProperty.OPEN_CIRCLE);
    simpleBiopaxStyle.addVisualMappingFunction(srcArrowShape);

    // Node shape
    simpleBiopaxStyle.setDefaultValue(NODE_SHAPE, NodeShapeVisualProperty.ELLIPSE);

    // create a discrete mapper, for mapping a biopax type to a shape
    DiscreteMapping<String, NodeShape> shape = (DiscreteMapping<String, NodeShape>) discreteFactory
      .createVisualMappingFunction(BIOPAX_ENTITY_TYPE, String.class, NODE_SHAPE);
    // map all physical entities to circles
    for (Class<? extends BioPAXElement> claz : BioPaxMapper.getSubclassNames(PhysicalEntity.class)) {
      String name = claz.getSimpleName();
      shape.putMapValue(name, NodeShapeVisualProperty.ELLIPSE);
    }
    // use a different shape for Complex nodes
    shape.putMapValue("Complex", NodeShapeVisualProperty.DIAMOND);
    // hack for phosphorylated proteins
    shape.putMapValue(BioPaxMapper.PROTEIN_PHOSPHORYLATED, NodeShapeVisualProperty.ELLIPSE);

    // map all interactions: control to triangles, others to square
    for (Class<?> c : BioPaxMapper.getSubclassNames(Interaction.class)) {
      if (Control.class.isAssignableFrom(c))
        shape.putMapValue(c.getSimpleName(), NodeShapeVisualProperty.TRIANGLE);
      else
        shape.putMapValue(c.getSimpleName(), NodeShapeVisualProperty.RECTANGLE);
    }

    simpleBiopaxStyle.addVisualMappingFunction(shape);
  }

  private VisualStyle findBiopaxVisualStyle(String biopaxVisualStyleTitle) {

    for (VisualStyle vs : mappingManager.getAllVisualStyles()) {
      if (biopaxVisualStyleTitle.equals(vs.getTitle())) {
        return vs;
      }
    }

    return null;
  }

  private void initBinarySifVisualStyle() {
    if (binarySifStyle == null) {
      binarySifStyle = findBiopaxVisualStyle(BINARY_SIF_VISUAL_STYLE);
    }
    //removing the style is required mostly when installing a new version of this app
    if (binarySifStyle != null)
      mappingManager.removeVisualStyle(binarySifStyle);

    // create and add new empty (for now) 'BioPAX_SIF' style
    binarySifStyle = styleFactory.createVisualStyle(BINARY_SIF_VISUAL_STYLE);
    mappingManager.addVisualStyle(binarySifStyle);


    // set node opacity
    binarySifStyle.setDefaultValue(NODE_TRANSPARENCY, 125);

    // unlock node size and color, edge/arrow color
    for (VisualPropertyDependency<?> vpd : binarySifStyle.getAllVisualPropertyDependencies()) {
      if (vpd.getIdString().equals("nodeSizeLocked")) {
        vpd.setDependency(false);
      } else if (vpd.getIdString().equals("arrowColorMatchesEdge")) {
        vpd.setDependency(true);
      }
    }

    //Node shape
    // Default shape is an ellipse.
    binarySifStyle.setDefaultValue(NODE_SHAPE, NodeShapeVisualProperty.ELLIPSE);
    // Complexes are Hexagons.
    DiscreteMapping<String, NodeShape> shapeFunction = (DiscreteMapping<String, NodeShape>) discreteFactory
      .createVisualMappingFunction(BIOPAX_ENTITY_TYPE, String.class, NODE_SHAPE);
    shapeFunction.putMapValue("Complex", NodeShapeVisualProperty.HEXAGON);
    binarySifStyle.addVisualMappingFunction(shapeFunction);

    // Node color
    binarySifStyle.setDefaultValue(NODE_FILL_COLOR, new Color(0xFF9999)); //LIGHTCORAL
    DiscreteMapping<String, Paint> paintFunction = (DiscreteMapping<String, Paint>) discreteFactory
      .createVisualMappingFunction(BIOPAX_ENTITY_TYPE, String.class, NODE_FILL_COLOR);
    paintFunction.putMapValue("Complex", new Color(0x99CCFF)); //LIGHTBLUE
    paintFunction.putMapValue("RnaReference", new Color(0x2BFFD3));
    paintFunction.putMapValue("RnaRegionReference", new Color(0x2BFFD3));
    paintFunction.putMapValue("DnaReference", new Color(0xC9FF99));
    paintFunction.putMapValue("DnaRegionReference", new Color(0xC9FF99));
    binarySifStyle.addVisualMappingFunction(paintFunction);

    // Node label: pass through mapper for node labels
    binarySifStyle.addVisualMappingFunction(passthroughFactory
      .createVisualMappingFunction(CyNetwork.NAME, String.class, NODE_LABEL));

    // Tooltips
    DiscreteMapping<String, String> ttFunc = (DiscreteMapping<String, String>) discreteFactory
      .createVisualMappingFunction(BIOPAX_ENTITY_TYPE, String.class, NODE_TOOLTIP);
    ttFunc.putMapValue("RnaReference", "RNA");
    ttFunc.putMapValue("RnaRegionReference", "RNA Region");
    ttFunc.putMapValue("DnaReference", "DNA");
    ttFunc.putMapValue("DnaRegionReference", "DNA Region");
    ttFunc.putMapValue("ProteinReference", "Protein");
    ttFunc.putMapValue("SmallMoleculeReference", "Chemical");
    ttFunc.putMapValue("Gene", "Gene"); //not sure it happens, anyway...
    ttFunc.putMapValue("PhycicalEntity", "Entity"); //not sure it happens
    ttFunc.putMapValue("Complex", "Complex");
    binarySifStyle.addVisualMappingFunction(ttFunc);

    binarySifStyle.addVisualMappingFunction(passthroughFactory
      .createVisualMappingFunction(INTERACTION, String.class, EDGE_TOOLTIP));

    binarySifStyle.setDefaultValue(EDGE_WIDTH, 4.0);

    // A discrete mapper, for mapping edge type to a particular edge color
    // for unselected edges' stroke paint
    binarySifStyle.setDefaultValue(EDGE_STROKE_UNSELECTED_PAINT, Color.BLACK);
    paintFunction = (DiscreteMapping<String, Paint>) discreteFactory
      .createVisualMappingFunction(INTERACTION, String.class, EDGE_STROKE_UNSELECTED_PAINT);
    initEdgeDiscreteMappingValues(paintFunction);
    binarySifStyle.addVisualMappingFunction(paintFunction);

    // for unselected edges' paint
    binarySifStyle.setDefaultValue(EDGE_UNSELECTED_PAINT, Color.BLACK);
    paintFunction = (DiscreteMapping<String, Paint>) discreteFactory
      .createVisualMappingFunction(INTERACTION, String.class, EDGE_UNSELECTED_PAINT);
    initEdgeDiscreteMappingValues(paintFunction);
    binarySifStyle.addVisualMappingFunction(paintFunction);

    //Edge direction styles
    DiscreteMapping<String, ArrowShape> discreteMapping = (DiscreteMapping<String, ArrowShape>) discreteFactory
      .createVisualMappingFunction(INTERACTION, String.class, EDGE_TARGET_ARROW_SHAPE);
    discreteMapping.putMapValue("controls-state-change-of", ArrowShapeVisualProperty.ARROW);
    discreteMapping.putMapValue("controls-transport-of", ArrowShapeVisualProperty.ARROW);
    discreteMapping.putMapValue("catalysis-precedes", ArrowShapeVisualProperty.ARROW);
    discreteMapping.putMapValue("controls-phosphorylation-of", ArrowShapeVisualProperty.ARROW);
    discreteMapping.putMapValue("controls-expression-of", ArrowShapeVisualProperty.ARROW);
    discreteMapping.putMapValue("controls-transport-of-chemical", ArrowShapeVisualProperty.ARROW);
    discreteMapping.putMapValue("controls-production-of", ArrowShapeVisualProperty.ARROW);
    discreteMapping.putMapValue("consumption-controled-by", ArrowShapeVisualProperty.ARROW);
    discreteMapping.putMapValue("chemical-affects", ArrowShapeVisualProperty.ARROW);
    discreteMapping.putMapValue("used-to-produce", ArrowShapeVisualProperty.ARROW);
    binarySifStyle.addVisualMappingFunction(discreteMapping);

    //set background color
    binarySifStyle.setDefaultValue(NETWORK_BACKGROUND_PAINT, Color.WHITE);
  }

  private void initEdgeDiscreteMappingValues(DiscreteMapping<String, Paint> paintFunction) {
    paintFunction.putMapValue("in-complex-with", new Color(0xf000a0));

    paintFunction.putMapValue("interacts-with", new Color(0x005500)); //MolecularInteraction (e.g. PSI-MI)
    paintFunction.putMapValue("neighbor-of", new Color(0x00aa00));
    paintFunction.putMapValue("reacts-with", new Color(0x00ff00));

    paintFunction.putMapValue("catalysis-precedes", new Color(0x7000a0));
    paintFunction.putMapValue("controls-state-change-of", new Color(0x000c0));
    paintFunction.putMapValue("controls-phosphorylation-of", new Color(0x0000ff));
    paintFunction.putMapValue("controls-expression-of", new Color(0x00a0a0));
    paintFunction.putMapValue("controls-production-of", new Color(0x00ccf0));
    paintFunction.putMapValue("controls-transport-of", new Color(0x700000));

    paintFunction.putMapValue("controls-transport-of-chemical", new Color(0xa00000));
    paintFunction.putMapValue("consumption-controled-by", new Color(0xff3300));
    paintFunction.putMapValue("used-to-produce", new Color(0xf75500));
    paintFunction.putMapValue("chemical-affects", new Color(0xf09000));
  }
}
