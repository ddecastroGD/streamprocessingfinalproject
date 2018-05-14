package com.gridu.business;

import com.gridu.model.BotRegistry;
import com.gridu.persistence.ignite.IgniteBotRegistryDao;
import com.gridu.spark.helpers.SparkArtifactsHelper;
import org.apache.ignite.spark.JavaIgniteContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BotRegistryBusinessServiceTest {

    private BotRegistryBusinessService service;
    private IgniteBotRegistryDao dao;
    private JavaSparkContext sparkContext;
    @Mock
    private JavaIgniteContext igniteContext;

    @Before
    public void setup(){
        sparkContext = SparkArtifactsHelper.createSparkContext("local[*]", "botServiceTest");
        dao = new IgniteBotRegistryDao(igniteContext);
        service = new BotRegistryBusinessService(dao);
    }

    @Test
    public void execute() {

    }

    @Test
    public void shouldIdentifyAndReturnOneBot(){
        long expectedCount = 19;

        Dataset<Row> aggregatedDs = aRowDataSet(expectedCount);

        Dataset<BotRegistry> bots = service.identifyBots(aggregatedDs).cache();

        assertThat(bots.count()).isEqualTo(1);
        assertThat(bots.first()).isNotNull();
        assertThat(bots.first().getCount()).isEqualTo(expectedCount);
    }

    @Test
    public void shouldReturnNullWhenCountDoesNotExceedThreshold(){
        assertThat(service.identifyBots(aRowDataSet(1)).count()).isZero();
    }

    @Test
    @Ignore
    public void shouldRemoveExpiredBotsFromBlackList(){
        assertThat(service.removeExpiredBotsFromBlackList().count()).isNotZero();
    }

    private Dataset<Row> aRowDataSet(long count) {
        StructType structType = DataTypes.createStructType(new StructField[]{DataTypes.createStructField("ip",
                DataTypes.StringType,false),
                DataTypes.createStructField("url", DataTypes.StringType,true),
                DataTypes.createStructField("count", DataTypes.LongType,false)});

        final SparkSession sparkSession = SparkArtifactsHelper.createSparkSession(sparkContext);
        final List<Row> rows = Collections.singletonList(RowFactory.create("123", "anyurl", count));

        return sparkSession
                .createDataFrame(rows,structType);
    }
}