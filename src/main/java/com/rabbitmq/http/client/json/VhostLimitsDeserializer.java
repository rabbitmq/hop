package com.rabbitmq.http.client.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.rabbitmq.http.client.domain.VhostLimits;

import java.io.IOException;

public class VhostLimitsDeserializer extends StdDeserializer<VhostLimits> {
        public static final JsonDeserializer<VhostLimits> INSTANCE = new VhostLimitsDeserializer();
        private static final long serialVersionUID = -1881403692606830843L;

        public static final String VHOST_FIELD = "vhost";
        public static final String VALUE_FIELD = "value";
        public static final String MAX_QUEUES_FIELD = "max-queues";
        public static final String MAX_CONNECTIONS_FIELD = "max-connections";

        private VhostLimitsDeserializer() {
            super(VhostLimits.class);
        }

        @Override
        public VhostLimits deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);
            if (node.isArray()) {
                if (node.isEmpty()) {
                    return new VhostLimits(null, -1, -1);
                }
                node = node.get(0);
            }
            JsonNode value = node.get(VALUE_FIELD);
            return new VhostLimits(getVhost(node), getLimit(value, MAX_QUEUES_FIELD), getLimit(value, MAX_CONNECTIONS_FIELD));
        }

        private String getVhost(JsonNode node) {
            return node.get(VHOST_FIELD).asText();
        }

        private int getLimit(JsonNode value, String name) {
            JsonNode limit = value.get(name);
            if (limit == null) {
                return -1;
            } else {
                return limit.asInt(-1);
            }
        }

    }
