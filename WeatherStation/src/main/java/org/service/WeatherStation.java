/*
 * Created by Amr Momtaz.
 */

package org.service;

import java.util.Properties;
import java.util.Random;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

/**
 * Driver code for the weather station.
 * args[0] -> Station ID
 */
public final class WeatherStation {

    // Constants
    private static final Integer COOLDOWN = 1000; // Weather station cool-down in ms
    private static final String KAFKA_SERVER_CONFIGS = "localhost:9092";
    private static final String KAFKA_TOPIC = "weather_data_topic";
    private static final Properties kafkaProperties;
    private static final Random random;

    private static final Logger logger;

    // Weather Station State Variables
    public static Long stationID;
    public static Integer sequenceNumber;
    public static String batteryStatus;

    private static char[] batteryStatusRandomArray;
    private static int batteryStatusRandomArrayIdx;

    static {
        kafkaProperties = new Properties();
        kafkaProperties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_SERVER_CONFIGS);
        kafkaProperties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProperties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        logger = LogManager.getLogger(WeatherStation.class);
        random = new Random(17);
    }

    public static void main(String[] args) throws InterruptedException {
        initializeStation(args);
        try (KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(kafkaProperties)) {
            while (true) {
                JSONObject apiResponse = WeatherDataServiceConnector.getWeatherData();
                JSONObject filteredMessage = ContentsFilter.filterMessage(apiResponse);
                if (filteredMessage == null) continue;
                JSONObject enrichedMessage = ContentsEnricher.enrichMessage(filteredMessage);
                if (enrichedMessage == null) continue;

                // Randomly drop a message with rate 10%
                if (random.nextDouble() >= 0.1) {
                    logger.info("Weather message is fed to Kafka service.");
                    kafkaProducer.send(new ProducerRecord<>(KAFKA_TOPIC, enrichedMessage.toString()));
                } else logger.info("Weather message is dropped.");
                updateState();
                Thread.sleep(COOLDOWN);
            }
        }
    }

    // Private Methods

    /**
     * Initializes the weather station state variables.
     */
    private static void initializeStation(String[] args) {
        stationID = Long.parseLong(args[0]);
        sequenceNumber = 1;
        batteryStatusRandomArray = new char[100];
        for (int i = 0 ; i < 30 ; i++) batteryStatusRandomArray[i] = 'L';
        for (int i = 30 ; i < 70 ; i++) batteryStatusRandomArray[i] = 'M';
        for (int i = 70 ; i < 100 ; i++) batteryStatusRandomArray[i] = 'H';
        shuffleCharArray(batteryStatusRandomArray);
        batteryStatusRandomArrayIdx = 0;
        batteryStatus = getBatteryStatusAndUpdateState();
    }

    /**
     * Updates the weather station's state.
     */
    private static void updateState() {
        sequenceNumber += 1;
        batteryStatus = getBatteryStatusAndUpdateState();
    }

    /**
     * Shuffles the given character using Fisher-Yates shuffle algorithm.
     */
    public static void shuffleCharArray(char[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    /**
     *  Returns the current battery status and update its state.
     */
    public static String getBatteryStatusAndUpdateState() {
        String batteryStatus;
        switch (batteryStatusRandomArray[batteryStatusRandomArrayIdx++]) {
            case 'L' -> batteryStatus = "low";
            case 'M' -> batteryStatus = "medium";
            case 'H' ->batteryStatus = "high";
            default -> throw new RuntimeException("Invalid initialization of battery status array");
        }
        if (batteryStatusRandomArrayIdx == 100) {
            batteryStatusRandomArrayIdx = 0;
            shuffleCharArray(batteryStatusRandomArray);
        }
        return batteryStatus;
    }
}