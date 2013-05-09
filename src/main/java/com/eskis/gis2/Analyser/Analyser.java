/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eskis.gis2.Analyser;

import com.eskis.gis2.MapFrame;
import com.vividsolutions.jts.geom.*;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.JOptionPane;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.FilterFactory2;
import com.eskis.gis2.Helpers.BufferedFeatureCollection;
import com.eskis.gis2.Helpers.ConversionUtils;
import com.eskis.gis2.Helpers.IntersectedFeatureCollection;
import org.geotools.feature.*;
import org.geotools.metadata.iso.ApplicationSchemaInformationImpl;
import org.opengis.feature.Attribute;
import org.opengis.parameter.ParameterNotFoundException;

/**
 *
 * @author Marcus
 */
public class Analyser {

    MapFrame mapFrame;
    protected Layer layer;
    private static final Color DEFAULT_LINE = Color.BLACK;
    private static final Color DEFAULT_FILL = Color.WHITE;
    Rectangle selectionRectangle;
    Rectangle rec;
    protected Layer cityAreaLayer;
    protected Geometry cityAreaGeometry;
    protected Layer riverAreaLayer;
    protected Geometry riverAreaGeometry;
    protected Layer roadAreaLayer;
    protected Geometry roadAreaGeometry;
    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
    
    /**
     * Results
     */
    protected FeatureCollection<SimpleFeatureType, SimpleFeature> resultCollection;
    private Geometry searchArea;
    private SimpleFeatureCollection finalSearchCollectio;
    private Layer reljefLayer;
    private SimpleFeatureCollection selectedReljefFeatures;
    /**
     * Found resulting areas
     */
    private SimpleFeatureCollection foundAreaObjects;

    public Analyser() {
    }

    public Analyser(MapFrame aThis) {
        mapFrame = aThis;
    }

    public void analyze() throws IOException {

        try {
            // Prepare selection
            prepare();
            
            constructCities();
            System.out.println("Cities constructed...");

//            constructRiverLayer();
//            System.out.println("Rivers constructed...");
//
//            constructRoadLayer();
//            System.out.println("Roads constructed...");
//
//            calcFinalArea();
//            System.out.println("Final area constructed...");

            constructReljef();
            System.out.println("Reljef  constructed...");

            searchArea();
            System.out.println("Search done.");

            System.out.println("marking peaks...");
            markPeaks();
            
            findPeaks();
            
            System.out.println("Done.");

            JOptionPane.showMessageDialog(null, "Analysis done.");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Klaida analizuojant: " + e.toString());
            JOptionPane.showMessageDialog(null, e.getStackTrace());
        }
    }

    protected void constructCities() throws IOException {
        // Filter cities
        Layer cityLayer = this.mapFrame.getCitylayer();

        SimpleFeatureCollection selectedFeatures = this.mapFrame.selectFeatures(selectionRectangle, this.mapFrame.getCitylayer(), "City Area");

        // Bufferize
        DefaultFeatureCollection bufferizedCities = new DefaultFeatureCollection(new BufferedFeatureCollection(selectedFeatures, "BufferedCi", mapFrame.getCityDistance()));

        // Join geometries
        Geometry cityFeatures = ConversionUtils.getGeometries(bufferizedCities);
        Geometry oneBigCity = cityFeatures.buffer(0); // sujungia i viena
        
        cityAreaGeometry = oneBigCity;

        SimpleFeatureCollection cities = ConversionUtils.geometryToFeatures(oneBigCity, "BigCityArea");
        Style style = SLD.createPolygonStyle(DEFAULT_LINE, Color.GRAY, 0.5f);
        Layer layer = new FeatureLayer(cities, style, "CityArea");
        layer.setVisible(false);
        this.mapFrame.getMapContent().addLayer(layer);
        cityAreaLayer = layer;
    }
//
    protected void constructRiverLayer() throws IOException {

        // 50 meters
        double riverWidth = 50;

        /**
         * River Buffer
         */
        Layer riverLayer = mapFrame.getRiverLayer();

        SimpleFeatureCollection selectedFeatures = this.mapFrame.selectFeatures(selectionRectangle, this.mapFrame.getRiverLayer(), "City Area");

        // Bufferize
        DefaultFeatureCollection bufferizedRivers = new DefaultFeatureCollection(new BufferedFeatureCollection(selectedFeatures, "BufferedRiver", riverWidth));

        // Join geometries
        Geometry riverFeatures = ConversionUtils.getGeometries(bufferizedRivers);
        Geometry riverArea = riverFeatures.buffer(0); // sujungia i viena

        /**
         * Second river buffer for final area
         */
        Geometry riverFullArea = riverArea.buffer(mapFrame.getRiverDistance());

        Geometry riverFinalArea = riverFullArea.difference(riverArea);
        riverAreaGeometry = riverFinalArea;

        // Add layer
        SimpleFeatureCollection singleColelctionItem = ConversionUtils.geometryToFeatures(riverFinalArea, "BigRiverArea");
        Style style = SLD.createPolygonStyle(DEFAULT_LINE, Color.BLUE, 0.5f);
        Layer layer = new FeatureLayer(singleColelctionItem, style, "RiverArea");
        layer.setVisible(false);
        this.mapFrame.getMapContent().addLayer(layer);
        riverAreaLayer = layer;
    }
//
    protected void constructRoadLayer() throws IOException {

        double roadWidth = 20;

        Layer roadLayer = mapFrame.getRoadLayer();

        SimpleFeatureCollection selectedFeatures = this.mapFrame.selectFeatures(selectionRectangle, this.mapFrame.getRoadLayer(), "Road Area");

        SimpleFeatureCollection roadCollection = FeatureCollections.newCollection();
        roadCollection.addAll(selectedFeatures);

        // Bufferize
        DefaultFeatureCollection bufferizedRoads = new DefaultFeatureCollection(new BufferedFeatureCollection(roadCollection, "BufferedRoads", roadWidth));

        // Join geometries
        Geometry roadFeatures = ConversionUtils.getGeometries(bufferizedRoads);
        Geometry roadArea = roadFeatures.buffer(0); // sujungia i viena

        /**
         * Construct available road buffer
         */
        Geometry roadFullArea = roadArea.buffer(mapFrame.getRoadDistance());

        Geometry roadFinalArea = roadFullArea.difference(roadArea);

        roadAreaGeometry = roadFinalArea;

        // Add layer
        SimpleFeatureCollection singleCollectionItem = ConversionUtils.geometryToFeatures(roadFinalArea, "BigRoadArea");
        Style style = SLD.createPolygonStyle(DEFAULT_LINE, Color.cyan, 0.5f);
        Layer layer = new FeatureLayer(singleCollectionItem, style, "RoadArea");
        layer.setVisible(false);
        this.mapFrame.getMapContent().addLayer(layer);
        roadAreaLayer = layer;
    }

    protected void constructReljef() {

        SimpleFeatureCollection reljefFeatures = this.mapFrame.selectFeatures(selectionRectangle, this.mapFrame.getReljefLayer(), "Reljef Area");

        selectedReljefFeatures = reljefFeatures;

        Style style = SLD.createPolygonStyle(DEFAULT_LINE, Color.ORANGE, 1);
        Layer layer = new FeatureLayer(reljefFeatures, style, "ReljefArea");
        layer.setVisible(false);
        this.mapFrame.getMapContent().addLayer(layer);
        reljefLayer = layer;

    }
//
    protected void prepare() {

        // With world coords
        selectionRectangle = mapFrame.getSelectedRectangle();
        
        if(selectionRectangle == null ){
            throw new ParameterNotFoundException("Please selecta rea first!", null);
        }

        System.out.println("RECTANGLE: maxX " + selectionRectangle.getMaxX()
                + " maxY " + selectionRectangle.getMaxY() + " minX "
                + selectionRectangle.getMinX() + " minY "
                + selectionRectangle.getMinY() + " centrX "
                + selectionRectangle.getCenterX() + " centrY "
                + selectionRectangle.getCenterY());
    }

    private void addNewLayer(FeatureCollection fc, String name) {
        Layer newLayer = new FeatureLayer(fc, SLD.createSimpleStyle(fc.getSchema()), name);
        MemoryDataStore mds = new MemoryDataStore(fc);
        this.mapFrame.getMapContent().addLayer(newLayer);
    }

    /**
     * Area calc logic
     */
    private void calcFinalArea() {
        searchArea = cityAreaGeometry.intersection(riverAreaGeometry).intersection(roadAreaGeometry);

        // Display final area
        finalSearchCollectio = ConversionUtils.geometryToFeatures(searchArea, "FinalArea");
        Style style = SLD.createPolygonStyle(DEFAULT_LINE, Color.pink, 0.5f);
        Layer layer = new FeatureLayer(finalSearchCollectio, style, "Final");
        layer.setVisible(true);
        this.mapFrame.getMapContent().addLayer(layer);
    }

    /**
     * Search area in final territory
     */
    private void searchArea() {

        if(searchArea == null){
            return;
        }
        
        SimpleFeatureCollection searchAreaObjects = ConversionUtils.geometryToFeatures(searchArea, "obj");
        
        if(searchAreaObjects.size() == 0){
            return;
        }

        DefaultFeatureCollection searchAreaObjectsWithReljef = new DefaultFeatureCollection(new IntersectedFeatureCollection(searchAreaObjects, selectedReljefFeatures));

        Style styleRM = SLD.createPolygonStyle(DEFAULT_LINE, Color.GREEN, 1);
        Layer layerRM = new FeatureLayer(searchAreaObjectsWithReljef, styleRM, "Search Area features");
        layerRM.setVisible(false);
        this.mapFrame.getMapContent().addLayer(layerRM);

        // Draw search area

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setCRS(searchAreaObjectsWithReljef.getSchema().getCoordinateReferenceSystem());
        typeBuilder.setName("Area");

        AttributeTypeBuilder builderA = new AttributeTypeBuilder();
        builderA.setBinding(MultiPolygon.class);
        AttributeDescriptor attributeDescriptor = builderA.buildDescriptor("the_geom", builderA.buildType());
        typeBuilder.add(attributeDescriptor);
        
        typeBuilder.add("height", Integer.class);
        typeBuilder.add("slope", Integer.class);
        typeBuilder.add("peak", Integer.class);
        typeBuilder.setDefaultGeometry("the_geom");
        SimpleFeatureType type = typeBuilder.buildFeatureType();


        SimpleFeatureIterator i = searchAreaObjectsWithReljef.features();

        Collection<Geometry> geometrijos = new ArrayList<Geometry>();

        SimpleFeatureCollection areaFeatures = FeatureCollections.newCollection();

        int id = 1;
        while (i.hasNext()) {
            SimpleFeature feature = i.next();
            Geometry fGeometry = (Geometry) feature.getDefaultGeometry();
            Geometry bbox = fGeometry.getEnvelope();
            double miX = bbox.getEnvelopeInternal().getMinX();
            double maX = bbox.getEnvelopeInternal().getMaxX();
            double miY = bbox.getEnvelopeInternal().getMinY();
            double maY = bbox.getEnvelopeInternal().getMaxY();
            double abcisis = maX - miX;
            double ordinate = maY - miY;
            
            // Sklypo krastine
            double side = this.mapFrame.getArea();
            
            int sideLine = (int) side;
            // jei maziau nei sklypo dydis nei nedet
            if ((abcisis < side) || (ordinate < side)) {
                continue;
            } else {
                int j = (int) abcisis;
                int k = (int) ordinate;
                boolean ardidint = true;
                for (int l = (int) miX; l < (int) maX - sideLine; l++) {
                    boolean keisti = false;
                    for (int l2 = (int) miY; l2 < (int) maY - sideLine; l2++) {
                        Geometry sklypas = areaMarker(l, l2, side);
                        if (!geometrijos.isEmpty()) {
                            Iterator<Geometry> ijk = geometrijos.iterator();
                            boolean breaking = false;
                            while (ijk.hasNext()) {
                                Geometry dgd = ijk.next();
                                if (dgd.overlaps(sklypas)) {
                                    breaking = true;
                                    break;
                                }
                            }
                            if (breaking) {
                                continue;
                            }
                        }
                        if (sklypas.coveredBy(fGeometry)) {
                            geometrijos.add(sklypas);
                            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
                                                        
                            builder.set("the_geom", sklypas);
                            builder.set("height", 0);
                            builder.set("slope", 0);
                            builder.set("peak", 0);
                            
                            builder.add(id);
                                                        
                            SimpleFeature resultFeature = builder.buildFeature(String.valueOf(id));
                            resultFeature.setDefaultGeometry(sklypas);
                            areaFeatures.add(resultFeature);
                            id++;
                            l2 += sideLine;
                            ardidint = true;
                        }

                    }
                }
            }
            if (id > 20) {
                i.close();
                break;
            }
            System.out.println("Found: " + id);

        };
        Style styleG = SLD.createPolygonStyle(DEFAULT_LINE, Color.RED, 1);
        Layer layerG = new FeatureLayer(areaFeatures, styleG);
        layerG.setTitle("Found areas");
        this.mapFrame.getMapContent().addLayer(layerG);
        
        foundAreaObjects = areaFeatures;

    }
    public Polygon areaMarker(double x, double y, double krastas) {
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
        Coordinate[] coords = new Coordinate[5];
        Coordinate X1 = new Coordinate(x, y);
        Coordinate X2 = new Coordinate(x + krastas, y);
        Coordinate X3 = new Coordinate(x + krastas, y + krastas);
        Coordinate X4 = new Coordinate(x, y + krastas);
        Coordinate XC = new Coordinate(x, y);
        coords[0] = X1;
        coords[1] = X2;
        coords[2] = X3;
        coords[3] = X4;
        coords[4] = XC;

        LinearRing ring = geometryFactory.createLinearRing(coords);
        LinearRing holes[] = null; // use LinearRing[] to represent holes
        Polygon polygon = geometryFactory.createPolygon(ring, holes);
        return polygon;
    }

    /**
     * Search for peaks and marking all.
     */
    private void markPeaks() throws IOException {
        
        if(foundAreaObjects == null){
            return;
        }

        // Iterate over all found areas to mark peaks 
        SimpleFeatureIterator iter = (SimpleFeatureIterator) foundAreaObjects.features();
        
        
        while (iter.hasNext()) {
            SimpleFeature areaFeature = iter.next();
            Geometry areaGeometry = (Geometry) areaFeature.getDefaultGeometry();
            
            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
            
            org.geotools.filter.Filter filter = (org.geotools.filter.Filter) ff.intersects(ff.property("the_geom"), ff.literal(areaGeometry));
            SimpleFeatureCollection peaks = (SimpleFeatureCollection) mapFrame.getReljefLayer().getFeatureSource().getFeatures(filter);
            
            SimpleFeatureIterator peakIterator = peaks.features();
            
            double highest = 0;
            
            while(peakIterator.hasNext()){
                SimpleFeature peakFeature = peakIterator.next();
                
//                double height = Double.parseDouble( peakFeature.getAttribute("AUKSTIS").toString());
//                
//                if(highest < height){
//                    highest = height;
//                    peakFeature.setAttribute("peak", highest);
//                }
//                
//                System.out.println("Aukstis: " + height);
//                
//                peakFeature.setAttribute("height", height);
            }

        }

    }

    private void findPeaks() {
        SimpleFeatureIterator reljefIterator = (SimpleFeatureIterator) selectedReljefFeatures.features();
        
        double highest = 0;
        SimpleFeature highestPeak = null;
        double lowest = 999999;
        
        while(reljefIterator.hasNext()){
            SimpleFeature reljefFeature = reljefIterator.next();
            
            double height = Double.parseDouble(reljefFeature.getAttribute("AUKSTIS").toString());
            
            // Most height
            if(highest < height){
                highest = height;
                highestPeak = reljefFeature;
            }
            
            // least height
            if(lowest > height){
                lowest = height;
            }
        }
        
        System.out.print("Difference between: " + (highest - lowest));
        
        // MARK HIGHEST PEAK
        
        if(highestPeak != null) {
            // Create highest peak 

            SimpleFeatureCollection peakCollection = FeatureCollections.newCollection();
            peakCollection.add(highestPeak);
            
            addNewLayer(peakCollection, "Highest Peak");
            
        }
        
    }
}
