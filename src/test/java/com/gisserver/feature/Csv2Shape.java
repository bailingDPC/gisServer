package com.gisserver.feature;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.UIManager;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * This example reads data for point locations and associated attributes from a comma separated text
 * (CSV) file and exports them as a new shapefile. It illustrates how to build a feature type.
 *
 * <p>Note: to keep things simple in the code below the input file should not have additional spaces
 * or tabs between fields.
 */
public class Csv2Shape
{

    public static void main(String[] args) throws Exception {
        // Set cross-platform look & feel for compatability
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

        File file = JFileDataStoreChooser.showOpenFile("csv", null);
//        File file = new File("/Users/bailing/geofile/point.csv");
        if (file == null) {
            return;
        }
        // 根据文件中的数据创建一个FeatureType并写入shapefile
        final SimpleFeatureType TYPE = DataUtilities.createType("location","the_geom:Point:srid=4326," + "name:String,"+"number:Integer");
        System.out.println("TYPE: " + TYPE);

        List<SimpleFeature> features = new ArrayList<>();

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
        try(BufferedReader reader = new BufferedReader(new FileReader(file))){
            String line = reader.readLine();
            System.out.println("Header:" + line);
            for(line = reader.readLine(); line != null; line = reader.readLine()){
                // skip blank lines
                if(line.trim().length() > 0){
                    String tokens[] = line.split("\\,");

                    double latitude = Double.parseDouble(tokens[0]);
                    double longitude = Double.parseDouble(tokens[1]);
                    String name = tokens[2].trim();
                    int number = Integer.parseInt(tokens[3].trim());

                    Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));

                    featureBuilder.add(point);
                    featureBuilder.add(name);
                    featureBuilder.add(number);;
                    SimpleFeature feature = featureBuilder.buildFeature(null);
                    features.add(feature);
                }
            }
        }
        // 从要素集中创建一个shapefile
        File newFile = getNewShapeFile(file);
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<>();
        params.put("url", newFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);

        newDataStore.createSchema(TYPE);

        /**
         * 将要素写入到shapeFile文件中
         * write the features to the shapefile
         * */
        Transaction transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
        SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();

        System.out.println("SHAPE:" + SHAPE_TYPE);
        if(featureSource instanceof SimpleFeatureStore){
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

            SimpleFeatureCollection featureCollection = new ListFeatureCollection(TYPE, features);
            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(featureCollection);
                transaction.commit();
            }catch (Exception problem){
                problem.printStackTrace();
                transaction.rollback();
            }finally {
                transaction.close();
            }
            System.exit(0);
        }else {
            System.out.println(typeName + "does not support read/write access");
            System.exit(1);
        }
    }

    private static File getNewShapeFile(File csvFile){
        String path = csvFile.getAbsolutePath();
        String newPath = path.substring(0, path.length() - 4) + ".shp";

        JFileDataStoreChooser chooser = new JFileDataStoreChooser("shp");
        chooser.setDialogTitle("Save shapefile");
        chooser.setSelectedFile(new File(newPath));

        int returnVal = chooser.showSaveDialog(null);

        if(returnVal != JFileDataStoreChooser.APPROVE_OPTION){
            System.exit(0);
        }

        File newFile = chooser.getSelectedFile();
        if(newFile.equals(csvFile)){
            System.out.println("Error: cannot replace " + csvFile);
            System.exit(0);
        }

        return newFile;
    }

    private static SimpleFeatureType createFeatureType(){
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Location");
        // coordinate reference system
        builder.setCRS(DefaultGeographicCRS.WGS84);
        // add attributes in order
        builder.add("the_geom", Point.class);
        // 15 chars with for name field
        builder.length(15).add("Name", String.class);
        builder.add("number", Integer.class);

        // build the type
        final SimpleFeatureType LOCATION = builder.buildFeatureType();
        return LOCATION;
    }
}
