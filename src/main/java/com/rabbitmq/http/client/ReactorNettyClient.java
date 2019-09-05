/*
 * Copyright 2018-2019 the original author or authors.
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

package com.rabbitmq.http.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.http.client.domain.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.reactivestreams.Publisher;
import org.springframework.util.StringUtils;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reactive client based on Reactor Netty.
 * Use the {@link ReactorNettyClientOptions} constructors for
 * advanced settings, e.g. TLS, authentication other than HTTP basic, etc.
 * The default settings for this class are the following:
 * <ul>
 * <li>{@link HttpClient}: created with the {@link HttpClient#baseUrl(String)}.
 * </li>
 * <li>
 * {@link ObjectMapper}: <code>DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES</code> and
 * <code>MapperFeature.DEFAULT_VIEW_INCLUSION</code> are disabled.
 * </li>
 * <li><code>Mono&lt;String&gt; token</code>: basic HTTP authentication used for the
 * <code>authorization</code> header.
 * </li>
 * <li><code>BiConsumer&lt;? super HttpRequest, ? super HttpResponse&gt; responseCallback</code>:
 * 4xx and 5xx responses on GET requests throw {@link HttpClientException} and {@link HttpServerException}
 * respectively.
 * </li>
 * </ul>
 *
 * @see ReactorNettyClientOptions
 * @since 2.1.0
 */
public class ReactorNettyClient {

    private static final Consumer<HttpHeaders> JSON_HEADER = headers ->
        headers.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
    private final ObjectMapper objectMapper;
    private final HttpClient client;
    private final Mono<String> token;
    private final Supplier<ByteBuf> byteBufSupplier;
    private final Consumer<HttpClientResponse> responseCallback;

    public ReactorNettyClient(String url, ReactorNettyClientOptions options) {
        this(urlWithoutCredentials(url),
            URI.create(url).getUserInfo().split(":")[0],
            URI.create(url).getUserInfo().split(":")[1], options);
    }

    public ReactorNettyClient(String url) {
        this(url, new ReactorNettyClientOptions());
    }

    public ReactorNettyClient(String url, String username, String password) {
        this(url, username, password, new ReactorNettyClientOptions());
    }

    public ReactorNettyClient(String url, String username, String password, ReactorNettyClientOptions options) {
        objectMapper = options.objectMapper() == null ? createDefaultObjectMapper() : options.objectMapper().get();

        client = options.client() == null ?
            HttpClient.create().baseUrl(url) : options.client().get();

        this.token = options.token() == null ? createBasicAuthenticationToken(username, password) : options.token();

        if (options.onResponseCallback() == null) {
            this.responseCallback = response -> {
                if (response.method() == HttpMethod.GET) {
                    if (response.status().code() >= 500) {
                        throw new HttpServerException(response.status().code(), response.status().reasonPhrase());
                    } else if (response.status().code() >= 400) {
                        throw new HttpClientException(response.status().code(), response.status().reasonPhrase());
                    }
                }
            };
        } else {
            this.responseCallback = response ->
                options.onResponseCallback().accept(new HttpEndpoint(response.uri(), response.method().name()), toHttpResponse(response));
        }
        this.byteBufSupplier = options.byteBufSupplier() == null ?
                () -> PooledByteBufAllocator.DEFAULT.buffer() :
                options.byteBufSupplier();
    }

    private static String urlWithoutCredentials(String url) {
        URI url1 = URI.create(url);
        return url.replace(url1.getUserInfo() + "@", "");
    }

    private static HttpResponse toHttpResponse(HttpClientResponse response) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (Map.Entry<String, String> headerEntry : response.responseHeaders().entries()) {
            headers.put(headerEntry.getKey(), headerEntry.getValue());
        }
        return new HttpResponse(response.status().code(), response.status().reasonPhrase(), headers);
    }

    public static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        return objectMapper;
    }

    public static Mono<String> createBasicAuthenticationToken(String username, String password) {
        return Mono.fromSupplier(() -> basicAuthentication(username, password)).cache();
    }

    public static String basicAuthentication(String username, String password) {
        String credentials = username + ":" + password;
        byte[] credentialsAsBytes = credentials.getBytes(StandardCharsets.ISO_8859_1);
        byte[] encodedBytes = Base64.getEncoder().encode(credentialsAsBytes);
        String encodedCredentials = new String(encodedBytes, StandardCharsets.ISO_8859_1);
        return "Basic " + encodedCredentials;
    }

    public Mono<OverviewResponse> getOverview() {
        return doGetMono(OverviewResponse.class, "overview");
    }

    public Flux<NodeInfo> getNodes() {
        return doGetFlux(NodeInfo.class, "nodes");
    }

    public Mono<NodeInfo> getNode(String name) {
        return doGetMono(NodeInfo.class, "nodes", enc(name));
    }

    public Flux<ConnectionInfo> getConnections() {
        return doGetFlux(ConnectionInfo.class, "connections");
    }

    public Mono<ConnectionInfo> getConnection(String name) {
        return doGetMono(ConnectionInfo.class, "connections", enc(name));
    }

    public Mono<HttpResponse> closeConnection(String name) {
        return doDelete("connections", enc(name));
    }

    public Mono<HttpResponse> closeConnection(String name, String reason) {
        return doDelete(headers -> headers.set("X-Reason", reason), "connections", enc(name));
    }

    public Mono<HttpResponse> declarePolicy(String vhost, String name, PolicyInfo info) {
        return doPut(info, "policies", enc(vhost), enc(name));
    }

    public Flux<PolicyInfo> getPolicies() {
        return doGetFlux(PolicyInfo.class, "policies");
    }

    public Flux<PolicyInfo> getPolicies(String vhost) {
        return doGetFlux(PolicyInfo.class, "policies", enc(vhost));
    }

    public Mono<HttpResponse> deletePolicy(String vhost, String name) {
        return doDelete("policies", enc(vhost), enc(name));
    }

    public Flux<ChannelInfo> getChannels() {
        return doGetFlux(ChannelInfo.class, "channels");
    }

    public Flux<ChannelInfo> getChannels(String connectionName) {
        return doGetFlux(ChannelInfo.class, "connections", enc(connectionName), "channels");
    }

    public Mono<ChannelInfo> getChannel(String name) {
        return doGetMono(ChannelInfo.class, "channels", enc(name));
    }

    public Flux<VhostInfo> getVhosts() {
        return doGetFlux(VhostInfo.class, "vhosts");
    }

    public Mono<VhostInfo> getVhost(String name) {
        return doGetMono(VhostInfo.class, "vhosts", enc(name));
    }

    /**
     * Create a virtual host with name, tracing flag, and metadata.
     * Note metadata (description and tags) are supported as of RabbitMQ 3.8.
     *
     * @param name        name of the virtual host
     * @param tracing     whether tracing is enabled or not
     * @param description virtual host description (requires RabbitMQ 3.8 or more)
     * @param tags        virtual host tags (requires RabbitMQ 3.8 or more)
     * @return response wrapped in {@link Mono}
     * @since 3.4.0
     */
    public Mono<HttpResponse> createVhost(String name, boolean tracing, String description, String ... tags) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("tracing", tracing);

        if (description != null && !description.isEmpty()) {
            body.put("description", description);
        }

        if (tags != null && tags.length > 0) {
            body.put("tags", String.join(",", tags));
        }
        return doPut(body, "vhosts", enc(name));
    }

    /**
     * Create a virtual host with name and metadata.
     * Note metadata (description and tags) are supported as of RabbitMQ 3.8.
     *
     * @param name        name of the virtual host
     * @param description virtual host description (requires RabbitMQ 3.8 or more)
     * @param tags        virtual host tags (requires RabbitMQ 3.8 or more)
     * @return response wrapped in {@link Mono}
     * @since 3.4.0
     */
    public Mono<HttpResponse> createVhost(String name, String description, String ... tags) {
        return createVhost(name, false, description, tags);
    }

    /**
     * Create a virtual host with name and tracing flag.
     *
     * @param name    name of the virtual host
     * @param tracing whether tracing is enabled or not
     * @return response wrapped in {@link Mono}
     * @since 3.4.0
     */
    public Mono<HttpResponse> createVhost(String name, boolean tracing) {
        return createVhost(name, tracing, null);
    }

    public Mono<HttpResponse> createVhost(String name) {
        return doPut("vhosts", enc(name));
    }

    public Mono<HttpResponse> deleteVhost(String name) {
        return doDelete("vhosts", enc(name));
    }

    public Flux<UserPermissions> getPermissionsIn(String vhost) {
        return doGetFlux(UserPermissions.class, "vhosts", enc(vhost), "permissions");
    }

    public Mono<HttpResponse> updatePermissions(String vhost, String username, UserPermissions permissions) {
        return doPut(permissions, "permissions", enc(vhost), enc(username));
    }

    public Flux<TopicPermissions> getTopicPermissionsIn(String vhost) {
        return doGetFlux(TopicPermissions.class, "vhosts", enc(vhost), "topic-permissions");
    }

    public Mono<HttpResponse> updateTopicPermissions(String vhost, String username, TopicPermissions permissions) {
        return doPut(permissions, "topic-permissions", enc(vhost), enc(username));
    }

    public Flux<UserInfo> getUsers() {
        return doGetFlux(UserInfo.class, "users");
    }

    public Mono<UserInfo> getUser(String username) {
        return doGetMono(UserInfo.class, "users", enc(username));
    }

    public Mono<HttpResponse> deleteUser(String username) {
        return doDelete("users", enc(username));
    }

    public Mono<HttpResponse> createUser(String username, char[] password, List<String> tags) {
        if (username == null) {
            throw new IllegalArgumentException("username cannot be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("password cannot be null or empty. If you need to create a user that "
                + "will only authenticate using an x509 certificate, use createUserWithPasswordHash with a blank hash.");
        }
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("password", new String(password));
        if (tags == null || tags.isEmpty()) {
            body.put("tags", "");
        } else {
            body.put("tags", String.join(",", tags));
        }
        return doPut(body, "users", enc(username));
    }

    public Mono<HttpResponse> updateUser(String username, char[] password, List<String> tags) {
        if (username == null) {
            throw new IllegalArgumentException("username cannot be null");
        }
        Map<String, Object> body = new HashMap<String, Object>();
        // only update password if provided
        if (password != null) {
            body.put("password", new String(password));
        }
        if (tags == null || tags.isEmpty()) {
            body.put("tags", "");
        } else {
            body.put("tags", String.join(",", tags));
        }

        return doPut(body, "users", enc(username));
    }

    public Flux<UserPermissions> getPermissionsOf(String username) {
        return doGetFlux(UserPermissions.class, "users", enc(username), "permissions");
    }

    public Flux<TopicPermissions> getTopicPermissionsOf(String username) {
        return doGetFlux(TopicPermissions.class, "users", enc(username), "topic-permissions");
    }

    public Mono<HttpResponse> createUserWithPasswordHash(String username, char[] passwordHash, List<String> tags) {
        if (username == null) {
            throw new IllegalArgumentException("username cannot be null");
        }
        // passwordless authentication is a thing. See
        // https://github.com/rabbitmq/hop/issues/94 and https://www.rabbitmq.com/authentication.html. MK.
        if (passwordHash == null) {
            passwordHash = "".toCharArray();
        }
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("password_hash", String.valueOf(passwordHash));
        if (tags == null || tags.isEmpty()) {
            body.put("tags", "");
        } else {
            body.put("tags", String.join(",", tags));
        }

        return doPut(body, "users", enc(username));
    }

    public Mono<CurrentUserDetails> whoAmI() {
        return doGetMono(CurrentUserDetails.class, "whoami");
    }

    public Flux<UserPermissions> getPermissions() {
        return doGetFlux(UserPermissions.class, "permissions");
    }

    public Mono<UserPermissions> getPermissions(String vhost, String username) {
        return doGetMono(UserPermissions.class, "permissions", enc(vhost), enc(username));
    }

    public Mono<HttpResponse> clearPermissions(String vhost, String username) {
        return doDelete("permissions", enc(vhost), enc(username));
    }

    public Flux<TopicPermissions> getTopicPermissions() {
        return doGetFlux(TopicPermissions.class, "topic-permissions");
    }

    public Flux<TopicPermissions> getTopicPermissions(String vhost, String username) {
        return doGetFlux(TopicPermissions.class, "topic-permissions", enc(vhost), enc(username));
    }

    public Mono<HttpResponse> clearTopicPermissions(String vhost, String username) {
        return doDelete("topic-permissions", enc(vhost), enc(username));
    }

    public Flux<ExchangeInfo> getExchanges() {
        return doGetFlux(ExchangeInfo.class, "exchanges");
    }

    public Flux<ExchangeInfo> getExchanges(String vhost) {
        return doGetFlux(ExchangeInfo.class, "exchanges", enc(vhost));
    }

    public Mono<ExchangeInfo> getExchange(String vhost, String name) {
        return doGetMono(ExchangeInfo.class, "exchanges", enc(vhost), enc(name));
    }

    public Mono<HttpResponse> declareExchange(String vhost, String name, ExchangeInfo info) {
        return doPut(info, "exchanges", enc(vhost), enc(name));
    }

    public Mono<HttpResponse> deleteExchange(String vhost, String name) {
        return doDelete("exchanges", enc(vhost), enc(name));
    }

    /**
     * Publishes a message to an exchange.
     * <p>
     * <b>DO NOT USE THIS METHOD IN PRODUCTION</b>. The HTTP API has to create a new TCP
     * connection for each message published, which is highly suboptimal.
     * <p>
     * Use this method for test or development code only.
     * In production, use AMQP 0-9-1 or any other messaging protocol that uses a long-lived connection.
     *
     * @param vhost the virtual host to use
     * @param exchange the target exchange
     * @param routingKey the routing key to use
     * @param outboundMessage the message to publish
     * @return true if message has been routed to at least a queue, false otherwise
     * @since 3.4.0
     */
    public Mono<Boolean> publish(String vhost, String exchange, String routingKey, OutboundMessage outboundMessage) {
        if (vhost == null || vhost.isEmpty()) {
            throw new IllegalArgumentException("vhost cannot be null or blank");
        }
        if (exchange == null || exchange.isEmpty()) {
            throw new IllegalArgumentException("exchange cannot be null or blank");
        }

        Map<String, Object> body = Utils.bodyForPublish(routingKey, outboundMessage);

        return doPostMono(body, Map.class, "exchanges", enc(vhost), enc(exchange), "publish").map(response -> {
            Boolean routed = (Boolean) response.get("routed");
            if (routed == null) {
                return Boolean.FALSE;
            } else {
                return routed;
            }
        });
    }

    public Mono<AlivenessTestResult> alivenessTest(String vhost) {
        return doGetMono(AlivenessTestResult.class, "aliveness-test", enc(vhost));
    }

    public Mono<ClusterId> getClusterName() {
        return doGetMono(ClusterId.class, "cluster-name");
    }

    public Mono<HttpResponse> setClusterName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or blank");
        }
        return doPut(Collections.singletonMap("name", name), "cluster-name");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Flux<Map> getExtensions() {
        return doGetFlux(Map.class, "extensions");
    }

    public Mono<Definitions> getDefinitions() {
        return doGetMono(Definitions.class, "definitions");
    }

    public Flux<QueueInfo> getQueues() {
        return doGetFlux(QueueInfo.class, "queues");
    }

    public Flux<QueueInfo> getQueues(String vhost) {
        return doGetFlux(QueueInfo.class, "queues", enc(vhost));
    }

    public Mono<QueueInfo> getQueue(String vhost, String name) {
        return doGetMono(QueueInfo.class, "queues", enc(vhost), enc(name));
    }

    public Mono<HttpResponse> declareQueue(String vhost, String name, QueueInfo info) {
        return doPut(info, "queues", enc(vhost), enc(name));
    }

    public Mono<HttpResponse> purgeQueue(String vhost, String name) {
        return doDelete("queues", enc(vhost), enc(name), "contents");
    }

    public Mono<HttpResponse> deleteQueue(String vhost, String name) {
        return doDelete("queues", enc(vhost), enc(name));
    }

    /**
     * Get messages from a queue.
     *
     * <b>DO NOT USE THIS METHOD IN PRODUCTION</b>. Getting messages with the HTTP API
     * is intended for diagnostics or tests. It does not implement reliable delivery
     * and so should be treated as a sysadmin's tool rather than a general API for messaging.
     *
     * @param vhost    the virtual host the target queue is in
     * @param queue    the queue to consume from
     * @param count    the maximum number of messages to get
     * @param ackMode  determines whether the messages will be removed from the queue
     * @param encoding the expected encoding of the message payload
     * @param truncate to truncate the message payload if it is larger than the size given (in bytes), -1 means no truncation
     * @return the messages wrapped in a {@link Flux}
     * @see GetAckMode
     * @see GetEncoding
     * @since 3.4.0
     */
    public Flux<InboundMessage> get(String vhost, String queue,
                                    int count, GetAckMode ackMode, GetEncoding encoding, int truncate) {
        if (vhost == null || vhost.isEmpty()) {
            throw new IllegalArgumentException("vhost cannot be null or blank");
        }
        if (queue == null || queue.isEmpty()) {
            throw new IllegalArgumentException("queue cannot be null or blank");
        }
        Map<String, Object> body = Utils.bodyForGet(count, ackMode, encoding, truncate);
        return doPostFlux(body, InboundMessage.class, "queues", enc(vhost), enc(queue), "get");
    }

    /**
     * Get messages from a queue, with no limit for message payload truncation.
     *
     * <b>DO NOT USE THIS METHOD IN PRODUCTION</b>. Getting messages with the HTTP API
     * is intended for diagnostics or tests. It does not implement reliable delivery
     * and so should be treated as a sysadmin's tool rather than a general API for messaging.
     *
     * @param vhost    the virtual host the target queue is in
     * @param queue    the queue to consume from
     * @param count    the maximum number of messages to get
     * @param ackMode  determines whether the messages will be removed from the queue
     * @param encoding the expected encoding of the message payload
     * @return the messages wrapped in a {@link Flux}
     * @see GetAckMode
     * @see GetEncoding
     * @since 3.4.0
     */
    public Flux<InboundMessage> get(String vhost, String queue,
                                    int count, GetAckMode ackMode, GetEncoding encoding) {
        return get(vhost, queue, count, ackMode, encoding, -1);
    }

    /**
     * Get one message from a queue and requeue it.
     *
     * <b>DO NOT USE THIS METHOD IN PRODUCTION</b>. Getting messages with the HTTP API
     * is intended for diagnostics or tests. It does not implement reliable delivery
     * and so should be treated as a sysadmin's tool rather than a general API for messaging.
     *
     * @param vhost the virtual host the target queue is in
     * @param queue the queue to consume from
     * @return the message wrapped in a {@link Mono}
     * @see GetAckMode
     * @see GetEncoding
     * @since 3.4.0
     */
    public Mono<InboundMessage> get(String vhost, String queue) {
        return get(vhost, queue, 1, GetAckMode.NACK_REQUEUE_TRUE, GetEncoding.AUTO, 50000).last();
    }

    public Flux<BindingInfo> getBindings() {
        return doGetFlux(BindingInfo.class, "bindings");
    }

    public Flux<BindingInfo> getBindings(String vhost) {
        return doGetFlux(BindingInfo.class, "bindings", enc(vhost));
    }

    public Flux<BindingInfo> getExchangeBindingsBySource(String vhost, String exchange) {
        final String x = exchange.equals("") ? "amq.default" : exchange;
        return doGetFlux(BindingInfo.class, "exchanges", enc(vhost), enc(x), "bindings", "source");
    }

    public Flux<BindingInfo> getExchangeBindingsByDestination(String vhost, String exchange) {
        final String x = exchange.equals("") ? "amq.default" : exchange;
        return doGetFlux(BindingInfo.class, "exchanges", enc(vhost), enc(x), "bindings", "destination");
    }

    public Flux<BindingInfo> getQueueBindings(String vhost, String queue) {
        return doGetFlux(BindingInfo.class, "queues", enc(vhost), enc(queue), "bindings");
    }

    public Flux<BindingInfo> getQueueBindingsBetween(String vhost, String exchange, String queue) {
        return doGetFlux(BindingInfo.class, "bindings", enc(vhost), "e", enc(exchange), "q", enc(queue));
    }

    public Flux<BindingInfo> getExchangeBindingsBetween(String vhost, String source, String destination) {
        return doGetFlux(BindingInfo.class, "bindings", enc(vhost), "e", enc(source), "e", enc(destination));
    }

    public Mono<HttpResponse> bindExchange(String vhost, String destination, String source, String routingKey) {
        return bindExchange(vhost, destination, source, routingKey, new HashMap<>());
    }

    public Mono<HttpResponse> bindExchange(String vhost, String destination, String source, String routingKey, Map<String, Object> args) {
        if (vhost == null || vhost.isEmpty()) {
            throw new IllegalArgumentException("vhost cannot be null or blank");
        }
        if (destination == null || destination.isEmpty()) {
            throw new IllegalArgumentException("destination cannot be null or blank");
        }
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("source cannot be null or blank");
        }
        Map<String, Object> body = new HashMap<String, Object>();
        if (!(args == null)) {
            body.put("arguments", args);
        }
        body.put("routing_key", routingKey);

        return doPost(body, "bindings", enc(vhost), "e", enc(source), "e", enc(destination));
    }

    public Mono<HttpResponse> bindQueue(String vhost, String queue, String exchange, String routingKey) {
        return bindQueue(vhost, queue, exchange, routingKey, new HashMap<>());
    }

    public Mono<HttpResponse> bindQueue(String vhost, String queue, String exchange, String routingKey, Map<String, Object> args) {
        if (vhost == null || vhost.isEmpty()) {
            throw new IllegalArgumentException("vhost cannot be null or blank");
        }
        if (queue == null || queue.isEmpty()) {
            throw new IllegalArgumentException("queue cannot be null or blank");
        }
        if (exchange == null || exchange.isEmpty()) {
            throw new IllegalArgumentException("exchange cannot be null or blank");
        }
        Map<String, Object> body = new HashMap<String, Object>();
        if (!(args == null)) {
            body.put("arguments", args);
        }
        body.put("routing_key", routingKey);

        return doPost(body, "bindings", enc(vhost), "e", enc(exchange), "q", enc(queue));
    }

    public Mono<HttpResponse> declareShovel(String vhost, ShovelInfo info) {
        Map<String, Object> props = info.getDetails().getPublishProperties();
        if(props != null && props.isEmpty()) {
            throw new IllegalArgumentException("Shovel publish properties must be a non-empty map or null");
        }
        return doPut(info, "parameters", "shovel", enc(vhost), enc(info.getName()));
    }

    public Flux<ShovelInfo> getShovels() {
        return doGetFlux(ShovelInfo.class, "parameters", "shovel");
    }

    public Flux<ShovelStatus> getShovelsStatus() {
        return doGetFlux(ShovelStatus.class, "shovels");
    }

    public Mono<HttpResponse> deleteShovel(String vhost, String shovelName) {
        return doDelete("parameters", "shovel", enc(vhost), enc(shovelName));
    }

    //
    // Federation support
    //

    /**
     * Declares an upstream
     *
     * @param vhost   virtual host for which to declare the upstream
     * @param name    name of the upstream to declare
     * @param details upstream arguments
     * @return HTTP response in a mono
     */
    public Mono<HttpResponse> declareUpstream(String vhost, String name, UpstreamDetails details) {
        if (StringUtils.isEmpty(details.getUri())) {
            throw new IllegalArgumentException("Upstream uri must not be null or empty");
        }
        UpstreamInfo body = new UpstreamInfo();
        body.setVhost(vhost);
        body.setName(name);
        body.setValue(details);
        return doPut(body, "parameters", "federation-upstream", enc(vhost), enc(name));
    }

    /**
     * Deletes an upstream
     *
     * @param vhost virtual host for which to delete the upstream
     * @param name  name of the upstream to delete
     * @return HTTP response in a mono
     */
    public Mono<HttpResponse> deleteUpstream(String vhost, String name) {
        return doDelete("parameters", "federation-upstream", enc(vhost), enc(name));
    }

    /**
     * Returns a list of upstreams for "/" virtual host
     *
     * @return flux of upstream info
     */
    public Flux<UpstreamInfo> getUpstreams() {
        return doGetFlux(UpstreamInfo.class, "parameters", "federation-upstream");
    }

    /**
     * Returns a list of upstreams
     *
     * @param vhost virtual host the upstreams are in.
     * @return flux of upstream info
     */
    public Flux<UpstreamInfo> getUpstreams(String vhost) {
        return doGetFlux(UpstreamInfo.class, "parameters", "federation-upstream", enc(vhost));

    }

    /**
     * Declares an upstream set.
     *
     * @param vhost   virtual host for which to declare the upstream set
     * @param name    name of the upstream set to declare
     * @param details upstream set arguments
     * @return HTTP response in a mono
     */
    public Mono<HttpResponse> declareUpstreamSet(String vhost, String name, List<UpstreamSetDetails> details) {
        for (UpstreamSetDetails item : details) {
            if (StringUtils.isEmpty(item.getUpstream())) {
                throw new IllegalArgumentException("Each federation upstream set item must have a non-null and not " +
                        "empty upstream name");
            }
        }
        UpstreamSetInfo body = new UpstreamSetInfo();
        body.setVhost(vhost);
        body.setName(name);
        body.setValue(details);
        return doPut(body, "parameters", "federation-upstream-set", enc(vhost), enc(name));
    }

    /**
     * Deletes an upstream set
     *
     * @param vhost virtual host for which to delete the upstream set
     * @param name  name of the upstream set to delete
     * @return HTTP response in a mono
     */
    public Mono<HttpResponse> deleteUpstreamSet(String vhost, String name) {
        return doDelete("parameters", "federation-upstream-set", enc(vhost), enc(name));
    }

    /**
     * Returns a list of upstream sets for "/" virtual host
     *
     * @return flux of upstream set info
     */
    public Flux<UpstreamSetInfo> getUpstreamSets() {
        return doGetFlux(UpstreamSetInfo.class, "parameters", "federation-upstream-set");

    }

    /**
     * Returns a ist of upstream sets
     *
     * @param vhost Virtual host from where to get upstreams.
     * @return flux of upstream set info
     */
    public Flux<UpstreamSetInfo> getUpstreamSets(String vhost) {
        return doGetFlux(UpstreamSetInfo.class, "parameters", "federation-upstream-set", enc(vhost));
    }

    private <T> Mono<T> doGetMono(Class<T> type, String... pathSegments) {
        return Mono.from(client
            .headersWhen(authorizedHeader())
            .get()
            .uri(uri(pathSegments))
            .response(decode(type)));
    }

    protected <T> BiFunction<? super HttpClientResponse, ? super ByteBufFlux, Publisher<T>> decode(Class<T> type) {
        return (response, byteBufFlux) -> {
            this.responseCallback.accept(response);
            if (response.status().code() == 404) {
                return Mono.empty();
            } else {
                return byteBufFlux.aggregate().asInputStream().map(bytes -> deserialize(bytes, type));
            }
        };
    }

    private <T> T deserialize(InputStream inputStream, Class<T> type) {
        try {
            T value = objectMapper.readValue(inputStream, type);
            inputStream.close();
            return value;
        } catch (IOException e) {
            throw Exceptions.propagate(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Flux<T> doGetFlux(Class<T> type, String... pathSegments) {
        return (Flux<T>) doGetMono(Array.newInstance(type, 0).getClass(), pathSegments).flatMapMany(items -> Flux.fromArray((Object[]) items));
    }

    protected Function<? super HttpHeaders, Mono<? extends HttpHeaders>> authorizedHeader() {
        return headers -> token.map(t -> headers.set(HttpHeaderNames.AUTHORIZATION, t));
    }

    private Mono<HttpResponse> doPost(Object body, String... pathSegments) {
        return client.headersWhen(authorizedHeader())
            .headers(JSON_HEADER)
            .post()
            .uri(uri(pathSegments))
            .send(bodyPublisher(body))
            .response()
            .doOnNext(applyResponseCallback())
            .map(ReactorNettyClient::toHttpResponse);
    }

    private <T> Mono<T> doPostMono(Object body, Class<T> type, String... pathSegments) {
        return Mono.from(client.headersWhen(authorizedHeader())
                .headers(JSON_HEADER)
                .post()
                .uri(uri(pathSegments))
                .send(bodyPublisher(body))
                .response(decode(type)));
    }

    @SuppressWarnings("unchecked")
    private <T> Flux<T> doPostFlux(Object body, Class<T> type, String... pathSegments) {
        return (Flux<T>) doPostMono(body, Array.newInstance(type, 0).getClass(), pathSegments).flatMapMany(items -> Flux.fromArray((Object[]) items));
    }

    protected Consumer<HttpClientResponse> applyResponseCallback() {
        return response -> this.responseCallback.accept(response);
    }

    private Mono<HttpResponse> doPut(Object body, String... pathSegments) {
        return client.headersWhen(authorizedHeader())
            .put()
            .uri(uri(pathSegments))
            .send(bodyPublisher(body))
            .response()
            .doOnNext(applyResponseCallback())
            .map(ReactorNettyClient::toHttpResponse);
    }

    private Mono<ByteBuf> bodyPublisher(Object body) {
        return Mono.fromCallable(() -> {
            ByteBuf byteBuf = this.byteBufSupplier.get();
            ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(byteBuf);
            objectMapper.writeValue((OutputStream) byteBufOutputStream, body);
            return byteBuf;
        });
    }

    private Mono<HttpResponse> doPut(String... pathSegments) {
        return client.headersWhen(authorizedHeader())
            .headers(JSON_HEADER)
            .put()
            .uri(uri(pathSegments))
            .response()
            .doOnNext(applyResponseCallback())
            .map(ReactorNettyClient::toHttpResponse);
    }

    private Mono<HttpResponse> doDelete(Consumer<? super HttpHeaders> headerBuilder, String... pathSegments) {
        return client.headersWhen(authorizedHeader())
            .headers(headerBuilder)
            .delete()
            .uri(uri(pathSegments))
            .response()
            .doOnNext(applyResponseCallback())
            .map(ReactorNettyClient::toHttpResponse);
    }

    private Mono<HttpResponse> doDelete(String... pathSegments) {
        return doDelete(headers -> { }, pathSegments);
    }

    private String uri(String... pathSegments) {
        return "/" + String.join("/", pathSegments);
    }

    private String enc(String pathSegment) {
        return Utils.encode(pathSegment);
    }
}
