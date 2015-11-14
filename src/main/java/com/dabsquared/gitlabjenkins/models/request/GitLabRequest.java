package com.dabsquared.gitlabjenkins.models.request;

import com.dabsquared.gitlabjenkins.GitLab;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabCommitStatus;
import org.gitlab.api.models.GitlabProject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public abstract class GitLabRequest {
    private static final String[] DATE_FORMATS = new String[]{
        "yyyy-MM-dd HH:mm:ss Z", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    };

    public abstract GitlabCommitStatus createCommitStatus(GitlabAPI api, String status, String targetUrl);

    public abstract GitlabProject getSourceProject(GitLab api) throws IOException;

    protected enum Builder {
        INSTANCE;
        private final Gson gson;

        Builder() {
            gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new GitLabRequest.DateSerializer())
                .create();
        }

        public Gson get() {
            return gson;
        }
    }

    private static class DateSerializer implements JsonDeserializer<Date> {
        public Date deserialize(JsonElement jsonElement, Type typeOF,
                                JsonDeserializationContext context) throws JsonParseException {
            for (String format : DATE_FORMATS) {
                try {
                    return new SimpleDateFormat(format, Locale.US)
                        .parse(jsonElement.getAsString());
                } catch (ParseException e) {
                }
            }
            throw new JsonParseException("Unparseable date: \""
                + jsonElement.getAsString() + "\". Supported formats: "
                + Arrays.toString(DATE_FORMATS));
        }
    }
}
