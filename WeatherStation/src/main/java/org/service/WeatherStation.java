/*
 * Created by Amr Momtaz.
 */

package org.service;

import java.util.Random;

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
    private static final Random random = new Random(17);
    private static final Logger logger = LogManager.getLogger(WeatherStation.class);

    // Weather Station State Variables
    public static Long stationID;
    public static Integer sequenceNumber;
    public static String batteryStatus;

    private static char[] batteryStatusRandomArray;
    private static int batteryStatusRandomArrayIdx;


    public static void main(String[] args) throws InterruptedException {
        initializeStation(args);
        while(true) {
            JSONObject apiResponse = WeatherDataServiceConnector.getWeatherData();
            JSONObject filteredMessage = ContentsFilter.filterMessage(apiResponse);
            if (filteredMessage == null) continue;
            JSONObject enrichedMessage = ContentsEnricher.enrichMessage(filteredMessage);
            if (enrichedMessage == null) continue;

            // Randomly drop a message with rate 10%
            if (random.nextDouble() >= 0.1) {
                // TODO: feed the message to Kafka queueing service
                logger.info("Weather message is fed to Kafka service.");
                System.out.println(enrichedMessage);
            }
            else logger.info("Weather message is dropped.");
            updateState();
            Thread.sleep(COOLDOWN);
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