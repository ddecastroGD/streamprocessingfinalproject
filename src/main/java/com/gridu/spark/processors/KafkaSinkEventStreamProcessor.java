package com.gridu.spark.processors;

import com.gridu.converters.JsonConverter;
import com.gridu.model.BotRegistry;
import com.gridu.model.Event;
import com.gridu.spark.StopBotJob;
import com.gridu.spark.sql.EventDao;
import com.gridu.spark.utils.OffsetUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.spark.sql.Dataset;
import org.apache.spark.streaming.Milliseconds;
import org.apache.spark.streaming.Minutes;
import org.apache.spark.streaming.Seconds;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import scala.collection.immutable.Stream;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class KafkaSinkEventStreamProcessor implements EventsProcessor {

    private List<String> topics;
    private Map<String, Object> props;
    public static final long ACTIONS_THRESHOLD = 18;
    private JavaStreamingContext jsc;
    private EventDao eventDao;

    public KafkaSinkEventStreamProcessor(List<String> topics, Map<String, Object> props,
                                         JavaStreamingContext jsc, EventDao eventDao) {
        this.topics = topics;
        this.props = props;
        this.jsc=jsc;
        this.eventDao = eventDao;
        jsc.sparkContext().setLogLevel("ERROR");
    }

    public void process() {
        JavaInputDStream<ConsumerRecord<String, String>> stream = KafkaUtils.createDirectStream(jsc,
                LocationStrategies.PreferConsistent(),
                ConsumerStrategies.Subscribe(topics, props));

        JavaDStream<Event> events = stream.map(consumerRecord -> {
            System.out.println(consumerRecord.value());
            return JsonConverter.fromJson(consumerRecord.value());

        })
                .window(Milliseconds.apply(StopBotJob.WINDOW_MS));

        events.foreachRDD((rdd, time) -> {
            rdd.cache();
            if(rdd.count() > 0){

                System.out.println("--------Time: "+SimpleDateFormat.getDateTimeInstance().format(new Date(time.milliseconds()))+"-------");
                Dataset<BotRegistry> bots = eventDao.findBots(rdd.filter(event -> event != null), ACTIONS_THRESHOLD);

                //TODO persist bots to blacklist
            }

        });
        stream.foreachRDD(consumerRecordJavaRDD -> OffsetUtils.commitOffSets(consumerRecordJavaRDD, stream));

        jsc.start();

        try{
            jsc.awaitTermination();
        } catch (InterruptedException e) {
            jsc.sparkContext().getConf().log().error(e.getMessage(), e);
        }
    }
}
