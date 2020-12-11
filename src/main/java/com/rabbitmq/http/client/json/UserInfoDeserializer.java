package com.rabbitmq.http.client.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.rabbitmq.http.client.domain.UserInfo;
import com.rabbitmq.http.client.domain.VhostLimits;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class UserInfoDeserializer extends StdDeserializer<UserInfo> {
    public static final JsonDeserializer<UserInfo> INSTANCE = new UserInfoDeserializer();
    private static final long serialVersionUID = -1871403623406830843L;

    public static final String USERNAME_FIELD = "name";
    public static final String PASSWORD_HASH_FIELD = "password_hash";
    public static final String HASHING_ALGORITHM_FIELD = "hashing_algorithm";
    public static final String TAGS_FIELD = "tags";

    private UserInfoDeserializer() {
        super(UserInfo.class);
    }

    @Override
    public UserInfo deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        return new UserInfo(getUsername(node), getPasswordHash(node), getHashingAlgorithm(node), getTags(node));
    }

    private String getUsername(JsonNode node) {
        return node.get(USERNAME_FIELD).asText();
    }

    private String getPasswordHash(JsonNode node) {
        return node.get(PASSWORD_HASH_FIELD).asText();
    }

    private String getHashingAlgorithm(JsonNode node) {
        return node.get(HASHING_ALGORITHM_FIELD).asText();
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
