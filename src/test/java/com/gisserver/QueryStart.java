package com.gisserver;

import org.geotools.data.*;
import org.geotools.data.collection.SpatialIndexFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.junit.Test;

import javax.activation.FileDataSource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class QueryStart
{
    @Test
    public static void main(String[] args) throws Exception {
        File file = new File("/Users/bailing/geofile/s/province.shp");
        /*
        // 使用连接参数映射
        Map<String, Object> params = new HashMap<>();
        params.put("url", file.toURI().toURL());
        params.put("create spatial index", false);
        params.put("memory mapped buffer", false);
        params.put("charset", StandardCharsets.UTF_8);
        DataStore dataStore = DataStoreFinder.getDataStore(params);
        SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0])
         */

        if(file == null){
            return;
        }
        FileDataStore fileDataStore = FileDataStoreFinder.getDataStore(file);
        SimpleFeatureSource featureSource = fileDataStore.getFeatureSource();

        // 内存中缓存shapefile
        SimpleFeatureSource simpleFeatureSource = DataUtilities.source(new SpatialIndexFeatureCollection(featureSource.getFeatures()));

        MapContent map = new MapContent();
        map.setTitle("QueryStart");

        Style style = SLD.createSimpleStyle(featureSource.getSchema());
        Layer layer = new FeatureLayer(featureSource, style);
        map.addLayer(layer);
        JMapFrame.showMap(map);
    }
}
