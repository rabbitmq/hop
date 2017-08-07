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
import com.rabbitmq.http.client.domain.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.util.StringUtils.collectionToCommaDelimitedString;

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
            // FIXME see https://github.com/reactor/reactor-netty/issues/138
            .clientConnector(new ReactorClientHttpConnector(builder -> builder.disablePool()))
            .exchangeStrategies(strategies)
            .baseUrl(url)
            .filter(ExchangeFilterFunctions.basicAuthentication(username, password))
            .filter(ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
                ClientRequest request = ClientRequest.from(clientRequest)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .build();
                return Mono.just(request);
            }))
            .build();

    }

    public Mono<OverviewResponse> getOverview() {
        return client
            .get()
            .uri("/overview")
            .retrieve()
            .bodyToMono(OverviewResponse.class);
    }

    public Mono<CurrentUserDetails> whoAmI() {
        return client
            .get()
            .uri("/whoami")
            .retrieve()
            .bodyToMono(CurrentUserDetails.class);
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

    public Mono<ChannelInfo> getChannel(String name) {
        return client
            .get()
            .uri("/channels/{name}", name)
            .retrieve()
            .bodyToMono(ChannelInfo.class);
    }

    public Flux<VhostInfo> getVhosts() {
        return client
            .get()
            .uri("/vhosts")
            .retrieve()
            .bodyToFlux(VhostInfo.class);
    }

    public Mono<VhostInfo> getVhost(String name) {
        return client
            .get()
            .uri("/vhosts/{name}", name)
            .retrieve()
            .bodyToMono(VhostInfo.class);
    }

    public Mono<ClientResponse> createVhost(String name) {
        return client
            .put()
            .uri("/vhosts/{name}", name)
            .contentLength(0)
            .exchange();
    }

    public Mono<ClientResponse> deleteVhost(String name) {
        return client
            .delete()
            .uri("/vhosts/{name}", name)
            .exchange();
    }

    public Flux<UserPermissions> getPermissionsIn(String vhost) {
        return client
            .get()
            .uri("/vhosts/{name}/permissions", vhost)
            .retrieve()
            .bodyToFlux(UserPermissions.class);
    }

    public Flux<UserPermissions> getPermissionsOf(String username) {
        return client
            .get()
            .uri("/users/{username}/permissions", username)
            .retrieve()
            .bodyToFlux(UserPermissions.class);
    }

    public Flux<UserPermissions> getPermissions() {
        return client
            .get()
            .uri("/permissions")
            .retrieve()
            .bodyToFlux(UserPermissions.class);
    }

    public Mono<UserPermissions> getPermissions(String vhost, String username) {
        return client
            .get()
            .uri("/permissions/{vhost}/{username}", vhost, username)
            .retrieve()
            .bodyToMono(UserPermissions.class);
    }

    public Flux<UserInfo> getUsers() {
        return client
            .get()
            .uri("/users")
            .retrieve()
            .bodyToFlux(UserInfo.class);
    }

    public Mono<UserInfo> getUser(String username) {
        return client
            .get()
            .uri("/users/{username}", username)
            .retrieve()
            .bodyToMono(UserInfo.class);
    }

    public Mono<ClientResponse> deleteUser(String username) {
        return client
            .delete()
            .uri("/users/{username}", username)
            .exchange();
    }

    public Mono<ClientResponse> createUser(String username, char[] password, List<String> tags) {
        if(username == null) {
            throw new IllegalArgumentException("username cannot be null");
        }
        if(password == null) {
            throw new IllegalArgumentException("password cannot be null or empty. If you need to create a user that "
                + "will only authenticate using an x509 certificate, use createUserWithPasswordHash with a blank hash.");
        }
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("password", new String(password));
        body.put("tags", collectionToCommaDelimitedString(tags));

        return client
            .put()
            .uri("/users/{username}", username)
            .syncBody(body)
            .exchange();
    }

    public Mono<ClientResponse> createUserWithPasswordHash(String username, char[] passwordHash, List<String> tags) {
        if(username == null) {
            throw new IllegalArgumentException("username cannot be null");
        }
        // passwordless authentication is a thing. See
        // https://github.com/rabbitmq/hop/issues/94 and https://www.rabbitmq.com/authentication.html. MK.
        if(passwordHash == null) {
            passwordHash = "".toCharArray();
        }
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("password_hash", String.valueOf(passwordHash));
        body.put("tags", collectionToCommaDelimitedString(tags));

        return client
            .put()
            .uri("/users/{username}", username)
            .syncBody(body)
            .exchange();
    }

    public Mono<ClientResponse> updateUser(String username, char[] password, List<String> tags) {
        if(username == null) {
            throw new IllegalArgumentException("username cannot be null");
        }
        Map<String, Object> body = new HashMap<String, Object>();
        // only update password if provided
        if(password != null) {
            body.put("password", new String(password));
        }
        body.put("tags", collectionToCommaDelimitedString(tags));

        return client
            .put()
            .uri("/users/{username}", username)
            .syncBody(body)
            .exchange();
    }

    public Mono<ClientResponse> updatePermissions(String vhost, String username, UserPermissions permissions) {
        return client
            .put()
            .uri("/permissions/{vhost}/{username}", vhost, username)
            .syncBody(permissions)
            .exchange();
    }

}
