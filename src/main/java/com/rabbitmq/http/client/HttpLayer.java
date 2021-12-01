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

import java.net.URI;
import java.util.Map;

/**
 * HTTP layer abstraction for {@link Client}.
 *
 * <p>This is considered a SPI and is susceptible to change at any time.
 *
 * <p>Use the {@link JdkHttpClientHttpLayer#configure()} to configure and create the default implementation.
 *
 * @since 4.0.0
 */
public interface HttpLayer {

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

}
