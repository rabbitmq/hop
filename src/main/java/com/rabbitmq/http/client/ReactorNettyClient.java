/*
 * Copyright 2018-2022 the original author or authors.
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

import static com.rabbitmq.http.client.PercentEncoder.encodeParameter;
import static com.rabbitmq.http.client.PercentEncoder.encodePathSegment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.http.client.domain.AlivenessTestResult;
import com.rabbitmq.http.client.domain.AuthenticationAttemptStatistics;
import com.rabbitmq.http.client.domain.BindingInfo;
import com.rabbitmq.http.client.domain.ChannelInfo;
import com.rabbitmq.http.client.domain.ClusterId;
import com.rabbitmq.http.client.domain.ConnectionInfo;
import com.rabbitmq.http.client.domain.ConsumerDetails;
import com.rabbitmq.http.client.domain.CurrentUserDetails;
import com.rabbitmq.http.client.domain.Definitions;
import com.rabbitmq.http.client.domain.DeleteQueueParameters;
import com.rabbitmq.http.client.domain.DeprecatedFeature;
import com.rabbitmq.http.client.domain.DetailsParameters;
import com.rabbitmq.http.client.domain.ExchangeInfo;
import com.rabbitmq.http.client.domain.FeatureFlag;
import com.rabbitmq.http.client.domain.GlobalRuntimeParameter;
import com.rabbitmq.http.client.domain.InboundMessage;
import com.rabbitmq.http.client.domain.MqttVhostPortInfo;
import com.rabbitmq.http.client.domain.NodeInfo;
import com.rabbitmq.http.client.domain.OAuthConfiguration;
import com.rabbitmq.http.client.domain.OutboundMessage;
import com.rabbitmq.http.client.domain.OverviewResponse;
import com.rabbitmq.http.client.domain.PolicyInfo;
import com.rabbitmq.http.client.domain.QueueInfo;
import com.rabbitmq.http.client.domain.ShovelInfo;
import com.rabbitmq.http.client.domain.ShovelStatus;
import com.rabbitmq.http.client.domain.StreamConsumer;
import com.rabbitmq.http.client.domain.StreamPublisher;
import com.rabbitmq.http.client.domain.TopicPermissions;
import com.rabbitmq.http.client.domain.UserLimits;
import com.rabbitmq.http.client.domain.UpstreamDetails;
import com.rabbitmq.http.client.domain.UpstreamInfo;
import com.rabbitmq.http.client.domain.UpstreamSetDetails;
import com.rabbitmq.http.client.domain.UpstreamSetInfo;
import com.rabbitmq.http.client.domain.UserInfo;
import com.rabbitmq.http.client.domain.UserPermissions;
import com.rabbitmq.http.client.domain.VhostInfo;
import com.rabbitmq.http.client.domain.VhostLimits;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

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
 * {@link JsonUtils#CURRENT_USER_DETAILS_DESERIALIZER_INSTANCE}, {@link JsonUtils#USER_INFO_DESERIALIZER_INSTANCE},
 * {@link JsonUtils#VHOST_LIMITS_DESERIALIZER_INSTANCE},
 * and {@link JsonUtils#CHANNEL_DETAILS_DESERIALIZER_INSTANCE} set up.
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
        this(Utils.urlWithoutCredentials(url),
                Utils.extractUsernamePassword(url)[0],
                Utils.extractUsernamePassword(url)[1], options);
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

    private static HttpResponse toHttpResponse(HttpClientResponse response) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (Map.Entry<String, String> headerEntry : response.responseHeaders().entries()) {
            headers.put(headerEntry.getKey(), headerEntry.getValue());
        }
        return new HttpResponse(response.status().code(), response.status().reasonPhrase(), headers);
    }

    public static ObjectMapper createDefaultObjectMapper() {
        return JsonUtils.createDefaultObjectMapper();
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
        return doGetMono(NodeInfo.class, "nodes", encodePathSegment(name));
    }

    public Flux<ConnectionInfo> getConnections() {
        return doGetFlux(ConnectionInfo.class, "connections");
    }

    public Mono<ConnectionInfo> getConnection(String name) {
        return doGetMono(ConnectionInfo.class, "connections", encodePathSegment(name));
    }

    public Mono<HttpResponse> closeConnection(String name) {
        return doDelete("connections", encodePathSegment(name));
    }

    public Mono<HttpResponse> closeConnection(String name, String reason) {
        return doDelete(headers -> headers.set("X-Reason", reason), "connections", encodePathSegment(name));
    }

    public Flux<ConsumerDetails> getConsumers() {
        return doGetFlux(ConsumerDetails.class, "consumers");
    }

    public Flux<ConsumerDetails> getConsumers(String vhost) {
        return doGetFlux(ConsumerDetails.class, "consumers", encodePathSegment(vhost));
    }

    public Mono<HttpResponse> declarePolicy(String vhost, String name, PolicyInfo info) {
        return doPut(info, "policies", encodePathSegment(vhost), encodePathSegment(name));
    }

    public Mono<HttpResponse> declareOperatorPolicy(String vhost, String name, PolicyInfo info) {
        return doPut(info, "operator-policies", encodePathSegment(vhost), encodePathSegment(name));
    }

    public Flux<PolicyInfo> getPolicies() {
        return doGetFlux(PolicyInfo.class, "policies");
    }

    public Flux<PolicyInfo> getPolicies(String vhost) {
        return doGetFlux(PolicyInfo.class, "policies", encodePathSegment(vhost));
    }

    public Flux<PolicyInfo> getOperatorPolicies() {
        return doGetFlux(PolicyInfo.class, "operator-policies");
    }

    public Flux<PolicyInfo> getOperatorPolicies(String vhost) {
        return doGetFlux(PolicyInfo.class, "operator-policies", encodePathSegment(vhost));
    }

    public Mono<HttpResponse> deletePolicy(String vhost, String name) {
        return doDelete("policies", encodePathSegment(vhost), encodePathSegment(name));
    }

    public Mono<HttpResponse> deleteOperatorPolicy(String vhost, String name) {
        return doDelete("operator-policies", encodePathSegment(vhost), encodePathSegment(name));
    }

    public Flux<ChannelInfo> getChannels() {
        return doGetFlux(ChannelInfo.class, "channels");
    }

    public Flux<ChannelInfo> getChannels(String connectionName) {
        return doGetFlux(ChannelInfo.class, "connections", encodePathSegment(connectionName), "channels");
    }

    public Mono<ChannelInfo> getChannel(String name) {
        return doGetMono(ChannelInfo.class, "channels", encodePathSegment(name));
    }

    public Flux<VhostInfo> getVhosts() {
        return doGetFlux(VhostInfo.class, "vhosts");
    }

    public Mono<VhostInfo> getVhost(String name) {
        return doGetMono(VhostInfo.class, "vhosts", encodePathSegment(name));
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
    public Mono<HttpResponse> createVhost(String name, boolean tracing, String description, String... tags) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("tracing", tracing);

        if (description != null && !description.isEmpty()) {
            body.put("description", description);
        }

        if (tags != null && tags.length > 0) {
            body.put("tags", String.join(",", tags));
        }
        return doPut(body, "vhosts", encodePathSegment(name));
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
    public Mono<HttpResponse> createVhost(String name, String description, String... tags) {
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
        return doPut("vhosts", encodePathSegment(name));
    }

    public Mono<HttpResponse> deleteVhost(String name) {
        return doDelete("vhosts", encodePathSegment(name));
    }

    /**
     * Enables deletion protection for a virtual host.
     * When enabled, the virtual host cannot be deleted until protection is disabled.
     *
     * @param name the virtual host name
     * @return HTTP response in a mono
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/vhosts">Virtual Hosts</a>
     */
    public Mono<HttpResponse> enableVhostDeletionProtection(String name) {
        return doPostWithoutBody("vhosts", encodePathSegment(name), "deletion", "protection");
    }

    /**
     * Disables deletion protection for a virtual host.
     *
     * @param name the virtual host name
     * @return HTTP response in a mono
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/vhosts">Virtual Hosts</a>
     */
    public Mono<HttpResponse> disableVhostDeletionProtection(String name) {
        return doDelete("vhosts", encodePathSegment(name), "deletion", "protection");
    }

    public Flux<UserPermissions> getPermissionsIn(String vhost) {
        return doGetFlux(UserPermissions.class, "vhosts", encodePathSegment(vhost), "permissions");
    }

    public Mono<HttpResponse> updatePermissions(String vhost, String username, UserPermissions permissions) {
        return doPut(permissions, "permissions", encodePathSegment(vhost), encodePathSegment(username));
    }

    public Flux<TopicPermissions> getTopicPermissionsIn(String vhost) {
        return doGetFlux(TopicPermissions.class, "vhosts", encodePathSegment(vhost), "topic-permissions");
    }

    public Mono<HttpResponse> updateTopicPermissions(String vhost, String username, TopicPermissions permissions) {
        return doPut(permissions, "topic-permissions", encodePathSegment(vhost), encodePathSegment(username));
    }

    public Flux<UserInfo> getUsers() {
        return doGetFlux(UserInfo.class, "users");
    }

    public Mono<UserInfo> getUser(String username) {
        return doGetMono(UserInfo.class, "users", encodePathSegment(username));
    }

    public Mono<HttpResponse> deleteUser(String username) {
        return doDelete("users", encodePathSegment(username));
    }

    /**
     * Lists users in the internal database that do not have access to any virtual hosts.
     * This is useful for finding users that may need permissions granted, or are not used
     * and should be cleaned up.
     *
     * @return flux of users without permissions
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/access-control">Access Control</a>
     */
    public Flux<UserInfo> getUsersWithoutPermissions() {
        return doGetFlux(UserInfo.class, "users", "without-permissions");
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
        return doPut(body, "users", encodePathSegment(username));
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

        return doPut(body, "users", encodePathSegment(username));
    }

    public Flux<UserPermissions> getPermissionsOf(String username) {
        return doGetFlux(UserPermissions.class, "users", encodePathSegment(username), "permissions");
    }

    public Flux<TopicPermissions> getTopicPermissionsOf(String username) {
        return doGetFlux(TopicPermissions.class, "users", encodePathSegment(username), "topic-permissions");
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

        return doPut(body, "users", encodePathSegment(username));
    }

    public Mono<CurrentUserDetails> whoAmI() {
        return doGetMono(CurrentUserDetails.class, "whoami");
    }

    public Flux<UserPermissions> getPermissions() {
        return doGetFlux(UserPermissions.class, "permissions");
    }

    public Mono<UserPermissions> getPermissions(String vhost, String username) {
        return doGetMono(UserPermissions.class, "permissions", encodePathSegment(vhost), encodePathSegment(username));
    }

    public Mono<HttpResponse> clearPermissions(String vhost, String username) {
        return doDelete("permissions", encodePathSegment(vhost), encodePathSegment(username));
    }

    public Flux<TopicPermissions> getTopicPermissions() {
        return doGetFlux(TopicPermissions.class, "topic-permissions");
    }

    public Flux<TopicPermissions> getTopicPermissions(String vhost, String username) {
        return doGetFlux(TopicPermissions.class, "topic-permissions", encodePathSegment(vhost), encodePathSegment(username));
    }

    public Mono<HttpResponse> clearTopicPermissions(String vhost, String username) {
        return doDelete("topic-permissions", encodePathSegment(vhost), encodePathSegment(username));
    }

    public Flux<ExchangeInfo> getExchanges() {
        return doGetFlux(ExchangeInfo.class, "exchanges");
    }

    public Flux<ExchangeInfo> getExchanges(String vhost) {
        return doGetFlux(ExchangeInfo.class, "exchanges", encodePathSegment(vhost));
    }

    public Mono<ExchangeInfo> getExchange(String vhost, String name) {
        return doGetMono(ExchangeInfo.class, "exchanges", encodePathSegment(vhost), encodePathSegment(name));
    }

    public Mono<HttpResponse> declareExchange(String vhost, String name, ExchangeInfo info) {
        return doPut(info, "exchanges", encodePathSegment(vhost), encodePathSegment(name));
    }

    public Mono<HttpResponse> deleteExchange(String vhost, String name) {
        return doDelete("exchanges", encodePathSegment(vhost), encodePathSegment(name));
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
     * @param vhost           the virtual host to use
     * @param exchange        the target exchange
     * @param routingKey      the routing key to use
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

        return doPostMono(body, Map.class, "exchanges", encodePathSegment(vhost), encodePathSegment(exchange), "publish").map(response -> {
            Boolean routed = (Boolean) response.get("routed");
            if (routed == null) {
                return Boolean.FALSE;
            } else {
                return routed;
            }
        });
    }

    public Mono<AlivenessTestResult> alivenessTest(String vhost) {
        return doGetMono(AlivenessTestResult.class, "aliveness-test", encodePathSegment(vhost));
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

    /**
     * Returns cluster metadata tags.
     *
     * @return map of cluster tags in a mono
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/parameters">Parameters and Policies</a>
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getClusterTags() {
        return getGlobalParameter("cluster_tags")
                .map(param -> {
                    if (param != null && param.getValue() instanceof Map) {
                        return (Map<String, Object>) param.getValue();
                    }
                    return Collections.<String, Object>emptyMap();
                })
                .onErrorResume(HttpClientException.class, e -> {
                    if (e.status() == 404) {
                        return Mono.just(Collections.emptyMap());
                    }
                    return Mono.error(e);
                });
    }

    /**
     * Sets cluster metadata tags.
     *
     * @param tags the tags to set
     * @return HTTP response in a mono
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/parameters">Parameters and Policies</a>
     */
    public Mono<HttpResponse> setClusterTags(Map<String, Object> tags) {
        return setGlobalParameter("cluster_tags", tags);
    }

    /**
     * Clears all cluster metadata tags.
     *
     * @return HTTP response in a mono
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/parameters">Parameters and Policies</a>
     */
    public Mono<HttpResponse> clearClusterTags() {
        return deleteGlobalParameter("cluster_tags");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Flux<Map> getExtensions() {
        return doGetFlux(Map.class, "extensions");
    }

    public Mono<Definitions> getDefinitions() {
        return doGetMono(Definitions.class, "definitions");
    }

    /**
     * Returns definitions for a specific virtual host.
     *
     * @param vhost the virtual host name
     * @return definitions for the virtual host in a mono
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/definitions">Definition Export and Import</a>
     */
    public Mono<Definitions> getDefinitions(String vhost) {
        return doGetMono(Definitions.class, "definitions", encodePathSegment(vhost));
    }

    /**
     * Returns all feature flags.
     *
     * @return flux of feature flags
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/feature-flags">Feature Flags</a>
     */
    public Flux<FeatureFlag> getFeatureFlags() {
        return doGetFlux(FeatureFlag.class, "feature-flags");
    }

    /**
     * Enables a feature flag.
     *
     * @param name the name of the feature flag to enable
     * @return HTTP response in a mono
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/feature-flags">Feature Flags</a>
     */
    public Mono<HttpResponse> enableFeatureFlag(String name) {
        return doPut(Collections.emptyMap(), "feature-flags", encodePathSegment(name), "enable");
    }

    /**
     * Enables all stable feature flags.
     * This iterates through all feature flags and enables those that are stable and disabled.
     *
     * @return mono that completes when all flags are enabled
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/feature-flags">Feature Flags</a>
     */
    public Mono<Void> enableAllStableFeatureFlags() {
        return getFeatureFlags()
                .filter(flag -> flag.getState() == com.rabbitmq.http.client.domain.FeatureFlagState.DISABLED
                        && flag.getStability() == com.rabbitmq.http.client.domain.FeatureFlagStability.STABLE)
                .flatMap(flag -> enableFeatureFlag(flag.getName()))
                .then();
    }

    /**
     * Performs a cluster-wide health check for any active resource alarms.
     * The returned Mono will error with an exception if the check fails (503).
     *
     * @return mono that completes on success
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/monitoring#health-checks">Health Checks</a>
     */
    public Mono<Void> healthCheckClusterAlarms() {
        return doGetMono(Object.class, "health", "checks", "alarms").then();
    }

    /**
     * Performs a health check for alarms on the local node only.
     * The returned Mono will error with an exception if the check fails (503).
     *
     * @return mono that completes on success
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/monitoring#health-checks">Health Checks</a>
     */
    public Mono<Void> healthCheckLocalAlarms() {
        return doGetMono(Object.class, "health", "checks", "local-alarms").then();
    }

    /**
     * Checks if a specific port has an active listener.
     * The returned Mono will error with an exception if the check fails (503).
     *
     * @param port the port to check
     * @return mono that completes on success
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/monitoring#health-checks">Health Checks</a>
     */
    public Mono<Void> healthCheckPortListener(int port) {
        return doGetMono(Object.class, "health", "checks", "port-listener", String.valueOf(port)).then();
    }

    /**
     * Checks if a specific protocol listener is active.
     * The returned Mono will error with an exception if the check fails (503).
     *
     * @param protocol the protocol to check (e.g., "amqp", "mqtt", "stomp")
     * @return mono that completes on success
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/monitoring#health-checks">Health Checks</a>
     */
    public Mono<Void> healthCheckProtocolListener(String protocol) {
        return doGetMono(Object.class, "health", "checks", "protocol-listener", encodePathSegment(protocol)).then();
    }

    /**
     * Checks if the target node is critical for maintaining quorum.
     * The returned Mono will error with an exception if the check fails (503).
     *
     * @return mono that completes on success
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/monitoring#health-checks">Health Checks</a>
     */
    public Mono<Void> healthCheckNodeIsQuorumCritical() {
        return doGetMono(Object.class, "health", "checks", "node-is-quorum-critical").then();
    }

    /**
     * Checks if all virtual hosts are running.
     * The returned Mono will error with an exception if the check fails (503).
     *
     * @return mono that completes on success
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/monitoring#health-checks">Health Checks</a>
     */
    public Mono<Void> healthCheckVirtualHosts() {
        return doGetMono(Object.class, "health", "checks", "virtual-hosts").then();
    }

    /**
     * Returns the current OAuth 2.0 configuration.
     *
     * @return OAuth configuration in a mono
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/oauth2">OAuth 2 Guide</a>
     */
    public Mono<OAuthConfiguration> getOAuthConfiguration() {
        return doGetMono(OAuthConfiguration.class, "auth");
    }

    /**
     * Returns authentication attempt statistics for a given node.
     *
     * @param nodeName the name of the node
     * @return flux of authentication attempt statistics per protocol
     * @since 5.5.0
     */
    public Flux<AuthenticationAttemptStatistics> getAuthAttemptStatistics(String nodeName) {
        return doGetFlux(AuthenticationAttemptStatistics.class, "auth", "attempts", encodePathSegment(nodeName));
    }

    /**
     * Returns all deprecated features.
     *
     * @return flux of deprecated features
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/deprecated">Deprecated Features</a>
     */
    public Flux<DeprecatedFeature> getDeprecatedFeatures() {
        return doGetFlux(DeprecatedFeature.class, "deprecated-features");
    }

    /**
     * Returns deprecated features that are currently in use.
     *
     * @return flux of deprecated features in use
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/deprecated">Deprecated Features</a>
     */
    public Flux<DeprecatedFeature> getDeprecatedFeaturesInUse() {
        return doGetFlux(DeprecatedFeature.class, "deprecated-features", "used");
    }

    /**
     * Triggers queue leader rebalancing.
     *
     * @return HTTP response in a mono
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/clustering#rebalancing">Queue Leader Rebalancing</a>
     */
    public Mono<HttpResponse> rebalanceQueueLeaders() {
        return doPost(Collections.emptyMap(), "rebalance", "queues");
    }

    /**
     * Returns all stream connections.
     *
     * @return flux of stream connections
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
     */
    public Flux<ConnectionInfo> getStreamConnections() {
        return doGetFlux(ConnectionInfo.class, "stream", "connections");
    }

    /**
     * Returns stream connections in a specific virtual host.
     *
     * @param vhost the virtual host name
     * @return flux of stream connections
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
     */
    public Flux<ConnectionInfo> getStreamConnections(String vhost) {
        return doGetFlux(ConnectionInfo.class, "stream", "connections", encodePathSegment(vhost));
    }

    /**
     * Returns information about a specific stream connection.
     *
     * @param vhost the virtual host name
     * @param name the connection name
     * @return stream connection info in a mono
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
     */
    public Mono<ConnectionInfo> getStreamConnection(String vhost, String name) {
        return doGetMono(ConnectionInfo.class, "stream", "connections", encodePathSegment(vhost), encodePathSegment(name));
    }

    /**
     * Closes a stream connection.
     *
     * @param vhost the virtual host name
     * @param name the connection name
     * @return HTTP response in a mono
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
     */
    public Mono<HttpResponse> closeStreamConnection(String vhost, String name) {
        return doDelete("stream", "connections", encodePathSegment(vhost), encodePathSegment(name));
    }

    /**
     * Returns all stream publishers.
     *
     * @return flux of stream publishers
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
     */
    public Flux<StreamPublisher> getStreamPublishers() {
        return doGetFlux(StreamPublisher.class, "stream", "publishers");
    }

    /**
     * Returns stream publishers in a specific virtual host.
     *
     * @param vhost the virtual host name
     * @return flux of stream publishers
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
     */
    public Flux<StreamPublisher> getStreamPublishers(String vhost) {
        return doGetFlux(StreamPublisher.class, "stream", "publishers", encodePathSegment(vhost));
    }

    /**
     * Returns stream publishers for a specific stream.
     *
     * @param vhost the virtual host name
     * @param stream the stream name
     * @return flux of stream publishers
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
     */
    public Flux<StreamPublisher> getStreamPublishers(String vhost, String stream) {
        return doGetFlux(StreamPublisher.class, "stream", "publishers", encodePathSegment(vhost), encodePathSegment(stream));
    }

    /**
     * Returns all stream consumers.
     *
     * @return flux of stream consumers
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
     */
    public Flux<StreamConsumer> getStreamConsumers() {
        return doGetFlux(StreamConsumer.class, "stream", "consumers");
    }

    /**
     * Returns stream consumers in a specific virtual host.
     *
     * @param vhost the virtual host name
     * @return flux of stream consumers
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
     */
    public Flux<StreamConsumer> getStreamConsumers(String vhost) {
        return doGetFlux(StreamConsumer.class, "stream", "consumers", encodePathSegment(vhost));
    }

    /**
     * Returns stream consumers for a specific stream.
     *
     * @param vhost the virtual host name
     * @param stream the stream name
     * @return flux of stream consumers
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
     */
    public Flux<StreamConsumer> getStreamConsumers(String vhost, String stream) {
        return doGetFlux(StreamConsumer.class, "stream", "consumers", encodePathSegment(vhost), encodePathSegment(stream));
    }

    /**
     * Returns stream publishers on a specific stream connection.
     *
     * @param vhost the virtual host name
     * @param connectionName the connection name
     * @return flux of stream publishers on the connection
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
     */
    public Flux<StreamPublisher> getStreamPublishersOnConnection(String vhost, String connectionName) {
        return doGetFlux(StreamPublisher.class, "stream", "connections", encodePathSegment(vhost), encodePathSegment(connectionName), "publishers");
    }

    /**
     * Returns stream consumers on a specific stream connection.
     *
     * @param vhost the virtual host name
     * @param connectionName the connection name
     * @return flux of stream consumers on the connection
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
     */
    public Flux<StreamConsumer> getStreamConsumersOnConnection(String vhost, String connectionName) {
        return doGetFlux(StreamConsumer.class, "stream", "connections", encodePathSegment(vhost), encodePathSegment(connectionName), "consumers");
    }

    /**
     * Returns all federation links.
     *
     * @return flux of federation links as untyped maps
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/federation">Federation Plugin</a>
     */
    @SuppressWarnings("rawtypes")
    public Flux<Map> getFederationLinks() {
        return doGetFlux(Map.class, "federation-links");
    }

    /**
     * Returns federation links in a specific virtual host.
     *
     * @param vhost the virtual host name
     * @return flux of federation links as untyped maps
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/federation">Federation Plugin</a>
     */
    @SuppressWarnings("rawtypes")
    public Flux<Map> getFederationLinks(String vhost) {
        return doGetFlux(Map.class, "federation-links", encodePathSegment(vhost));
    }

    /**
     * Returns all global runtime parameters.
     *
     * @return flux of global parameters
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/parameters">Parameters and Policies</a>
     */
    @SuppressWarnings("rawtypes")
    public Flux<GlobalRuntimeParameter> getGlobalParameters() {
        return doGetFlux(GlobalRuntimeParameter.class, "global-parameters");
    }

    /**
     * Returns a specific global runtime parameter.
     *
     * @param name the parameter name
     * @return the global parameter in a mono
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/parameters">Parameters and Policies</a>
     */
    @SuppressWarnings("rawtypes")
    public Mono<GlobalRuntimeParameter> getGlobalParameter(String name) {
        return doGetMono(GlobalRuntimeParameter.class, "global-parameters", encodePathSegment(name));
    }

    /**
     * Sets a global runtime parameter.
     *
     * @param name the parameter name
     * @param value the parameter value
     * @return HTTP response in a mono
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/parameters">Parameters and Policies</a>
     */
    public Mono<HttpResponse> setGlobalParameter(String name, Object value) {
        GlobalRuntimeParameter<Object> param = new GlobalRuntimeParameter<>();
        param.setName(name);
        param.setValue(value);
        return doPut(param, "global-parameters", encodePathSegment(name));
    }

    /**
     * Deletes a global runtime parameter.
     *
     * @param name the parameter name
     * @return HTTP response in a mono
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/parameters">Parameters and Policies</a>
     */
    public Mono<HttpResponse> deleteGlobalParameter(String name) {
        return doDelete("global-parameters", encodePathSegment(name));
    }

    /**
     * Deletes multiple users in a single operation.
     *
     * @param usernames the list of usernames to delete
     * @return HTTP response in a mono
     * @since 5.5.0
     * @see <a href="https://www.rabbitmq.com/docs/access-control">Access Control</a>
     */
    public Mono<HttpResponse> deleteUsers(List<String> usernames) {
        Map<String, Object> body = Collections.singletonMap("users", usernames);
        return doPost(body, "users", "bulk-delete");
    }

    public Flux<QueueInfo> getQueues() {
        return getQueues((DetailsParameters) null);
    }

    public Flux<QueueInfo> getQueues(DetailsParameters detailsParameters) {
        return doGetFlux(QueueInfo.class,
            detailsParameters == null ? Collections.emptyMap() : detailsParameters.parameters(),
            "queues");
    }

    public Flux<QueueInfo> getQueues(String vhost) {
        return getQueues(vhost, null);
    }

    public Flux<QueueInfo> getQueues(String vhost, DetailsParameters detailsParameters) {
        return doGetFlux(QueueInfo.class,
            detailsParameters == null ? Collections.emptyMap() : detailsParameters.parameters(),
            "queues", encodePathSegment(vhost));
    }

    public Mono<QueueInfo> getQueue(String vhost, String name, DetailsParameters detailsParameters) {
        return doGetMono(QueueInfo.class,
            detailsParameters == null ? Collections.emptyMap() : detailsParameters.parameters(),
            "queues", encodePathSegment(vhost), encodePathSegment(name));
    }

    public Mono<QueueInfo> getQueue(String vhost, String name) {
        return this.getQueue(vhost, name, null);
    }

    public Mono<HttpResponse> declareQueue(String vhost, String name, QueueInfo info) {
        return doPut(info, "queues", encodePathSegment(vhost), encodePathSegment(name));
    }

    public Mono<HttpResponse> purgeQueue(String vhost, String name) {
        return doDelete("queues", encodePathSegment(vhost), encodePathSegment(name), "contents");
    }

    public Mono<HttpResponse> deleteQueue(String vhost, String name) {
        return doDelete("queues", encodePathSegment(vhost), encodePathSegment(name));
    }

    public Mono<HttpResponse> deleteQueue(String vhost, String name, DeleteQueueParameters parameters) {
        return doDelete(headers -> {
        }, parameters.getAsQueryParams(), "queues", encodePathSegment(vhost), encodePathSegment(name));
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
        return doPostFlux(body, InboundMessage.class, "queues", encodePathSegment(vhost), encodePathSegment(queue), "get");
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
        return doGetFlux(BindingInfo.class, "bindings", encodePathSegment(vhost));
    }

    public Flux<BindingInfo> getExchangeBindingsBySource(String vhost, String exchange) {
        final String x = exchange.equals("") ? "amq.default" : exchange;
        return doGetFlux(BindingInfo.class, "exchanges", encodePathSegment(vhost), encodePathSegment(x), "bindings", "source");
    }

    public Flux<BindingInfo> getExchangeBindingsByDestination(String vhost, String exchange) {
        final String x = exchange.equals("") ? "amq.default" : exchange;
        return doGetFlux(BindingInfo.class, "exchanges", encodePathSegment(vhost), encodePathSegment(x), "bindings", "destination");
    }

    public Flux<BindingInfo> getQueueBindings(String vhost, String queue) {
        return doGetFlux(BindingInfo.class, "queues", encodePathSegment(vhost), encodePathSegment(queue), "bindings");
    }

    public Flux<BindingInfo> getQueueBindingsBetween(String vhost, String exchange, String queue) {
        return doGetFlux(BindingInfo.class, "bindings", encodePathSegment(vhost), "e", encodePathSegment(exchange), "q", encodePathSegment(queue));
    }

    public Flux<BindingInfo> getExchangeBindingsBetween(String vhost, String source, String destination) {
        return doGetFlux(BindingInfo.class, "bindings", encodePathSegment(vhost), "e", encodePathSegment(source), "e", encodePathSegment(destination));
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

        return doPost(body, "bindings", encodePathSegment(vhost), "e", encodePathSegment(source), "e", encodePathSegment(destination));
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

        return doPost(body, "bindings", encodePathSegment(vhost), "e", encodePathSegment(exchange), "q", encodePathSegment(queue));
    }

    public Mono<HttpResponse> declareShovel(String vhost, ShovelInfo info) {
        Map<String, Object> props = info.getDetails().getPublishProperties();
        if (props != null && props.isEmpty()) {
            throw new IllegalArgumentException("Shovel publish properties must be a non-empty map or null");
        }
        return doPut(info, "parameters", "shovel", encodePathSegment(vhost), encodePathSegment(info.getName()));
    }

    public Flux<ShovelInfo> getShovels() {
        return doGetFlux(ShovelInfo.class, "parameters", "shovel");
    }

    public Flux<ShovelStatus> getShovelsStatus() {
        return doGetFlux(ShovelStatus.class, "shovels");
    }

    public Mono<HttpResponse> deleteShovel(String vhost, String shovelName) {
        return doDelete("parameters", "shovel", encodePathSegment(vhost), encodePathSegment(shovelName));
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
        if (isEmpty(details.getUri())) {
            throw new IllegalArgumentException("Upstream uri must not be null or empty");
        }
        UpstreamInfo body = new UpstreamInfo();
        body.setVhost(vhost);
        body.setName(name);
        body.setValue(details);
        return doPut(body, "parameters", "federation-upstream", encodePathSegment(vhost), encodePathSegment(name));
    }

    /**
     * Deletes an upstream
     *
     * @param vhost virtual host for which to delete the upstream
     * @param name  name of the upstream to delete
     * @return HTTP response in a mono
     */
    public Mono<HttpResponse> deleteUpstream(String vhost, String name) {
        return doDelete("parameters", "federation-upstream", encodePathSegment(vhost), encodePathSegment(name));
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
        return doGetFlux(UpstreamInfo.class, "parameters", "federation-upstream", encodePathSegment(vhost));

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
            if (isEmpty(item.getUpstream())) {
                throw new IllegalArgumentException("Each federation upstream set item must have a non-null and not " +
                        "empty upstream name");
            }
        }
        UpstreamSetInfo body = new UpstreamSetInfo();
        body.setVhost(vhost);
        body.setName(name);
        body.setValue(details);
        return doPut(body, "parameters", "federation-upstream-set", encodePathSegment(vhost), encodePathSegment(name));
    }

    /**
     * Deletes an upstream set
     *
     * @param vhost virtual host for which to delete the upstream set
     * @param name  name of the upstream set to delete
     * @return HTTP response in a mono
     */
    public Mono<HttpResponse> deleteUpstreamSet(String vhost, String name) {
        return doDelete("parameters", "federation-upstream-set", encodePathSegment(vhost), encodePathSegment(name));
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
     * Returns a list of upstream sets
     *
     * @param vhost Virtual host from where to get upstreams.
     * @return flux of upstream set info
     */
    public Flux<UpstreamSetInfo> getUpstreamSets(String vhost) {
        return doGetFlux(UpstreamSetInfo.class, "parameters", "federation-upstream-set", encodePathSegment(vhost));
    }

    public Mono<MqttVhostPortInfo> getMqttPortToVhostMapping(){
        return doGetMono(MqttVhostPortInfo.class, "global-parameters", "mqtt_port_to_vhost_mapping");
    }

    public Mono<HttpResponse> deleteMqttPortToVhostMapping(){
        return doDelete( "global-parameters", "mqtt_port_to_vhost_mapping");
    }

    public Mono<HttpResponse> setMqttPortToVhostMapping(Map<Integer, String> portMappings){
        for (String vhost : portMappings.values()){
            if (vhost.isBlank()) {
                throw new IllegalArgumentException("Map with undefined vhosts provided!");
            }
        }

        MqttVhostPortInfo body = new MqttVhostPortInfo();
        body.setValue(portMappings);
        return doPut(body, "global-parameters", "mqtt_port_to_vhost_mapping");
    }


    /**
     * Returns the limits (max queues and connections) for all virtual hosts.
     *
     * @return flux of the limits
     * @since 3.7.0
     */
    public Flux<VhostLimits> getVhostLimits() {
        return doGetFlux(VhostLimits.class, "vhost-limits");
    }

    /**
     * Returns the limits (max queues and connections) for a given virtual host.
     *
     * @param vhost the virtual host
     * @return flux of the limits for this virtual host
     * @since 3.7.0
     */
    public Mono<VhostLimits> getVhostLimits(String vhost) {
        return doGetMono(VhostLimits.class, "vhost-limits", encodePathSegment(vhost))
                .map(limits -> limits.getVhost() == null ?
                        new VhostLimits(vhost, -1, -1) : limits);
    }

    /**
     * Sets the max number (limit) of connections for a virtual host.
     *
     * @param vhost the virtual host
     * @param limit the max number of connections allowed
     * @return HTTP response in a mono
     * @since 3.7.0
     */
    public Mono<HttpResponse> limitMaxNumberOfConnections(String vhost, int limit) {
        return doPut(Collections.singletonMap("value", limit),
                "vhost-limits", encodePathSegment(vhost), "max-connections");
    }

    /**
     * Sets the max number (limit) of queues for a virtual host.
     *
     * @param vhost the virtual host
     * @param limit the max number of queues allowed
     * @return HTTP response in a mono
     * @since 3.7.0
     */
    public Mono<HttpResponse> limitMaxNumberOfQueues(String vhost, int limit) {
        return doPut(Collections.singletonMap("value", limit),
                "vhost-limits", encodePathSegment(vhost), "max-queues");
    }

    /**
     * Clears the connection limit for a virtual host.
     *
     * @param vhost the virtual host
     * @return HTTP response in a mono
     * @since 3.7.0
     */
    public Mono<HttpResponse> clearMaxConnectionsLimit(String vhost) {
        return doDelete("vhost-limits", encodePathSegment(vhost), "max-connections");
    }

    /**
     * Clears the queue limit for a virtual host.
     *
     * @param vhost the virtual host
     * @return HTTP response in a mono
     * @since 3.7.0
     */
    public Mono<HttpResponse> clearMaxQueuesLimit(String vhost) {
        return doDelete("vhost-limits", encodePathSegment(vhost), "max-queues");
    }

    /**
     * Returns the limits (max connections and channels) for all users.
     *
     * @return flux of the limits
     * @since 5.5.0
     */
    public Flux<UserLimits> getUserLimits() {
        return doGetFlux(UserLimits.class, "user-limits");
    }

    /**
     * Returns the limits (max connections and channels) for a given user.
     *
     * @param username the username
     * @return mono of the limits for this user
     * @since 5.5.0
     */
    public Mono<UserLimits> getUserLimits(String username) {
        return doGetMono(UserLimits.class, "user-limits", encodePathSegment(username))
                .map(limits -> limits.getUser() == null ?
                        new UserLimits(username, -1, -1) : limits);
    }

    /**
     * Sets the max number (limit) of connections for a user.
     *
     * @param username the username
     * @param limit the max number of connections allowed
     * @return HTTP response in a mono
     * @since 5.5.0
     */
    public Mono<HttpResponse> limitUserMaxConnections(String username, int limit) {
        return doPut(Collections.singletonMap("value", limit),
                "user-limits", encodePathSegment(username), "max-connections");
    }

    /**
     * Sets the max number (limit) of channels for a user.
     *
     * @param username the username
     * @param limit the max number of channels allowed
     * @return HTTP response in a mono
     * @since 5.5.0
     */
    public Mono<HttpResponse> limitUserMaxChannels(String username, int limit) {
        return doPut(Collections.singletonMap("value", limit),
                "user-limits", encodePathSegment(username), "max-channels");
    }

    /**
     * Clears the connection limit for a user.
     *
     * @param username the username
     * @return HTTP response in a mono
     * @since 5.5.0
     */
    public Mono<HttpResponse> clearUserMaxConnectionsLimit(String username) {
        return doDelete("user-limits", encodePathSegment(username), "max-connections");
    }

    /**
     * Clears the channel limit for a user.
     *
     * @param username the username
     * @return HTTP response in a mono
     * @since 5.5.0
     */
    public Mono<HttpResponse> clearUserMaxChannelsLimit(String username) {
        return doDelete("user-limits", encodePathSegment(username), "max-channels");
    }

    private <T> Mono<T> doGetMono(Class<T> type, String... pathSegments) {
       return doGetMono(type, null, pathSegments);
    }

    private <T> Mono<T> doGetMono(Class<T> type, Map<String, String> queryParameters, String... pathSegments) {
        String uri = uri(pathSegments);
        if (queryParameters != null && !queryParameters.isEmpty()) {
            uri += queryParameters.entrySet().stream()
                .map(e -> String.format("%s=%s", e.getKey(), encodeParameter(e.getValue())))
                .collect(Collectors.joining("&", "?", ""));
        }
        return Mono.from(client
                .headersWhen(authorizedHeader())
                .get()
                .uri(uri)
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

    private <T> Flux<T> doGetFlux(Class<T> type, String... pathSegments) {
        return doGetFlux(type, null, pathSegments);
    }

    @SuppressWarnings("unchecked")
    private <T> Flux<T> doGetFlux(Class<T> type, Map<String, String> queryParameters, String... pathSegments) {
        return (Flux<T>) doGetMono(Array.newInstance(type, 0).getClass(), queryParameters, pathSegments)
            .flatMapMany(items -> Flux.fromArray((Object[]) items));
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

    private Mono<HttpResponse> doPostWithoutBody(String... pathSegments) {
        return client.headersWhen(authorizedHeader())
                .post()
                .uri(uri(pathSegments))
                .response()
                .doOnNext(applyResponseCallback())
                .doOnNext(response -> {
                    if (response.status().code() >= 500) {
                        throw new HttpServerException(response.status().code(), response.status().reasonPhrase());
                    } else if (response.status().code() >= 400) {
                        throw new HttpClientException(response.status().code(), response.status().reasonPhrase());
                    }
                })
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
                .headers(JSON_HEADER)
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

    private Mono<HttpResponse> doDelete(Consumer<? super HttpHeaders> headerBuilder, Map<String, String> queryParams, String... pathSegments) {
        String uri = uri(pathSegments);
        if (queryParams != null && !queryParams.isEmpty()) {
            uri += queryParams.entrySet().stream()
                    .map(e -> String.format("%s=%s", e.getKey(), encodeParameter(e.getValue())))
                    .collect(Collectors.joining("&", "?", ""));
        }
        return client.headersWhen(authorizedHeader())
                .headers(headerBuilder)
                .delete()
                .uri(uri)
                .response()
                .doOnNext(applyResponseCallback())
                .doOnNext(response -> {
                    int statusCode = response.status().code();
                    // 404 is acceptable for DELETE (idempotent operation)
                    if (statusCode == 404) {
                        return;
                    }
                    if (statusCode >= 500) {
                        throw new HttpServerException(statusCode, response.status().reasonPhrase());
                    } else if (statusCode >= 400) {
                        throw new HttpClientException(statusCode, response.status().reasonPhrase());
                    }
                })
                .map(ReactorNettyClient::toHttpResponse);
    }

    private Mono<HttpResponse> doDelete(Consumer<? super HttpHeaders> headerBuilder, String... pathSegments) {
        return doDelete(headerBuilder, Collections.emptyMap(), pathSegments);
    }

    private Mono<HttpResponse> doDelete(String... pathSegments) {
        return doDelete(headers -> {
        }, pathSegments);
    }

    private static String uri(String... pathSegments) {
        return "/" + String.join("/", pathSegments);
    }

    private static boolean isEmpty(String str) {
        return (str == null || "".equals(str));
    }
}
