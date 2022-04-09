package com.gisserver.filter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.swing.action.SafeAction;
import org.geotools.swing.data.JDataStoreWizard;
import org.geotools.swing.table.FeatureCollectionTableModel;
import org.geotools.swing.wizard.JWizard;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;

@SuppressWarnings("serial")
public class QueryLab extends JFrame
{
    private DataStore dataStore;
    private JComboBox<String> featureTypeCBox;
    private JTable table;
    private JTextField text;

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        JFrame frame = new QueryLab();
        frame.setVisible(true);
    }

    public QueryLab(){
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        text = new JTextField(80);
        text.setText("include");
        getContentPane().add(text, BorderLayout.NORTH);

        table = new JTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setModel(new DefaultTableModel(5, 5));
        table.setPreferredScrollableViewportSize(new Dimension(500, 200));

        JScrollPane scrollPane = new JScrollPane(table);
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        featureTypeCBox = new JComboBox<>();
        menuBar.add(featureTypeCBox);

        JMenu dataMenu = new JMenu("data");
        menuBar.add(dataMenu);
        pack();

        fileMenu.add(
                new SafeAction("Open shapefile...")
                {
                    @Override
                    public void action(ActionEvent actionEvent) throws Throwable {
                        connect(new ShapefileDataStoreFactory());
                    }
                }
        );
        fileMenu.add(
                new SafeAction("Connect to PostGis database")
                {
                    @Override
                    public void action(ActionEvent actionEvent) throws Throwable {
                        connect(new PostgisNGDataStoreFactory());
                    }
                }
        );
        fileMenu.addSeparator();
        fileMenu.add(
                new SafeAction("Exit")
                {
                    @Override
                    public void action(ActionEvent actionEvent) throws Throwable {
                        System.exit(0);
                    }
                }
        );
        dataMenu.add(
                new SafeAction("Get features")
                {
                    @Override
                    public void action(ActionEvent actionEvent) throws Throwable {
                        filterFeatures();
                    }
                }
        );
        dataMenu.add(
                new SafeAction("Count")
                {
                    @Override
                    public void action(ActionEvent actionEvent) throws Throwable {
                        countFeatures();
                    }
                }
        );
        dataMenu.add(
                new SafeAction("Geometry")
                {
                    @Override
                    public void action(ActionEvent actionEvent) throws Throwable {
                        queryFeatures();
                    }
                }
        );
    }

    /**
     * 连接到数据库
     * */
    private void connect(DataStoreFactorySpi format) throws Exception {
        JDataStoreWizard wizard = new JDataStoreWizard(format);
        int result = wizard.showModalDialog();
        if(result == JWizard.FINISH){
            Map<String, Object> connectionParameters = wizard.getConnectionParameters();
            dataStore = DataStoreFinder.getDataStore(connectionParameters);
            if(dataStore == null){
                JOptionPane.showMessageDialog(null, "Could not connect - check parameters");
            }
            upDateUI();
        }
    }
    /**
     * 帮助方法更新组合框，用于选择一个功能类型
     * */
    private void upDateUI() throws Exception {
        ComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>(dataStore.getTypeNames());
        featureTypeCBox.setModel(comboBoxModel);

        table.setModel(new DefaultTableModel(5, 5));
    }

    /**
     * 查询
     * 过滤器类似与SQL语句的where子句； 定义每个特性需要满足的条件， 以便选择。
     * 以下是策略，显示选定的功能：
     * 1.获取用户选择的功能类型名称， 并从DataStore中检索相应的功能资源。
     * 2.获取在文本字段中输入的查询条件， 并使用CQL类创建筛选器对象
     * 3.将Filter传递给getFeature方法， 该方法将与查询匹配的功能作为FeatureCollection返回。
     * 4.为对话框的JTable创建一个FeatureCollectionTableModel. 这个GeoTools类接受一个FeatureCollection 并检索每个特性的特性属性名称和数据。
     * 考虑这个策略， 这里是实现
     * */
    // 1。 使用featureSource.getFeatures(过滤器)获取特性数据
    private void filterFeatures() throws Exception {
        String typeName = (String) featureTypeCBox.getSelectedItem();
        SimpleFeatureSource source = dataStore.getFeatureSource(typeName);

        Filter filter = CQL.toFilter(text.getText());
        SimpleFeatureCollection features = source.getFeatures(filter);
        FeatureCollectionTableModel model = new FeatureCollectionTableModel(features);
        table.setModel(model);
    }
    // 2.FeatureCollection 表现为与定义的查询或结果集， 不将数据加载到内存中。您可以使用可用的方法来询问整个FeatureCollection 的问题。
    private void countFeatures() throws Exception {
        String typeName = (String) featureTypeCBox.getSelectedItem();
        SimpleFeatureSource source = dataStore.getFeatureSource(typeName);

        Filter filter = CQL.toFilter(text.getText());
        SimpleFeatureCollection features = source.getFeatures(filter);

        int count = features.size();
        JOptionPane.showMessageDialog(text, "Number of selected features: " + count);
    }
    // 3. 通过使用查询数据结构， 可以更好的控制请求， 允许只选择所需的属性， 控制返回多少特性， 并要求一些特定的处理步骤， 如重新投影。
    // 下面是一个仅选择几何属性并将其显示在表中的示例
    private void queryFeatures() throws Exception {
        String typeName = (String) featureTypeCBox.getSelectedItem();
        SimpleFeatureSource source = dataStore.getFeatureSource(typeName);

        FeatureType schema = source.getSchema();
        String name = schema.getGeometryDescriptor().getLocalName();

        Filter filter = CQL.toFilter(text.getText());
        Query query = new Query(typeName, filter, new String[]{name});

        SimpleFeatureCollection features = source.getFeatures(query);

        FeatureCollectionTableModel model = new FeatureCollectionTableModel(features);
        table.setModel(model);
    }
}
