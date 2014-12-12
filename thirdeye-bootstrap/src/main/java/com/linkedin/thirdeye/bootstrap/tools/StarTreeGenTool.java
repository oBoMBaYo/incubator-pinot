package com.linkedin.thirdeye.bootstrap.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.SequenceFile.Reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.thirdeye.api.StarTreeConfig;
import com.linkedin.thirdeye.api.StarTreeManager;
import com.linkedin.thirdeye.api.StarTreeNode;
import com.linkedin.thirdeye.api.StarTreeRecord;
import com.linkedin.thirdeye.bootstrap.DimensionKey;
import com.linkedin.thirdeye.bootstrap.startree.generation.StarTreeGenerationConfig;
import com.linkedin.thirdeye.impl.StarTreeManagerImpl;
import com.linkedin.thirdeye.impl.StarTreeRecordImpl;

public class StarTreeGenTool {
  public static void main(String[] args) throws Exception {

    Path inputPath = new Path(args[0]);
    Path configPath = new Path(args[1]);
    Path outputPath = new Path(args[2]);

    SequenceFile.Reader reader = new SequenceFile.Reader(new Configuration(),
        Reader.file(inputPath));
    System.out.println(reader.getKeyClass());
    System.out.println(reader.getValueClassName());
    WritableComparable<?> key = (WritableComparable<?>) reader.getKeyClass()
        .newInstance();
    Writable val = (Writable) reader.getValueClass().newInstance();

    FileSystem fs = FileSystem.get(new Configuration());
    StarTreeGenerationConfig config = new ObjectMapper().readValue(
        fs.open(configPath), StarTreeGenerationConfig.class);

    String collectionName = config.getCollectionName();
    String timeColumnName = config.getTimeColumnName();
    List<String> splitOrder = config.getSplitOrder();
    int maxRecordStoreEntries = config.getSplitThreshold();
    List<String> dimensionNames = config.getDimensionNames();
    List<String> metricNames = config.getMetricNames();
    String recordStoreFactoryClass="com.linkedin.thirdeye.impl.StarTreeRecordStoreFactoryCircularBufferImpl";
    Properties recordStoreFactoryConfig = new Properties();
    recordStoreFactoryConfig.setProperty("numTimeBuckets", "672");
    recordStoreFactoryConfig.setProperty("rootDir", "");

    StarTreeConfig starTreeConfig = new StarTreeConfig.Builder()
        .setCollection(collectionName) //
        .setDimensionNames(dimensionNames)//
        .setMetricNames(metricNames)//
        .setTimeColumnName(timeColumnName) //
        .setRecordStoreFactoryClass(recordStoreFactoryClass)
        .setRecordStoreFactoryConfig(recordStoreFactoryConfig)
        // .setSplitOrder(splitOrder)//
        .setMaxRecordStoreEntries(maxRecordStoreEntries).build();
    System.out.println(starTreeConfig.toJson());
    if(true){
      System.exit(1);
    }
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    StarTreeManager starTreeManager = new StarTreeManagerImpl(executorService);
    starTreeManager.registerConfig(collectionName, starTreeConfig);
    starTreeManager.create(collectionName);
    starTreeManager.open(collectionName);
    while (reader.next(key, val)) {
      BytesWritable writable = (BytesWritable) key;
      DimensionKey dimensionKey = DimensionKey.fromBytes(writable.getBytes());
      System.out.println(dimensionKey);
      Map<String, String> dimensionValuesMap = new HashMap<String, String>();
      for (int i = 0; i < dimensionNames.size(); i++) {
        dimensionValuesMap.put(dimensionNames.get(i),
            dimensionKey.getDimensionsValues()[i]);
      }
      Map<String, Integer> metricValuesMap = new HashMap<String, Integer>();
      for (int i = 0; i < metricNames.size(); i++) {
        metricValuesMap.put(metricNames.get(i), 0);
      }
      Long time = 0l;
      StarTreeRecord record = new StarTreeRecordImpl(dimensionValuesMap,
          metricValuesMap, time);
      starTreeManager.getStarTree(collectionName).add(record);

    }
    // dump the star tree
    StarTreeNode root = starTreeManager.getStarTree(collectionName).getRoot();
    FileOutputStream fileOutputStream = new FileOutputStream(new File(
        outputPath + "/" + "tree.bin"));
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(
        fileOutputStream);
    objectOutputStream.writeObject(root);
    objectOutputStream.close();
  }

}
