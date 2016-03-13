package com.dabsquared.gitlabjenkins.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author Robin Müller
 */
public final class GsonUtil {

    private static final Gson prettyPrint = new GsonBuilder().setPrettyPrinting().create();

    private GsonUtil() { }

    public static String toPrettyPrint(String json) {
        JsonParser parser = new JsonParser();
        JsonObject object = parser.parse(json).getAsJsonObject();
        return prettyPrint.toJson(object);
    }
}
