package com.dabsquared.gitlabjenkins.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * @author Robin Müller
 */
public final class GsonUtil {

    private static final Gson prettyPrint = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Date.class, new DateSerializer())
            .create();

    private GsonUtil() { }

    public static Gson getGson() {
        return gson;
    }

    public static String toPrettyPrint(String json) {
        JsonParser parser = new JsonParser();
        return prettyPrint.toJson(parser.parse(json));
    }

    private static final String[] DATE_FORMATS = new String[] {
            "yyyy-MM-dd HH:mm:ss Z", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ssX" };

    private static class DateSerializer implements JsonDeserializer<Date> {
        public Date deserialize(JsonElement jsonElement, Type typeOF,
                                JsonDeserializationContext context) throws JsonParseException {
            for (String format : DATE_FORMATS) {
                try {
                    return new SimpleDateFormat(format, Locale.US)
                            .parse(jsonElement.getAsString());
                } catch (ParseException e) {
                    // nothing to do
                }
            }
            throw new JsonParseException("Unparseable date: \""
                    + jsonElement.getAsString() + "\". Supported formats: "
                    + Arrays.toString(DATE_FORMATS));
        }
    }
}
