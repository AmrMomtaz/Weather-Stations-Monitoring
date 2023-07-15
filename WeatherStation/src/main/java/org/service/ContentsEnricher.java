/*
 * Created by Amr Momtaz.
 */

package org.service;

import org.json.JSONObject;

/**
 * Enriches the message with the missing fields.
 */
public final class ContentsEnricher {

    public static JSONObject enrichMessage(JSONObject filteredMessage) {
        return filteredMessage.put("station_id", WeatherStation.stationID)
                .put("s_no", WeatherStation.sequenceNumber)
                .put("battery_status", WeatherStation.batteryStatus);
    }
}
