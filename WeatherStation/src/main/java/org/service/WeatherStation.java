/*
 * Created by Amr Momtaz.
 */

package org.service;

import java.util.Random;
import org.json.JSONObject;

/**
 * Driver code for the weather station.
 * args[0] -> Station ID
 */
public final class WeatherStation {

    // Constants
    private static final Integer COOLDOWN = 1000; // Weather station cool-down in ms

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
            JSONObject enrichedMessage = ContentsEnricher.enrichMessage(filteredMessage);
            System.out.println(enrichedMessage);
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
        final Random random = new Random();
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