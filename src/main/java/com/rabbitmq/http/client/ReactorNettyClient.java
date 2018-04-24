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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Function;

/**
 *
 */
public class ReactorNettyClient {

    private static final int MAX_PAYLOAD_SIZE = 100 * 1024 * 1024;

    private final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl("http://localhost:15672/api");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient client;

    private final String authorizationHeader;

    public ReactorNettyClient() {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);

        client = HttpClient.create(options -> options.host(uriBuilder.build().getHost()).port(uriBuilder.build().getPort()));

        String credentials = "guest" + ":" + "guest";
        byte[] credentialsAsBytes = credentials.getBytes(StandardCharsets.ISO_8859_1);
        byte[] encodedBytes = Base64.getEncoder().encode(credentialsAsBytes);
        String encodedCredentials = new String(encodedBytes, StandardCharsets.ISO_8859_1);
        authorizationHeader = "Basic " + encodedCredentials;
    }

    static Function<Mono<HttpClientRequest>, Publisher<Void>> encode(ObjectMapper objectMapper, Object requestPayload) {
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

    public Mono<OverviewResponse> getOverview() {
        return client.get(uriBuilder.cloneBuilder().pathSegment("overview").toUriString(), request -> request
            .addHeader(HttpHeaderNames.AUTHORIZATION, authorizationHeader)
            .send()).transform(decode(OverviewResponse.class));
    }

    public Flux<NodeInfo> getNodes() {
        return client.get(uriBuilder.cloneBuilder().pathSegment("nodes").toUriString(), request -> request
            .addHeader(HttpHeaderNames.AUTHORIZATION, authorizationHeader)
            .send()).transform(decode(NodeInfo[].class)).flatMapMany(nodes -> Flux.fromArray(nodes));
    }

    public Mono<HttpClientResponse> declarePolicy(String vhost, String name, PolicyInfo info) {
        return client.put(uriBuilder.cloneBuilder()
            .pathSegment("policies", "{vhost}", "{name}")
            .build(vhost, name).toASCIIString(), request -> {
            request.addHeader(HttpHeaderNames.AUTHORIZATION, authorizationHeader)
                .chunkedTransfer(false)
                .failOnClientError(false)
                .failOnServerError(false);

            return Mono.just(request).transform(encode(objectMapper, info));
        });
    }

    public Flux<PolicyInfo> getPolicies() {
        return client.get(uriBuilder.cloneBuilder().pathSegment("policies").toUriString(), request -> request
            .addHeader(HttpHeaderNames.AUTHORIZATION, authorizationHeader)
            .send()).transform(decode(PolicyInfo[].class)).flatMapMany(nodes -> Flux.fromArray(nodes));
    }

    public Mono<HttpClientResponse> deletePolicy(String vhost, String name) {
        return client.delete(uriBuilder.cloneBuilder()
            .pathSegment("policies", "{vhost}", "{name}")
            .build(vhost, name).toASCIIString(), request -> request
            .addHeader(HttpHeaderNames.AUTHORIZATION, authorizationHeader)
            .send());
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
}
