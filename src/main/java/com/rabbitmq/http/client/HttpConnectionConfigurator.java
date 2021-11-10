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
import java.net.HttpURLConnection;

/**
 * Implementations of this interface can perform post-configuration on the
 * {@link HttpURLConnection} used in the {@link Client}'s {@link org.springframework.web.client.RestTemplate}.
 * <p>
 * Note the {@link SimpleRestTemplateConfigurator} performs essential settings before the
 * configurator.
 *
 * @since 3.6.0
 * @deprecated use {@link ClientParameters#httpLayerFactory(HttpLayerFactory)} instead
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@FunctionalInterface
public interface HttpConnectionConfigurator {

    /**
     * Configure the {@link HttpURLConnection}
     *
     * @param connection the connection created by the {@link org.springframework.http.client.SimpleClientHttpRequestFactory}
     */
    void configure(HttpURLConnection connection);

}
