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

import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Options for {@link ReactorNettyClient}.
 * An instance of this class can be passed in to {@link ReactorNettyClient}
 * constructor for settings like Jackson JSON object mapper, authentication,
 * TLS, error handling.
 *
 * @see ReactorNettyClient
 * @since 2.1.0
 */
public class ReactorNettyClientOptions {

    private Supplier<HttpClient> client;

    private Supplier<ObjectMapper> objectMapper;

    private Mono<String> token;

    private BiConsumer<? super HttpEndpoint, ? super HttpResponse> onResponseCallback;

    public Supplier<ObjectMapper> objectMapper() {
        return objectMapper;
    }

    public ReactorNettyClientOptions objectMapper(Supplier<ObjectMapper> objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public Mono<String> token() {
        return token;
    }

    public ReactorNettyClientOptions token(Mono<String> token) {
        this.token = token;
        return this;
    }

    public ReactorNettyClientOptions onResponseCallback(
        BiConsumer<? super HttpEndpoint, ? super HttpResponse> onResponseCallback) {
        this.onResponseCallback = onResponseCallback;
        return this;
    }

    public BiConsumer<? super HttpEndpoint, ? super HttpResponse> onResponseCallback() {
        return onResponseCallback;
    }

    public Supplier<HttpClient> client() {
        return client;
    }

    public ReactorNettyClientOptions client(Supplier<HttpClient> client) {
        this.client = client;
        return this;
    }
}
