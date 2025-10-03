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
package com.rabbitmq.http.client;

import static com.rabbitmq.http.client.JdkHttpClientHttpLayer.maybeThrowClientServerException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rabbitmq.http.client.HttpLayer.HttpLayerFactory;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class JdkHttpClientHttpLayerTest {

  HttpServer server;

  private static int randomNetworkPort() throws IOException {
    ServerSocket socket = new ServerSocket();
    socket.bind(null);
    int port = socket.getLocalPort();
    socket.close();
    return port;
  }

  private static HttpHandler staticHandler(String content) {
    return staticHandler(content, "application/json", new ConcurrentHashMap<>());
  }

  private static HttpHandler staticHandler(String content, Map<String, AtomicLong> calls) {
    return staticHandler(content, "application/json", calls);
  }

  private static HttpHandler staticHandler(
      String content, String contentType, Map<String, AtomicLong> calls) {
    return exchange -> {
      String target = exchange.getRequestURI().getPath();
      calls.computeIfAbsent(target, path -> new AtomicLong(0)).incrementAndGet();
      exchange.getResponseHeaders().set("Content-Type", contentType);
      byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, bytes.length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(bytes);
      }
    };
  }

  private static HttpServer startHttpServer(int port, HttpHandler handler) throws Exception {
    com.sun.net.httpserver.HttpServer server =
        com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/", handler);
    server.start();
    return server;
  }

  @AfterEach
  public void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

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

  @Test
  void clientServerErrorsShouldTriggerClientServerExceptions() throws Exception {
    int port = randomNetworkPort();
    Map<String, Integer> responseCodes = new ConcurrentHashMap<>();
    responseCodes.put("/client-error", 403);
    responseCodes.put("/client-not-found", 404);
    responseCodes.put("/server-error", 503);
    Map<String, AtomicLong> calls = new ConcurrentHashMap<>();
    server =
        startHttpServer(
            port,
            exchange -> {
              String target = exchange.getRequestURI().getPath();
              calls.computeIfAbsent(target, path -> new AtomicLong(0)).incrementAndGet();
              Integer responseCode = responseCodes.get(target);
              byte[] body = responseCode.toString().getBytes(StandardCharsets.UTF_8);
              exchange.sendResponseHeaders(responseCode, body.length);
              OutputStream responseBody = exchange.getResponseBody();
              responseBody.write(body);
              responseBody.close();
            });
    HttpLayerFactory factory = JdkHttpClientHttpLayer.configure().create();
    HttpLayer httpLayer = factory.create(new ClientParameters());
    URI baseUri = new URI("http://localhost:" + port);
    assertThatThrownBy(() -> httpLayer.get(baseUri.resolve("/client-error"), String[].class))
        .isInstanceOf(HttpClientException.class);
    assertThat(calls.get("/client-error")).hasValue(1);

    assertThat(httpLayer.get(baseUri.resolve("/client-not-found"), String[].class)).isNull();
    assertThat(calls.get("/client-not-found")).hasValue(1);

    assertThatThrownBy(() -> httpLayer.get(baseUri.resolve("/server-error"), String[].class))
        .isInstanceOf(HttpServerException.class);
    assertThat(calls.get("/server-error")).hasValue(1);
  }

  @Test
  void tls() throws Exception {
    int port = randomNetworkPort();
    HttpHandler httpHandler = staticHandler("[]");
    KeyStore keyStore = startHttpsServer(port, httpHandler);
    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
    tmf.init(keyStore);
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, tmf.getTrustManagers(), null);
    HttpLayerFactory factory =
        JdkHttpClientHttpLayer.configure()
            .clientBuilderConsumer(builder -> builder.sslContext(sslContext))
            .requestBuilderConsumer(builder -> {})
            .create();
    HttpLayer httpLayer = factory.create(new ClientParameters());
    URI uri = new URI("https://localhost:" + port + "/foo");

    httpLayer.get(uri, String[].class);
  }

  private KeyStore startHttpsServer(int port, HttpHandler handler) throws Exception {
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    String keyStorePassword = "password";
    keyStore.load(null, keyStorePassword.toCharArray());

    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(2048);
    KeyPair kp = kpg.generateKeyPair();

    JcaX509v3CertificateBuilder certificateBuilder =
        new JcaX509v3CertificateBuilder(
            new X500NameBuilder().addRDN(BCStyle.CN, "localhost").build(),
            BigInteger.valueOf(new SecureRandom().nextInt()),
            Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
            Date.from(Instant.now().plus(10, ChronoUnit.DAYS)),
            new X500NameBuilder().addRDN(BCStyle.CN, "localhost").build(),
            kp.getPublic());

    X509CertificateHolder certificateHolder =
        certificateBuilder.build(
            new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(kp.getPrivate()));

    X509Certificate certificate =
        new JcaX509CertificateConverter().getCertificate(certificateHolder);

    keyStore.setKeyEntry(
        "default",
        kp.getPrivate(),
        keyStorePassword.toCharArray(),
        new Certificate[] {certificate});

    KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
    HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(port), 0);
    httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
    httpsServer.createContext("/", handler);
    httpsServer.start();
    server = httpsServer;
    return keyStore;
  }

  @Test
  void builderConsumers() throws Exception {
    int port = randomNetworkPort();
    Map<String, AtomicLong> calls = new ConcurrentHashMap<>();
    server = startHttpServer(port, staticHandler("[]", calls));

    AtomicBoolean clientBuilderConsumerCalled = new AtomicBoolean(false);
    AtomicBoolean requestBuilderConsumerCalled = new AtomicBoolean(false);
    HttpLayerFactory factory =
        JdkHttpClientHttpLayer.configure()
            .clientBuilderConsumer(builder -> clientBuilderConsumerCalled.set(true))
            .requestBuilderConsumer(builder -> requestBuilderConsumerCalled.set(true))
            .create();
    HttpLayer httpLayer = factory.create(new ClientParameters());
    URI uri = new URI("http://localhost:" + port + "/foo");
    httpLayer.get(uri, String[].class);
    assertThat(calls.get("/foo")).hasValue(1);
    assertThat(clientBuilderConsumerCalled).isTrue();
    assertThat(requestBuilderConsumerCalled).isTrue();
  }
}
