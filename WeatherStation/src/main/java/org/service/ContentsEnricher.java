/*
 * Created by Amr Momtaz.
 */

package org.service;

import org.json.JSONObject;

/**
 * Enriches the message with the missing fields.
 * Adds the current weather station state in the message.
 */
public final class ContentsEnricher {

    public static JSONObject enrichMessage(JSONObject filteredMessage) {
        try {
            return filteredMessage.put("station_id", WeatherStation.stationID)
                    .put("s_no", WeatherStation.sequenceNumber)
                    .put("battery_status", WeatherStation.batteryStatus)
                    .put("status_timestamp", System.currentTimeMillis()/1000);
        }
        catch (Exception e) {
            InvalidMessageChannel.addInvalidMessage(filteredMessage);
            return null;
        }
    }
}
