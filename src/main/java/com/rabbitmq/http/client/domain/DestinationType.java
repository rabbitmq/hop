package com.rabbitmq.http.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DestinationType {
    @JsonProperty("queue")
    QUEUE,

    @JsonProperty("exchange")
    EXCHANGE
}
