/*
 * Created by Amr Momtaz.
 */

package org.service;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

/**
 * Connects to the weather data service, fetches WeatherData object, parses it and returns it in JSONObject.
 * It waits for the GRPC server to start gracefully using the defined number of tries and cooldown duration defined below.
 */
public final class WeatherDataServiceConnector {

    private static final int TRIALS = 5;
    private static final int COOLDOWN = 10; // cooldown in seconds
    private static final Logger logger = LogManager.getLogger(WeatherDataServiceConnector.class);

    public static JSONObject getWeatherData() throws InterruptedException {
        WeatherDataOuterClass.WeatherData weatherData = null;
        for (int t = 0 ; t <= TRIALS ; t++) {
            try {
                weatherData = getWeatherDataObject();
            }
            catch (StatusRuntimeException exception) {
                if (t == TRIALS) {
                    String errMsg = "GRPC server couldn't start";
                    logger.error(errMsg);
                    throw new RuntimeException(errMsg, exception);
                }
                else {
                    logger.info("Weather station will sleep for (" + COOLDOWN + ") seconds waiting " +
                                "for the GRPC to start. [TRAIL=" + (t+1) + "]");
                    Thread.sleep(COOLDOWN * 1000);
                }
            }
        }
        assert weatherData != null;
        return parseWeatherData(weatherData);
    }

    //
    // Private Methods
    //

    /**
     * Makes rpc call to weather data service and returns its response.
     */
    private static WeatherDataOuterClass.WeatherData getWeatherDataObject() {
        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", 6565).usePlaintext().build();
        org.service.WeatherDataServiceGrpc.WeatherDataServiceBlockingStub blockingStub
                = org.service.WeatherDataServiceGrpc.newBlockingStub(managedChannel);
        return blockingStub.getWeatherData(org.service.WeatherDataOuterClass.WeatherDataRequest
                .newBuilder().setClientId(WeatherStation.stationID).build());
    }

    /**
     * Parses the given weather data object and returns a JSONObject.
     */
    private static JSONObject parseWeatherData(WeatherDataOuterClass.WeatherData weatherData) {
        JSONObject currentWeather = new JSONObject();
        currentWeather.put("weathercode", weatherData.getCurrentWeather().getWeathercode())
                .put("temperature", weatherData.getCurrentWeather().getTemperature())
                .put("windspeed", weatherData.getCurrentWeather().getWindspeed())
                .put("is_day", weatherData.getCurrentWeather().getIsDay())
                .put("time", weatherData.getCurrentWeather().getTime())
                .put("winddirection", weatherData.getCurrentWeather().getWinddirection());
        JSONObject hourly = new JSONObject();
        hourly.put("relativehumidity_2m", weatherData.getHourly().getRelativehumidity2MList())
                .put("time", weatherData.getHourly().getTimeList());
        JSONObject hourlyUnits = new JSONObject();
        hourlyUnits.put("relativehumidity_2m", weatherData.getHourlyUnits().getRelativehumidity2M())
                        .put("time", weatherData.getHourlyUnits().getTime());
        JSONObject parsedWeatherData = new JSONObject();
        parsedWeatherData.put("elevation", weatherData.getElevation())
                .put("generationtime_ms", weatherData.getGenerationtimeMs())
                .put("timezone_abbreviation", weatherData.getTimezoneAbbreviation())
                .put("timezone", weatherData.getTimezone())
                .put("latitude", weatherData.getLatitude())
                .put("utc_offset_seconds", weatherData.getUtcOffsetSeconds())
                .put("longitude", weatherData.getLongitude())
                .put("current_weather", currentWeather)
                .put("hourly", hourly)
                .put("hourly_units", hourlyUnits);
        return parsedWeatherData;
    }
}
