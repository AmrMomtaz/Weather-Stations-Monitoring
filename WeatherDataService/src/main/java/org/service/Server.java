/*
 * Created by Amr Momtaz.
 */

package org.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.lognet.springboot.grpc.GRpcService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@GRpcService
public final class Server extends org.service.WeatherDataServiceGrpc.WeatherDataServiceImplBase {

    private static final Logger logger = LogManager.getLogger(Server.class);
    private static final String API_URL = "https://api.open-meteo.com/v1/forecast?latitude=31.2018&longitude=29.9158" +
            "&hourly=relativehumidity_2m&current_weather=true&timezone=Africa%2FCairo&forecast_days=1&timeformat=unixtime&temperature_unit=fahrenheit";
    private static final Integer TRIALS = 5;
    @Override
    public void getWeatherData(org.service.WeatherDataOuterClass.WeatherDataRequest request,
                               io.grpc.stub.StreamObserver<org.service.WeatherDataOuterClass.WeatherData> responseObserver) {

        logger.info("Received weather data request from client {" + request.getClientId() + "}");
        JSONObject jsonResponse = fetchData();

        if (jsonResponse == null) logger.error("Internet connection failure." +
                "Couldn't fetch data for the request sent by client {" + request.getClientId() + "}.");
        else {
            WeatherDataOuterClass.WeatherData weatherData = parseJsonData(jsonResponse);
            responseObserver.onNext(weatherData);
            responseObserver.onCompleted();
            logger.info("Weather data is sent sent successfully to client {" + request.getClientId() + "}");
        }
    }

    /**
     * Returns JSONOject containing the weather data fetched from open-meteo API.
     */
    private JSONObject fetchData() {
        for (int i = 0 ; i < TRIALS ; i++) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
                connection.setRequestMethod("GET");
                JSONObject jsonResponse;
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String response = in.lines().reduce("", (accumulator, actual) -> accumulator + actual);
                    jsonResponse = new JSONObject(response);
                }
                connection.disconnect();
                return jsonResponse;
            }
            catch (IOException ignored) {}
        }
        return null;
    }

    /**
     * Parses the given Json response and returns a weather data object.
     */
    private WeatherDataOuterClass.WeatherData parseJsonData(JSONObject jsonResponse) {
        WeatherDataOuterClass.WeatherData.Hourly.Builder hourlyBuilder
                = WeatherDataOuterClass.WeatherData.Hourly.newBuilder();
        jsonResponse.getJSONObject("hourly").getJSONArray("time").forEach
                (time -> hourlyBuilder.addTime(Long.parseLong(time.toString())));
        jsonResponse.getJSONObject("hourly").getJSONArray("relativehumidity_2m").forEach
                (humidity -> hourlyBuilder.addRelativehumidity2M(Integer.parseInt(humidity.toString())));
        return WeatherDataOuterClass.WeatherData.newBuilder()
                .setLatitude(jsonResponse.getDouble("latitude"))
                .setLongitude(jsonResponse.getDouble("longitude"))
                .setGenerationtimeMs(jsonResponse.getDouble("generationtime_ms"))
                .setUtcOffsetSeconds(jsonResponse.getInt("utc_offset_seconds"))
                .setTimezone(jsonResponse.getString("timezone"))
                .setTimezoneAbbreviation(jsonResponse.getString("timezone_abbreviation"))
                .setElevation(jsonResponse.getDouble("elevation"))
                .setCurrentWeather(WeatherDataOuterClass.WeatherData.Current_weather.newBuilder()
                        .setTemperature(jsonResponse.getJSONObject("current_weather").getDouble("temperature"))
                        .setWindspeed(jsonResponse.getJSONObject("current_weather").getDouble("windspeed"))
                        .setWinddirection(jsonResponse.getJSONObject("current_weather").getDouble("winddirection"))
                        .setWeathercode(jsonResponse.getJSONObject("current_weather").getInt("weathercode"))
                        .setIsDay(jsonResponse.getJSONObject("current_weather").getInt("is_day"))
                        .setTime(jsonResponse.getJSONObject("current_weather").getLong("time"))
                        .build())
                .setHourlyUnits(WeatherDataOuterClass.WeatherData.Hourly_units.newBuilder()
                        .setTime(jsonResponse.getJSONObject("hourly_units").getString("time"))
                        .setRelativehumidity2M(jsonResponse.getJSONObject("hourly_units").getString("relativehumidity_2m"))
                        .build())
                .setHourly(hourlyBuilder.build())
                .build();
    }
}
