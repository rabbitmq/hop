/*
 * Copyright 2021 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * {@link HttpLayer} using JDK 11's {@link HttpClient}.
 *
 * @since 4.0.0
 */
class JdkHttpClientHttpLayer implements HttpLayer {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

  private final HttpClient client;
  private final ObjectMapper mapper;
  private final Consumer<HttpRequest.Builder> requestBuilderConsumer;

  JdkHttpClientHttpLayer(
      HttpClient client,
      ObjectMapper mapper,
      Consumer<HttpRequest.Builder> requestBuilderConsumer) {
    this.client = client;
    this.mapper = mapper;
    this.requestBuilderConsumer = requestBuilderConsumer;
  }

  static <T> T get(
      URI uri,
      ObjectMapper mapper,
      HttpClient client,
      Consumer<HttpRequest.Builder> requestBuilderConsumer,
      Type type) {
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri);
    requestBuilder.header("accept", "application/json");
    requestBuilderConsumer.accept(requestBuilder);
    HttpRequest request = requestBuilder.GET().build();
    try {
      HttpResponse<Supplier<T>> response =
          client.send(request, new JsonBodyHandler<>(mapper, type));
      if (response.statusCode() == 404) {
        return null;
      } else {
        return response.body().get();
      }
    } catch (IOException e) {
      throw new HttpException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new HttpException(e);
    }
  }

  private static <W> java.net.http.HttpResponse.BodySubscriber<Supplier<W>> asJson(
      ObjectMapper mapper, Type type) {
    java.net.http.HttpResponse.BodySubscriber<InputStream> upstream =
        java.net.http.HttpResponse.BodySubscribers.ofInputStream();

    return java.net.http.HttpResponse.BodySubscribers.mapping(
        upstream, inputStream -> toSupplierOfType(mapper, inputStream, type));
  }

  private static <W> Supplier<W> toSupplierOfType(
      ObjectMapper mapper, InputStream inputStream, Type type) {
    return () -> {
      try (InputStream stream = inputStream) {
        return mapper.readValue(stream, mapper.constructType(type));
      } catch (IOException e) {
        throw new HttpException(e);
      }
    };
  }

  static void maybeThrowClientServerException(int statusCode, String message, int... ignores) {
    for (int ignore : ignores) {
      if (statusCode == ignore) {
        return;
      }
    }
    int errorClass = statusCode - statusCode % 100;
    if (errorClass == 400) {
      throw new HttpClientException(statusCode, message);
    } else if (errorClass == 500) {
      throw new HttpServerException(statusCode, message);
    }
  }

  @Override
  public <T> T get(URI uri, Class<T> responseClass) {
    return get(uri, this.mapper, this.client, this.requestBuilderConsumer, responseClass);
  }

  @Override
  public <T> T get(URI uri, ParameterizedTypeReference<T> typeReference) {
    return get(uri, this.mapper, this.client, this.requestBuilderConsumer, typeReference.getType());
  }

  @Override
  public <T> T post(URI uri, Object requestBody, Class<T> responseClass) {
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri);
    requestBuilder.header("content-type", "application/json");
    requestBuilderConsumer.accept(requestBuilder);
    try {
      HttpRequest request =
          requestBuilder
              .POST(BodyPublishers.ofByteArray(mapper.writeValueAsBytes(requestBody)))
              .build();
      if (responseClass == null) {
        HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
        int statusCode = response.statusCode();
        maybeThrowClientServerException(statusCode, "POST returned " + statusCode);
        return null;
      } else {
        HttpResponse<Supplier<T>> response =
            client.send(request, new JsonBodyHandler<>(mapper, responseClass));
        int statusCode = response.statusCode();
        maybeThrowClientServerException(statusCode, "POST returned " + statusCode);
        return response.body().get();
      }
    } catch (IOException e) {
      throw new HttpException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new HttpException(e);
    }
  }

  @Override
  public void put(URI uri, Object requestBody) {
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri);
    requestBuilder.header("content-type", "application/json");
    requestBuilderConsumer.accept(requestBuilder);
    try {
      HttpRequest request =
          requestBuilder
              .PUT(BodyPublishers.ofByteArray(mapper.writeValueAsBytes(requestBody)))
              .build();
      HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
      int statusCode = response.statusCode();
      maybeThrowClientServerException(statusCode, "PUT returned " + statusCode);
    } catch (IOException e) {
      throw new HttpException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new HttpException(e);
    }
  }

  @Override
  public void delete(URI uri, Map<String, String> headers) {
    headers = headers == null ? Collections.emptyMap() : headers;
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri);
    headers.forEach((n, v) -> requestBuilder.header(n, v));
    requestBuilderConsumer.accept(requestBuilder);
    try {
      HttpRequest request = requestBuilder.DELETE().build();
      HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
      int statusCode = response.statusCode();
      maybeThrowClientServerException(statusCode, "DELETE returned " + statusCode, 404);
    } catch (IOException e) {
      throw new HttpException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new HttpException(e);
    }
  }

  static class Factory implements HttpLayerFactory {

    private final Consumer<HttpClient.Builder> clientBuilderConsumer;
    private final Consumer<HttpRequest.Builder> requestBuilderConsumer;

    Factory(
        Consumer<HttpClient.Builder> clientBuilderConsumer,
        Consumer<HttpRequest.Builder> requestBuilderConsumer) {
      this.clientBuilderConsumer = clientBuilderConsumer;
      this.requestBuilderConsumer = requestBuilderConsumer;
    }

    @Override
    public HttpLayer create(ClientParameters parameters) {
      HttpClient.Builder builder =
          HttpClient.newBuilder()
              .version(Version.HTTP_1_1)
              .followRedirects(Redirect.NORMAL)
              .connectTimeout(Duration.ofSeconds(10));
      this.clientBuilderConsumer.accept(builder);
      HttpClient client = builder.build();
      ObjectMapper mapper = JsonUtils.createDefaultObjectMapper();
      String username = parameters.getUsername();
      String password = parameters.getPassword();
      Consumer<HttpRequest.Builder> requestBuilderConsumer;
      if (this.requestBuilderConsumer == null) {
        requestBuilderConsumer =
            requestBuilder ->
                requestBuilder
                    .timeout(REQUEST_TIMEOUT)
                    .setHeader("Authorization", "Basic " + Utils.base64(username + ":" + password));
      } else {
        requestBuilderConsumer = this.requestBuilderConsumer;
      }
      return new JdkHttpClientHttpLayer(client, mapper, requestBuilderConsumer);
    }

  }

  private static class JsonBodyHandler<W>
      implements java.net.http.HttpResponse.BodyHandler<Supplier<W>> {

    private final ObjectMapper mapper;
    private final Type type;

    public JsonBodyHandler(ObjectMapper mapper, Type type) {
      this.mapper = mapper;
      this.type = type;
    }

    @Override
    public java.net.http.HttpResponse.BodySubscriber<Supplier<W>> apply(
        HttpResponse.ResponseInfo responseInfo) {
      return asJson(mapper, type);
    }
  }
}
