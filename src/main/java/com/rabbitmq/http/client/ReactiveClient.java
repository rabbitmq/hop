/*
 * Copyright 2017 the original author or authors.
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
import com.rabbitmq.http.client.domain.ChannelInfo;
import com.rabbitmq.http.client.domain.ConnectionInfo;
import com.rabbitmq.http.client.domain.NodeInfo;
import com.rabbitmq.http.client.domain.OverviewResponse;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URISyntaxException;

/**
 *
 */
public class ReactiveClient {

    private final WebClient client;

    public ReactiveClient(String url, String username, String password) throws URISyntaxException {
        ExchangeStrategies strategies = ExchangeStrategies
            .builder()
            .codecs(clientDefaultCodecsConfigurer -> {
                final Jackson2ObjectMapperBuilder jacksonBuilder = Jackson2ObjectMapperBuilder
                    .json()
                    .serializationInclusion(JsonInclude.Include.NON_NULL);

                clientDefaultCodecsConfigurer.defaultCodecs()
                    .jackson2JsonEncoder(new Jackson2JsonEncoder(jacksonBuilder.build(), MediaType.APPLICATION_JSON));

                clientDefaultCodecsConfigurer.defaultCodecs()
                    .jackson2JsonDecoder(new Jackson2JsonDecoder(jacksonBuilder.build(), MediaType.APPLICATION_JSON));

            }).build();
        this.client = WebClient.builder()
            .exchangeStrategies(strategies)
            .baseUrl(url)
            .filter(ExchangeFilterFunctions.basicAuthentication(username, password))
            .build();

    }

    public Mono<OverviewResponse> getOverview() {
        return client
            .get()
            .uri("/overview")
            .retrieve()
            .bodyToMono(OverviewResponse.class);
    }

    public Flux<NodeInfo> getNodes() {
        return client
            .get()
            .uri("/nodes")
            .retrieve()
            .bodyToFlux(NodeInfo.class);
    }

    public Mono<NodeInfo> getNode(String name) {
        return client
            .get()
            .uri("/nodes/{name}", name)
            .retrieve()
            .bodyToMono(NodeInfo.class);
    }

    public Flux<ConnectionInfo> getConnections() {
        return client
            .get()
            .uri("/connections")
            .retrieve()
            .bodyToFlux(ConnectionInfo.class);
    }

    public Mono<ConnectionInfo> getConnection(String name) {
        return client
            .get()
            .uri("/connections/{name}", name)
            .retrieve()
            .bodyToMono(ConnectionInfo.class);
    }

    public Mono<ClientResponse> closeConnection(String name) {
        return client
            .delete()
            .uri("/connections/{name}", name)
            .exchange();
    }

    public Mono<ClientResponse> closeConnection(String name, String reason) {
        return client
            .delete()
            .uri("/connections/{name}", name)
            .header("X-Reason", reason)
            .exchange();
    }

    public Flux<ChannelInfo> getChannels() {
        return client
            .get()
            .uri("/channels")
            .retrieve()
            .bodyToFlux(ChannelInfo.class);
    }

    public Flux<ChannelInfo> getChannels(String connectionName) {
        return client
            .get()
            .uri("/connections/{connectionName}/channels", connectionName)
            .retrieve()
            .bodyToFlux(ChannelInfo.class);
    }


}
