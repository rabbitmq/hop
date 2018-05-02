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
import com.rabbitmq.http.client.domain.ConnectionInfo;
import com.rabbitmq.http.client.domain.NodeInfo;
import com.rabbitmq.http.client.domain.OverviewResponse;
import com.rabbitmq.http.client.domain.PolicyInfo;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 *
 */
public class ReactorNettyClient {

    private static final int MAX_PAYLOAD_SIZE = 100 * 1024 * 1024;

    private static final String ENCODING_CHARSET = "UTF-8";

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
        return doGetMono(NodeInfo.class, "nodes", name);
    }

    public Flux<ConnectionInfo> getConnections() {
        return doGetFlux(ConnectionInfo.class, "connections");
    }

    public Mono<ConnectionInfo> getConnection(String name) {
        return doGetMono(ConnectionInfo.class, "connections", name);
    }

    public Mono<HttpResponse> closeConnection(String name) {
        return doDelete("connections", name);
    }

    public Mono<HttpResponse> closeConnection(String name, String reason) {
        return doDelete(request -> request.header("X-Reason", reason), "connections", name);
    }

    public Mono<HttpResponse> declarePolicy(String vhost, String name, PolicyInfo info) {
        return doPost(info, "policies", vhost, name);
    }

    private HttpClientRequest disableChunkTransfer(HttpClientRequest request) {
        return request.chunkedTransfer(false);
    }

    public Flux<PolicyInfo> getPolicies() {
        return doGetFlux(PolicyInfo.class, "policies");
    }

    public Mono<HttpResponse> deletePolicy(String vhost, String name) {
        return doDelete("policies", vhost, name);
    }

    private <T> Mono<T> doGetMono(Class<T> type, String... pathSegments) {
        return client.get(uri(pathSegments), request -> Mono.just(request)
            .transform(this::addAuthorization)
            .flatMap(pRequest -> pRequest.send())).transform(decode(type));
    }

    private <T> Flux<T> doGetFlux(Class<T> type, String... pathSegments) {
        return (Flux<T>) doGetMono(Array.newInstance(type, 0).getClass(), pathSegments).flatMapMany(items -> Flux.fromArray((Object[]) items));
    }

    private Mono<HttpResponse> doPost(Object body, String... pathSegments) {
        return client.put(uri(pathSegments), request -> Mono.just(request)
            .transform(this::addAuthorization)
            .map(this::disableChunkTransfer)
            .transform(encode(body)))
            .map(ReactorNettyClient::toHttpResponse);
    }

    private Mono<HttpResponse> doDelete(UnaryOperator<HttpClientRequest> operator, String... pathSegments) {
        return client.delete(uri(pathSegments), request -> Mono.just(request)
            .transform(this::addAuthorization)
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
        StringBuilder builder = new StringBuilder();
        if (pathSegments != null && pathSegments.length > 0) {
            for (String pathSegment : pathSegments) {
                builder.append("/");
                builder.append(Utils.encode(pathSegment));
            }
        }
        return rootUrl + builder.toString();
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
}
