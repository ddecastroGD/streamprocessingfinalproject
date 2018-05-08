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

public class SparkSQLEventDao implements SparkSqlDao<Event>{

    private SparkSession session;

    public void setLoggerLevel(String level) {
        session.sparkContext().setLogLevel(level);
    }

    public SparkSQLEventDao(SparkSession session) {
        this.session = session;
    }

    public SparkSQLEventDao(SparkContext sparkContext) {
        session = SparkSession.builder().sparkContext(sparkContext).getOrCreate();
    }

    public SparkSQLEventDao(String master, String appName) {
        session = SparkSession.builder().master(master).appName(appName).getOrCreate();
    }

    public SparkSQLEventDao(SparkConf sparkConf) {
        session = SparkSession.builder().config(sparkConf).getOrCreate();
    }

    public Dataset<Event> getEventsDataSetFromJavaRdd(JavaRDD<Event> rdd) {
        return session.createDataset(rdd.filter(event -> event != null).rdd(), Encoders.bean(Event.class)).cache();
    }

    public Dataset<BotRegistry> findBots(JavaRDD<Event> rdd, long threshold) {
        Dataset<BotRegistry> result = getEventsDataSetFromJavaRdd(rdd).select(col("ip"), col("type"), col("url"))
                .groupBy(col("ip"), col("type"), col("url"))
                .count()
                .filter(col("count").gt(threshold))
                .orderBy(col("count").desc())
                .as(Encoders.bean(BotRegistry.class)).cache();
        session.log().info(result.count()+" WERE FOUND");
        result.show();
        return result;
    }

    public void closeResource() {
        session.close();
    }

}
