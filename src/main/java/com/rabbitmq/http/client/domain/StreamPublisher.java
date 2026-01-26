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

/**
 * A stream publisher.
 *
 * @since 5.5.0
 * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamPublisher {

    @JsonProperty("connection_details")
    private ConnectionDetails connectionDetails;

    private QueueDetails queue;

    private String reference;

    @JsonProperty("publisher_id")
    private int publisherId;

    private long published;

    private long confirmed;

    private long errored;

    public ConnectionDetails getConnectionDetails() {
        return connectionDetails;
    }

    public void setConnectionDetails(ConnectionDetails connectionDetails) {
        this.connectionDetails = connectionDetails;
    }

    public QueueDetails getQueue() {
        return queue;
    }

    public void setQueue(QueueDetails queue) {
        this.queue = queue;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public int getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(int publisherId) {
        this.publisherId = publisherId;
    }

    public long getPublished() {
        return published;
    }

    public void setPublished(long published) {
        this.published = published;
    }

    public long getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(long confirmed) {
        this.confirmed = confirmed;
    }

    public long getErrored() {
        return errored;
    }

    public void setErrored(long errored) {
        this.errored = errored;
    }

    @Override
    public String toString() {
        return "StreamPublisher{" +
                "publisherId=" + publisherId +
                ", reference='" + reference + '\'' +
                ", queue=" + queue +
                '}';
    }

    /**
     * Connection details for stream publishers/consumers.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConnectionDetails {

        private String name;

        @JsonProperty("peer_host")
        private String peerHost;

        @JsonProperty("peer_port")
        private int peerPort;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPeerHost() {
            return peerHost;
        }

        public void setPeerHost(String peerHost) {
            this.peerHost = peerHost;
        }

        public int getPeerPort() {
            return peerPort;
        }

        public void setPeerPort(int peerPort) {
            this.peerPort = peerPort;
        }

        @Override
        public String toString() {
            return "ConnectionDetails{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }

    /**
     * Queue/stream details for stream publishers/consumers.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QueueDetails {

        private String name;

        private String vhost;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVhost() {
            return vhost;
        }

        public void setVhost(String vhost) {
            this.vhost = vhost;
        }

        @Override
        public String toString() {
            return "QueueDetails{" +
                    "name='" + name + '\'' +
                    ", vhost='" + vhost + '\'' +
                    '}';
        }
    }
}
