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

import com.rabbitmq.http.client.domain.ConnectionInfo;
import com.rabbitmq.http.client.domain.NodeInfo;
import com.rabbitmq.http.client.domain.OverviewResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
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
        this.client = WebClient.builder()
            .baseUrl(url)
            .build()
            .filter(ExchangeFilterFunctions.basicAuthentication(username, password));
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

}
