package com.gisserver.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geojson.feature.FeatureJSON;
import org.json.simple.JSONArray;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bailing
 */
@RestController
@RequestMapping("/geo")
public class GeoJson
{
    @GetMapping("/geojson/${name}")
    @ResponseBody
    public Object shp2geojson(@PathVariable("name") String name){
        // String shpPath = this.getClass().getResource("/").getFile() + "/file/pointgbk.shp";
        // String jsonPath = this.getClass().getResource("/").getFile() + "file/point.geojson";
        String shpPath = "/Users/bailing/geofile/s/" + name + ".shp";
        String jsonPath = "/Users/bailing/geofile/s/"+ name +".geojson";
        Map map = new HashMap();
        // 新建json对象
        FeatureJSON featureJSON = new FeatureJSON();
        JSONObject geojsonObject = new JSONObject();
        geojsonObject.put("type", "FeatureCollection");
        try {
            File file = new File(shpPath);
            ShapefileDataStore shapefileDataStore = null;
            shapefileDataStore = new ShapefileDataStore(file.toURL());
            // 设置编码
            String typeName = shapefileDataStore.getTypeNames()[0];
            SimpleFeatureSource simpleFeatureSource = null;
            simpleFeatureSource = shapefileDataStore.getFeatureSource(typeName);
            SimpleFeatureCollection resulut = simpleFeatureSource.getFeatures();
            SimpleFeatureIterator iterator = resulut.features();
            JSONArray array = new JSONArray();
            while (iterator.hasNext()){
                SimpleFeature feature = iterator.next();
                StringWriter writer = new StringWriter();
                featureJSON.writeFeature(feature, writer);
                String temp = writer.toString();
                byte[] bytes = temp.getBytes(StandardCharsets.ISO_8859_1);
                temp = new String(bytes, "GBK");
                System.out.println(temp);
                JSONObject json = JSON.parseObject(temp);
                array.add(json);
            }
            geojsonObject.put("features", array);
            iterator.close();
            Long startTime = System.currentTimeMillis();
            // 将json字符传使用字符流写入文件
            File outputfile = new File(jsonPath);
            FileOutputStream fileOutputStream = new FileOutputStream(outputfile);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
            outputStreamWriter.write(JSON.toJSONString(geojsonObject));
            outputStreamWriter.flush();
            outputStreamWriter.close();

            long endTime = System.currentTimeMillis();
            System.out.println("当前程序耗时：" + (endTime - startTime) + "ms");

        }catch (Exception e){
            map.put("status", "failure");
            map.put("message", e.getMessage());
            e.printStackTrace();

        }
        return geojsonObject;
    }
}
