package com.dabsquared.gitlabjenkins.util;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * @author Robin Müller
 */
public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .registerModule(new DateModule());

    private JsonUtil() { }

    public static String toPrettyPrint(String json) {
        try {
            return OBJECT_MAPPER.writeValueAsString(OBJECT_MAPPER.readValue(json, Object.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T read(String json, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class DateModule extends SimpleModule {
        private static final String[] DATE_FORMATS = new String[] {
                "yyyy-MM-dd HH:mm:ss Z", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        };

        private DateModule() {
            addDeserializer(Date.class, new com.fasterxml.jackson.databind.JsonDeserializer<Date>() {
                @Override
                public Date deserialize(com.fasterxml.jackson.core.JsonParser p, DeserializationContext ctxt) throws IOException {
                    for (String format : DATE_FORMATS) {
                        try {
                            return new SimpleDateFormat(format, Locale.US)
                                    .parse(p.getValueAsString());
                        } catch (ParseException e) {
                            // nothing to do
                        }
                    }
                    throw new IOException("Unparseable date: \""
                            + p.getValueAsString() + "\". Supported formats: "
                            + Arrays.toString(DATE_FORMATS));
                }
            });
        }
    }
}
