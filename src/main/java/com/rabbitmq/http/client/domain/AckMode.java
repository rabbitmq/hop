package com.rabbitmq.http.client.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AckMode {
    /**
     * Consumes in manual acknowledgement mode.
     * Messages are acknowledged to the upstream broker after they have been confirmed downstream.
     *
     * This is the safest option that offers lowest throughput.
     * @see https://www.rabbitmq.com/confirms.html
     */
    ON_CONFIRM("on-confirm"),

    /**
     * Consumes in manual acknowledgement mode.
     * Messages are acknowledged to the upstream broker after they have been published downstream
     * without waiting for publisher confirms. In other words, this uses fire-and-forget publishing.
     *
     * This is a moderately safe safe option that does not handle downstream node failures.
     * @see https://www.rabbitmq.com/confirms.html
     */
    ON_PUBLISH("on-publish"),

    /**
     * Consumes in automatic acknowledgement mode.
     *
     * Unsafe, offers best throughput.
     * @see https://www.rabbitmq.com/confirms.html
     */
    NO_ACK("no-ack");

    @JsonCreator
    AckMode(String value) {
        this.value = value;
    }

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }
}
