package com.gisserver.crs;

import org.geotools.data.*;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.JProgressWindow;
import org.geotools.swing.action.SafeAction;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.ProgressListener;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CRSLab
{
    private File sourceFile;
    private SimpleFeatureSource featureSource;
    private MapContent map;

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        CRSLab crsLab = new CRSLab();
        crsLab.displayShapeFile();
    }
    private void displayShapeFile() throws Exception {
        sourceFile = JFileDataStoreChooser.showOpenFile("*", null);
        if(sourceFile == null){
            return;
        }

        FileDataStore fileDataStore = FileDataStoreFinder.getDataStore(sourceFile);
        featureSource = fileDataStore.getFeatureSource();

        // create a map context and add our shapefile to it
        map = new MapContent();
        Style style = SLD.createSimpleStyle(featureSource.getSchema());
        Layer layer = new FeatureLayer(featureSource, style);
        map.layers().add(layer);

        // create a JMapFrame with custom toolbar buttons
        JMapFrame mapFrame = new JMapFrame();
        mapFrame.enableToolBar(true);
        mapFrame.enableStatusBar(true);

        JToolBar toolBar = mapFrame.getToolBar();
        toolBar.addSeparator();
         toolBar.add(new JButton(new ValidateGeometryAction()));
         toolBar.add(new JButton(new ExportShapefileAction()));

        // Display the map frame, when it is closed the application will exit
        mapFrame.setSize(800, 600);
        mapFrame.setVisible(true);
    }

    public class ValidateGeometryAction extends SafeAction
    {
        private static final long serialVersionUID = -1954665476704543470L;

        ValidateGeometryAction(){
            super("Validate geometry");
            putValue(Action.SHORT_DESCRIPTION, "check each geometry");
        }
        public void action(ActionEvent e) throws Throwable {
            int numInvalid = validateFeatureGeometry(null);
            String msg;
            if(numInvalid == 0){
                msg = "All feature geometries are valid";
            }else {
                msg = "Invalid geometries: " + numInvalid;
            }
            JOptionPane.showMessageDialog(null, msg, "Geometry results", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    public class ValidateGeometryAction2 extends SafeAction {
        ValidateGeometryAction2(){
            super("validate geometry");
            putValue(Action.SHORT_DESCRIPTION, "Check each geometry");
        }
        public void action(ActionEvent e) throws Throwable {
            SwingWorker worker = new SwingWorker<String, Object>()
            {
                @Override
                protected String doInBackground() throws Exception {
                    final JProgressWindow progress = new JProgressWindow(null);
                    progress.setTitle("Validating feature geometry");

                    int numInvalid = validateFeatureGeometry(progress);
                    if(numInvalid == 0){
                        return "All feature geometries are valid";
                    }else {
                        return "Invalid geometries: " + numInvalid;
                    }
                }
                protected void done(){
                    try {
                        Object result = get();
                        JOptionPane.showMessageDialog(
                                null,
                                result,
                                "Geometry results",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } catch (Exception ignore){

                    }
                }
            };
            worker.execute();
        }
    }

    private int validateFeatureGeometry(ProgressListener progress) throws Exception {
        final SimpleFeatureCollection featureCollection = featureSource.getFeatures();

        class ValidationVisitor implements FeatureVisitor {
            public int numInvalidGeometries = 0;

            public void visit(Feature f){
                SimpleFeature feature = (SimpleFeature) f;
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if(geometry != null && !geometry.isValid()){
                    numInvalidGeometries++;
                    System.out.println("Invalid Geometry: " + feature.getID());
                }
            }
        }

        ValidationVisitor visitor = new ValidationVisitor();
        // pass visitor and the progress bar to feature collection
        featureCollection.accepts(visitor, progress);
        return visitor.numInvalidGeometries;
    }

    class ExportShapefileAction extends SafeAction {
        private static final long serialVersionUID = 3825805853116528284L;

        ExportShapefileAction(){
            super("Export...");
            putValue(Action.SHORT_DESCRIPTION, "Export using current crs");
        }
        public void action(ActionEvent e) throws Throwable {
            exportToShapefile();
        }
    }
    private void exportToShapefile() throws Exception {
        SimpleFeatureType schema = featureSource.getSchema();
        JFileDataStoreChooser chooser = new JFileDataStoreChooser("shp");
        chooser.setDialogTitle("Save reprojected shapefile");
        chooser.setSaveFile(sourceFile);
        int returnVal = chooser.showSaveDialog(null);
        if(returnVal != JFileDataStoreChooser.APPROVE_OPTION){
            return;
        }
        File file = chooser.getSelectedFile();
        if(file.equals(sourceFile)){
            JOptionPane.showMessageDialog(null, "Cannot replace " + file);
            return;
        }

        // 设置一个数字转换用于处理数据
        CoordinateReferenceSystem dataCRS = schema.getCoordinateReferenceSystem();
        CoordinateReferenceSystem worldCRS = map.getCoordinateReferenceSystem();
        boolean lenient = true; // allow for some error due to different datums
        MathTransform transform = CRS.findMathTransform(dataCRS, worldCRS, lenient);
        // 抓取所有要素
        SimpleFeatureCollection featureCollection = featureSource.getFeatures();
        /**
         *  为了创建一个新的shapefile, 我们需要生成一个与原始的类似的功能类型， 唯一的区别是
         *  集合描述符的CoordinateReferenceSystem
         * */
        DataStoreFactorySpi factory = new ShapefileDataStoreFactory();
        Map<String, Serializable> create = new HashMap<>();
        create.put("url", file.toURI().toURL());
        create.put("create spatial index", Boolean.TRUE);
        DataStore dataStore = factory.createNewDataStore(create);
        SimpleFeatureType featureType = SimpleFeatureTypeBuilder.retype(schema, worldCRS);
        dataStore.createSchema(featureType);

        // Get the name of the new Shapefile, which will be used to open the FeatureWriter
        String createdName = dataStore.getTypeNames()[0];

        // 打开迭代器遍历内容， 并使用一个写入器来写出新的shapeFile
        Transaction transaction = new DefaultTransaction("Reproject");
        try (
                FeatureWriter<SimpleFeatureType, SimpleFeature> writer = dataStore.getFeatureWriterAppend(createdName, transaction);
                SimpleFeatureIterator iterator = featureCollection.features();
                ){
            while (iterator.hasNext()){
                // copy the contents of each feature and transform the geometry
                SimpleFeature feature = iterator.next();
                SimpleFeature copy = writer.next();
                copy.setAttributes(feature.getAttributes());

                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Geometry geometry2 = JTS.transform(geometry, transform);

                copy.setDefaultGeometry(geometry2);
                writer.write();
            }
            transaction.commit();
            JOptionPane.showMessageDialog(null, "export to shapefile complete");

        }catch (Exception problem){
            problem.printStackTrace();
            transaction.rollback();
            JOptionPane.showMessageDialog(null,"Export to shapefile failed");
        } finally {
            transaction.close();
        }
    }
}
