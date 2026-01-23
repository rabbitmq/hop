/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rabbitmq.http.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Information about a stream protocol consumer.
 *
 * @since 5.5.0
 * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamConsumer {

    @JsonProperty("connection_details")
    private StreamPublisher.ConnectionDetails connectionDetails;

    private StreamPublisher.QueueDetails queue;

    @JsonProperty("subscription_id")
    private int subscriptionId;

    private long credits;

    private long consumed;

    @JsonProperty("offset_lag")
    private long offsetLag;

    private long offset;

    private Map<String, Object> properties;

    public StreamPublisher.ConnectionDetails getConnectionDetails() {
        return connectionDetails;
    }

    public void setConnectionDetails(StreamPublisher.ConnectionDetails connectionDetails) {
        this.connectionDetails = connectionDetails;
    }

    public StreamPublisher.QueueDetails getQueue() {
        return queue;
    }

    public void setQueue(StreamPublisher.QueueDetails queue) {
        this.queue = queue;
    }

    public int getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(int subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public long getCredits() {
        return credits;
    }

    public void setCredits(long credits) {
        this.credits = credits;
    }

    public long getConsumed() {
        return consumed;
    }

    public void setConsumed(long consumed) {
        this.consumed = consumed;
    }

    public long getOffsetLag() {
        return offsetLag;
    }

    public void setOffsetLag(long offsetLag) {
        this.offsetLag = offsetLag;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "StreamConsumer{" +
                "subscriptionId=" + subscriptionId +
                ", queue=" + queue +
                ", offset=" + offset +
                '}';
    }
}
