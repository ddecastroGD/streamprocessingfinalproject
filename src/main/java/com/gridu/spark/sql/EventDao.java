package com.gridu.spark.sql;

import com.gridu.model.BotRegistry;
import com.gridu.model.Event;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.col;

public class EventDao {

    private SparkSession session;

    public void setLoggerLevel(String level){
        session.sparkContext().setLogLevel(level);
    }

    public EventDao(SparkSession session){
        this.session = session;
    }

    public EventDao(SparkContext sparkContext){
        session = SparkSession.builder().sparkContext(sparkContext).getOrCreate();
    }

    public EventDao(String master, String appName){
        session = SparkSession.builder().master(master).appName(appName).getOrCreate();
    }

    public EventDao(SparkConf sparkConf){
        session = SparkSession.builder().config(sparkConf).getOrCreate();
    }

    public Dataset<BotRegistry> findBots(JavaRDD<Event> rdd, long treshold) {
        Dataset<Event> eventDS = session.createDataset(rdd.rdd(), Encoders.bean(Event.class));
        Dataset<BotRegistry> result = eventDS.select(col("ip"),col("type"), col("url"))
                .groupBy(col("ip"),col("type"), col("url"))
                .count()
                .filter(col("count").gt(treshold))
                .orderBy(col("count").desc())
                .as(Encoders.bean(BotRegistry.class));
        if(result != null)
            result.show();
        else
            session.log().info("No BOTS WERE FOUND");
        session.close();
        return result;
    }

}
