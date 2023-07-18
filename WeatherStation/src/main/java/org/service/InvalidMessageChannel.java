/*
 * Created by Amr Momtaz.
 */

package org.service;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the invalid messages headers and formats from the API.
 */
public final class InvalidMessageChannel {
    private static final List<JSONObject> invalidMessages;

    static {
        invalidMessages = new ArrayList<>();
    }

    /**
     * Adds invalid message to the list.
     */
    public static void addInvalidMessage(JSONObject invalidMessage) {
        invalidMessages.add(invalidMessage);
    }

    /**
     * Prints the invalid messages in the console.
     */
    public static void showInvalidMessages() {
        invalidMessages.forEach(message -> System.out.println(message.toString()));
    }
}
