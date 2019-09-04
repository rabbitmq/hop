package com.rabbitmq.http.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * POJO for messages consumed from the HTTP API.
 *
 * @since 3.4.0
 */
public class InboundMessage {

    @JsonProperty("payload_bytes")
    private int payloadBytes;

    private boolean redelivered;

    @JsonProperty("routing_key")
    private String routingKey;

    @JsonProperty("message_count")
    private int messageCount;

    private Map<String, Object> properties;

    private String payload;

    @JsonProperty("payload_encoding")
    private String payloadEncoding;

    public int getPayloadBytes() {
        return payloadBytes;
    }

    public void setPayloadBytes(int payloadBytes) {
        this.payloadBytes = payloadBytes;
    }

    public boolean isRedelivered() {
        return redelivered;
    }

    public void setRedelivered(boolean redelivered) {
        this.redelivered = redelivered;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getPayloadEncoding() {
        return payloadEncoding;
    }

    public void setPayloadEncoding(String payloadEncoding) {
        this.payloadEncoding = payloadEncoding;
    }

    @Override
    public String toString() {
        return "InboundMessage{" +
                "payloadBytes=" + payloadBytes +
                ", redelivered=" + redelivered +
                ", routingKey='" + routingKey + '\'' +
                ", messageCount=" + messageCount +
                ", properties=" + properties +
                ", payload='" + payload + '\'' +
                ", payloadEncoding='" + payloadEncoding + '\'' +
                '}';
    }
}
