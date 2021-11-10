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

import com.rabbitmq.http.client.HttpLayer.HttpLayerFactory;
import org.springframework.web.client.RestTemplate;

/**
 * An extension point to configure the {@link RestTemplate} used in a {@link Client} instance.
 * <p>
 * This is typically used to configure the {@link org.springframework.http.client.ClientHttpRequestFactory}
 * to use, and thus the library to create HTTP requests.
 * <p>
 * Hop provides support for <a href="https://hc.apache.org/">Apache HttpComponents</a> (the default),
 * <a href="https://square.github.io/okhttp/">OkHttp</a>, and standard JDK HTTP facilities.
 *
 * @see HttpComponentsRestTemplateConfigurator
 * @see OkHttpRestTemplateConfigurator
 * @see RestTemplate
 * @see org.springframework.http.client.ClientHttpRequestFactory
 * @since 3.6.0
 * @deprecated use {@link ClientParameters#httpLayerFactory(HttpLayerFactory)} instead
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@FunctionalInterface
public interface RestTemplateConfigurator {

    /**
     * Configure a {@link RestTemplate} instance and return it for use in the {@link Client}.
     *
     * @param context some context during client creation
     * @return the {@link RestTemplate} to use
     */
    RestTemplate configure(ClientCreationContext context);

}
