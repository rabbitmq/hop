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
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.JdkSslContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.net.ssl.SSLContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 *
 */
public class ReactiveClient {

    private final WebClient client;

    public ReactiveClient(String url) throws MalformedURLException {
        this(url, c -> {}, null);
    }

    public ReactiveClient(String url, Consumer<WebClient.Builder> configurator) throws MalformedURLException {
        this(url, configurator, null);
    }

    public ReactiveClient(String url, Consumer<WebClient.Builder> configurator, SSLContext sslContext) throws MalformedURLException {
        this(urlWithoutCredentials(url),
            StringUtils.split(new URL(url).getUserInfo(),":")[0],
            StringUtils.split(new URL(url).getUserInfo(),":")[1],
            configurator,
            sslContext);
    }

    public ReactiveClient(String url, String username, String password) {
        this(url, username, password, builder -> {}, null);
    }

    public ReactiveClient(String url, String username, String password, Consumer<WebClient.Builder> configurator) {
        this(url, username, password, configurator, null);
    }

    public ReactiveClient(String url, String username, String password, Consumer<WebClient.Builder> configurator, SSLContext sslContext) {
        this.client = buildWebClient(url, username, password, configurator, sslContext);
    }

    protected WebClient buildWebClient(String url, String username, String password, Consumer<WebClient.Builder> configurator, SSLContext sslContext) {
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
        WebClient.Builder builder = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(clientBuilder -> {
                if (sslContext != null) {
                    clientBuilder.sslContext(new JdkSslContext(sslContext, true, ClientAuth.NONE));
                }
                // FIXME
                // see https://jira.spring.io/browse/SPR-15972
                clientBuilder.disablePool();
            }))
            .exchangeStrategies(strategies)
            .baseUrl(url)
            .filter(ExchangeFilterFunctions.basicAuthentication(username, password))
            .filter(ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
                ClientRequest request = ClientRequest.from(clientRequest)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .build();
                return Mono.just(request);
            }));
        if (configurator != null) {
            configurator.accept(builder);
        }
        return builder.build();
    }

    private static String urlWithoutCredentials(String url) throws MalformedURLException {
        URL url1 = new URL(url);
        return StringUtils.replace(url, url1.getUserInfo() + "@", "");
    }

    public Mono<OverviewResponse> getOverview() {
        return client
            .get()
            .uri("/overview")
            .retrieve()
            .bodyToMono(OverviewResponse.class);
    }

    public Mono<AlivenessTestResult> alivenessTest(String vhost) {
        return client
            .get()
            .uri("/aliveness-test/{vhost}", vhost)
            .retrieve()
            .bodyToMono(AlivenessTestResult.class);
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

    public Flux<ExchangeInfo> getExchanges() {
        return client
            .get()
            .uri("/exchanges")
            .retrieve()
            .bodyToFlux(ExchangeInfo.class);
    }

    public Flux<ExchangeInfo> getExchanges(String vhost) {
        return client
            .get()
            .uri("/exchanges/{vhost}", vhost)
            .retrieve()
            .bodyToFlux(ExchangeInfo.class);
    }

    public Flux<ExchangeInfo> getExchange(String vhost, String name) {
        return client
            .get()
            .uri("/exchanges/{vhost}/{name}", vhost, name)
            .retrieve()
            .bodyToFlux(ExchangeInfo.class);
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
        body.put("tags", StringUtils.collectionToCommaDelimitedString(tags));

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
        body.put("tags", StringUtils.collectionToCommaDelimitedString(tags));

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
        body.put("tags", StringUtils.collectionToCommaDelimitedString(tags));

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

    public Mono<ClientResponse> clearPermissions(String vhost, String username) {
        return client
            .delete()
            .uri("/permissions/{vhost}/{username}", vhost, username)
            .exchange();
    }

    public Mono<ClientResponse> declarePolicy(String vhost, String name, PolicyInfo info) {
        return client
            .put()
            .uri("/policies/{vhost}/{name}", vhost, name)
            .syncBody(info)
            .exchange();
    }

    public Flux<PolicyInfo> getPolicies() {
        return client
            .get()
            .uri("/policies")
            .retrieve()
            .bodyToFlux(PolicyInfo.class);
    }

    public Flux<PolicyInfo> getPolicies(String vhost) {
        return client
            .get()
            .uri("/policies/{vhost}", vhost)
            .retrieve()
            .bodyToFlux(PolicyInfo.class);
    }

    public Mono<ClientResponse> deletePolicy(String vhost, String name) {
        return client
            .delete()
            .uri("/policies/{vhost}/{name}", vhost, name)
            .exchange();
    }

    public Flux<BindingInfo> getBindings() {
        return client
            .get()
            .uri("/bindings")
            .retrieve()
            .bodyToFlux(BindingInfo.class);
    }

    public Flux<BindingInfo> getBindings(String vhost) {
        return client
            .get()
            .uri("/bindings/{vhost}", vhost)
            .retrieve()
            .bodyToFlux(BindingInfo.class);
    }

    public Flux<BindingInfo> getExchangeBindingsBySource(String vhost, String exchange) {
        final String x = exchange.equals("") ? "amq.default" : exchange;
        return client
            .get()
            .uri("/exchanges/{vhost}/{exchange}/bindings/source", vhost, x)
            .retrieve()
            .bodyToFlux(BindingInfo.class);
    }

    public Flux<BindingInfo> getExchangeBindingsByDestination(String vhost, String exchange) {
        final String x = exchange.equals("") ? "amq.default" : exchange;
        return client
            .get()
            .uri("/exchanges/{vhost}/{exchange}/bindings/destination", vhost, x)
            .retrieve()
            .bodyToFlux(BindingInfo.class);
    }

    public Flux<BindingInfo> getQueueBindings(String vhost, String queue) {
        return client
            .get()
            .uri("/queues/{vhost}/{queue}/bindings", vhost, queue)
            .retrieve()
            .bodyToFlux(BindingInfo.class);
    }

    public Flux<BindingInfo> getQueueBindingsBetween(String vhost, String exchange, String queue) {
        return client
            .get()
            .uri("/bindings/{vhost}/e/{exchange}/q/{queue}", vhost, exchange, queue)
            .retrieve()
            .bodyToFlux(BindingInfo.class);
    }

    public Flux<BindingInfo> getExchangeBindingsBetween(String vhost, String source, String destination) {
        return client
            .get()
            .uri("/bindings/{vhost}/e/{source}/e/{destination}", vhost, source, destination)
            .retrieve()
            .bodyToFlux(BindingInfo.class);
    }

    public Mono<ClusterId> getClusterName() {
        return client
            .get()
            .uri("/cluster-name")
            .retrieve()
            .bodyToMono(ClusterId.class);
    }

    public Mono<ClientResponse> setClusterName(String name) {
        if(name== null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or blank");
        }
        return client
            .put()
            .uri("/cluster-name")
            .syncBody(Collections.singletonMap("name", name))
            .exchange();
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    public Flux<Map> getExtensions() {
        return client
            .get()
            .uri("/extensions")
            .retrieve()
            .bodyToFlux(Map.class);
    }

    public Mono<Definitions> getDefinitions() {
        return client
            .get()
            .uri("/definitions")
            .retrieve()
            .bodyToMono(Definitions.class);
    }

    public Mono<ClientResponse> declareQueue(String vhost, String name, QueueInfo info) {
        return client
            .put()
            .uri("/queues/{vhost}/{name}", vhost, name)
            .syncBody(info)
            .exchange();
    }

    public Mono<ClientResponse> purgeQueue(String vhost, String name) {
        return client
            .delete()
            .uri("/queues/{vhost}/{name}/contents", vhost, name)
            .exchange();
    }

    public Mono<ClientResponse> deleteQueue(String vhost, String name) {
        return client
            .delete()
            .uri("/queues/{vhost}/{name}", vhost, name)
            .exchange();
    }

    public Mono<ClientResponse> declareExchange(String vhost, String name, ExchangeInfo info) {
        return client
            .put()
            .uri("/exchanges/{vhost}/{name}", vhost, name)
            .syncBody(info)
            .exchange();
    }

    public Mono<ClientResponse> deleteExchange(String vhost, String name) {
        return client
            .delete()
            .uri("/exchanges/{vhost}/{name}", vhost, name)
            .exchange();
    }

    public Flux<QueueInfo> getQueues() {
        return client
            .get()
            .uri("/queues")
            .retrieve()
            .bodyToFlux(QueueInfo.class);
    }

    public Flux<QueueInfo> getQueues(String vhost) {
        return client
            .get()
            .uri("/queues/{vhost}", vhost)
            .retrieve()
            .bodyToFlux(QueueInfo.class);
    }

    public Mono<QueueInfo> getQueue(String vhost, String name) {
        return client
            .get()
            .uri("/queues/{vhost}/{name}", vhost, name)
            .retrieve()
            .bodyToMono(QueueInfo.class);
    }

    public Mono<ClientResponse> bindQueue(String vhost, String queue, String exchange, String routingKey) {
        return bindQueue(vhost, queue, exchange, routingKey, new HashMap<>());
    }

    public Mono<ClientResponse> bindQueue(String vhost, String queue, String exchange, String routingKey, Map<String, Object> args) {
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

        return client
            .post()
            .uri("/bindings/{vhost}/e/{exchange}/q/{queue}", vhost, exchange, queue)
            .syncBody(body)
            .exchange();
    }

    public Mono<ClientResponse> bindExchange(String vhost, String destination, String source, String routingKey) {
        return bindExchange(vhost, destination, source, routingKey, new HashMap<>());
    }

    public Mono<ClientResponse> bindExchange(String vhost, String destination, String source, String routingKey, Map<String, Object> args) {
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

        return client
            .post()
            .uri("/bindings/{vhost}/e/{source}/e/{destination}", vhost, source, destination)
            .syncBody(body)
            .exchange();
    }

    public Mono<ClientResponse> declareShovel(String vhost, ShovelInfo info) {
        return client
            .put()
            .uri("/parameters/shovel/{vhost}/{name}", vhost, info.getName())
            .syncBody(info)
            .exchange();
    }

    public Flux<ShovelInfo> getShovels() {
        return client
            .get()
            .uri("/parameters/shovel")
            .retrieve()
            .bodyToFlux(ShovelInfo.class);
    }

    public Flux<ShovelStatus> getShovelsStatus() {
        return client
            .get()
            .uri("/shovels")
            .retrieve()
            .bodyToFlux(ShovelStatus.class);
    }

    public Mono<ClientResponse> deleteShovel(String vhost, String shovelName) {
        return client
            .delete()
            .uri("/parameters/shovel/{vhost}/{name}", vhost, shovelName)
            .exchange();
    }

}
