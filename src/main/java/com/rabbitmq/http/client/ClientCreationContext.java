/*
 * Copyright 2020 the original author or authors.
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

import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * Some information that can be useful when configuring a {@link Client}'s {@link RestTemplate}.
 *
 * @see RestTemplateConfigurator#configure(ClientCreationContext)
 * @since 3.6.0
 * @deprecated
 */
@Deprecated(since = "4.0.0", forRemoval = true)
public class ClientCreationContext {

    private RestTemplate restTemplate;
    private final ClientParameters clientParameters;
    private final URI rootUri;

    ClientCreationContext(RestTemplate restTemplate, ClientParameters clientParameters, URI rootUri) {
        this.restTemplate = restTemplate;
        this.clientParameters = clientParameters;
        this.rootUri = rootUri;
    }

    /**
     * The {@link RestTemplate} created by the {@link Client}.
     * <p>
     * {@link RestTemplateConfigurator#configure(ClientCreationContext)} can create a new
     * instance and return it, but it usually changes the passed-in instance.
     *
     * @return the REST template
     */
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    /**
     * {@link ClientParameters} used for this {@link Client} instance.
     * <p>
     * Useful for information like username and password.
     *
     * @return the client parameters used for the {@link Client} instance creation
     */
    public ClientParameters getClientParameters() {
        return clientParameters;
    }

    /**
     * The root URI of the management API.
     *
     * @return the root URI
     */
    public URI getRootUri() {
        return rootUri;
    }

    public ClientCreationContext restTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        return this;
    }
}
