package com.dabsquared.gitlabjenkins.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * @author Robin Müller
 */
public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .addModule(new DateModule())
            .build();

    private JsonUtil() {}

    public static String toPrettyPrint(String json) {
        try {
            return toPrettyPrint(OBJECT_MAPPER.readValue(json, Object.class));
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toPrettyPrint(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode readTree(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T read(String json, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T read(JsonNode json, Class<T> type) {
        try {
            return OBJECT_MAPPER.treeToValue(json, type);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    private static class DateModule extends SimpleModule {
        private static final String[] DATE_FORMATS = new String[] {
            "yyyy-MM-dd HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        };

        private DateModule() {
            addDeserializer(Date.class, new ValueDeserializer<Date>() {
                @Override
                public Date deserialize(tools.jackson.core.JsonParser p, DeserializationContext ctxt) {
                    for (String format : DATE_FORMATS) {
                        try {
                            return new SimpleDateFormat(format, Locale.US).parse(p.getValueAsString());
                        } catch (ParseException e) {
                            // nothing to do
                        }
                    }
                    throw new UncheckedIOException(new IOException("Unparseable date: \""
                            + p.getValueAsString() + "\". Supported formats: "
                            + Arrays.toString(DATE_FORMATS)));
                }
            });
        }
    }
}
