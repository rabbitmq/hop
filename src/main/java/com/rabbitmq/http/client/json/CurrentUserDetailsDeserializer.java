package com.rabbitmq.http.client.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.rabbitmq.http.client.domain.CurrentUserDetails;
import com.rabbitmq.http.client.domain.UserInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class CurrentUserDetailsDeserializer extends StdDeserializer<CurrentUserDetails> {
    public static final JsonDeserializer<CurrentUserDetails> INSTANCE = new CurrentUserDetailsDeserializer();
    private static final long serialVersionUID = -1871403623406830843L;

    public static final String USERNAME_FIELD = "name";
    public static final String TAGS_FIELD = "tags";
    public static final String AUTH_BACKEND_FIELD = "auth_backend";

    private CurrentUserDetailsDeserializer() {
        super(UserInfo.class);
    }

    @Override
    public CurrentUserDetails deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        return new CurrentUserDetails(getUsername(node), getTags(node), getAuthBackend(node));
    }

    private String getUsername(JsonNode node) {
        return node.get(USERNAME_FIELD).asText();
    }

    private String getAuthBackend(JsonNode node) {
        return node.get(AUTH_BACKEND_FIELD).asText();
    }

    private List<String> getTags(JsonNode node) {
        JsonNode tags = node.get(TAGS_FIELD);

        if (tags.isArray()) {
            Iterator<JsonNode> values = tags.elements();
            List<String> result = new ArrayList<>();
            for (Iterator<JsonNode> it = values; it.hasNext(); ) {
                JsonNode el = it.next();
                result.add(el.asText());
            }

            return result;
        }

        String commaSeparatedList = tags.asText();
        return Arrays.asList(commaSeparatedList.split(","));
    }
}
