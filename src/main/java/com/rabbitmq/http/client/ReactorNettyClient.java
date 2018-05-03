/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rabbitmq.http.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.http.client.domain.AlivenessTestResult;
import com.rabbitmq.http.client.domain.BindingInfo;
import com.rabbitmq.http.client.domain.ChannelInfo;
import com.rabbitmq.http.client.domain.ClusterId;
import com.rabbitmq.http.client.domain.ConnectionInfo;
import com.rabbitmq.http.client.domain.CurrentUserDetails;
import com.rabbitmq.http.client.domain.Definitions;
import com.rabbitmq.http.client.domain.ExchangeInfo;
import com.rabbitmq.http.client.domain.NodeInfo;
import com.rabbitmq.http.client.domain.OverviewResponse;
import com.rabbitmq.http.client.domain.PolicyInfo;
import com.rabbitmq.http.client.domain.QueueInfo;
import com.rabbitmq.http.client.domain.ShovelInfo;
import com.rabbitmq.http.client.domain.ShovelStatus;
import com.rabbitmq.http.client.domain.UserInfo;
import com.rabbitmq.http.client.domain.UserPermissions;
import com.rabbitmq.http.client.domain.VhostInfo;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.json.JsonObjectDecoder;
import org.reactivestreams.Publisher;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClient;
import reactor.ipc.netty.http.client.HttpClientRequest;
import reactor.ipc.netty.http.client.HttpClientResponse;

import java.lang.reflect.Array;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 *
 */
public class ReactorNettyClient {

    private static final int MAX_PAYLOAD_SIZE = 100 * 1024 * 1024;

    private final String rootUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient client;

    private final Mono<String> token;

    public ReactorNettyClient(String url) {
        this(urlWithoutCredentials(url),
            URI.create(url).getUserInfo().split(":")[0],
            URI.create(url).getUserInfo().split(":")[1]);
    }

    public ReactorNettyClient(String url, String username, String password) {
        rootUrl = url;
        // FIXME make Jackson ObjectMapper configurable
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);

        URI uri = URI.create(url);
        client = HttpClient.create(options -> options.host(uri.getHost()).port(uri.getPort()));

        // FIXME make Authentication header value configurable (default being Basic)
        this.token = createBasicAuthenticationToken(username, password);

        // FIXME make SSLContext configurable when using TLS
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

    private static HttpClientRequest disableChunkTransfer(HttpClientRequest request) {
        return request.chunkedTransfer(false);
    }

    private static HttpClientRequest disableFailOnError(HttpClientRequest request) {
        return request
            .failOnClientError(false)
            .failOnServerError(false);
    }

    protected Mono<String> createBasicAuthenticationToken(String username, String password) {
        return Mono.fromSupplier(() -> {
            String credentials = username + ":" + password;
            byte[] credentialsAsBytes = credentials.getBytes(StandardCharsets.ISO_8859_1);
            byte[] encodedBytes = Base64.getEncoder().encode(credentialsAsBytes);
            String encodedCredentials = new String(encodedBytes, StandardCharsets.ISO_8859_1);
            return "Basic " + encodedCredentials;
        }).cache();
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
        return doDelete(request -> request.header("X-Reason", reason), "connections", enc(name));
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

    public Flux<ExchangeInfo> getExchanges() {
        return doGetFlux(ExchangeInfo.class, "exchanges");
    }

    public Flux<ExchangeInfo> getExchanges(String vhost) {
        return doGetFlux(ExchangeInfo.class, "exchanges", enc(vhost));
    }

    public Mono<HttpResponse> declareExchange(String vhost, String name, ExchangeInfo info) {
        return doPut(info, "exchanges", enc(vhost), enc(name));
    }

    public Mono<HttpResponse> deleteExchange(String vhost, String name) {
        return doDelete("exchanges", enc(vhost), enc(name));
    }

    public Mono<AlivenessTestResult> alivenessTest(String vhost) {
        return doGetMono(AlivenessTestResult.class, "aliveness-test", enc(vhost));
    }

    public Mono<ClusterId> getClusterName() {
        return doGetMono(ClusterId.class, "cluster-name");
    }

    public Mono<HttpResponse> setClusterName(String name) {
        if(name== null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or blank");
        }
        return doPut(Collections.singletonMap("name", name),"cluster-name");
    }

    @SuppressWarnings({"unchecked","rawtypes"})
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
        return doPut(info,"queues", enc(vhost), enc(name));
    }

    public Mono<HttpResponse> purgeQueue(String vhost, String name) {
        return doDelete("queues", enc(vhost), enc(name), "contents");
    }

    public Mono<HttpResponse> deleteQueue(String vhost, String name) {
        return doDelete("queues", enc(vhost), enc(name));
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
            body.put("args", args);
        }
        body.put("routing_key", routingKey);

        return doPost(body, "bindings", enc(vhost), "e", enc(source), "e", enc(destination));
    }

    public Mono<HttpResponse> bindQueue(String vhost, String queue, String exchange, String routingKey) {
        return bindQueue(vhost, queue, exchange, routingKey, new HashMap<>());
    }

    public Mono<HttpResponse> bindQueue(String vhost, String queue, String exchange, String routingKey, Map<String, Object> args) {
        if(vhost == null || vhost.isEmpty()) {
            throw new IllegalArgumentException("vhost cannot be null or blank");
        }
        if(queue == null || queue.isEmpty()) {
            throw new IllegalArgumentException("queue cannot be null or blank");
        }
        if(exchange == null || exchange.isEmpty()) {
            throw new IllegalArgumentException("exchange cannot be null or blank");
        }
        Map<String, Object> body = new HashMap<String, Object>();
        if(!(args == null)) {
            body.put("args", args);
        }
        body.put("routing_key", routingKey);

        return doPost(body, "bindings", enc(vhost), "e", enc(exchange), "q", enc(queue));
    }

    public Mono<HttpResponse> declareShovel(String vhost, ShovelInfo info) {
        return doPut(info,"parameters", "shovel", enc(vhost), enc(info.getName()));
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

    private <T> Mono<T> doGetMono(Class<T> type, String... pathSegments) {
        return client.get(uri(pathSegments), request -> Mono.just(request)
            .transform(this::addAuthorization)
            .flatMap(HttpClientRequest::send))
            .onErrorMap(this::handleError)
            .transform(decode(type));
    }

    @SuppressWarnings("unchecked")
    private <T> Flux<T> doGetFlux(Class<T> type, String... pathSegments) {
        return (Flux<T>) doGetMono(Array.newInstance(type, 0).getClass(), pathSegments).flatMapMany(items -> Flux.fromArray((Object[]) items));
    }

    private Mono<HttpResponse> doPost(Object body, String... pathSegments) {
        return client.post(uri(pathSegments), request -> Mono.just(request)
            .transform(this::addAuthorization)
            .map(ReactorNettyClient::disableChunkTransfer)
            .map(ReactorNettyClient::disableFailOnError)
            .transform(encode(body)))
            .map(ReactorNettyClient::toHttpResponse);
    }

    private Mono<HttpResponse> doPut(Object body, String... pathSegments) {
        return client.put(uri(pathSegments), request -> Mono.just(request)
            .transform(this::addAuthorization)
            .map(ReactorNettyClient::disableChunkTransfer)
            .map(ReactorNettyClient::disableFailOnError)
            .transform(encode(body)))
            .map(ReactorNettyClient::toHttpResponse);
    }

    private Mono<HttpResponse> doPut(String... pathSegments) {
        return client.put(uri(pathSegments), request -> Mono.just(request)
            .transform(this::addAuthorization)
            .map(ReactorNettyClient::disableChunkTransfer)
            .map(ReactorNettyClient::disableFailOnError)
            .flatMap(HttpClientRequest::send))
            .map(ReactorNettyClient::toHttpResponse);
    }

    private Mono<HttpResponse> doDelete(UnaryOperator<HttpClientRequest> operator, String... pathSegments) {
        return client.delete(uri(pathSegments), request -> Mono.just(request)
            .transform(this::addAuthorization)
            .map(ReactorNettyClient::disableFailOnError)
            .map(operator)
            .flatMap(HttpClientRequest::send)
        ).map(ReactorNettyClient::toHttpResponse);
    }

    private Mono<HttpResponse> doDelete(String... pathSegments) {
        return doDelete(request -> request, pathSegments);
    }

    private Mono<HttpClientRequest> addAuthorization(Mono<HttpClientRequest> request) {
        return Mono
            .zip(request, this.token)
            .map(tuple -> tuple.getT1().header(HttpHeaderNames.AUTHORIZATION, tuple.getT2()));
    }

    private String uri(String... pathSegments) {
        return rootUrl + "/" + String.join("/", pathSegments);
    }

    private String enc(String pathSegment) {
        return Utils.encode(pathSegment);
    }

    private <T> Function<Mono<HttpClientResponse>, Flux<T>> decode(Class<T> type) {
        return inbound ->
            inbound.flatMapMany(response -> response.addHandler(new JsonObjectDecoder(MAX_PAYLOAD_SIZE)).receive().asByteArray()
                .map(payload -> {
                    try {
                        return objectMapper.readValue(payload, type);
                    } catch (Throwable t) {
                        // FIXME exception handling
                        throw new RuntimeException(t);
                    }
                })
            );
    }

    private Function<Mono<HttpClientRequest>, Publisher<Void>> encode(Object requestPayload) {
        return outbound -> outbound
            .flatMapMany(request -> {
                try {
                    byte[] bytes = objectMapper.writeValueAsBytes(requestPayload);

                    return request
                        .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                        .header(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(bytes.length))
                        .sendByteArray(Mono.just(bytes));
                } catch (JsonProcessingException e) {
                    throw Exceptions.propagate(e);
                }
            });
    }

    // FIXME make this configurable
    @SuppressWarnings("unchecked")
    private <T extends Throwable> T handleError(T cause) {
        if (cause instanceof reactor.ipc.netty.http.client.HttpClientException) {
            return (T) new HttpClientException((reactor.ipc.netty.http.client.HttpClientException) cause);
        }
        return (T) new HttpException(cause);
    }
}
