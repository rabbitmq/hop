package com.rabbitmq.http.client.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AckMode {
    /**
     * Messages are acknowledged to the upstream broker after they have been confirmed downstream. This handles network
     * errors and broker failures without losing messages, and is the slowest option.
     */
    ON_CONFIRM("on-confirm"),

    /**
     * Messages are acknowledged to the upstream broker after they have been published downstream. This handles network
     * errors without losing messages, but may lose messages in the event of broker failures.
     */
    ON_PUBLISH("on-publish"),

    /**
     * Message acknowledgements are not used. This is the fastest option, but may lose messages in the event of network
     * or broker failures.
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
