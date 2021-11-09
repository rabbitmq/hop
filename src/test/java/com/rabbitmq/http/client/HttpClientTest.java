package com.rabbitmq.http.client;

import static com.rabbitmq.http.client.Utils.encode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.TypeResolutionContext;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.http.client.domain.ConnectionInfo;
import com.rabbitmq.http.client.domain.ExchangeType;
import com.rabbitmq.http.client.domain.OverviewResponse;
import com.rabbitmq.http.client.domain.Page;
import com.rabbitmq.http.client.domain.QueryParameters;
import com.rabbitmq.http.client.domain.QueueInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class HttpClientTest {

  private static URI uriWithPath(URI rootUri, String path) {
    return rootUri.resolve(path);
  }

  private URI uriWithPath(URI rootUri, String path, QueryParameters queryParameters) {
    Map<String, String> parameters = queryParameters.parameters();
    if (parameters.isEmpty()) {
      return uriWithPath(rootUri, path);
    } else {
      String parametersString = parameters.entrySet().stream().map(entry ->
          entry.getKey() + "=" + Utils.encodeHttpParameter(entry.getValue())).collect(Collectors.joining("&"));
      return rootUri.resolve(path + "?" + parametersString);
    }
  }

  static String base64(String in) {
    return Base64.getEncoder().encodeToString(in.getBytes(StandardCharsets.UTF_8));
  }

  static <W> java.net.http.HttpResponse.BodySubscriber<Supplier<W>> asJson(
      ObjectMapper mapper, Type type) {
    java.net.http.HttpResponse.BodySubscriber<InputStream> upstream =
        java.net.http.HttpResponse.BodySubscribers.ofInputStream();

    return java.net.http.HttpResponse.BodySubscribers.mapping(
        upstream, inputStream -> toSupplierOfType(mapper, inputStream, type));
  }

  public static <W> Supplier<W> toSupplierOfType(
      ObjectMapper mapper, InputStream inputStream, Type type) {
    return () -> {
      try (InputStream stream = inputStream) {
        return mapper.readValue(stream, mapper.constructType(type));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    };
  }

  static <T> T get(
      URI rootUri, String path, ObjectMapper mapper, HttpClient client, Type type) {
    return get(uriWithPath(rootUri, path), mapper, client, type);
  }

  static <T> T get(
      URI uri, ObjectMapper mapper, HttpClient client, Type type) {
    Duration timeout = Duration.ofSeconds(60);
    Builder requestBuilder = HttpRequest.newBuilder(uri);
    requestBuilder = auth(requestBuilder, "guest", "guest");
    HttpRequest request =
        requestBuilder.timeout(timeout).header("accept", "application/json").GET().build();
    try {
      HttpResponse<Supplier<T>> connectionsResponse =
          client.send(request, new JsonBodyHandler<>(mapper, type));
      if (connectionsResponse.statusCode() == 404) {
        return null;
      } else {
        return connectionsResponse.body().get();
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static Builder auth(Builder builder, String username, String password) {
    return builder.setHeader("Authorization", "Basic " + base64(username + ":" + password));
  }

  public static Duration waitAtMost(CallableBooleanSupplier condition) throws Exception {
    return waitAtMost(10, condition, null);
  }

  static Duration waitAtMost(
      int timeoutInSeconds, CallableBooleanSupplier condition, Supplier<String> message)
      throws Exception {
    if (condition.getAsBoolean()) {
      return Duration.ZERO;
    }
    int waitTime = 100;
    int waitedTime = 0;
    int timeoutInMs = timeoutInSeconds * 1000;
    while (waitedTime <= timeoutInMs) {
      Thread.sleep(waitTime);
      waitedTime += waitTime;
      if (condition.getAsBoolean()) {
        return Duration.ofMillis(waitedTime);
      }
    }
    if (message == null) {
      fail("Waited " + timeoutInSeconds + " second(s), condition never got true");
    } else {
      fail("Waited " + timeoutInSeconds + " second(s), " + message.get());
    }
    return Duration.ofMillis(waitedTime);
  }

  @Test
  void httpClient() throws Exception {
    URL url = new URL(HttpLayerTest.url());
    URI rootUri;
    if (url.toString().endsWith("/")) {
      rootUri = url.toURI();
    } else {
      rootUri = new URL(url + "/").toURI();
    }
    HttpClient.Builder builder =
        HttpClient.newBuilder()
            .version(Version.HTTP_1_1)
            .followRedirects(Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20));
    HttpClient client = builder.build();

    Duration timeout = Duration.ofSeconds(60);
    ObjectMapper objectMapper = JsonUtils.createDefaultObjectMapper();

    OverviewResponse overview =
        get(rootUri, "./overview", objectMapper, client, OverviewResponse.class);

    assertThat(overview.getClusterName()).startsWith("rabbit@");
    assertThat(overview.getExchangeTypes().stream().map(ExchangeType::getName))
        .hasSizeGreaterThanOrEqualTo(4)
        .contains("topic", "fanout", "direct", "headers");

    Callable<ConnectionInfo[]> connectionsRequestCall =
        () -> get(rootUri, "./connections/", objectMapper, client, ConnectionInfo[].class);
    int initialConnectionCount = connectionsRequestCall.call().length;
    Collection<Connection> connections = new ArrayList<>();
    try {
      ConnectionFactory cf = new ConnectionFactory();
      for (int i = 0; i < 15; i++) {
        connections.add(cf.newConnection());
      }
      waitAtMost(() -> connectionsRequestCall.call().length == initialConnectionCount + connections.size());

      QueryParameters queryParameters = new QueryParameters()
          .pagination()
          .pageSize(10)
          .query();

      URI uri = uriWithPath(rootUri, "./connections/", queryParameters);
      TypeReference<Page<ConnectionInfo>> type = new TypeReference<>() {
      };
      Page<ConnectionInfo> page = get(uri, objectMapper, client, type.getType());
      assertThat(page.getFilteredCount()).isEqualTo(connections.size());
      assertThat(page.getItemCount()).isEqualTo(10);
      assertThat(page.getPageCount()).isEqualTo(2);
      assertThat(page.getTotalCount()).isGreaterThanOrEqualTo(page.getFilteredCount());
      assertThat(page.getPage()).isEqualTo(1);

    } finally {
      for (Connection connection : connections) {
        connection.close();
      }
      waitAtMost(() -> connectionsRequestCall.call().length == initialConnectionCount);
    }

    QueueInfo queueInfo = new QueueInfo(false, false, false);
    String path = "./queues/" + encode("/") + "/" + encode("hop.test");
    final URI uri = uriWithPath(rootUri, path);
    Builder requestBuilder = HttpRequest.newBuilder(uri);
    requestBuilder = auth(requestBuilder, "guest", "guest");
    HttpRequest request =
        requestBuilder
            .timeout(timeout)
            .PUT(BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(queueInfo)))
            .build();
    HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
    assertThat(response.statusCode()).isEqualTo(201);

    queueInfo = get(rootUri, path, objectMapper, client, QueueInfo.class);
    assertThat(queueInfo.getName()).isEqualTo("hop.test");

    requestBuilder = HttpRequest.newBuilder(uri);
    requestBuilder = auth(requestBuilder, "guest", "guest");
    request = requestBuilder.timeout(timeout).DELETE().build();
    response = client.send(request, BodyHandlers.discarding());
    assertThat(response.statusCode()).isEqualTo(204);

    queueInfo = get(rootUri, path, objectMapper, client, QueueInfo.class);
    assertThat(queueInfo).isNull();
  }

  @FunctionalInterface
  public interface CallableBooleanSupplier {
    boolean getAsBoolean() throws Exception;
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
