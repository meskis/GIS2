/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eskis.gis2;

import com.eskis.gis2.Actions.AreaSelectorAction;
import com.eskis.gis2.Actions.ExecuteQueryAction;
import com.eskis.gis2.Actions.PerformAnalysisAction;
import com.eskis.gis2.GUI.GUI;
import com.eskis.gis2.Helpers.LayerHelper;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.StyleFactory;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.table.FeatureCollectionTableModel;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;

/**
 *
 * @author Marcus
 */
public class MapFrame extends JMapFrame {

    public GUI gui;
    private JMenuBar menubar = new JMenuBar();
    private JToolBar toolbar;
    private JPanel infoPanel = new JPanel(new BorderLayout());
    private JPanel searchPanel = new JPanel();
    private JTable infoTable = new JTable();
    private JButton search = new JButton("Search");
    private JButton displayOnMap = new JButton("Display info");
    private JScrollPane scrollPane = new JScrollPane(infoTable);
    private Layer lastSelected;
    private String geometryAttributeName;
    private SimpleFeatureCollection selectedFeatures = FeatureCollections.newCollection();
    private GeometryType geometryType;
    public static JTextField searchField = new JTextField(50);
    public MapContent map = new MapContent();
    public JMapFrame mapFrame;
    public Rectangle selectionRectangle;
    public Point2D startPosWorld = new DirectPosition2D();
    public Point2D endPosWorld = new DirectPosition2D();
    public StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);
    public FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    /**
     * Constructor
     */
    public MapFrame() {
        map.setTitle("GIS App v2.0");

        this.getContentPane().setLayout(new BorderLayout());
        this.enableToolBar(true);

        this.enableLayerTable(true);
        this.enableStatusBar(true);
        toolbar = getToolBar();
        toolbar.addSeparator();

        setUpFrame();

        setUp();
    }

    protected void bindEvents() {
    }

    private void setUpFrame() {

        setUpTable();
        setUpToolbar();

        this.pack();
        this.setMapContent(map);
        this.setSize(800, 600);
        this.setMinimumSize(new Dimension(850, 700));
        this.setMaximumSize(new Dimension(1366, 768));
        this.setLocation(250, 50);
        this.setVisible(true);
    }

    protected void preloadLayers() throws IOException {
        LayerHelper.preloadLayers(map);
    }

    protected void preloadlayers2() {
    }

    private void setUp() {
        try {
            System.out.println("Loading layers...");
            preloadLayers();
            System.out.println("Done.");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Klaida: " + e.toString());
            System.out.println(e.getStackTrace());
        }
    }

    private void setUpTable() {
        infoPanel.add(searchPanel, BorderLayout.NORTH);
        infoTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        infoTable.setModel(new DefaultTableModel(0, 0));
        infoTable.setPreferredScrollableViewportSize(new Dimension(500, 200));
        infoPanel.add(scrollPane, BorderLayout.CENTER);

        infoPanel.add(searchPanel, BorderLayout.NORTH);
        infoTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        infoTable.setModel(new DefaultTableModel(0, 0));
        infoTable.setPreferredScrollableViewportSize(new Dimension(500, 200));
        infoPanel.add(scrollPane, BorderLayout.CENTER);
        this.getContentPane().add(infoPanel, BorderLayout.SOUTH);
    }

    private void setUpToolbar() {
        toolbar = getToolBar();
        toolbar.addSeparator();
        toolbar.add(new JButton(new AreaSelectorAction(this)));
        toolbar.add(new JButton(new PerformAnalysisAction(this)));

        searchField.setText("include");
        searchPanel.add(searchField);
        searchPanel.add(search);
        displayOnMap.setEnabled(false);
        searchPanel.add(displayOnMap);

        search.addActionListener(new ExecuteQueryAction(this));
    }

    public void executeQuery(String queryText) throws IOException {
        Layer selectedLayer = this.getSelectedLayer();
        if (selectedLayer == null) {
            JOptionPane.showMessageDialog(this, "Please select a layer, before searching.");
            return;
        }
        if (queryText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter query!");
            return;
        }
        SimpleFeatureSource source = (SimpleFeatureSource) selectedLayer.getFeatureSource();
        FeatureType schema = source.getSchema();

        String name = schema.getGeometryDescriptor().getLocalName();

        try {
            Filter filter = CQL.toFilter(queryText);

            Query query = new Query(schema.getName().getLocalPart(), filter,
                    new String[]{name});

            SimpleFeatureCollection features = source.getFeatures(filter);

            FeatureCollectionTableModel model = new FeatureCollectionTableModel(
                    features);
            infoTable.setModel(model);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this.infoPanel, e.getMessage().toString(), "Warning", JOptionPane.WARNING_MESSAGE);
            System.out.println("error:");
            System.out.println(e.toString());
        }
    }

    /**
     * return Selected map layer
     *
     * @return
     */
    public Layer getSelectedLayer() throws IOException {
        List<Layer> layers = this.getMapContent().layers();
        for (Layer element : layers) {
            if (element.isSelected()) {
                System.out.println("Selected layer: " + element.getTitle());
                return element;
            }
        }
        throw new IOException("Layer not selected!");
    }

    public void setSelectedRectangle(Rectangle rectangle) {
        this.selectionRectangle = rectangle;
    }

    public Rectangle getSelectedRectangle() {
        return this.selectionRectangle;
    }

    public SimpleFeatureCollection selectFeatures(Rectangle rectangle, Layer layer, String name) {

        lastSelected = layer;
        this.setGeometry(layer.getFeatureSource().getSchema().getGeometryDescriptor());
        AffineTransform screenToWorld = this.getMapPane().getScreenToWorldTransform();
        System.out.println("Renkamasi is sluoksnio: " + name + " kvadratas: " + rectangle);
        Rectangle2D worldRect = screenToWorld.createTransformedShape(rectangle).getBounds2D();
        ReferencedEnvelope bbox = new ReferencedEnvelope(worldRect, this.getMapContent().getCoordinateReferenceSystem());
        /*
         * Create a Filter to select features that intersect with the bounding
         * box
         */
        Filter filter = ff.intersects(ff.property(geometryAttributeName),
                ff.literal(bbox));

        /*
         * Use the filter to identify the selected features
         */
        try {
            SimpleFeatureCollection selectedFeatures = (SimpleFeatureCollection) layer.getFeatureSource().getFeatures(filter);
            this.selectedFeatures.clear();
            this.selectedFeatures.addAll(selectedFeatures);
            System.out.println("Pasirinkta features: "
                    + this.selectedFeatures.size());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return selectedFeatures;
    }

    /**
     * Retrieve information about the feature geometry
     */
    private void setGeometry(GeometryDescriptor geoD) {
        GeometryDescriptor geomDesc = geoD;
        geometryAttributeName = geomDesc.getLocalName();

        Class<?> clazz = geomDesc.getType().getBinding();

        if (Polygon.class.isAssignableFrom(clazz)
                || MultiPolygon.class.isAssignableFrom(clazz)) {
            geometryType = GeometryType.POLYGON;

        } else if (LineString.class.isAssignableFrom(clazz)
                || MultiLineString.class.isAssignableFrom(clazz)) {

            geometryType = GeometryType.LINE;

        } else {
            geometryType = GeometryType.POINT;
        }

    }

    /*
     * PARAMETERS
     */
    public float getRiverDistance() {
        return gui.getDistanceToRiver();
    }

    public float getArea() {
        return gui.getArea();
    }

    public float getCityDistance() {
        return gui.getDistance();
    }

    public float getSlope() {
        return gui.getSlope();
    }

    public float getForests() {
        return gui.getForests();
    }

    public Layer getCitylayer() {
        return this.getMapContent().layers().get(3);
    }

    public Layer getRoadLayer() {
        return this.getMapContent().layers().get(0);
    }

    public Layer getRegionLayer() {
        return this.getMapContent().layers().get(4);
    }

    public Layer getRiverLayer() {
        return this.getMapContent().layers().get(1);
    }

    public double getRoadDistance() {
        return gui.getRoadDistance();
    }

    public Layer getReljefLayer() {
        return this.getMapContent().layers().get(2);
    }

    private enum GeometryType {

        POINT, LINE, POLYGON
    };
}
