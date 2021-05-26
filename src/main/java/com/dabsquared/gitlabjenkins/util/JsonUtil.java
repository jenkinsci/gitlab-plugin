package com.dabsquared.gitlabjenkins.util;

import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.StaplerRequest;

import com.dabsquared.gitlabjenkins.webhook.ActionResolver;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import hudson.util.HttpResponses;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
public final class JsonUtil {

    private static final int          BUFFER_SIZE   = 100000;
    private static final Logger       LOGGER        = Logger.getLogger (ActionResolver.class.getName ());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .registerModule(new DateModule());

    private JsonUtil() { }

    public static String toPrettyPrint(String json) {
        try {
            return toPrettyPrint(OBJECT_MAPPER.readValue(json, Object.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toPrettyPrint(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode readTree(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
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

    public static <T> T read(JsonNode json, Class<T> type) {
        try {
            return OBJECT_MAPPER.treeToValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readFromStaplerRequest (StaplerRequest request,
                                                Class<T> type) {
        return read (getRequestBody (request), type);
    }

    public static String getRequestBody (StaplerRequest request) {
        String requestBody;
        try {
            if (request.getCharacterEncoding () == null) {
                request.setCharacterEncoding (StandardCharsets.UTF_8.name ());
            }
            // the request's body may be read several times throughout the different paths.
            // preventing multiple reads would require some major refactoring so we decided
            // on the following approach since a regular read of the stream/reader would cause
            // the request body to be empty on subsequent read operations.
            BufferedReader br = request.getReader ();
            br.mark (BUFFER_SIZE);
            requestBody = IOUtils.toString (br);
            br.reset ();
        } catch (IOException e) {
            throw HttpResponses.error (500, "Failed to read request body");
        }
        return requestBody;
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
