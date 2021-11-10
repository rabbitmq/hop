package com.rabbitmq.http.client;

import static com.rabbitmq.http.client.JdkHttpClientHttpLayer.maybeThrowClientServerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class JdkHttpClientHttpLayerTest {

  @ParameterizedTest
  @ValueSource(ints = {404, 400})
  void maybeThrowClientServerExceptionClient(int code) {
    assertThatThrownBy(() -> maybeThrowClientServerException(code, String.valueOf(code)))
        .isInstanceOf(HttpClientException.class)
        .hasMessage(String.valueOf(code));
  }

  @ParameterizedTest
  @ValueSource(ints = {503, 500})
  void maybeThrowClientServerExceptionServer(int code) {
    assertThatThrownBy(() -> maybeThrowClientServerException(code, String.valueOf(code)))
        .isInstanceOf(HttpServerException.class)
        .hasMessage(String.valueOf(code));
  }

  @ParameterizedTest
  @ValueSource(ints = {101, 201, 301})
  void maybeThrowClientServerExceptionNoException(int code) {
    maybeThrowClientServerException(code, String.valueOf(code));
  }

  @Test
  void maybeThrowClientServerExceptionIgnore() {
    maybeThrowClientServerException(404, String.valueOf(404), 404);
    assertThatThrownBy(() -> maybeThrowClientServerException(403, String.valueOf(403), 404))
        .isInstanceOf(HttpClientException.class)
        .hasMessage(String.valueOf(403));
  }
}
