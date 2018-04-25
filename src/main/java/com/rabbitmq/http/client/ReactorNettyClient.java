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
import com.rabbitmq.http.client.domain.NodeInfo;
import com.rabbitmq.http.client.domain.OverviewResponse;
import com.rabbitmq.http.client.domain.PolicyInfo;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.json.JsonObjectDecoder;
import org.reactivestreams.Publisher;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClient;
import reactor.ipc.netty.http.client.HttpClientRequest;
import reactor.ipc.netty.http.client.HttpClientResponse;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Function;

/**
 *
 */
public class ReactorNettyClient {

    private static final int MAX_PAYLOAD_SIZE = 100 * 1024 * 1024;

    private final Mono<String> root = Mono.just("http://localhost:15672/api");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient client;

    private final String authorizationHeader;

    public ReactorNettyClient() {
        // FIXME make Jackson ObjectMapper configurable
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);

        // FIXME make URL configurable
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(root.block());
        client = HttpClient.create(options -> options.host(uriBuilder.build().getHost()).port(uriBuilder.build().getPort()));

        // FIXME make Authentication header value configurable (default being Basic)
        String credentials = "guest" + ":" + "guest";
        byte[] credentialsAsBytes = credentials.getBytes(StandardCharsets.ISO_8859_1);
        byte[] encodedBytes = Base64.getEncoder().encode(credentialsAsBytes);
        String encodedCredentials = new String(encodedBytes, StandardCharsets.ISO_8859_1);
        authorizationHeader = "Basic " + encodedCredentials;

        // FIXME make SSLContext configurable when using TLS
    }

    public Mono<OverviewResponse> getOverview() {
        return doGetMono(builder -> builder.pathSegment("overview"), OverviewResponse.class);
    }

    public Flux<NodeInfo> getNodes() {
        return doGetFlux(builder -> builder.pathSegment("nodes"), NodeInfo.class);
    }

    public Mono<HttpClientResponse> declarePolicy(String vhost, String name, PolicyInfo info) {
        return doPost(builder -> builder.pathSegment("policies", vhost, name), info);
    }

    private HttpClientRequest disableChunkTransfer(HttpClientRequest request) {
        return request.chunkedTransfer(false);
    }

    public Flux<PolicyInfo> getPolicies() {
        return doGetFlux(builder -> builder.pathSegment("policies"), PolicyInfo.class);
    }

    public Mono<HttpClientResponse> deletePolicy(String vhost, String name) {
        return doDelete(builder -> builder.pathSegment("policies", vhost, name));
    }

    private <T> Mono<T> doGetMono(Function<UriComponentsBuilder, UriComponentsBuilder> uriTransformer, Class<T> type) {
        return client.get(uri(uriTransformer), request -> Mono.just(request)
            .map(this::addAuthentication)
            .flatMap(pRequest -> pRequest.send())).transform(decode(type));
    }

    private <T> Flux<T> doGetFlux(Function<UriComponentsBuilder, UriComponentsBuilder> uriTransformer, Class<T> type) {
        return (Flux<T>) doGetMono(uriTransformer, Array.newInstance(type, 0).getClass()).flatMapMany(items -> Flux.fromArray((Object[]) items));
    }

    private Mono<HttpClientResponse> doPost(Function<UriComponentsBuilder, UriComponentsBuilder> uriTransformer, Object body) {
        return client.put(uri(uriTransformer), request -> Mono.just(request)
            .map(this::addAuthentication)
            .map(this::disableChunkTransfer)
            .transform(encode(body)));
    }

    private Mono<HttpClientResponse> doDelete(Function<UriComponentsBuilder, UriComponentsBuilder> uriTransformer) {
        return client.delete(uri(uriTransformer), request -> Mono.just(request)
            .map(this::addAuthentication)
            .flatMap(HttpClientRequest::send)
        );
    }

    private HttpClientRequest addAuthentication(HttpClientRequest request) {
        return request.addHeader(HttpHeaderNames.AUTHORIZATION, authorizationHeader);
    }

    private String uri(Function<UriComponentsBuilder, UriComponentsBuilder> transformer) {
        // FIXME encode URL without Spring dependencies
        // the encoding should be simple enough to not depend on Spring just for this
        return root.map(UriComponentsBuilder::fromHttpUrl)
            .map(transformer)
            .map(builder -> builder.build().encode())
            .block().toUriString();
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
