/*
 * Created by Amr Momtaz.
 */

package org.service;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.api.BitcaskStore;
import org.json.JSONObject;
import org.store.BitCaskHandle;
import org.store.BitcaskStoreImpl;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Represents the base central station.
 */
public class BaseCentralStation {

    private static final BitcaskStore bitcaskStore;
    private static final BitCaskHandle bitcaskHandle;
    private static final Properties kafkaProperties;
    private static final Logger logger;
    private static final String KAFKA_TOPIC = "weather_data_topic";
    private static final String KAFKA_SERVER_CONFIGS = "localhost:9092";
    private static final String CONSUMER_GROUP_ID = "base_central_station";
    private static final String BITCASK_ROOT_DIRECTORY = "bitcask_store";

    static {
        logger = LogManager.getLogger(BaseCentralStation.class);

        kafkaProperties = new Properties();
        kafkaProperties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_SERVER_CONFIGS);
        kafkaProperties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaProperties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID);

        bitcaskStore = new BitcaskStoreImpl();
        bitcaskHandle = bitcaskStore.open(BITCASK_ROOT_DIRECTORY,  List.of(BitcaskStore.OPTIONS.READ_WRITE_OPTION));
    }

    public static void main(String[] args) {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaProperties)) {
            consumer.subscribe(Collections.singletonList(KAFKA_TOPIC));
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> consumerRecord : records) {
                    JSONObject response = new JSONObject(consumerRecord.value());
                    logger.debug(response);

                    // Storing the response in the bitcask store
                    bitcaskStore.put(bitcaskHandle, String.valueOf(response.getLong("station_id")), response.toString());

                    // Writing records to parquet files
                    ParquetWriterManager.writeParquetRecord(response);
                }
            }
        }
    }
}
