package com.gisserver;

import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.collection.ClippedFeatureCollection;
import org.geotools.feature.collection.ClippedFeatureIterator;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class TestShapeClipe
{
    @Test
    public static void main(String[] args){
        File file = new File("/Users/bailing/geofile/s/province.shp");
        SimpleFeatureCollection featureCollection = null;
        try {
            // 读取源文件shape
            ShapefileDataStore shapefileDataStore = new ShapefileDataStore(file.toURI().toURL());
            shapefileDataStore.setCharset(StandardCharsets.UTF_8);
            System.out.println(shapefileDataStore.getCharset() + "获取编码11111111111111111111");
            SimpleFeatureSource featureSource = shapefileDataStore.getFeatureSource();
            featureCollection = featureSource.getFeatures();
            // 自定义的裁剪范围
            GeometryFactory geometryFactory = new GeometryFactory();
            Coordinate[] coordinates = {
                    new Coordinate(119.8535158272736, 36.0208825916344),
                    new Coordinate(119.909758272736, 36.091077415795),
                    new Coordinate(119.97536774812598, 36.1120377415795),
                    new Coordinate(120.03026774812598, 36.0559377415795),
                    new Coordinate(120.01796774812598, 36.023648259164),
                    new Coordinate(119.8535158272736, 36.0208825916344)
            };
            Polygon polygon = geometryFactory.createPolygon(coordinates);
            polygon.setSRID(4326);
            // 调用geoTools的裁剪要素方法
            ClippedFeatureCollection clippedFeatureCollection = new ClippedFeatureCollection(featureCollection, polygon, true);
            ClippedFeatureIterator clippedFeatureIterator = (ClippedFeatureIterator) clippedFeatureCollection.features();
            // 根据图层名称获取要素的source
            SimpleFeatureSource simpleFeatureSource = shapefileDataStore.getFeatureSource(shapefileDataStore.getTypeNames()[0]);
            // 根据参数创建shape存储空间
            String strPath = "/Users/bailing/geofile/s/clipprovince.shp";
            File s1 = new File(strPath);
            File fileParent = s1.getParentFile();
            if(!fileParent.exists()){
                fileParent.mkdirs();
            }
            file.createNewFile();

            ShapefileDataStore dataStore = new ShapefileDataStore(s1.toURI().toURL());
            dataStore.setCharset(Charset.forName("GBK"));
            System.out.println(dataStore.getCharset() + "获取编码22222");

            SimpleFeatureType simpleFeatureType = simpleFeatureSource.getSchema();
            // 创建
            dataStore.createSchema(simpleFeatureType);

            // 设置write 并设置为自动提交
            FeatureWriter<SimpleFeatureType, SimpleFeature> writer = dataStore.getFeatureWriter(dataStore.getTypeNames()[0], Transaction.AUTO_COMMIT);
            // 循环写入要素
            while (clippedFeatureIterator.hasNext()){
                // 获取要写入的要素
                SimpleFeature feature = clippedFeatureIterator.next();
                // 将要素写入位置
                SimpleFeature featureBuffer = writer.next();
                // 设置写入要素所有属性
                featureBuffer.setAttributes(feature.getAttributes());
                // 获取the_geom属性的值
                Geometry geo = (Geometry) feature.getAttribute("the_geom");
                // 重新覆盖the_geom属性的值， 这里的geoBuff必须为Geometry类型
                featureBuffer.setAttribute("the_geom", geo);
            }
            // 将所有数据写入
            writer.write();
            // 关闭写入流
            writer.close();
            clippedFeatureIterator.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
