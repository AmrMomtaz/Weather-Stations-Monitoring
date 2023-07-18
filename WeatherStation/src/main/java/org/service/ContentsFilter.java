/*
 * Created by Amr Momtaz.
 */

package org.service;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Filters unused fields and renames fields to match the target schema.
 * Removes unnecessary fields and extracts the current humidity from the API response.
 */
public final class ContentsFilter {

    public static JSONObject filterMessage(JSONObject apiMessage) {
        try {
            long currentTimestamp = apiMessage.getJSONObject("current_weather").getLong("time");
            JSONObject weatherMessage = new JSONObject();
            weatherMessage.put("humidity", getCurrentHumidity(apiMessage, currentTimestamp))
                    .put("temperature", (int) apiMessage.getJSONObject("current_weather").getDouble("temperature"))
                    .put("wind_speed", (int) apiMessage.getJSONObject("current_weather").getDouble("windspeed"));
            return new JSONObject().put("weather", weatherMessage);
        }
        catch (Exception e) {
            InvalidMessageChannel.addInvalidMessage(apiMessage);
            return null;
        }
    }

    /**
     * Returns the current humidity from the humidity array.
     */
    private static int getCurrentHumidity(JSONObject apiMessage, long currentTimeStamp) {
        JSONArray timeArray = apiMessage.getJSONObject("hourly").getJSONArray("time");
        JSONArray humidityArray = apiMessage.getJSONObject("hourly").getJSONArray("relativehumidity_2m");
        int idx = -1;
        for (int i = 0 ; i < 24 ; i++) {
            if (timeArray.getLong(i) == currentTimeStamp) {
                idx = i;
                break;
            }
        }
        return humidityArray.getInt(idx);
    }
}
