/*
 * Copyright 2019 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpstreamDetails {

    /**
     * The AMQP URI for the upstream. Mandatory.<br/>
     * E.g. amqp://guest:guest@localhost:5672/vhost<br/>
     * E.g. amqp://guest:guest@localhost:5672 (nb! no slash at the end at in case of "/" virtual host)<br/>
     * E.g. amqp://guest:guest@localhost:5671?cacertfile=/certs/cacert.pem
     * (<a href="https://www.rabbitmq.com/uri-query-parameters.html">query-parameter reference</a>)<br/>
     * Applying to federated exchanges and federated queues
     */
    private String uri;

    /**
     * The maximum number of unacknowledged messages copied over a link at any one time. Default is 1000.<br/>
     * Applying to federated exchanges and federated queues
     */
    @JsonProperty("prefetch-count")
    private Integer prefetchCount;

    /**
     * The duration (in seconds) to wait before reconnecting to the broker after being disconnected. Default is 1.<br/>
     * Applying to federated exchanges and federated queues
     */
    @JsonProperty("reconnect-delay")
    private Integer reconnectDelay;

    /**
     * <p>Determines how the link should acknowledge messages. If set to <b>on-confirm</b> (the default), messages are
     * acknowledged to the upstream broker after they have been confirmed downstream. This handles network errors and
     * broker failures without losing messages, and is the slowest option.</p>
     * <p>If set to <b>on-publish</b>, messages are acknowledged to the upstream broker after they have been published
     * downstream. This handles network errors without losing messages, but may lose messages in the event of broker
     * failures.</p>
     * <p>If set to <b>no-ack</b>, message acknowledgements are not used. This is the fastest option, but may lose
     * messages in the event of network or broker failures.</p>
     * Applying to federated exchanges and federated queues
     */
    @JsonProperty("ack-mode")
    private AckMode ackMode;

    /**
     * Determines how federation should interact with the <a href="https://www.rabbitmq.com/validated-user-id.html">
     * validated user-id</a> feature. If set to true, federation will pass through any validated user-id from the
     * upstream, even though it cannot validate it itself. If set to false or not set, it will clear any validated
     * user-id it encounters. You should only set this to true if you trust the upstream server (and by extension, all
     * its upstreams) not to forge user-ids.<br/>
     * Applying to federated exchanges and federated queues
     * @see <a href="https://www.rabbitmq.com/validated-user-id.html">validated user-id reference</a>
     */
    @JsonProperty("trust-user-id")
    private Boolean trustUserId;

    /**
     * The name of the upstream exchange. Default is to use the same name as the federated exchange.
     */
    private String exchange;

    /**
     * The maximum number of federation links that a message published to a federated exchange can traverse before it is
     * discarded. Default is 1. Note that even if max-hops is set to a value greater than 1, messages will never visit
     * the same node twice due to travelling in a loop. However, messages may still be duplicated if it is possible for
     * them to travel from the source to the destination via multiple routes.
     * Applying to federated exchanges only
     */
    @JsonProperty("max-hops")
    private Integer maxHops;

    /**
     * <p>The expiry time (in milliseconds) after which an upstream queue for a federated exchange may be deleted, if a
     * connection to the upstream broker is lost. The default is 'none', meaning the queue should never expire.</p>
     * <p>This setting controls how long the upstream queue will last before it is eligible for deletion if the
     * connection is lost.</p>
     * <p>This value is used to set the <b>"x-expires"</b> argument for the upstream queue.</p>
     * Applying to federated exchanges only
     */
    @JsonProperty("expires")
    private Long expiresMillis;

    /**
     * <p>The expiry time for messages in the upstream queue for a federated exchange (see {@link
     * UpstreamDetails#expiresMillis}), in milliseconds. Default is 'none', meaning messages should never expire.</p>
     * <p>This value is used to set the <b>"x-message-ttl"</b> argument for the upstream queue.</p>
     * Applying to federated exchanges only
     */
    @JsonProperty("message-ttl")
    private Long messageTtl;

    /**
     * The name of the upstream queue. Default is to use the same name as the federated queue.<br/>
     * Applying to federated queues only
     */
    private String queue;

    public String getUri() {
        return uri;
    }

    public UpstreamDetails setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public Integer getPrefetchCount() {
        return prefetchCount;
    }

    public UpstreamDetails setPrefetchCount(Integer prefetchCount) {
        this.prefetchCount = prefetchCount;
        return this;
    }

    public Integer getReconnectDelay() {
        return reconnectDelay;
    }

    public UpstreamDetails setReconnectDelay(Integer reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
        return this;
    }

    public AckMode getAckMode() {
        return ackMode;
    }

    public UpstreamDetails setAckMode(AckMode ackMode) {
        this.ackMode = ackMode;
        return this;
    }

    public Boolean getTrustUserId() {
        return trustUserId;
    }

    public UpstreamDetails setTrustUserId(Boolean trustUserId) {
        this.trustUserId = trustUserId;
        return this;
    }

    public String getExchange() {
        return exchange;
    }

    public UpstreamDetails setExchange(String exchange) {
        this.exchange = exchange;
        return this;
    }

    public Integer getMaxHops() {
        return maxHops;
    }

    public UpstreamDetails setMaxHops(Integer maxHops) {
        this.maxHops = maxHops;
        return this;
    }

    public Long getExpiresMillis() {
        return expiresMillis;
    }

    public UpstreamDetails setExpiresMillis(Long expiresMillis) {
        this.expiresMillis = expiresMillis;
        return this;
    }

    public Long getMessageTtl() {
        return messageTtl;
    }

    public UpstreamDetails setMessageTtl(Long messageTtl) {
        this.messageTtl = messageTtl;
        return this;
    }

    public String getQueue() {
        return queue;
    }

    public UpstreamDetails setQueue(String queue) {
        this.queue = queue;
        return this;
    }

    @Override
    public String toString() {
        return "UpstreamDetails{" +
                "uri='" + uri + '\'' +
                ", prefetchCount=" + prefetchCount +
                ", reconnectDelay=" + reconnectDelay +
                ", ackMode=" + ackMode +
                ", trustUserId=" + trustUserId +
                ", exchange='" + exchange + '\'' +
                ", maxHops=" + maxHops +
                ", expiresMillis=" + expiresMillis +
                ", messageTtl=" + messageTtl +
                ", queue='" + queue + '\'' +
                '}';
    }
}
