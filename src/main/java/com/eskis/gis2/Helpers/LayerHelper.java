/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eskis.gis2.Helpers;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;

/**
 *
 * @author Marcus
 */
public class LayerHelper {
    
    
    public static void preloadLayers(MapContent content) throws IOException{
        
        String current = new java.io.File(".").getCanonicalPath();
        System.out.println(current);

        HashSet<String> files = new HashSet();

        files.add("sven_KEL_L.shp");
        files.add("sven_HID_L.shp");
        //files.add("sven_PLO_P.shp");
        files.add("sven_REL_L.shp");

        for (Iterator<String> i = files.iterator(); i.hasNext();) {
            String filename = i.next();

            File file = new File(current + "\\..\\..\\gis_data\\LTsventoji\\" + filename);

            FileDataStore store = FileDataStoreFinder.getDataStore(file);
            SimpleFeatureSource featureSource = store.getFeatureSource();
            Style style = SLD.createSimpleStyle(featureSource.getSchema());
            Layer layer = new FeatureLayer(featureSource, style);
            layer.setTitle(file.getName());
            content.addLayer(layer);
        }


        HashSet<String> files2 = new HashSet();

        files2.add("gyvenvie.shp");
        files2.add("rajonai.shp");

        for (Iterator<String> i = files2.iterator(); i.hasNext();) {
            String filename = i.next();

            File file = new File(current + "\\..\\..\\gis_data\\lt200shp\\" + filename);

            FileDataStore store = FileDataStoreFinder.getDataStore(file);
            SimpleFeatureSource featureSource = store.getFeatureSource();
            Style style = SLD.createSimpleStyle(featureSource.getSchema());
            Layer layer = new FeatureLayer(featureSource, style);
            layer.setTitle(file.getName());
            content.addLayer(layer);
        }
    }
    
    
    
}
