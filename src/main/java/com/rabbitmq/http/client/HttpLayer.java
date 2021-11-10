/*
 * Copyright 2018-2020 the original author or authors.
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

import com.rabbitmq.http.client.JdkHttpClientHttpLayer.Factory;
import java.net.URI;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpRequest;
import java.util.Map;
import java.util.function.Consumer;

/**
 * HTTP layer abstraction for {@link Client}.
 *
 * <p>This is considered a SPI and is susceptible to change at any time.
 *
 * <p>Use the {@link #configure()} to configure and create the default implementation.
 *
 * @since 4.0.0
 */
public interface HttpLayer {

  /**
   * Configure the default {@link HttpLayer} implementation.
   *
   * @return a configuration instance
   */
  static Configuration configure() {
    return new Configuration();
  }

  /**
   * GET operation when the expected class does not use generics.
   *
   * @param uri
   * @param responseClass
   * @param <T>
   * @return
   */
  <T> T get(URI uri, Class<T> responseClass);

  /**
   * Get operation when the expected class uses generics, like <code>List&lt;T&gt;</code> or <code>
   * Page&lt;T&gt;</code>.
   *
   * @param uri
   * @param typeReference
   * @param <T>
   * @return
   */
  <T> T get(URI uri, ParameterizedTypeReference<T> typeReference);

  /**
   * POST operation.
   *
   * <p>Response class is optional.
   *
   * @param uri
   * @param requestBody
   * @param responseClass
   * @param <T>
   * @return
   */
  <T> T post(URI uri, Object requestBody, Class<T> responseClass);

  /**
   * PUT operation.
   *
   * @param uri
   * @param requestBody
   */
  void put(URI uri, Object requestBody);

  /**
   * DELETE operation.
   *
   * @param uri
   * @param headers
   */
  void delete(URI uri, Map<String, String> headers);

  /**
   * Contract to create the {@link HttpLayer}.
   *
   * <p>The {@link HttpLayer} usually requires credentials (username, password) that are available
   * once the {@link ClientParameters} instance is created. This explains the need for a factory
   * mechanism.
   *
   * @since 4.0.0
   */
  @FunctionalInterface
  interface HttpLayerFactory {

    HttpLayer create(ClientParameters parameters);
  }

  /**
   * Class to configure the default {@link HttpLayer} implementation.
   *
   * @see JdkHttpClientHttpLayer
   */
  class Configuration {

    private Consumer<Builder> clientBuilderConsumer = b -> {};
    private Consumer<HttpRequest.Builder> requestBuilderConsumer = null;

    /**
     * Callback to configure the {@link java.net.http.HttpClient.Builder}.
     *
     * @param clientBuilderConsumer
     * @return this configuration instance
     */
    public Configuration clientBuilderConsumer(Consumer<Builder> clientBuilderConsumer) {
      if (clientBuilderConsumer == null) {
        throw new IllegalArgumentException("Client builder consumer cannot be null");
      }
      this.clientBuilderConsumer = clientBuilderConsumer;
      return this;
    }

    /**
     * Callback to configure the {@link java.net.http.HttpRequest.Builder}.
     *
     * @param requestBuilderConsumer
     * @return the configuration instance
     */
    public Configuration requestBuilderConsumer(
        Consumer<HttpRequest.Builder> requestBuilderConsumer) {
      if (requestBuilderConsumer == null) {
        throw new IllegalArgumentException("Request builder consumer cannot be null");
      }
      this.requestBuilderConsumer = requestBuilderConsumer;
      return this;
    }

    HttpLayerFactory create() {
      return new Factory(this.clientBuilderConsumer, this.requestBuilderConsumer);
    }
  }
}
