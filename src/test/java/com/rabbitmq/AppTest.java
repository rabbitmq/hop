/*
 * Copyright 2025 the original author or authors.
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
package com.rabbitmq;

import static com.rabbitmq.http.client.JdkHttpClientHttpLayer.authorization;

import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.ClientParameters;
import com.rabbitmq.http.client.HttpLayer.HttpLayerFactory;
import com.rabbitmq.http.client.JdkHttpClientHttpLayer;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class AppTest {

  @Test
  void createUseClient() throws Exception {
    HttpLayerFactory httpLayerFactory =
        JdkHttpClientHttpLayer.configure()
            .clientBuilderConsumer(
                clientBuilder -> clientBuilder.connectTimeout(Duration.ofSeconds(10)))
            .requestBuilderConsumer(
                requestBuilder ->
                    requestBuilder
                        .timeout(Duration.ofSeconds(10))
                        .setHeader("Authorization", authorization("guest", "guest")))
            .create();

    Client c =
        new Client(
            new ClientParameters()
                .url("http://127.0.0.1:15672/api/")
                .username("guest")
                .password("guest")
                .httpLayerFactory(httpLayerFactory));

    c.getOverview();
  }
}
