/*
 * Copyright 2018-2022 the original author or authors.
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

import static com.rabbitmq.http.client.TestUtils.isVersion36orLater;
import static com.rabbitmq.http.client.domain.DestinationType.EXCHANGE;
import static com.rabbitmq.http.client.domain.DestinationType.QUEUE;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AuthenticationFailureException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.http.client.domain.AckMode;
import com.rabbitmq.http.client.domain.BindingInfo;
import com.rabbitmq.http.client.domain.ChannelInfo;
import com.rabbitmq.http.client.domain.ClusterId;
import com.rabbitmq.http.client.domain.ConnectionInfo;
import com.rabbitmq.http.client.domain.ConsumerDetails;
import com.rabbitmq.http.client.domain.CurrentUserDetails;
import com.rabbitmq.http.client.domain.Definitions;
import com.rabbitmq.http.client.domain.DeleteQueueParameters;
import com.rabbitmq.http.client.domain.DetailsParameters;
import com.rabbitmq.http.client.domain.ExchangeInfo;
import com.rabbitmq.http.client.domain.ExchangeType;
import com.rabbitmq.http.client.domain.FeatureFlag;
import com.rabbitmq.http.client.domain.FeatureFlagStability;
import com.rabbitmq.http.client.domain.FeatureFlagState;
import com.rabbitmq.http.client.domain.InboundMessage;
import com.rabbitmq.http.client.domain.MessageStats;
import com.rabbitmq.http.client.domain.MqttVhostPortInfo;
import com.rabbitmq.http.client.domain.NodeInfo;
import com.rabbitmq.http.client.domain.ObjectTotals;
import com.rabbitmq.http.client.domain.OutboundMessage;
import com.rabbitmq.http.client.domain.OverviewResponse;
import com.rabbitmq.http.client.domain.PolicyInfo;
import com.rabbitmq.http.client.domain.QueueInfo;
import com.rabbitmq.http.client.domain.QueueTotals;
import com.rabbitmq.http.client.domain.ShovelDetails;
import com.rabbitmq.http.client.domain.ShovelInfo;
import com.rabbitmq.http.client.domain.ShovelStatus;
import com.rabbitmq.http.client.domain.TopicPermissions;
import com.rabbitmq.http.client.domain.UpstreamDetails;
import com.rabbitmq.http.client.domain.UpstreamInfo;
import com.rabbitmq.http.client.domain.UpstreamSetDetails;
import com.rabbitmq.http.client.domain.UpstreamSetInfo;
import com.rabbitmq.http.client.domain.UserInfo;
import com.rabbitmq.http.client.domain.UserPermissions;
import com.rabbitmq.http.client.domain.VhostInfo;
import com.rabbitmq.http.client.domain.VhostLimits;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

public class ReactorNettyClientTest {

  protected static final String DEFAULT_USERNAME = "guest";
  protected static final String DEFAULT_PASSWORD = "guest";

  protected ReactorNettyClient client;
  String brokerVersion;

  private final ConnectionFactory cf = initializeConnectionFactory();

  protected static ConnectionFactory initializeConnectionFactory() {
    ConnectionFactory cf = new ConnectionFactory();
    cf.setAutomaticRecoveryEnabled(false);
    return cf;
  }

  @BeforeEach
  void setUp() throws Exception {
    client = newLocalhostNodeClient();
    client.getConnections().toStream().forEach(c -> client.closeConnection(c.getName()).block());
    brokerVersion = client.getOverview().block().getServerVersion();
  }

  protected static ReactorNettyClient newLocalhostNodeClient() {
    return newLocalhostNodeClient(new ReactorNettyClientOptions());
  }

  protected static ReactorNettyClient newLocalhostNodeClient(ReactorNettyClientOptions options) {
    return new ReactorNettyClient(
        String.format(
            "http://%s:%s@127.0.0.1:%d/api", DEFAULT_USERNAME, DEFAULT_PASSWORD, managementPort()),
        options);
  }

  static int managementPort() {
    return System.getProperty("rabbitmq.management.port") == null
        ? 15672
        : Integer.valueOf(System.getProperty("rabbitmq.management.port"));
  }

  @Test
  void getApiOverview() throws Exception {
    // when: client requests GET /api/overview
    Connection conn = openConnection();
    Channel ch = conn.createChannel();
    for (int i = 0; i < 1000; i++) {
      ch.basicPublish("", "", null, null);
    }

    OverviewResponse res = client.getOverview().block();
    List<String> xts =
        res.getExchangeTypes().stream().map(ExchangeType::getName).collect(Collectors.toList());

    // then: the response is converted successfully
    assertThat(res.getNode()).startsWith("rabbit@");
    assertThat(res.getErlangVersion()).isNotNull();

    MessageStats msgStats = res.getMessageStats();
    assertThat(msgStats.getBasicPublish()).isGreaterThanOrEqualTo(0L);
    assertThat(msgStats.getPublisherConfirm()).isGreaterThanOrEqualTo(0L);
    assertThat(msgStats.getBasicDeliver()).isGreaterThanOrEqualTo(0L);
    assertThat(msgStats.getBasicReturn()).isGreaterThanOrEqualTo(0L);

    QueueTotals qTotals = res.getQueueTotals();
    assertThat(qTotals.getMessages()).isGreaterThanOrEqualTo(0L);
    assertThat(qTotals.getMessagesReady()).isGreaterThanOrEqualTo(0L);
    assertThat(qTotals.getMessagesUnacknowledged()).isGreaterThanOrEqualTo(0L);

    ObjectTotals oTotals = res.getObjectTotals();
    assertThat(oTotals.getConnections()).isGreaterThanOrEqualTo(0L);
    assertThat(oTotals.getChannels()).isGreaterThanOrEqualTo(0L);
    assertThat(oTotals.getExchanges()).isGreaterThanOrEqualTo(0L);
    assertThat(oTotals.getQueues()).isGreaterThanOrEqualTo(0L);
    assertThat(oTotals.getConsumers()).isGreaterThanOrEqualTo(0L);

    assertThat(res.getListeners()).hasSizeGreaterThanOrEqualTo(1);
    assertThat(res.getContexts()).hasSizeGreaterThanOrEqualTo(1);

    assertThat(xts).contains("topic", "fanout", "direct", "headers");

    // cleanup
    if (conn.isOpen()) {
      conn.close();
    }
  }

  @Test
  void userInfoDecoding() throws Exception {
    // when: username and password are encoded in the URL
    AtomicReference<String> authorization = new AtomicReference<>();
    HttpClient httpClient =
        HttpClient.create()
            .baseUrl("http://localhost:" + managementPort() + "/api/")
            .doAfterRequest(
                (request, connection) ->
                    authorization.set(request.requestHeaders().get("authorization")));

    ReactorNettyClient localClient =
        new ReactorNettyClient(
            "http://test+user:test%40password@localhost:" + managementPort() + "/api/",
            new ReactorNettyClientOptions().client(() -> httpClient));

    try {
      localClient.getOverview().block();
    } catch (Exception e) {
      // OK
    }

    // then: username and password are decoded before going into the request
    // the authorization header is the same as with the decoded credentials
    assertThat(authorization.get())
        .isEqualTo(ReactorNettyClient.basicAuthentication("test user", "test@password"));
  }

  @Test
  void getApiNodes() throws Exception {
    // when: client retrieves a list of cluster nodes
    Flux<NodeInfo> res = client.getNodes();
    NodeInfo node = res.blockFirst();

    // then: the list is returned
    assertThat(res.count().block()).isGreaterThanOrEqualTo(1L);
    verifyNode(node);
  }

  @Test
  void getApiNodesName() throws Exception {
    // when: client retrieves a list of cluster nodes
    Flux<NodeInfo> res = client.getNodes();
    String name = res.blockFirst().getName();
    NodeInfo node = client.getNode(name).block();

    // then: the list is returned
    assertThat(res.count().block()).isGreaterThanOrEqualTo(1L);
    verifyNode(node);
  }

  @Test
  void getApiConnections() throws Exception {
    // given: an open RabbitMQ client connection
    Connection conn = openConnection();

    // when: client retrieves a list of connections
    Flux<ConnectionInfo> res = awaitEventPropagation(() -> client.getConnections());
    ConnectionInfo fst = res.blockFirst();

    // then: the list is returned
    assertThat(res.count().block()).isGreaterThanOrEqualTo(1L);
    verifyConnectionInfo(fst);

    // cleanup
    conn.close();
  }

  @Test
  void getApiConnectionsName() throws Exception {
    // given: an open RabbitMQ client connection
    Connection conn = openConnection();

    // when: client retrieves connection info with the correct name
    Flux<ConnectionInfo> xs = awaitEventPropagation(() -> client.getConnections());
    ConnectionInfo x = client.getConnection(xs.blockFirst().getName()).block();

    // then: the info is returned
    verifyConnectionInfo(x);

    // cleanup
    conn.close();
  }

  @Test
  void getApiConnectionsNameWithClientProvidedName() throws Exception {
    // given: an open RabbitMQ client connection with client-provided name
    String s = "client-name";
    Connection conn = openConnection(s);

    // when: client retrieves connection info with the correct name
    Flux<ConnectionInfo> xs = awaitEventPropagation(() -> client.getConnections());

    ConnectionInfo x =
        client
            .getConnection(
                xs.filter(c -> s.equals(c.getClientProperties().getConnectionName()))
                    .blockFirst()
                    .getName())
            .block();

    // then: the info is returned
    verifyConnectionInfo(x);
    assertThat(x.getClientProperties().getConnectionName()).isEqualTo(s);

    // cleanup
    conn.close();
  }

  @Test
  void deleteApiConnectionsName() throws Exception {
    // given: an open RabbitMQ client connection
    CountDownLatch latch = new CountDownLatch(1);
    String s = "client-name";
    Connection conn = openConnection(s);

    conn.addShutdownListener(e -> latch.countDown());

    assertThat(conn.isOpen()).isTrue();

    // when: client closes the connection
    Flux<ConnectionInfo> xs = awaitEventPropagation(() -> client.getConnections());
    ConnectionInfo x =
        client
            .getConnection(
                xs.filter(c -> s.equals(c.getClientProperties().getConnectionName()))
                    .blockFirst()
                    .getName())
            .block();
    client.closeConnection(x.getName()).block();

    // and: some time passes
    assertThat(awaitOn(latch)).isTrue();

    // then: the connection is closed
    assertThat(conn.isOpen()).isFalse();

    // cleanup
    if (conn.isOpen()) {
      conn.close();
    }
  }

  @Test
  void deleteApiConnectionsNameWithAUserProvidedReason() throws Exception {
    // given: an open RabbitMQ client connection
    CountDownLatch latch = new CountDownLatch(1);
    String s = "client-name";
    Connection conn = openConnection(s);
    AtomicReference<String> closingMessage = new AtomicReference<>();
    conn.addShutdownListener(
        e -> {
          closingMessage.set(e.getMessage());
          latch.countDown();
        });
    assertThat(conn.isOpen()).isTrue();

    // when: client closes the connection
    Flux<ConnectionInfo> xs = awaitEventPropagation(() -> client.getConnections());
    ConnectionInfo x =
        client
            .getConnection(
                xs.filter(c -> s.equals(c.getClientProperties().getConnectionName()))
                    .blockFirst()
                    .getName())
            .block();
    String reason = "because reasons!";
    client.closeConnection(x.getName(), reason).block();

    // and: some time passes
    assertThat(awaitOn(latch)).isTrue();

    // then: the connection is closed and the reason has been propagated to the
    // connected client
    assertThat(conn.isOpen()).isFalse();
    assertThat(closingMessage.get()).contains(reason);

    // cleanup
    if (conn.isOpen()) {
      conn.close();
    }
  }

  @Test
  void getApiChannels() throws Exception {
    // given: an open RabbitMQ client connection with 1 channel
    Connection conn = openConnection();
    Channel ch = conn.createChannel();

    // when: client lists channels
    awaitEventPropagation(() -> client.getConnections());
    Flux<ChannelInfo> chs = awaitEventPropagation(() -> client.getChannels());
    ChannelInfo chi = chs.blockFirst();

    // then: the list is returned
    verifyChannelInfo(chi, ch);

    // cleanup
    if (conn.isOpen()) {
      conn.close();
    }
  }

  @Test
  void getApiConnectionsNameChannels() throws Exception {
    // given: an open RabbitMQ client connection with 1 channel
    String s = UUID.randomUUID().toString();
    Connection conn = openConnection(s);
    Channel ch = conn.createChannel();

    // when: client lists channels on that connection
    Flux<ConnectionInfo> xs = awaitEventPropagation(() -> client.getConnections());
    // applying filter as some previous connections can still show up the management
    // API
    List<ConnectionInfo> filtered =
        xs.toStream()
            .filter(it -> s.equals(it.getClientProperties().getConnectionName()))
            .collect(Collectors.toList());
    String cn = filtered.get(0).getName();

    Flux<ChannelInfo> chs = awaitEventPropagation(() -> client.getChannels(cn));
    ChannelInfo chi = chs.blockFirst();

    // then: the list is returned
    verifyChannelInfo(chi, ch);

    // cleanup
    if (conn.isOpen()) {
      conn.close();
    }
  }

  @Test
  void getApiChannelsName() throws Exception {
    // given: an open RabbitMQ client connection with 1 channel
    String s = UUID.randomUUID().toString();
    Connection conn = openConnection(s);
    Channel ch = conn.createChannel();

    // when: client retrieves channel info
    Flux<ConnectionInfo> xs = awaitEventPropagation(() -> client.getConnections());
    // applying filter as some previous connections can still show up the management
    // API
    List<ConnectionInfo> filtered =
        xs.toStream()
            .filter(it -> s.equals(it.getClientProperties().getConnectionName()))
            .collect(Collectors.toList());
    String cn = filtered.get(0).getName();
    ChannelInfo chs = awaitEventPropagation(() -> client.getChannels(cn)).blockFirst();

    ChannelInfo chi = client.getChannel(chs.getName()).block();

    // then: the info is returned
    verifyChannelInfo(chi, ch);

    // cleanup
    if (conn.isOpen()) {
      conn.close();
    }
  }

  @Test
  void getApiVhosts() throws Exception {
    // when: client retrieves a list of vhosts
    Flux<VhostInfo> vhs = client.getVhosts();
    VhostInfo vhi = vhs.blockFirst();

    // then: the info is returned
    verifyVhost(vhi, client.getOverview().block().getServerVersion());
  }

  @Test
  void getApiVhostsName() throws Exception {
    // when: client retrieves vhost info
    VhostInfo vhi = client.getVhost("/").block();

    // then: the info is returned
    verifyVhost(vhi, client.getOverview().block().getServerVersion());
  }

  @ParameterizedTest
  @MethodSource("providePutApiVhostsNameParameters")
  void putApiVhostsName(String name) throws Exception {
    // when: client creates a vhost named {name}
    client.createVhost(name).block();

    VhostInfo vhi = client.getVhost(name).block();

    // then: the vhost is created
    assertThat(vhi.getName()).isEqualTo(name);

    // cleanup
    client.deleteVhost(name).block();
  }

  static Stream<String> providePutApiVhostsNameParameters() {
    return Stream.of(
        "http-created",
        "http-created2",
        "http_created",
        "http created",
        "создан по хатэтэпэ",
        "creado a través de HTTP",
        "通过http",
        "HTTP를 통해 생성",
        "HTTPを介して作成",
        "created over http?",
        "created @ http API",
        "erstellt über http",
        "http पर बनाया",
        "ถูกสร้างขึ้นผ่าน HTTP",
        "±!@^&#*");
  }

  @Test
  void putApiVhostsNameWithMetadata() throws Exception {
    if (!isVersion38orLater()) return;
    // when: client creates a vhost with metadata
    String vhost = "vhost-with-metadata";
    client.deleteVhost(vhost).block();
    client
        .createVhost(vhost, true, "vhost description", "production", "application1", "realm1")
        .block();

    VhostInfo vhi = client.getVhost(vhost).block();

    // then: the vhost is created
    assertThat(vhi.getName()).isEqualTo(vhost);
    assertThat(vhi.getDescription()).isEqualTo("vhost description");
    assertThat(vhi.getTags()).hasSize(3);
    assertThat(vhi.getTags()).contains("production", "application1", "realm1");
    assertThat(vhi.isTracing()).isTrue();

    // cleanup
    if (isVersion38orLater()) {
      client.deleteVhost(vhost).block();
    }
  }

  @Test
  void deleteApiVhostsNameWhenVhostExists() throws Exception {
    // given: a vhost named hop-test-to-be-deleted
    String s = "hop-test-to-be-deleted";
    client.createVhost(s).block();

    // when: the vhost is deleted
    client.deleteVhost(s).block();

    // then: it no longer exists
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getVhost(s).block();
            });
    assertThat(exception.status()).isEqualTo(404);
  }

  @Test
  void getApiVhostsNameWhenVhostExistsAndResponseCallbackIsCustom() throws Exception {
    // given: a vhost with a random name and custom response callback
    String s = UUID.randomUUID().toString();
    AtomicBoolean called = new AtomicBoolean(false);
    ReactorNettyClientOptions options =
        new ReactorNettyClientOptions()
            .onResponseCallback((request, response) -> called.getAndSet(true));
    ReactorNettyClient c = newLocalhostNodeClient(options);

    // when: the client tries to retrieve the vhost infos
    Mono<VhostInfo> vhost = c.getVhost(s);

    // then: the result is empty and the response handling code has been called
    assertThat(vhost.hasElement().block()).isFalse();
    waitAtMostUntilTrue(5, called::get);
  }

  @Test
  void deleteApiVhostsNameWhenVhostDoesNotExist() throws Exception {
    // given: no vhost named hop-test-to-be-deleted
    String s = "hop-test-to-be-deleted";

    // when: the vhost is deleted
    HttpResponse response = client.deleteVhost(s).block();

    // then: the response is 404
    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void getApiVhostsNamePermissionsWhenVhostExists() throws Exception {
    // when: permissions for vhost / are listed
    String s = "/";
    Flux<UserPermissions> xs = client.getPermissionsIn(s);

    // then: they include permissions for the guest user
    UserPermissions x = xs.filter(perm -> perm.getUser().equals("guest")).blockFirst();
    assertThat(x.getRead()).isEqualTo(".*");
  }

  @Test
  void getApiVhostsNamePermissionsWhenVhostDoesNotExist() throws Exception {
    // when: permissions for vhost trololowut are listed
    String s = "trololowut";

    // then: flux throws an exception
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getPermissionsIn(s).blockFirst();
            });
    assertThat(exception.status()).isEqualTo(404);
  }

  @Test
  void getApiVhostsNameTopicPermissionsWhenVhostExists() throws Exception {
    if (!isVersion37orLater()) return;
    // when: topic permissions for vhost / are listed
    String s = "/";
    Flux<TopicPermissions> xs = client.getTopicPermissionsIn(s);

    // then: they include topic permissions for the guest user
    TopicPermissions x = xs.filter(perm -> perm.getUser().equals("guest")).blockFirst();
    assertThat(x.getExchange()).isEqualTo("amq.topic");
    assertThat(x.getRead()).isEqualTo(".*");
  }

  @Test
  void getApiVhostsNameTopicPermissionsWhenVhostDoesNotExist() throws Exception {
    if (!isVersion37orLater()) return;
    // when: permissions for vhost trololowut are listed
    String s = "trololowut";

    // then: flux throws an exception
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getTopicPermissionsIn(s).blockFirst();
            });
    assertThat(exception.status()).isEqualTo(404);
  }

  @Test
  void getApiUsers() throws Exception {
    // when: users are listed
    Flux<UserInfo> xs = client.getUsers();
    String version = client.getOverview().block().getServerVersion();

    // then: a list of users is returned
    UserInfo x = xs.filter(user -> user.getName().equals("guest")).blockFirst();
    assertThat(x.getName()).isEqualTo("guest");
    assertThat(x.getPasswordHash()).isNotNull();
    if (isVersion36orLater(version)) {
      assertThat(x.getHashingAlgorithm()).isNotNull();
    }
    assertThat(x.getTags()).contains("administrator");
  }

  @Test
  void getApiUsersNameWhenUserExists() throws Exception {
    // when: user guest if fetched
    UserInfo x = client.getUser("guest").block();
    String version = client.getOverview().block().getServerVersion();

    // then: user info returned
    assertThat(x.getName()).isEqualTo("guest");
    assertThat(x.getPasswordHash()).isNotNull();
    if (isVersion36orLater(version)) {
      assertThat(x.getHashingAlgorithm()).isNotNull();
    }
    assertThat(x.getTags()).contains("administrator");
  }

  @Test
  void getApiUsersNameWhenUserDoesNotExist() throws Exception {
    // when: user lolwut if fetched
    // then: mono throws exception
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getUser("lolwut").block();
            });
    assertThat(exception.status()).isEqualTo(404);
  }

  @Test
  void putApiUsersNameUpdatesUserTags() throws Exception {
    // given: user alt-user
    String u = "alt-user";
    client.deleteUser(u).subscribe(r -> {}, e -> {});
    client.createUser(u, u.toCharArray(), Arrays.asList("original", "management")).block();
    awaitEventPropagation();

    // when: alt-user's tags are updated
    client.updateUser(u, u.toCharArray(), Arrays.asList("management", "updated")).block();
    awaitEventPropagation();

    // and: alt-user info is reloaded
    UserInfo x = client.getUser(u).block();

    // then: alt-user has new tags
    assertThat(x.getTags()).contains("updated");
    assertThat(x.getTags()).doesNotContain("original");
  }

  @Test
  void deleteApiUsersName() throws Exception {
    // given: user alt-user
    String u = "alt-user";
    client.deleteUser(u).subscribe(r -> {}, e -> {});
    client.createUser(u, u.toCharArray(), Arrays.asList("original", "management")).block();
    awaitEventPropagation();

    // when: alt-user is deleted
    client.deleteUser(u).block();
    awaitEventPropagation();

    // and: alt-user info is reloaded
    // then: deleted user is gone
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getUser(u).block();
            });
    assertThat(exception.status()).isEqualTo(404);
  }

  @Test
  void getApiUsersNamePermissionsWhenUserExists() throws Exception {
    // when: permissions for user guest are listed
    String s = "guest";
    Flux<UserPermissions> xs = client.getPermissionsOf(s);

    // then: they include permissions for the guest user
    UserPermissions x = xs.filter(perm -> perm.getUser().equals("guest")).blockFirst();
    assertThat(x.getRead()).isEqualTo(".*");
  }

  @Test
  void getApiUsersNamePermissionsWhenUserDoesNotExist() throws Exception {
    // when: permissions for user trololowut are listed
    String s = "trololowut";

    // then: mono throws exception
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getPermissionsOf(s).blockFirst();
            });
    assertThat(exception.status()).isEqualTo(404);
  }

  @Test
  void getApiUsersNameTopicPermissionsWhenUserExists() throws Exception {
    if (!isVersion37orLater()) return;
    // when: topic permissions for user guest are listed
    String s = "guest";
    Flux<TopicPermissions> xs = client.getTopicPermissionsOf(s);

    // then: they include topic permissions for the guest user
    TopicPermissions x = xs.filter(perm -> perm.getUser().equals("guest")).blockFirst();
    assertThat(x.getExchange()).isEqualTo("amq.topic");
    assertThat(x.getRead()).isEqualTo(".*");
  }

  @Test
  void getApiUsersNameTopicPermissionsWhenUserDoesNotExist() throws Exception {
    if (!isVersion37orLater()) return;
    // when: permissions for user trololowut are listed
    String s = "trololowut";

    // then: mono throws exception
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getTopicPermissionsOf(s).blockFirst();
            });
    assertThat(exception.status()).isEqualTo(404);
  }

  @Test
  void putApiUsersNameWithABlankPasswordHash() throws Exception {
    // given: user alt-user with a blank password hash
    String u = "alt-user";
    // blank password hash means only authentication using alternative
    // authentication mechanisms such as x509 certificates is possible. MK.
    String h = "";
    client.deleteUser(u).subscribe(r -> {}, e -> {});
    client
        .createUserWithPasswordHash(u, h.toCharArray(), Arrays.asList("original", "management"))
        .block();
    client
        .updatePermissions("/", u, new UserPermissions(".*", ".*", ".*"))
        .flatMap(r -> Mono.just(r.getStatus()))
        .onErrorReturn(t -> "Connection closed prematurely".equals(t.getMessage()), 500)
        .block();

    // when: alt-user tries to connect with a blank password
    // then: connection is refused
    // it would have a chance of being accepted if the x509 authentication mechanism
    // was used. MK.
    Exception ex =
        assertThrows(
            Exception.class,
            () -> {
              openConnection("alt-user", "alt-user");
            });
    assertThat(ex instanceof AuthenticationFailureException || ex instanceof IOException).isTrue();

    // cleanup
    client.deleteUser(u).block();
  }

  @Test
  void getApiExchanges() throws Exception {
    // when: client retrieves the list of exchanges across all vhosts
    Flux<ExchangeInfo> xs = client.getExchanges();
    ExchangeInfo x = xs.blockFirst();

    // then: the list is returned
    verifyExchangeInfo(x);
  }

  @Test
  void getApiWhoami() throws Exception {
    // when: client retrieves active name authentication details
    CurrentUserDetails res = client.whoAmI().block();

    // then: the details are returned
    assertThat(res.getName()).isEqualTo(DEFAULT_USERNAME);
    assertThat(res.getTags()).contains("administrator");
  }

  @Test
  void getApiPermissions() throws Exception {
    // when: all permissions are listed
    String s = "guest";
    Flux<UserPermissions> xs = client.getPermissions();

    // then: they include permissions for user guest in vhost /
    UserPermissions x =
        xs.filter(perm -> perm.getVhost().equals("/") && perm.getUser().equals(s)).blockFirst();
    assertThat(x.getRead()).isEqualTo(".*");
  }

  @Test
  void getApiPermissionsVhostUserWhenBothVhostAndUserExist() throws Exception {
    // when: permissions of user guest in vhost / are listed
    String u = "guest";
    String v = "/";
    UserPermissions x = client.getPermissions(v, u).block();

    // then: a single permissions object is returned
    assertThat(x.getRead()).isEqualTo(".*");
  }

  @Test
  void getApiTopicPermissions() throws Exception {
    if (!isVersion37orLater()) return;
    // when: all topic permissions are listed
    String s = "guest";
    Flux<TopicPermissions> xs = client.getTopicPermissions();

    // then: they include topic permissions for user guest in vhost /
    TopicPermissions x =
        xs.filter(perm -> perm.getVhost().equals("/") && perm.getUser().equals(s)).blockFirst();
    assertThat(x.getExchange()).isEqualTo("amq.topic");
    assertThat(x.getRead()).isEqualTo(".*");
  }

  @Test
  void getApiTopicPermissionsVhostUserWhenBothVhostAndUserExist() throws Exception {
    if (!isVersion37orLater()) return;
    // when: topic permissions of user guest in vhost / are listed
    String u = "guest";
    String v = "/";
    Flux<TopicPermissions> xs = client.getTopicPermissions(v, u);

    // then: a list of permissions objects is returned
    TopicPermissions x =
        xs.filter(perm -> perm.getVhost().equals(v) && perm.getUser().equals(u)).blockFirst();
    assertThat(x.getExchange()).isEqualTo("amq.topic");
    assertThat(x.getRead()).isEqualTo(".*");
  }

  @Test
  void getApiExchangesVhostWhenVhostExists() throws Exception {
    // when: client retrieves the list of exchanges in a particular vhost
    Flux<ExchangeInfo> xs = client.getExchanges("/");

    // then: the list is returned
    Flux<ExchangeInfo> x = xs.filter(e -> e.getName().equals("amq.fanout"));
    verifyExchangeInfo(x.blockFirst());
  }

  @Test
  void getApiPermissionsVhostUserWhenVhostDoesNotExist() throws Exception {
    // when: permissions of user guest in vhost lolwut are listed
    String u = "guest";
    String v = "lolwut";

    // then: mono throws exception
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getPermissions(v, u).block();
            });
    assertThat(exception.status()).isEqualTo(404);
  }

  @Test
  void getApiPermissionsVhostUserWhenUsernameDoesNotExist() throws Exception {
    // when: permissions of user lolwut in vhost / are listed
    String u = "lolwut";
    String v = "/";

    // then: mono throws exception
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getPermissions(v, u).block();
            });
    assertThat(exception.status()).isEqualTo(404);
  }

  @Test
  void putApiPermissionsVhostUserWhenBothUserAndVhostExist() throws Exception {
    // given: vhost hop-vhost1 exists
    String v = "hop-vhost1";
    client.createVhost(v).block();
    // and: user hop-user1 exists
    String u = "hop-user1";
    client
        .createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker"))
        .block();

    // when: permissions of user guest in vhost / are updated
    client.updatePermissions(v, u, new UserPermissions("read", "write", "configure")).block();

    // and: permissions are reloaded
    UserPermissions x = client.getPermissions(v, u).block();

    // then: a single permissions object is returned
    assertThat(x.getRead()).isEqualTo("read");
    assertThat(x.getWrite()).isEqualTo("write");
    assertThat(x.getConfigure()).isEqualTo("configure");

    // cleanup
    client.deleteVhost(v).block();
    client.deleteUser(u).block();
  }

  @Test
  void putApiPermissionsVhostUserWhenVhostDoesNotExist() throws Exception {
    // given: vhost hop-vhost1 DOES NOT exist
    String v = "hop-vhost1";
    client.deleteVhost(v).block();
    // and: user hop-user1 exists
    String u = "hop-user1";
    client
        .createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker"))
        .block();

    // when: permissions of user guest in vhost / are updated
    // throws an exception for RabbitMQ 3.7.4+
    // because of the way Cowboy 2.2.2 handles chunked transfer-encoding
    // so we handle both 404 and the error
    Integer status =
        client
            .updatePermissions(v, u, new UserPermissions("read", "write", "configure"))
            .flatMap(r -> Mono.just(r.getStatus()))
            .onErrorReturn(
                t -> ("Connection prematurely closed BEFORE response".equals(t.getMessage())), 500)
            .block();

    // then: HTTP status is 400 BAD REQUEST or exception is thrown
    assertThat(status == 400 || status == 500).isTrue();

    // cleanup
    client.deleteUser(u).block();
  }

  @Test
  void deleteApiPermissionsVhostUserWhenBothVhostAndUsernameExist() throws Exception {
    // given: vhost hop-vhost1 exists
    String v = "hop-vhost1";
    client.createVhost(v).block();
    // and: user hop-user1 exists
    String u = "hop-user1";
    client
        .createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker"))
        .block();

    // and: permissions of user guest in vhost / are set
    client.updatePermissions(v, u, new UserPermissions("read", "write", "configure")).block();
    UserPermissions x = client.getPermissions(v, u).block();
    assertThat(x.getRead()).isEqualTo("read");

    // when: permissions are cleared
    client.clearPermissions(v, u).block();

    // then: an exception is thrown on reload
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getPermissions(v, u).block();
            });
    assertThat(exception.status()).isEqualTo(404);

    // cleanup
    client.deleteVhost(v).block();
    client.deleteUser(u).block();
  }

  @Test
  void getApiTopicPermissionsVhostUserWhenVhostDoesNotExist() throws Exception {
    if (!isVersion37orLater()) return;
    // when: topic permissions of user guest in vhost lolwut are listed
    String u = "guest";
    String v = "lolwut";

    // then: mono throws exception
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getTopicPermissions(v, u).blockFirst();
            });
    assertThat(exception.status()).isEqualTo(404);
  }

  @Test
  void getApiTopicPermissionsVhostUserWhenUsernameDoesNotExist() throws Exception {
    if (!isVersion37orLater()) return;
    // when: topic permissions of user lolwut in vhost / are listed
    String u = "lolwut";
    String v = "/";

    // then: mono throws exception
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getTopicPermissions(v, u).blockFirst();
            });
    assertThat(exception.status()).isEqualTo(404);
  }

  @Test
  void putApiTopicPermissionsVhostUserWhenBothUserAndVhostExist() throws Exception {
    // given: vhost hop-vhost1 exists
    String v = "hop-vhost1";
    client.createVhost(v).block();
    // and: user hop-user1 exists
    String u = "hop-user1";
    client
        .createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker"))
        .block();

    if (!isVersion37orLater()) return;

    // when: topic permissions of user guest in vhost / are updated
    client.updateTopicPermissions(v, u, new TopicPermissions("amq.topic", "read", "write")).block();

    // and: permissions are reloaded
    TopicPermissions x = client.getTopicPermissions(v, u).blockFirst();

    // then: a list with a single topic permissions object is returned
    assertThat(x.getExchange()).isEqualTo("amq.topic");
    assertThat(x.getRead()).isEqualTo("read");
    assertThat(x.getWrite()).isEqualTo("write");

    // cleanup
    client.deleteVhost(v).block();
    client.deleteUser(u).block();
  }

  @Test
  void putApiTopicPermissionsVhostUserWhenVhostDoesNotExist() throws Exception {
    // given: vhost hop-vhost1 DOES NOT exist
    String v = "hop-vhost1";
    client.deleteVhost(v).block();
    // and: user hop-user1 exists
    String u = "hop-user1";
    client
        .createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker"))
        .block();

    if (!isVersion37orLater()) return;

    // when: permissions of user guest in vhost / are updated
    // throws an exception for RabbitMQ 3.7.4+
    // because of the way Cowboy 2.2.2 handles chunked transfer-encoding
    // so we handle both 404 and the error
    Integer status =
        client
            .updateTopicPermissions(v, u, new TopicPermissions("amq.topic", "read", "write"))
            .flatMap(r -> Mono.just(r.getStatus()))
            .onErrorReturn(
                t -> ("Connection prematurely closed BEFORE response".equals(t.getMessage())), 500)
            .block();

    // then: HTTP status is 400 BAD REQUEST or exception is thrown
    assertThat(status == 400 || status == 500).isTrue();

    // cleanup
    client.deleteUser(u).block();
  }

  @Test
  void deleteApiTopicPermissionsVhostUserWhenBothVhostAndUsernameExist() throws Exception {
    // given: vhost hop-vhost1 exists
    String v = "hop-vhost1";
    client.createVhost(v).block();
    // and: user hop-user1 exists
    String u = "hop-user1";
    client
        .createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker"))
        .block();

    if (!isVersion37orLater()) return;

    // and: permissions of user guest in vhost / are set
    client.updateTopicPermissions(v, u, new TopicPermissions("amq.topic", "read", "write")).block();
    TopicPermissions x = client.getTopicPermissions(v, u).blockFirst();
    assertThat(x.getExchange()).isEqualTo("amq.topic");
    assertThat(x.getRead()).isEqualTo("read");

    // when: permissions are cleared
    client.clearTopicPermissions(v, u).block();

    // then: an exception is thrown on reload
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getTopicPermissions(v, u).blockFirst();
            });
    assertThat(exception.status()).isEqualTo(404);

    // cleanup
    client.deleteVhost(v).block();
    client.deleteUser(u).block();
  }

  @Test
  void getApiConsumers() throws Exception {
    // given: at least one queue with an online consumer
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();
    String consumerTag =
        ch.basicConsume(
            q,
            true,
            (_ctag, _msg) -> {
              // do nothing
            },
            _ctag -> {});

    // when: client lists consumers
    Flux<ConsumerDetails> cs = awaitEventPropagation(() -> client.getConsumers());

    // then: a flux of consumers is returned
    ConsumerDetails cons =
        cs.collectList().block().stream()
            .filter(it -> it.getConsumerTag().equals(consumerTag))
            .findFirst()
            .orElse(null);
    assertThat(cons).isNotNull();

    // cleanup
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiConsumersVhost() throws Exception {
    // given: at least one queue with an online consumer in a given virtual host
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();
    String consumerTag =
        ch.basicConsume(
            q,
            true,
            (_ctag, _msg) -> {
              // do nothing
            },
            _ctag -> {});
    waitAtMostUntilTrue(10, () -> client.getConsumers().hasElements().block());

    // when: client lists consumers in a specific virtual host
    Flux<ConsumerDetails> cs = awaitEventPropagation(() -> client.getConsumers("/"));

    // then: a list of consumers in that virtual host is returned
    ConsumerDetails cons =
        cs.collectList().block().stream()
            .filter(it -> it.getConsumerTag().equals(consumerTag))
            .findFirst()
            .orElse(null);
    assertThat(cons).isNotNull();

    // cleanup
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiConsumersVhostWithNoConsumersInVhost() throws Exception {
    // given: at least one queue with an online consumer in a given virtual host
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();
    ch.basicConsume(
        q,
        true,
        (_ctag, _msg) -> {
          // do nothing
        },
        _ctag -> {});
    waitAtMostUntilTrue(10, () -> client.getConsumers().hasElements().block());

    // when: client lists consumers in another virtual host
    Flux<ConsumerDetails> cs = client.getConsumers("vh1");

    // then: no consumers are returned
    assertThat(cs.hasElements().block()).isFalse();

    // cleanup
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiPolicies() throws Exception {
    // given: at least one policy was declared
    String v = "/";
    String s = "hop.test";
    Map<String, Object> d = new HashMap<>();
    String p = ".*";
    d.put("expires", 30000);
    client.declarePolicy(v, s, new PolicyInfo(p, 0, null, d)).block();

    // when: client lists policies
    Flux<PolicyInfo> xs = awaitEventPropagation(() -> client.getPolicies());

    // then: a list of policies is returned
    PolicyInfo x = xs.blockFirst();
    verifyPolicyInfo(x);

    // cleanup
    client.deletePolicy(v, s).block();
  }

  @Test
  void getApiPoliciesVhostWhenVhostExists() throws Exception {
    // given: at least one policy was declared in vhost /
    String v = "/";
    String s = "hop.test";
    Map<String, Object> d = new HashMap<>();
    String p = ".*";
    d.put("expires", 30000);
    client.declarePolicy(v, s, new PolicyInfo(p, 0, null, d)).block();

    // when: client lists policies
    Flux<PolicyInfo> xs = awaitEventPropagation(() -> client.getPolicies("/"));

    // then: a list of queues is returned
    PolicyInfo x = xs.blockFirst();
    verifyPolicyInfo(x);

    // cleanup
    client.deletePolicy(v, s).block();
  }

  @Test
  void getApiPoliciesVhostWhenVhostDoesNotExists() throws Exception {
    // given: vhost lolwut DOES not exist
    String v = "lolwut";
    client.deleteVhost(v).block();

    // when: client lists policies
    // then: exception is thrown
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              awaitEventPropagation(() -> client.getPolicies(v));
            });
    assertThat(exception.status()).isEqualTo(404);
  }

  @Test
  void getApiOperatorPolicies() throws Exception {
    // given: at least one operator policy was declared
    String v = "/";
    String s = "hop.test";
    Map<String, Object> d = new HashMap<>();
    String p = ".*";
    d.put("max-length", 6);
    client.declareOperatorPolicy(v, s, new PolicyInfo(p, 0, null, d)).block();

    // when: client lists policies
    Flux<PolicyInfo> xs = awaitEventPropagation(() -> client.getOperatorPolicies());

    // then: a list of policies is returned
    PolicyInfo x = xs.blockFirst();
    verifyPolicyInfo(x);

    // cleanup
    client.deleteOperatorPolicy(v, s).block();
  }

  @Test
  void getApiOperatorPoliciesVhostWhenVhostExists() throws Exception {
    // given: at least one operator policy was declared in vhost /
    String v = "/";
    String s = "hop.test";
    Map<String, Object> d = new HashMap<>();
    String p = ".*";
    d.put("max-length", 6);
    client.declareOperatorPolicy(v, s, new PolicyInfo(p, 0, null, d)).block();

    // when: client lists policies
    Flux<PolicyInfo> xs = awaitEventPropagation(() -> client.getOperatorPolicies("/"));

    // then: a list of queues is returned
    PolicyInfo x = xs.blockFirst();
    verifyPolicyInfo(x);

    // cleanup
    client.deleteOperatorPolicy(v, s).block();
  }

  @Test
  void getApiOperatorPoliciesVhostWhenVhostDoesNotExists() throws Exception {
    // given: vhost lolwut DOES not exist
    String v = "lolwut";
    client.deleteVhost(v).block();

    // when: client lists operator policies
    // then: exception is thrown
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              awaitEventPropagation(() -> client.getOperatorPolicies(v));
            });
    assertThat(exception.status()).isEqualTo(404);
  }

  @Test
  void getApiAlivenessTestVhost() throws Exception {
    // when: client performs aliveness check for the / vhost
    boolean hasSucceeded = client.alivenessTest("/").block().isSuccessful();

    // then: the check succeeds
    assertThat(hasSucceeded).isTrue();
  }

  @Test
  void getApiClusterName() throws Exception {
    // when: client fetches cluster name
    ClusterId s = client.getClusterName().block();

    // then: cluster name is returned
    assertThat(s.getName()).isNotNull();
  }

  @Test
  void putApiClusterName() throws Exception {
    // given: cluster name
    String s = client.getClusterName().block().getName();

    // when: cluster name is set to rabbit@warren
    client.setClusterName("rabbit@warren").block();

    // and: cluster name is reloaded
    String x = client.getClusterName().block().getName();

    // then: the name is updated
    assertThat(x).isEqualTo("rabbit@warren");

    // cleanup
    client.setClusterName(s).block();
  }

  @Test
  void getApiExtensions() throws Exception {
    // given: a node with the management plugin enabled
    // when: client requests a list of (plugin) extensions
    Flux<Map> xs = client.getExtensions();

    // then: a list of extensions is returned
    assertThat(xs.hasElements().block()).isTrue();
  }

  @Test
  void getApiDefinitionsVersionVhostsUsersPermissionsTopicPermissions() throws Exception {
    // when: client requests the definitions
    Definitions d = client.getDefinitions().block();

    // then: broker definitions are returned
    assertThat(d.getServerVersion()).isNotNull();
    assertThat(d.getServerVersion().trim()).isNotEmpty();
    assertThat(d.getVhosts()).isNotEmpty();
    assertThat(d.getVhosts().get(0).getName()).isNotEmpty();
    assertThat(d.getUsers()).isNotEmpty();
    assertThat(d.getUsers().get(0).getName()).isNotNull();
    assertThat(d.getUsers().get(0).getName()).isNotEmpty();
    assertThat(d.getPermissions()).isNotEmpty();
    assertThat(d.getPermissions().get(0).getUser()).isNotNull();
    assertThat(d.getPermissions().get(0).getUser()).isNotEmpty();
    if (isVersion37orLater()) {
      assertThat(d.getTopicPermissions().get(0).getUser()).isNotNull();
      assertThat(d.getTopicPermissions().get(0).getUser()).isNotEmpty();
    }
  }

  @Test
  void getApiQueues() throws Exception {
    // given: at least one queue was declared
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();

    // when: client lists queues
    Flux<QueueInfo> xs = client.getQueues();

    // then: a list of queues is returned
    QueueInfo x = xs.blockFirst();
    verifyQueueInfo(x);

    // cleanup
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiQueuesVhostNameWithPolicyDefinition() throws Exception {
    // given: a policy applies to all queues
    String v = "/";
    String s = "hop.test";
    Map<String, Object> pd = singletonMap("expires", 30000);
    PolicyInfo pi = new PolicyInfo(".*", 1, "queues", pd);
    client.declarePolicy(v, s, pi).block();

    // when: a queue is created
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();

    // then: the policy definition should be listed in the queue info
    waitAtMostUntilTrue(
        10,
        () -> {
          QueueInfo qi = client.getQueue(v, q).block();
          return qi != null && qi.getEffectivePolicyDefinition() != null;
        });
    QueueInfo queueInfo = client.getQueue(v, q).block();
    assertThat(queueInfo.getEffectivePolicyDefinition()).isEqualTo(pd);

    // cleanup
    ch.queueDelete(q);
    conn.close();
    client.deletePolicy(v, s).block();
  }

  @Test
  void getApiQueuesWithDetails() throws Exception {
    // given: at least one queue was declared and some messages published
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Runnable publishing =
        () -> {
          while (!Thread.currentThread().isInterrupted()) {
            try {
              ch.basicPublish("", q, null, "".getBytes(StandardCharsets.UTF_8));
              Thread.sleep(10);
            } catch (InterruptedException e) {
              return;
            } catch (Exception e) {
              // ignore
            }
          }
        };
    executorService.submit(publishing);

    // when: client lists queues with details
    DetailsParameters detailsParameters =
        new DetailsParameters().messageRates(60, 5).lengths(60, 5);
    Supplier<Flux<QueueInfo>> request =
        () -> client.getQueues(detailsParameters).filter(qi -> qi.getName().equals(q));
    waitAtMostUntilTrue(
        10,
        () -> {
          QueueInfo p = request.get().blockFirst();
          return p != null
              && p.getMessagesDetails() != null
              && p.getMessagesDetails().getRate() > 0;
        });
    Flux<QueueInfo> xs = request.get();

    // then: a list of queues with details is returned
    QueueInfo x = xs.blockFirst();
    verifyQueueInfo(x);
    assertThat(x.getMessagesDetails()).isNotNull();
    assertThat(x.getMessagesDetails().getAverage()).isGreaterThan(0);
    assertThat(x.getMessagesDetails().getAverageRate()).isGreaterThan(0);
    assertThat(x.getMessagesDetails().getSamples()).hasSizeGreaterThan(0);
    assertThat(x.getMessagesDetails().getSamples().get(0).getSample()).isGreaterThan(0);
    assertThat(x.getMessagesDetails().getSamples().get(0).getTimestamp()).isGreaterThan(0);

    // cleanup
    executorService.shutdownNow();
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiQueuesVhostWhenVhostExists() throws Exception {
    // given: at least one queue was declared in vhost /
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();

    // when: client lists queues
    Flux<QueueInfo> xs = client.getQueues("/");

    // then: a list of queues is returned
    QueueInfo x = xs.blockFirst();
    verifyQueueInfo(x);

    // cleanup
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiQueuesVhostWhenVhostDoesNotExist() throws Exception {
    // given: vhost lolwut DOES not exist
    String v = "lolwut";
    client.deleteVhost(v).block();

    // when: client lists queues
    // then: exception is thrown
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getQueues(v).blockFirst();
            });
    assertThat(exception.status()).isEqualTo(404);
  }

  @Test
  void getApiQueuesVhostNameWhenBothVhostAndQueueExist() throws Exception {
    // given: a queue was declared in vhost /
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();

    // when: client fetches info of the queue
    QueueInfo x = client.getQueue("/", q).block();

    // then: the info is returned
    assertThat(x.getVhost()).isEqualTo("/");
    assertThat(x.getName()).isEqualTo(q);
    verifyQueueInfo(x);

    // cleanup
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiQueuesVhostNameWithAnExclusiveQueue() throws Exception {
    // given: an exclusive queue named hop.q1.exclusive
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String s = "hop.q1.exclusive";
    ch.queueDelete(s);
    String q = ch.queueDeclare(s, false, true, false, null).getQueue();

    // when: client fetches info of the queue
    QueueInfo x = client.getQueue("/", q).block();

    // then: the queue is exclusive according to the response
    assertThat(x.isExclusive()).isTrue();

    // cleanup
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiQueuesNameWithDetails() throws Exception {
    // given: at least one queue was declared and some messages published
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();
    Runnable publishing =
        () -> {
          while (!Thread.currentThread().isInterrupted()) {
            try {
              ch.basicPublish("", q, null, "".getBytes(StandardCharsets.UTF_8));
              Thread.sleep(10);
            } catch (InterruptedException e) {
              return;
            } catch (Exception e) {
              // ignore
            }
          }
        };
    executorService.submit(publishing);

    // when: client get queue with details
    DetailsParameters detailsParameters =
        new DetailsParameters().messageRates(60, 5).lengths(60, 5);
    Supplier<Mono<QueueInfo>> request = () -> client.getQueue("/", q, detailsParameters);
    waitAtMostUntilTrue(
        10,
        () -> {
          QueueInfo qi = request.get().block();
          return qi != null
              && qi.getMessagesDetails() != null
              && qi.getMessagesDetails().getRate() > 0;
        });
    QueueInfo x = request.get().block();

    // then: the queue with details info is returned
    verifyQueueInfo(x);
    assertThat(x.getMessagesDetails()).isNotNull();
    assertThat(x.getMessagesDetails().getAverage()).isGreaterThan(0);
    assertThat(x.getMessagesDetails().getAverageRate()).isGreaterThan(0);
    assertThat(x.getMessagesDetails().getSamples()).hasSizeGreaterThan(0);
    assertThat(x.getMessagesDetails().getSamples().get(0).getSample()).isGreaterThan(0);
    assertThat(x.getMessagesDetails().getSamples().get(0).getTimestamp()).isGreaterThan(0);

    assertThat(x.getMessagesReadyDetails()).isNotNull();
    assertThat(x.getMessagesReadyDetails().getAverage()).isGreaterThan(0);
    assertThat(x.getMessagesReadyDetails().getAverageRate()).isGreaterThan(0);
    assertThat(x.getMessagesReadyDetails().getSamples()).hasSizeGreaterThan(0);
    assertThat(x.getMessagesReadyDetails().getSamples().get(0).getSample()).isGreaterThan(0);
    assertThat(x.getMessagesReadyDetails().getSamples().get(0).getTimestamp()).isGreaterThan(0);

    assertThat(x.getMessagesUnacknowledgedDetails()).isNotNull();

    assertThat(x.getMessageStats()).isNotNull();
    assertThat(x.getMessageStats().getBasicPublishDetails().getAverage()).isGreaterThan(0);
    assertThat(x.getMessageStats().getBasicPublishDetails().getAverageRate()).isGreaterThan(0);
    assertThat(x.getMessageStats().getBasicPublishDetails().getSamples()).hasSizeGreaterThan(0);
    assertThat(x.getMessageStats().getBasicPublishDetails().getSamples().get(0).getSample())
        .isGreaterThan(0);
    assertThat(x.getMessageStats().getBasicPublishDetails().getSamples().get(0).getTimestamp())
        .isGreaterThan(0);

    // cleanup
    executorService.shutdown();
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiQueuesVhostNameWhenQueueDoesNotExist() throws Exception {
    // given: queue lolwut does not exist in vhost /
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = "lolwut";
    ch.queueDelete(q);

    // when: client fetches info of the queue
    // then: exception is thrown
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getQueue("/", q).block();
            });
    assertThat(exception.status()).isEqualTo(404);

    // cleanup
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void putApiQueuesVhostNameWhenVhostExists() throws Exception {
    // given: vhost /
    String v = "/";

    // when: client declares a queue hop.test
    String s = "hop.test";
    client.declareQueue(v, s, new QueueInfo(false, false, false)).block();

    // and: client lists queues in vhost /
    Flux<QueueInfo> xs = client.getQueues(v);

    // then: hop.test is listed
    QueueInfo x = xs.filter(q -> q.getName().equals(s)).blockFirst();
    assertThat(x).isNotNull();
    assertThat(x.getVhost()).isEqualTo(v);
    assertThat(x.getName()).isEqualTo(s);
    assertThat(x.isDurable()).isFalse();
    assertThat(x.isExclusive()).isFalse();
    assertThat(x.isAutoDelete()).isFalse();

    // cleanup
    client.deleteQueue(v, s).block();
  }

  @Test
  void putApiPoliciesVhostName() throws Exception {
    // given: vhost / and definition
    String v = "/";
    Map<String, Object> d = new HashMap<>();
    d.put("expires", 30000);

    // when: client declares a policy hop.test
    String s = "hop.test";
    client.declarePolicy(v, s, new PolicyInfo(".*", 1, null, d)).block();

    // and: client lists policies in vhost /
    Flux<PolicyInfo> ps = client.getPolicies(v);

    // then: hop.test is listed
    PolicyInfo p = ps.filter(it -> it.getName().equals(s)).blockFirst();
    assertThat(p).isNotNull();
    assertThat(p.getVhost()).isEqualTo(v);
    assertThat(p.getName()).isEqualTo(s);
    assertThat(p.getPriority()).isEqualTo(1);
    assertThat(p.getApplyTo()).isEqualTo("all");
    assertThat(p.getDefinition()).isEqualTo(d);

    // cleanup
    client.deletePolicy(v, s).block();
  }

  @Test
  void putApiQueuesVhostNameWhenVhostDoesNotExist() throws Exception {
    // given: vhost lolwut which does not exist
    String v = "lolwut";
    client.deleteVhost(v).block();

    // when: client declares a queue hop.test
    String s = "hop.test";
    // throws an exception for RabbitMQ 3.7.4+
    // because of the way Cowboy 2.2.2 handles chunked transfer-encoding
    // so we handle both 404 and the error
    Integer status =
        client
            .declareQueue(v, s, new QueueInfo(false, false, false))
            .flatMap(r -> Mono.just(r.getStatus()))
            .onErrorReturn(
                t -> ("Connection prematurely closed BEFORE response".equals(t.getMessage())), 500)
            .block();

    // then: status code is 404 or exception is thrown
    assertThat(status == 404 || status == 500).isTrue();
  }

  @Test
  void deleteApiQueuesVhostName() throws Exception {
    String s = UUID.randomUUID().toString();
    // given: queue in vhost /
    String v = "/";
    client.declareQueue(v, s, new QueueInfo(false, false, false)).block();

    Flux<QueueInfo> xs = client.getQueues(v);
    QueueInfo x = xs.filter(q -> q.getName().equals(s)).blockFirst();
    assertThat(x).isNotNull();
    verifyQueueInfo(x);

    // when: client deletes queue in vhost /
    client.deleteQueue(v, s).block();

    // and: queue list in / is reloaded
    xs = client.getQueues(v);

    // then: no longer exists
    assertThat(xs.filter(q -> q.getName().equals(s)).hasElements().block()).isFalse();
  }

  @Test
  void deleteApiQueuesVhostNameIfEmptyTrue() throws Exception {
    String queue = UUID.randomUUID().toString();
    // given: queue in vhost /
    String v = "/";
    client.declareQueue(v, queue, new QueueInfo(false, false, false)).block();

    Flux<QueueInfo> xs = client.getQueues(v);
    QueueInfo x = xs.filter(q -> q.getName().equals(queue)).blockFirst();
    assertThat(x).isNotNull();
    verifyQueueInfo(x);

    // and: queue has a message
    client.publish(v, "amq.default", queue, new OutboundMessage().payload("test")).block();

    // when: client tries to delete queue in vhost /
    Integer status =
        client
            .deleteQueue(v, queue, new DeleteQueueParameters(true, false))
            .flatMap(r -> Mono.just(r.getStatus()))
            .onErrorReturn(
                t -> ("Connection prematurely closed BEFORE response".equals(t.getMessage())), 500)
            .block();

    // then: HTTP status is 400 BAD REQUEST
    assertThat(status).isEqualTo(400);

    // cleanup
    client.deleteQueue(v, queue).block(Duration.ofSeconds(10));
  }

  @Test
  void getApiBindings() throws Exception {
    // given: 3 queues bound to amq.fanout
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String x = "amq.fanout";
    String q1 = ch.queueDeclare().getQueue();
    String q2 = ch.queueDeclare().getQueue();
    String q3 = ch.queueDeclare().getQueue();
    ch.queueBind(q1, x, "");
    ch.queueBind(q2, x, "");
    ch.queueBind(q3, x, "");

    // when: all queue bindings are listed
    Flux<BindingInfo> xs = client.getBindings();

    // then: amq.fanout bindings are listed
    long count =
        xs.filter(b -> b.getDestinationType() == QUEUE && b.getSource().equals(x))
            .toStream()
            .count();
    assertThat(count).isGreaterThanOrEqualTo(3);

    // cleanup
    ch.queueDelete(q1);
    ch.queueDelete(q2);
    ch.queueDelete(q3);
    conn.close();
  }

  @Test
  void getApiBindingsVhost() throws Exception {
    // given: 2 queues bound to amq.topic in vhost /
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String x = "amq.topic";
    String q1 = ch.queueDeclare().getQueue();
    String q2 = ch.queueDeclare().getQueue();
    ch.queueBind(q1, x, "hop.*");
    ch.queueBind(q2, x, "api.test.#");

    // when: all queue bindings are listed
    Flux<BindingInfo> xs = client.getBindings("/");

    // then: amq.fanout bindings are listed
    long count =
        xs.filter(b -> b.getDestinationType() == QUEUE && b.getSource().equals(x)).count().block();
    assertThat(count).isGreaterThanOrEqualTo(2);

    // cleanup
    ch.queueDelete(q1);
    ch.queueDelete(q2);
    conn.close();
  }

  @Test
  void getApiBindingsVhostExample2() throws Exception {
    // given: queues hop.test bound to amq.topic in vhost /
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String x = "amq.topic";
    String q = "hop.test";
    ch.queueDeclare(q, false, false, false, null);
    ch.queueBind(q, x, "hop.*");

    // when: all queue bindings are listed
    Flux<BindingInfo> xs = client.getBindings("/");

    // then: the amq.fanout binding is listed
    long count =
        xs.filter(
                b ->
                    b.getDestinationType() == QUEUE
                        && b.getSource().equals(x)
                        && b.getDestination().equals(q))
            .count()
            .block();
    assertThat(count).isEqualTo(1);

    // cleanup
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiQueuesVhostNameBindings() throws Exception {
    // given: queues hop.test bound to amq.topic in vhost /
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String x = "amq.topic";
    String q = "hop.test";
    ch.queueDeclare(q, false, false, false, null);
    ch.queueBind(q, x, "hop.*");

    // when: all queue bindings are listed
    Flux<BindingInfo> xs = client.getQueueBindings("/", q);

    // then: the amq.fanout binding is listed
    long count =
        xs.filter(
                b ->
                    b.getDestinationType() == QUEUE
                        && b.getSource().equals(x)
                        && b.getDestination().equals(q))
            .count()
            .block();
    assertThat(count).isEqualTo(1);

    // cleanup
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiBindingsVhostEExchangeQQueue() throws Exception {
    // given: queues hop.test bound to amq.topic in vhost /
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String x = "amq.topic";
    String q = "hop.test";
    ch.queueDeclare(q, false, false, false, null);
    ch.queueBind(q, x, "hop.*");

    // when: bindings between hop.test and amq.topic are listed
    Flux<BindingInfo> xs = client.getQueueBindingsBetween("/", x, q);

    // then: the amq.fanout binding is listed
    BindingInfo b = xs.blockFirst();
    assertThat(xs.count().block()).isEqualTo(1);
    assertThat(b.getSource()).isEqualTo(x);
    assertThat(b.getDestination()).isEqualTo(q);
    assertThat(b.getDestinationType()).isEqualTo(QUEUE);

    // cleanup
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiBindingsVhostESourceEDestination() throws Exception {
    // given: fanout exchange hop.test bound to amq.fanout in vhost /
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String s = "amq.fanout";
    String d = "hop.test";
    ch.exchangeDeclare(d, "fanout", false);
    ch.exchangeBind(d, s, "");

    // when: bindings between hop.test and amq.topic are listed
    Flux<BindingInfo> xs = client.getExchangeBindingsBetween("/", s, d);

    // then: the amq.topic binding is listed
    BindingInfo b = xs.blockFirst();
    assertThat(xs.count().block()).isEqualTo(1);
    assertThat(b.getSource()).isEqualTo(s);
    assertThat(b.getDestination()).isEqualTo(d);
    assertThat(b.getDestinationType()).isEqualTo(EXCHANGE);

    // cleanup
    ch.exchangeDelete(d);
    conn.close();
  }

  @Test
  void postApiBindingsVhostESourceEDestination() throws Exception {
    // given: fanout hop.test bound to amq.fanout in vhost /
    String v = "/";
    String s = "amq.fanout";
    String d = "hop.test";
    client.deleteExchange(v, d).block();
    client.declareExchange(v, d, new ExchangeInfo("fanout", false, false)).block();
    Map<String, Object> args = new HashMap<>();
    args.put("arg1", "value1");
    args.put("arg2", "value2");
    client.bindExchange(v, d, s, "", args).block();

    // when: bindings between hop.test and amq.fanout are listed
    Flux<BindingInfo> xs = client.getExchangeBindingsBetween(v, s, d);

    // then: the amq.fanout binding is listed
    BindingInfo b = xs.blockFirst();
    assertThat(xs.count().block()).isEqualTo(1);
    assertThat(b.getSource()).isEqualTo(s);
    assertThat(b.getDestination()).isEqualTo(d);
    assertThat(b.getDestinationType()).isEqualTo(EXCHANGE);
    assertThat(b.getArguments()).containsKey("arg1");
    assertThat(b.getArguments().get("arg1")).isEqualTo("value1");
    assertThat(b.getArguments()).containsKey("arg2");
    assertThat(b.getArguments().get("arg2")).isEqualTo("value2");

    // cleanup
    client.deleteExchange(v, d).block();
  }

  @Test
  void postApiBindingsVhostEExchangeQQueue() throws Exception {
    // given: queues hop.test bound to amq.topic in vhost /
    String v = "/";
    String x = "amq.topic";
    String q = "hop.test";
    client.declareQueue(v, q, new QueueInfo(false, false, false)).block();
    Map<String, Object> args = new HashMap<>();
    args.put("arg1", "value1");
    args.put("arg2", "value2");
    client.bindQueue(v, q, x, "", args).block();

    // when: bindings between hop.test and amq.topic are listed
    Flux<BindingInfo> xs = client.getQueueBindingsBetween(v, x, q);

    // then: the amq.fanout binding is listed
    BindingInfo b = xs.blockFirst();
    assertThat(xs.count().block()).isEqualTo(1);
    assertThat(b.getSource()).isEqualTo(x);
    assertThat(b.getDestination()).isEqualTo(q);
    assertThat(b.getDestinationType()).isEqualTo(QUEUE);
    assertThat(b.getArguments()).containsKey("arg1");
    assertThat(b.getArguments().get("arg1")).isEqualTo("value1");
    assertThat(b.getArguments()).containsKey("arg2");
    assertThat(b.getArguments().get("arg2")).isEqualTo("value2");

    // cleanup
    client.deleteQueue(v, q).block();
  }

  @Test
  void postApiQueuesVhostExchangeGet() throws Exception {
    // given: a queue named hop.get and some messages in this queue
    String v = "/";
    Connection conn = openConnection();
    Channel ch = conn.createChannel();
    String q = "hop.get";
    ch.queueDeclare(q, false, false, false, null);
    ch.confirmSelect();
    int messageCount = 5;
    AMQP.BasicProperties properties =
        new AMQP.BasicProperties.Builder()
            .contentType("text/plain")
            .deliveryMode(1)
            .priority(5)
            .headers(Collections.singletonMap("header1", "value1"))
            .build();
    for (int i = 1; i <= messageCount; i++) {
      ch.basicPublish("", q, properties, ("payload" + i).getBytes(Charset.forName("UTF-8")));
    }
    ch.waitForConfirms(5_000);

    // when: client GETs from this queue
    Flux<InboundMessage> messages =
        client
            .get(v, q, messageCount, GetAckMode.NACK_REQUEUE_TRUE, GetEncoding.AUTO, -1)
            .cache(); // we cache the
    // flux to avoid
    // sending the
    // requests
    // several times

    // then: the messages are returned
    assertThat(messages.count().block()).isEqualTo(messageCount);
    InboundMessage message = messages.blockFirst();
    assertThat(message.getPayload()).startsWith("payload");
    assertThat(message.getPayloadBytes()).isEqualTo("payload".length() + 1);
    assertThat(message.isRedelivered()).isFalse();
    assertThat(message.getRoutingKey()).isEqualTo(q);
    assertThat(message.getPayloadEncoding()).isEqualTo("string");
    assertThat(message.getProperties()).isNotNull();
    assertThat(message.getProperties()).hasSize(4);
    assertThat(message.getProperties().get("priority")).isEqualTo(5);
    assertThat(message.getProperties().get("delivery_mode")).isEqualTo(1);
    assertThat(message.getProperties().get("content_type")).isEqualTo("text/plain");
    assertThat(message.getProperties().get("headers")).isNotNull();
    @SuppressWarnings("unchecked")
    Map<String, Object> headers = (Map<String, Object>) message.getProperties().get("headers");
    assertThat(headers).hasSize(1);
    assertThat(headers.get("header1")).isEqualTo("value1");

    // cleanup
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void postApiQueuesVhostExchangeGetForOneMessage() throws Exception {
    // given: a queue named hop.get and a message in this queue
    String v = "/";
    Connection conn = openConnection();
    Channel ch = conn.createChannel();
    String q = "hop.get";
    ch.queueDeclare(q, false, false, false, null);
    ch.confirmSelect();
    ch.basicPublish("", q, null, "payload".getBytes(Charset.forName("UTF-8")));
    ch.waitForConfirms(5_000);

    // when: client GETs from this queue
    InboundMessage message = client.get(v, q).block();

    // then: the messages are returned
    assertThat(message.getPayload()).isEqualTo("payload");
    assertThat(message.getPayloadBytes()).isEqualTo("payload".length());
    assertThat(message.isRedelivered()).isFalse();
    assertThat(message.getRoutingKey()).isEqualTo(q);
    assertThat(message.getPayloadEncoding()).isEqualTo("string");

    // cleanup
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void deleteApiQueuesVhostNameContents() throws Exception {
    // given: queue hop.test with 100 messages
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = "hop.test";
    ch.queueDelete(q);
    ch.queueDeclare(q, false, false, false, null);
    ch.queueBind(q, "amq.fanout", "");
    ch.confirmSelect();
    for (int i = 0; i < 100; i++) {
      ch.basicPublish("amq.fanout", "", null, "msg".getBytes());
    }
    assertThat(ch.waitForConfirms()).isTrue();
    AMQP.Queue.DeclareOk qi1 = ch.queueDeclarePassive(q);
    assertThat(qi1.getMessageCount()).isEqualTo(100);

    // when: client purges the queue
    client.purgeQueue("/", q).block();

    // then: the queue becomes empty
    AMQP.Queue.DeclareOk qi2 = ch.queueDeclarePassive(q);
    assertThat(qi2.getMessageCount()).isEqualTo(0);

    // cleanup
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiDefinitionsQueues() throws Exception {
    // given: a basic topology
    client.declareQueue("/", "queue1", new QueueInfo(false, false, false)).block();
    client.declareQueue("/", "queue2", new QueueInfo(false, false, false)).block();
    client.declareQueue("/", "queue3", new QueueInfo(false, false, false)).block();

    // when: client requests the definitions
    Definitions d = client.getDefinitions().block();

    // then: broker definitions are returned
    assertThat(d.getQueues()).isNotEmpty();
    assertThat(d.getQueues()).hasSizeGreaterThanOrEqualTo(3);
    QueueInfo q =
        d.getQueues().stream()
            .filter(it -> it.getName().equals("queue1") && it.getVhost().equals("/"))
            .findFirst()
            .orElse(null);
    assertThat(q).isNotNull();
    assertThat(q.getVhost()).isEqualTo("/");
    assertThat(q.getName()).isEqualTo("queue1");
    assertThat(q.isDurable()).isFalse();
    assertThat(q.isExclusive()).isFalse();
    assertThat(q.isAutoDelete()).isFalse();

    // cleanup
    client.deleteQueue("/", "queue1").block();
    client.deleteQueue("/", "queue2").block();
    client.deleteQueue("/", "queue3").block();
  }

  @Test
  void getApiDefinitionsExchanges() throws Exception {
    // given: a basic topology
    client.declareExchange("/", "exchange1", new ExchangeInfo("fanout", false, false)).block();
    client.declareExchange("/", "exchange2", new ExchangeInfo("direct", false, false)).block();
    client.declareExchange("/", "exchange3", new ExchangeInfo("topic", false, false)).block();

    // when: client requests the definitions
    Definitions d = client.getDefinitions().block();

    // then: broker definitions are returned
    assertThat(d.getExchanges()).isNotEmpty();
    assertThat(d.getExchanges()).hasSizeGreaterThanOrEqualTo(3);
    ExchangeInfo e =
        d.getExchanges().stream()
            .filter(it -> it.getName().equals("exchange1"))
            .findFirst()
            .orElse(null);
    assertThat(e).isNotNull();
    assertThat(e.getVhost()).isEqualTo("/");
    assertThat(e.getName()).isEqualTo("exchange1");
    assertThat(e.isDurable()).isFalse();
    assertThat(e.isInternal()).isFalse();
    assertThat(e.isAutoDelete()).isFalse();

    // cleanup
    client.deleteExchange("/", "exchange1").block();
    client.deleteExchange("/", "exchange2").block();
    client.deleteExchange("/", "exchange3").block();
  }

  @Test
  void getApiDefinitionsBindings() throws Exception {
    // given: a basic topology
    client.declareQueue("/", "queue1", new QueueInfo(false, false, false)).block();
    client.bindQueue("/", "queue1", "amq.fanout", "").block();

    // when: client requests the definitions
    Definitions d = client.getDefinitions().block();

    // then: broker definitions are returned
    assertThat(d.getBindings()).isNotEmpty();
    assertThat(d.getBindings()).hasSizeGreaterThanOrEqualTo(1);
    BindingInfo b =
        d.getBindings().stream()
            .filter(
                it ->
                    it.getSource().equals("amq.fanout")
                        && it.getDestination().equals("queue1")
                        && it.getDestinationType() == QUEUE)
            .findFirst()
            .orElse(null);
    assertThat(b).isNotNull();
    assertThat(b.getVhost()).isEqualTo("/");
    assertThat(b.getSource()).isEqualTo("amq.fanout");
    assertThat(b.getDestination()).isEqualTo("queue1");
    assertThat(b.getDestinationType()).isEqualTo(QUEUE);

    // cleanup
    client.deleteQueue("/", "queue1").block();
  }

  @Test
  void putApiParametersShovelShovelDetailsSourcePrefetchCountNotSentIfNotSet() throws Exception {
    // given: a client that retrieves the body of the request
    AtomicReference<String> requestBody = new AtomicReference<>();
    ObjectMapper delegate = ReactorNettyClient.createDefaultObjectMapper();
    ByteBufValueRetrieverObjectMapper objectMapper =
        new ByteBufValueRetrieverObjectMapper(requestBody, delegate);
    ReactorNettyClient c =
        newLocalhostNodeClient(new ReactorNettyClientOptions().objectMapper(() -> objectMapper));

    // and: a basic topology with null sourcePrefetchCount
    ShovelDetails details = new ShovelDetails("amqp://", "amqp://", 30, true, null);
    details.setSourceQueue("queue1");
    details.setDestinationExchange("exchange1");

    // when: client declares the shovels
    c.declareShovel("/", new ShovelInfo("shovel1", details)).block();

    // then: the json will not include src-prefetch-count
    @SuppressWarnings("unchecked")
    Map<String, Object> body =
        (Map<String, Object>) new ObjectMapper().readValue(requestBody.get(), Map.class);
    //    Map<String, Object> body = (Map<String, Object>) new
    // JsonSlurper().parseText(requestBody.get());
    @SuppressWarnings("unchecked")
    Map<String, Object> value = (Map<String, Object>) body.get("value");
    assertThat(value.get("src-prefetch-count")).isNull();

    // cleanup
    c.deleteShovel("/", "shovel1").block();
    c.deleteQueue("/", "queue1").block();
  }

  @Test
  void putApiParametersShovelShovelDetailsDestinationAddTimestampHeaderNotSentIfNotSet()
      throws Exception {
    // given: a client that retrieves the body of the request
    AtomicReference<String> requestBody = new AtomicReference<>();
    ObjectMapper delegate = ReactorNettyClient.createDefaultObjectMapper();
    ByteBufValueRetrieverObjectMapper objectMapper =
        new ByteBufValueRetrieverObjectMapper(requestBody, delegate);
    ReactorNettyClient c =
        newLocalhostNodeClient(new ReactorNettyClientOptions().objectMapper(() -> objectMapper));

    // and: a basic topology with null destinationAddTimestampHeader
    ShovelDetails details = new ShovelDetails("amqp://", "amqp://", 30, true, null);
    details.setSourceQueue("queue1");
    details.setDestinationExchange("exchange1");

    // when: client declares the shovels
    c.declareShovel("/", new ShovelInfo("shovel1", details)).block();

    // then: the json will not include dest-add-timestamp-header
    @SuppressWarnings("unchecked")
    Map<String, Object> body =
        (Map<String, Object>) new ObjectMapper().readValue(requestBody.get(), Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> value = (Map<String, Object>) body.get("value");
    assertThat(value.get("dest-add-timestamp-header")).isNull();

    // cleanup
    c.deleteShovel("/", "shovel1").block();
    c.deleteQueue("/", "queue1").block();
  }

  @Test
  void getApiParametersShovel() throws Exception {
    // given: a shovel defined
    ShovelDetails value =
        new ShovelDetails("amqp://localhost:5672/vh1", "amqp://localhost:5672/vh2", 30, true, null);
    value.setSourceQueue("queue1");
    value.setDestinationExchange("exchange1");
    value.setSourcePrefetchCount(50L);
    value.setSourceDeleteAfter("never");
    value.setDestinationAddTimestampHeader(true);
    client.declareShovel("/", new ShovelInfo("shovel1", value)).block();

    // when: client requests the shovels
    Flux<ShovelInfo> shovels = awaitEventPropagation(() -> client.getShovels());

    // then: shovel definitions are returned
    assertThat(shovels.hasElements().block()).isTrue();
    ShovelInfo s = shovels.filter(it -> it.getName().equals("shovel1")).blockFirst();
    assertThat(s).isNotNull();
    assertThat(s.getName()).isEqualTo("shovel1");
    assertThat(s.getVirtualHost()).isEqualTo("/");
    assertThat(s.getDetails().getSourceURIs())
        .isEqualTo(Arrays.asList("amqp://localhost:5672/vh1"));
    assertThat(s.getDetails().getSourceExchange()).isNull();
    assertThat(s.getDetails().getSourceQueue()).isEqualTo("queue1");
    assertThat(s.getDetails().getDestinationURIs())
        .isEqualTo(Arrays.asList("amqp://localhost:5672/vh2"));
    assertThat(s.getDetails().getDestinationExchange()).isEqualTo("exchange1");
    assertThat(s.getDetails().getDestinationQueue()).isNull();
    assertThat(s.getDetails().getReconnectDelay()).isEqualTo(30);
    assertThat(s.getDetails().isAddForwardHeaders()).isTrue();
    assertThat(s.getDetails().getPublishProperties()).isNull();
    assertThat(s.getDetails().getSourcePrefetchCount()).isEqualTo(50L);
    assertThat(s.getDetails().getSourceDeleteAfter()).isEqualTo("never");
    assertThat(s.getDetails().isDestinationAddTimestampHeader()).isTrue();

    // cleanup
    client.deleteShovel("/", "shovel1").block();
    client.deleteQueue("/", "queue1").block();
  }

  @Test
  void getApiParametersShovelWithMultipleUris() throws Exception {
    // given: a shovel defined with multiple URIs
    ShovelDetails value =
        new ShovelDetails(
            Arrays.asList("amqp://localhost:5672/vh1", "amqp://localhost:5672/vh3"),
            Arrays.asList("amqp://localhost:5672/vh2", "amqp://localhost:5672/vh4"),
            30,
            true,
            null);
    value.setSourceQueue("queue1");
    value.setDestinationExchange("exchange1");
    value.setSourcePrefetchCount(50L);
    value.setSourceDeleteAfter("never");
    value.setDestinationAddTimestampHeader(true);
    client.declareShovel("/", new ShovelInfo("shovel1", value)).block();

    // when: client requests the shovels
    Flux<ShovelInfo> shovels = awaitEventPropagation(() -> client.getShovels());

    // then: shovel definitions are returned
    assertThat(shovels.hasElements().block()).isTrue();
    ShovelInfo s = shovels.filter(it -> it.getName().equals("shovel1")).blockFirst();
    assertThat(s).isNotNull();
    assertThat(s.getName()).isEqualTo("shovel1");
    assertThat(s.getVirtualHost()).isEqualTo("/");
    assertThat(s.getDetails().getSourceURIs())
        .isEqualTo(Arrays.asList("amqp://localhost:5672/vh1", "amqp://localhost:5672/vh3"));
    assertThat(s.getDetails().getSourceExchange()).isNull();
    assertThat(s.getDetails().getSourceQueue()).isEqualTo("queue1");
    assertThat(s.getDetails().getDestinationURIs())
        .isEqualTo(Arrays.asList("amqp://localhost:5672/vh2", "amqp://localhost:5672/vh4"));
    assertThat(s.getDetails().getDestinationExchange()).isEqualTo("exchange1");
    assertThat(s.getDetails().getDestinationQueue()).isNull();
    assertThat(s.getDetails().getReconnectDelay()).isEqualTo(30);
    assertThat(s.getDetails().isAddForwardHeaders()).isTrue();
    assertThat(s.getDetails().getPublishProperties()).isNull();
    assertThat(s.getDetails().getSourcePrefetchCount()).isEqualTo(50L);
    assertThat(s.getDetails().getSourceDeleteAfter()).isEqualTo("never");
    assertThat(s.getDetails().isDestinationAddTimestampHeader()).isTrue();

    // cleanup
    client.deleteShovel("/", "shovel1").block();
    client.deleteQueue("/", "queue1").block();
  }

  @Test
  void putApiParametersShovelWithAnEmptyPublishPropertiesMap() throws Exception {
    // given: a Shovel with empty publish properties
    ShovelDetails value =
        new ShovelDetails("amqp://localhost:5672/vh1", "amqp://localhost:5672/vh2", 30, true, null);
    value.setSourceQueue("queue1");
    value.setDestinationExchange("exchange1");

    // when: client tries to declare a Shovel with empty publish properties (should
    // use null
    // instead)
    // then: validation should work correctly
    ShovelInfo shovelInfo = new ShovelInfo("shovel1", value);
    HttpResponse response = client.declareShovel("/", shovelInfo).block();
    assertThat(response.getStatus()).isEqualTo(201);

    // cleanup
    client.deleteShovel("/", "shovel1").block();
    client.deleteQueue("/", "queue1").block();
  }

  @Test
  void getApiShovels() throws Exception {
    // given: a basic topology
    ShovelDetails value =
        new ShovelDetails("amqp://localhost:5672/vh1", "amqp://localhost:5672/vh2", 30, true, null);
    value.setSourceQueue("queue1");
    value.setDestinationExchange("exchange1");
    String shovelName = "shovel2";
    client.declareShovel("/", new ShovelInfo(shovelName, value)).block();

    // when: client requests the shovels status
    Flux<ShovelStatus> shovels = awaitEventPropagation(() -> client.getShovelsStatus());

    // then: shovels status are returned
    assertThat(shovels.hasElements().block()).isTrue();
    ShovelStatus s = shovels.filter(it -> it.getName().equals(shovelName)).blockFirst();
    assertThat(s).isNotNull();
    assertThat(s.getName()).isEqualTo(shovelName);
    assertThat(s.getVirtualHost()).isEqualTo("/");
    assertThat(s.getType()).isEqualTo("dynamic");
    waitAtMostUntilTrue(
        30,
        () -> {
          ShovelStatus shovelStatus =
              client.getShovelsStatus().filter(it -> it.getName().equals(shovelName)).blockFirst();
          return shovelStatus != null && "running".equals(shovelStatus.getState());
        });
    ShovelStatus status =
        client.getShovelsStatus().filter(it -> it.getName().equals(shovelName)).blockFirst();
    assertThat(status.getState()).isEqualTo("running");
    assertThat(status.getSourceURI()).isEqualTo("amqp://localhost:5672/vh1");
    assertThat(status.getDestinationURI()).isEqualTo("amqp://localhost:5672/vh2");

    // cleanup
    client.deleteShovel("/", shovelName).block();
  }

  @Test
  void deleteApiExchangesVhostName() throws Exception {
    // given: fanout exchange hop.test in vhost /
    String v = "/";
    String s = "hop.test";
    client.declareExchange(v, s, new ExchangeInfo("fanout", false, false)).block();

    Flux<ExchangeInfo> xs = client.getExchanges(v);
    Flux<ExchangeInfo> x = xs.filter(e -> e.getName().equals(s));
    verifyExchangeInfo(x.blockFirst());

    // when: client deletes exchange hop.test in vhost /
    client.deleteExchange(v, s).block();

    // and: exchange list in / is reloaded
    xs = client.getExchanges(v);

    // then: hop.test no longer exists
    assertThat(xs.filter(e -> e.getName().equals(s)).hasElements().block()).isFalse();
  }

  @Test
  void postApiExchangesVhostNamePublish() throws Exception {
    // given: a queue named hop.publish and a consumer on this queue
    String v = "/";
    Connection conn = openConnection();
    Channel ch = conn.createChannel();
    String q = "hop.publish";
    ch.queueDeclare(q, false, false, false, null);
    ch.queueBind(q, "amq.direct", q);
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<String> payloadReference = new AtomicReference<>();
    AtomicReference<AMQP.BasicProperties> propertiesReference = new AtomicReference<>();
    ch.basicConsume(
        q,
        true,
        (ctag, message) -> {
          payloadReference.set(new String(message.getBody()));
          propertiesReference.set(message.getProperties());
          latch.countDown();
        },
        ctag -> {});

    // when: client publishes a message to the queue
    Map<String, Object> properties = new HashMap<>();
    properties.put("delivery_mode", 1);
    properties.put("content_type", "text/plain");
    properties.put("priority", 5);
    properties.put("headers", Collections.singletonMap("header1", "value1"));
    Boolean routed =
        client
            .publish(
                v,
                "amq.direct",
                q,
                new OutboundMessage().payload("Hello world!").utf8Encoded().properties(properties))
            .block();

    // then: the message is routed to the queue and consumed
    assertThat(routed).isTrue();
    latch.await(5, TimeUnit.SECONDS);
    assertThat(payloadReference.get()).isEqualTo("Hello world!");
    assertThat(propertiesReference.get().getDeliveryMode()).isEqualTo(1);
    assertThat(propertiesReference.get().getContentType()).isEqualTo("text/plain");
    assertThat(propertiesReference.get().getPriority()).isEqualTo(5);
    assertThat(propertiesReference.get().getHeaders()).isNotNull();
    assertThat(propertiesReference.get().getHeaders()).hasSize(1);
    assertThat(propertiesReference.get().getHeaders().get("header1").toString())
        .isEqualTo("value1");

    // cleanup
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiExchangesVhostWhenVhostDoesNotExist() throws Exception {
    // given: vhost lolwut does not exist
    String v = "lolwut";
    client.deleteVhost(v).block();

    // when: client retrieves the list of exchanges in that vhost
    // then: exception is thrown
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getExchanges(v).blockFirst();
            });
    assertThat(exception.status()).isEqualTo(404);
  }

  @Test
  void getApiExchangesVhostNameWhenBothVhostAndExchangeExist() throws Exception {
    // when: client retrieves exchange amq.fanout in vhost /
    Mono<ExchangeInfo> xs = client.getExchange("/", "amq.fanout");

    // then: exchange info is returned
    ExchangeInfo x =
        xs.filter(e -> e.getName().equals("amq.fanout") && e.getVhost().equals("/")).block();
    verifyExchangeInfo(x);
  }

  @Test
  void getApiExchangesVhostNameBindingsDestination() throws Exception {
    // given: an exchange named hop.exchange1 which is bound to amq.fanout
    Connection conn = openConnection();
    Channel ch = conn.createChannel();
    String src = "amq.fanout";
    String dest = "hop.exchange1";
    ch.exchangeDeclare(dest, "fanout");
    ch.exchangeBind(dest, src, "");

    // when: client lists bindings of amq.fanout
    Flux<BindingInfo> xs = client.getExchangeBindingsByDestination("/", dest);

    // then: there is a binding for hop.exchange1
    Flux<BindingInfo> x =
        xs.filter(
            b ->
                b.getSource().equals(src)
                    && b.getDestinationType() == EXCHANGE
                    && b.getDestination().equals(dest));
    assertThat(x.hasElements().block()).isTrue();

    // cleanup
    ch.exchangeDelete(dest);
    conn.close();
  }

  @Test
  void getApiExchangesVhostNameBindingsSource() throws Exception {
    // given: a queue named hop.queue1
    Connection conn = openConnection();
    Channel ch = conn.createChannel();
    String q = "hop.queue1";
    ch.queueDeclare(q, false, false, false, null);

    // when: client lists bindings of default exchange
    Flux<BindingInfo> xs = client.getExchangeBindingsBySource("/", "");

    // then: there is an automatic binding for hop.queue1
    Flux<BindingInfo> x =
        xs.filter(
            b ->
                b.getSource().equals("")
                    && b.getDestinationType() == QUEUE
                    && b.getDestination().equals(q));
    assertThat(x.hasElements().block()).isTrue();

    // cleanup
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void putApiExchangesVhostNameWhenVhostExists() throws Exception {
    // given: fanout exchange hop.test in vhost /
    String v = "/";
    String s = "hop.test";
    client.declareExchange(v, s, new ExchangeInfo("fanout", false, false)).block();

    // when: client lists exchanges in vhost /
    Flux<ExchangeInfo> xs = client.getExchanges(v);

    // then: hop.test is listed
    Flux<ExchangeInfo> x = xs.filter(e -> e.getName().equals(s));
    verifyExchangeInfo(x.blockFirst());

    // cleanup
    client.deleteExchange(v, s).block();
  }

  @Test
  void getApiParametersFederationUpstreamDeclareAndGetAtRootVhostWithNonNullAckMode()
      throws Exception {
    // given: an upstream
    String vhost = "/";
    String upstreamName = "upstream1";
    UpstreamDetails upstreamDetails = new UpstreamDetails();
    upstreamDetails.setUri("amqp://localhost:5672");
    upstreamDetails.setAckMode(AckMode.ON_CONFIRM);
    client.declareUpstream(vhost, upstreamName, upstreamDetails).block();

    // when: client requests the upstreams
    Flux<UpstreamInfo> upstreams = awaitEventPropagation(() -> client.getUpstreams());

    // then: list of upstreams that contains the new upstream is returned and ack
    // mode is correctly
    // retrieved
    verifyUpstreamDefinitions(vhost, upstreams, upstreamName);
    UpstreamInfo upstream = upstreams.filter(it -> it.getName().equals(upstreamName)).blockFirst();
    assertThat(upstream.getValue().getAckMode()).isEqualTo(AckMode.ON_CONFIRM);

    // cleanup
    client.deleteUpstream(vhost, upstreamName).block();
  }

  @Test
  void getApiParametersFederationUpstreamDeclareAndGetAtRootVhost() throws Exception {
    // given: an upstream
    String vhost = "/";
    String upstreamName = "upstream1";
    declareUpstream(client, vhost, upstreamName);

    // when: client requests the upstreams
    Flux<UpstreamInfo> upstreams = awaitEventPropagation(() -> client.getUpstreams());

    // then: list of upstreams that contains the new upstream is returned
    verifyUpstreamDefinitions(vhost, upstreams, upstreamName);

    // cleanup
    client.deleteUpstream(vhost, upstreamName).block();
  }

  @Test
  void getApiParametersFederationUpstreamDeclareAndGetAtNonRootVhost() throws Exception {
    // given: an upstream
    String vhost = "foo";
    String upstreamName = "upstream2";
    client.createVhost(vhost).block();
    declareUpstream(client, vhost, upstreamName);

    // when: client requests the upstreams
    Flux<UpstreamInfo> upstreams = awaitEventPropagation(() -> client.getUpstreams(vhost));

    // then: list of upstreams that contains the new upstream is returned
    verifyUpstreamDefinitions(vhost, upstreams, upstreamName);

    // cleanup
    client.deleteUpstream(vhost, upstreamName).block();
    client.deleteVhost(vhost).block();
  }

  @Test
  void putApiParametersFederationUpstreamWithNullUpstreamUri() throws Exception {
    // given: an Upstream without upstream uri
    UpstreamDetails upstreamDetails = new UpstreamDetails();

    // when: client tries to declare an Upstream
    // then: an illegal argument exception is thrown
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          client.declareUpstream("/", "upstream3", upstreamDetails).block();
        });
  }

  @Test
  void deleteApiParametersFederationUpstreamVhostName() throws Exception {
    // given: upstream upstream4 in vhost /
    String vhost = "/";
    String upstreamName = "upstream4";
    declareUpstream(client, vhost, upstreamName);

    Flux<UpstreamInfo> upstreams = awaitEventPropagation(() -> client.getUpstreams());
    verifyUpstreamDefinitions(vhost, upstreams, upstreamName);

    // when: client deletes upstream upstream4 in vhost /
    client.deleteUpstream(vhost, upstreamName).block();

    // and: upstream list in / is reloaded
    upstreams = client.getUpstreams();

    // then: upstream4 no longer exists
    assertThat(upstreams.filter(it -> it.getName().equals(upstreamName)).hasElements().block())
        .isFalse();
  }

  // Helper classes

  static class ByteBufValueRetrieverObjectMapper extends ObjectMapper {
    final AtomicReference<String> value;
    final ObjectMapper delegate;

    ByteBufValueRetrieverObjectMapper(AtomicReference<String> value, ObjectMapper delegate) {
      this.value = value;
      this.delegate = delegate;
    }

    @Override
    public void writeValue(OutputStream out, Object v)
        throws IOException, JsonGenerationException, JsonMappingException {
      delegate.writeValue(out, v);
      ByteBuf byteBuf = ((ByteBufOutputStream) out).buffer();
      value.set(byteBuf.toString(Charset.forName("UTF-8")));
    }
  }

  // Helper methods

  protected Connection openConnection() throws Exception {
    return this.cf.newConnection();
  }

  protected Connection openConnection(String clientProvidedName) throws Exception {
    return this.cf.newConnection(clientProvidedName);
  }

  protected Connection openConnection(String username, String password) throws Exception {
    ConnectionFactory cf = new ConnectionFactory();
    cf.setUsername(username);
    cf.setPassword(password);
    return cf.newConnection();
  }

  protected static void verifyNode(NodeInfo node) {
    assertThat(node).isNotNull();
    assertThat(node.getName()).isNotNull();
    assertThat(node.getErlangProcessesUsed()).isLessThanOrEqualTo(node.getErlangProcessesTotal());
    assertThat(node.getErlangRunQueueLength()).isGreaterThanOrEqualTo(0);
    assertThat(node.getMemoryUsed()).isLessThanOrEqualTo(node.getMemoryLimit());
  }

  protected static void verifyExchangeInfo(ExchangeInfo x) {
    assertThat(x.getType()).isNotNull();
    assertThat(x.isDurable()).isNotNull();
    assertThat(x.getName()).isNotNull();
    assertThat(x.isAutoDelete()).isNotNull();
  }

  protected static void verifyPolicyInfo(PolicyInfo x) {
    assertThat(x.getName()).isNotNull();
    assertThat(x.getVhost()).isNotNull();
    assertThat(x.getPattern()).isNotNull();
    assertThat(x.getDefinition()).isNotNull();
    assertThat(x.getApplyTo()).isNotNull();
  }

  protected static void verifyConnectionInfo(ConnectionInfo info) {
    assertThat(info.getPort()).isEqualTo(ConnectionFactory.DEFAULT_AMQP_PORT);
    assertThat(info.isUsesTLS()).isFalse();
  }

  protected static void verifyChannelInfo(ChannelInfo chi, Channel ch) {
    assertThat(chi.getConsumerCount()).isEqualTo(0);
    assertThat(chi.getNumber()).isEqualTo(ch.getChannelNumber());
    assertThat(chi.getNode()).startsWith("rabbit@");
    assertThat(chi.getState() == null || chi.getState().equals("running")).isTrue();
    assertThat(chi.usesPublisherConfirms()).isFalse();
    assertThat(chi.isTransactional()).isFalse();
  }

  protected static void verifyVhost(VhostInfo vhi, String version) {
    assertThat(vhi.getName()).isEqualTo("/");
    assertThat(vhi.isTracing()).isFalse();
    if (TestUtils.isVersion37orLater(version)) {
      assertThat(vhi.getClusterState()).isNotNull();
    }
    if (TestUtils.isVersion38orLater(version)) {
      assertThat(vhi.getDescription()).isNotNull();
    }
  }

  protected static void verifyQueueInfo(QueueInfo x) {
    assertThat(x.getName()).isNotNull();
    assertThat(x.isDurable()).isNotNull();
    assertThat(x.isExclusive()).isNotNull();
    assertThat(x.isAutoDelete()).isNotNull();
  }

  /**
   * Statistics tables in the server are updated asynchronously, in particular starting with
   * rabbitmq/rabbitmq-management#236, so in some cases we need to wait before GET'ing e.g. a newly
   * opened connection.
   */
  @SuppressWarnings("unchecked")
  protected static <T> T awaitEventPropagation(Supplier<T> callback) {
    if (callback != null) {
      int n = 0;
      T result = callback.get();
      boolean hasElements = false;
      while (!hasElements && n < 10000) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
        n += 100;
        // we cache the result to avoid additional requests
        // when the flux content is accessed later on
        result = callback.get();
        if (result instanceof Flux) {
          Flux<?> flux = ((Flux<?>) result).cache();
          hasElements = Boolean.TRUE.equals(flux.hasElements().block());
          result = (T) flux;
        }
      }
      assertThat(n).isLessThan(10000);
      return result;
    } else {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
      return null;
    }
  }

  protected static void awaitEventPropagation() {
    awaitEventPropagation(null);
  }

  protected static boolean waitAtMostUntilTrue(int timeoutInSeconds, Supplier<Boolean> callback) {
    if (callback.get()) {
      return true;
    }
    int timeout = timeoutInSeconds * 1000;
    int waited = 0;
    while (waited <= timeout) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
      waited += 100;
      if (callback.get()) {
        return true;
      }
    }
    return false;
  }

  protected static boolean awaitOn(CountDownLatch latch) {
    try {
      return latch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  protected static void declareUpstream(
      ReactorNettyClient client, String vhost, String upstreamName) {
    UpstreamDetails upstreamDetails = new UpstreamDetails();
    upstreamDetails.setUri("amqp://localhost:5672");
    client.declareUpstream(vhost, upstreamName, upstreamDetails).block();
  }

  protected static void verifyUpstreamDefinitions(
      String vhost, Flux<UpstreamInfo> upstreams, String upstreamName) {
    assertThat(upstreams.hasElements().block()).isTrue();
    UpstreamInfo upstream = upstreams.filter(it -> it.getName().equals(upstreamName)).blockFirst();
    assertThat(upstream).isNotNull();
    assertThat(upstream.getName()).isEqualTo(upstreamName);
    assertThat(upstream.getVhost()).isEqualTo(vhost);
    assertThat(upstream.getComponent()).isEqualTo("federation-upstream");
    assertThat(upstream.getValue().getUri()).isEqualTo("amqp://localhost:5672");
  }

  @Test
  void getApiParametersFederationUpstreamSetDeclareAndGet() throws Exception {
    // given: an upstream set with two upstreams
    String vhost = "/";
    String upstreamSetName = "upstream-set-1";
    String upstreamA = "A";
    String upstreamB = "B";
    String policyName = "federation-policy";
    declareUpstream(client, vhost, upstreamA);
    declareUpstream(client, vhost, upstreamB);
    UpstreamSetDetails d1 = new UpstreamSetDetails();
    d1.setUpstream(upstreamA);
    d1.setExchange("amq.direct");
    UpstreamSetDetails d2 = new UpstreamSetDetails();
    d2.setUpstream(upstreamB);
    d2.setExchange("amq.fanout");
    List<UpstreamSetDetails> detailsSet = new ArrayList<>();
    detailsSet.add(d1);
    detailsSet.add(d2);
    client.declareUpstreamSet(vhost, upstreamSetName, detailsSet).block();
    PolicyInfo p = new PolicyInfo();
    p.setApplyTo("exchanges");
    p.setName(policyName);
    p.setPattern("amq\\.topic");
    p.setDefinition(Collections.singletonMap("federation-upstream-set", upstreamSetName));
    client.declarePolicy(vhost, policyName, p).block();

    // when: client requests the upstream set list
    Flux<UpstreamSetInfo> upstreamSets = awaitEventPropagation(() -> client.getUpstreamSets());

    // then: upstream set with two upstreams is returned
    assertThat(upstreamSets.hasElements().block()).isTrue();
    UpstreamSetInfo upstreamSet =
        upstreamSets.filter(it -> it.getName().equals(upstreamSetName)).blockFirst();
    assertThat(upstreamSet).isNotNull();
    assertThat(upstreamSet.getName()).isEqualTo(upstreamSetName);
    assertThat(upstreamSet.getVhost()).isEqualTo(vhost);
    assertThat(upstreamSet.getComponent()).isEqualTo("federation-upstream-set");
    List<UpstreamSetDetails> upstreams = upstreamSet.getValue();
    assertThat(upstreams).isNotNull();
    assertThat(upstreams).hasSize(2);
    UpstreamSetDetails responseUpstreamA =
        upstreams.stream()
            .filter(it -> it.getUpstream().equals(upstreamA))
            .findFirst()
            .orElse(null);
    assertThat(responseUpstreamA).isNotNull();
    assertThat(responseUpstreamA.getUpstream()).isEqualTo(upstreamA);
    assertThat(responseUpstreamA.getExchange()).isEqualTo("amq.direct");
    UpstreamSetDetails responseUpstreamB =
        upstreams.stream()
            .filter(it -> it.getUpstream().equals(upstreamB))
            .findFirst()
            .orElse(null);
    assertThat(responseUpstreamB).isNotNull();
    assertThat(responseUpstreamB.getUpstream()).isEqualTo(upstreamB);
    assertThat(responseUpstreamB.getExchange()).isEqualTo("amq.fanout");

    // cleanup
    client.deletePolicy(vhost, policyName);
    client.deleteUpstreamSet(vhost, upstreamSetName).block();
    client.deleteUpstream(vhost, upstreamA).block();
    client.deleteUpstream(vhost, upstreamB).block();
  }

  @Test
  void putApiParametersFederationUpstreamSetWithoutUpstreams() throws Exception {
    // given: an Upstream without upstream uri
    UpstreamSetDetails upstreamSetDetails = new UpstreamSetDetails();
    List<UpstreamSetDetails> detailsSet = new ArrayList<>();
    detailsSet.add(upstreamSetDetails);

    // when: client tries to declare an Upstream
    // then: an illegal argument exception is thrown
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          client.declareUpstreamSet("/", "upstream-set-2", detailsSet).block();
        });
  }

  @Test
  void getApiVhostLimits() throws Exception {
    // given: several virtual hosts with limits
    String vhost1 = "virtual-host-with-limits-1";
    String vhost2 = "virtual-host-with-limits-2";
    client.createVhost(vhost1).block();
    client.createVhost(vhost2).block();
    client.limitMaxNumberOfQueues(vhost1, 100).block();
    client.limitMaxNumberOfConnections(vhost1, 10).block();
    client.limitMaxNumberOfQueues(vhost2, 200).block();
    client.limitMaxNumberOfConnections(vhost2, 20).block();
    client.limitMaxNumberOfQueues("/", 300).block();
    client.limitMaxNumberOfConnections("/", 30).block();

    // when: client tries to look up limits
    List<VhostLimits> limits = client.getVhostLimits().collectList().block();

    // then: limits match the definitions
    assertThat(limits).hasSize(3);
    VhostLimits limits1 =
        limits.stream().filter(it -> it.getVhost().equals(vhost1)).findFirst().orElse(null);
    assertThat(limits1.getMaxQueues()).isEqualTo(100);
    assertThat(limits1.getMaxConnections()).isEqualTo(10);
    VhostLimits limits2 =
        limits.stream().filter(it -> it.getVhost().equals(vhost2)).findFirst().orElse(null);
    assertThat(limits2.getMaxQueues()).isEqualTo(200);
    assertThat(limits2.getMaxConnections()).isEqualTo(20);
    VhostLimits limits3 =
        limits.stream().filter(it -> it.getVhost().equals("/")).findFirst().orElse(null);
    assertThat(limits3.getMaxQueues()).isEqualTo(300);
    assertThat(limits3.getMaxConnections()).isEqualTo(30);

    // cleanup
    client.deleteVhost(vhost1).block();
    client.deleteVhost(vhost2).block();
    client.clearMaxQueuesLimit("/").block();
    client.clearMaxConnectionsLimit("/").block();
  }

  @Test
  void getApiGlobalParametersMqttPortToVhostMappingWithoutMqttVhostPortMapping() throws Exception {
    // given: rabbitmq deployment without mqtt port mappings defined
    client.deleteMqttPortToVhostMapping().block();

    // when: client tries to look up mqtt vhost port mappings
    // then: mono throws exception
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getMqttPortToVhostMapping().block();
            });
    assertThat(exception.status()).isEqualTo(404);
  }

  @Test
  void getApiGlobalParametersMqttPortToVhostMappingWithASampleMapping() throws Exception {
    // given: a mqtt mapping with 2 vhosts defined
    Map<Integer, String> mqttInputMap = Map.of(2024, "vhost1", 2025, "vhost2");
    client.setMqttPortToVhostMapping(mqttInputMap).block();

    // when: client tries to get mqtt port mappings
    MqttVhostPortInfo mqttInfo = client.getMqttPortToVhostMapping().block();
    Map<Integer, String> mqttReturnValues = mqttInfo.getValue();

    // then: a map with 2 mqtt ports and vhosts is returned
    assertThat(mqttReturnValues).isEqualTo(mqttInputMap);

    // cleanup
    client.deleteMqttPortToVhostMapping().block();
  }

  @Test
  void putApiGlobalParametersMqttPortToVhostMappingWithABlankVhostValue() throws Exception {
    // given: a mqtt mapping with blank vhost
    Map<Integer, String> mqttInputMap = Map.of(2024, " ", 2025, "vhost2");

    // when: client tries to set mqtt port mappings
    // then: an illegal argument exception is thrown
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          client.setMqttPortToVhostMapping(mqttInputMap).block();
        });
  }

  @Test
  void getApiVhostLimitsWithoutLimitsOnAnyHost() throws Exception {
    // given: the default configuration
    // when: client tries to look up limits
    List<VhostLimits> limits = client.getVhostLimits().collectList().block();

    // then: it should return one row for the default virtual host
    assertThat(limits).hasSize(1);
    assertThat(limits.get(0).getVhost()).isEqualTo("/");
    assertThat(limits.get(0).getMaxQueues()).isEqualTo(-1);
    assertThat(limits.get(0).getMaxConnections()).isEqualTo(-1);
  }

  @Test
  void getApiVhostLimitsVhost() throws Exception {
    // given: a virtual host with limits
    String vhost = "virtual-host-with-limits";
    client.createVhost(vhost).block();
    client.limitMaxNumberOfQueues(vhost, 100).block();
    client.limitMaxNumberOfConnections(vhost, 10).block();

    // when: client tries to look up limits for this virtual host
    VhostLimits limits = client.getVhostLimits(vhost).block();

    // then: limits match the definitions
    assertThat(limits.getMaxQueues()).isEqualTo(100);
    assertThat(limits.getMaxConnections()).isEqualTo(10);

    // cleanup
    client.deleteVhost(vhost).block();
  }

  @Test
  void getApiVhostLimitsVhostVhostWithNoLimits() {
    // given: a virtual host without limits
    String vhost = "virtual-host-without-limits";
    client.createVhost(vhost).block();

    // when: client tries to look up limits for this virtual host
    VhostLimits limits = client.getVhostLimits(vhost).block();

    // then: limits are set to -1
    assertThat(limits.getMaxQueues()).isEqualTo(-1);
    assertThat(limits.getMaxConnections()).isEqualTo(-1);

    // cleanup
    client.deleteVhost(vhost).block();
  }

  @Test
  void getApiVhostLimitsVhostWithNonExistingVhost() {
    // given: a virtual host that does not exist
    String vhost = "virtual-host-that-does-not-exist";

    // when: client tries to look up limits for this virtual host
    // then: an exception is thrown
    HttpClientException exception =
        assertThrows(
            HttpClientException.class,
            () -> {
              client.getVhostLimits(vhost).block();
            });
    assertThat(exception.status()).isEqualTo(404);

    // cleanup
    client.deleteVhost(vhost).block();
  }

  @Test
  void deleteApiVhostLimitsVhostMaxQueues() {
    // given: a virtual host with max queues limit
    String vhost = "virtual-host-max-queues-limit";
    client.createVhost(vhost).block();
    client.limitMaxNumberOfQueues(vhost, 42).block();

    // when: client clears the limit
    client.clearMaxQueuesLimit(vhost).block();

    // then: limit is then looked up with value -1
    assertThat(client.getVhostLimits(vhost).block().getMaxQueues()).isEqualTo(-1);
    assertThat(client.getVhostLimits(vhost).block().getMaxConnections()).isEqualTo(-1);

    // cleanup
    client.deleteVhost(vhost).block();
  }

  @Test
  void deleteApiVhostLimitsVhostMaxConnections() {
    // given: a virtual host with max connections limit
    String vhost = "virtual-host-max-connections-limit";
    client.createVhost(vhost).block();
    client.limitMaxNumberOfConnections(vhost, 42).block();

    // when: client clears the limit
    client.clearMaxConnectionsLimit(vhost).block();

    // then: limit is then looked up with value -1
    assertThat(client.getVhostLimits(vhost).block().getMaxConnections()).isEqualTo(-1);
    assertThat(client.getVhostLimits(vhost).block().getMaxQueues()).isEqualTo(-1);

    // cleanup
    client.deleteVhost(vhost).block();
  }

  @Test
  void deleteApiVhostLimitsVhostWithOnlyOneLimit() {
    // given: a virtual host with max queues and connections limits
    String vhost = "virtual-host-max-queues-connections-limits";
    client.createVhost(vhost).block();
    client.limitMaxNumberOfQueues(vhost, 314).block();
    client.limitMaxNumberOfConnections(vhost, 42).block();

    // when: client clears one of the limits
    client.clearMaxQueuesLimit(vhost).block();

    // then: the cleared limit is then returned as -1
    assertThat(client.getVhostLimits(vhost).block().getMaxQueues()).isEqualTo(-1);

    // then: the other limit is left as-is
    assertThat(client.getVhostLimits(vhost).block().getMaxConnections()).isEqualTo(42);

    // cleanup
    client.deleteVhost(vhost).block();
  }

  @Test
  void getFeatureFlags() {
    List<FeatureFlag> flags = client.getFeatureFlags().collectList().block();
    assertThat(flags).isNotEmpty();
    FeatureFlag flag = flags.get(0);
    assertThat(flag.getName()).isNotNull();
    assertThat(flag.getState()).isNotNull();
    assertThat(flag.getStability()).isNotNull();
  }

  @Test
  void enableFeatureFlag() {
    List<FeatureFlag> flags = client.getFeatureFlags().collectList().block();
    FeatureFlag disabledFlag =
        flags.stream()
            .filter(
                f ->
                    f.getState() == FeatureFlagState.DISABLED
                        && f.getStability() == FeatureFlagStability.STABLE)
            .findFirst()
            .orElse(null);

    if (disabledFlag == null) {
      return;
    }

    client.enableFeatureFlag(disabledFlag.getName()).block();

    List<FeatureFlag> updatedFlags = client.getFeatureFlags().collectList().block();
    FeatureFlag enabledFlag =
        updatedFlags.stream()
            .filter(f -> f.getName().equals(disabledFlag.getName()))
            .findFirst()
            .orElse(null);

    assertThat(enabledFlag).isNotNull();
    assertThat(enabledFlag.getState()).isEqualTo(FeatureFlagState.ENABLED);
  }

  boolean isVersion37orLater() {
    return TestUtils.isVersion37orLater(brokerVersion);
  }

  boolean isVersion38orLater() {
    return TestUtils.isVersion38orLater(brokerVersion);
  }
}
