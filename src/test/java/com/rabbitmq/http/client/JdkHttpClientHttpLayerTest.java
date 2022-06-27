package com.rabbitmq.http.client;

import static com.rabbitmq.http.client.JdkHttpClientHttpLayer.maybeThrowClientServerException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rabbitmq.http.client.HttpLayer.HttpLayerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.URI;
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
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class JdkHttpClientHttpLayerTest {

  Server server;

  static boolean isJava13() {
    String javaVersion = System.getProperty("java.version");
    return javaVersion != null && javaVersion.startsWith("13.");
  }

  private static int randomNetworkPort() throws IOException {
    ServerSocket socket = new ServerSocket();
    socket.bind(null);
    int port = socket.getLocalPort();
    socket.close();
    return port;
  }

  private static AbstractHandler staticJsonHandler(String content) {
    return staticHandler(content, "application/json", new ConcurrentHashMap<>());
  }

  private static AbstractHandler staticJsonHandler(String content, Map<String, AtomicLong> calls) {
    return staticHandler(content, "application/json", calls);
  }

  private static AbstractHandler staticHandler(
      String content, String contentType, Map<String, AtomicLong> calls) {
    return new AbstractHandler() {
      @Override
      public void handle(
          String target,
          Request request,
          HttpServletRequest httpRequest,
          HttpServletResponse response)
          throws IOException {
        calls.computeIfAbsent(target, path -> new AtomicLong(0)).incrementAndGet();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentLength(content.length());
        response.setContentType(contentType);
        response.getWriter().print(content);
        request.setHandled(true);
      }
    };
  }

  private static Server startHttpServer(int port, Handler handler) throws Exception {
    Server server = new Server(port);
    Connector connector = new ServerConnector(server);
    server.addConnector(connector);
    server.setHandler(handler);
    server.start();
    return server;
  }

  @BeforeEach
  public void init() {
    if (isJava13()) {
      // for Java 13.0.7, see https://github.com/bcgit/bc-java/issues/941
      System.setProperty("keystore.pkcs12.keyProtectionAlgorithm", "PBEWithHmacSHA256AndAES_256");
    }
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (isJava13()) {
      System.setProperty("keystore.pkcs12.keyProtectionAlgorithm", "");
    }
    if (server != null) {
      server.stop();
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
    responseCodes.put("/client-error", Response.SC_FORBIDDEN);
    responseCodes.put("/client-not-found", Response.SC_NOT_FOUND);
    responseCodes.put("/server-error", Response.SC_SERVICE_UNAVAILABLE);
    Map<String, AtomicLong> calls = new ConcurrentHashMap<>();
    server =
        startHttpServer(
            port,
            new AbstractHandler() {
              @Override
              public void handle(
                  String target,
                  Request baseRequest,
                  HttpServletRequest request,
                  HttpServletResponse response) {
                calls.computeIfAbsent(target, path -> new AtomicLong(0)).incrementAndGet();
                response.setStatus(responseCodes.get(target));
                baseRequest.setHandled(true);
              }
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
    Handler httpHandler = staticJsonHandler("[]");
    KeyStore keyStore = startHttpsServer(port, httpHandler);
    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
    tmf.init(keyStore);
    SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
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

  KeyStore startHttpsServer(int port, Handler handler) throws Exception {
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

    server = new Server();
    SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
    sslContextFactory.setKeyStore(keyStore);
    sslContextFactory.setKeyStorePassword(keyStorePassword);

    HttpConfiguration httpsConfiguration = new HttpConfiguration();
    httpsConfiguration.setSecureScheme("https");
    httpsConfiguration.setSecurePort(port);
    httpsConfiguration.setOutputBufferSize(32768);

    SecureRequestCustomizer src = new SecureRequestCustomizer();
    src.setStsMaxAge(2000);
    src.setStsIncludeSubDomains(true);
    httpsConfiguration.addCustomizer(src);

    ServerConnector https =
        new ServerConnector(
            server,
            new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
            new HttpConnectionFactory(httpsConfiguration));
    https.setPort(port);
    https.setIdleTimeout(500000);

    server.setConnectors(new Connector[] {https});

    ContextHandler context = new ContextHandler();
    context.setContextPath("/");
    context.setHandler(handler);

    server.setHandler(context);

    server.start();
    return keyStore;
  }

  @Test
  void builderConsumers() throws Exception {
    int port = randomNetworkPort();
    Map<String, AtomicLong> calls = new ConcurrentHashMap<>();
    server = startHttpServer(port, staticJsonHandler("[]", calls));

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
