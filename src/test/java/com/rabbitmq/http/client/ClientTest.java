/*
 * Copyright 2015-2022 the original author or authors.
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
import static org.assertj.core.api.Assertions.assertThat;

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
import com.rabbitmq.http.client.domain.DeprecatedFeature;
import com.rabbitmq.http.client.domain.DestinationType;
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
import com.rabbitmq.http.client.domain.Page;
import com.rabbitmq.http.client.domain.PolicyInfo;
import com.rabbitmq.http.client.domain.QueryParameters;
import com.rabbitmq.http.client.domain.QueueInfo;
import com.rabbitmq.http.client.domain.QueueTotals;
import com.rabbitmq.http.client.domain.RuntimeParameter;
import com.rabbitmq.http.client.domain.ShovelDetails;
import com.rabbitmq.http.client.domain.ShovelInfo;
import com.rabbitmq.http.client.domain.ShovelStatus;
import com.rabbitmq.http.client.domain.TopicPermissions;
import com.rabbitmq.http.client.domain.UpstreamDetails;
import com.rabbitmq.http.client.domain.UpstreamInfo;
import com.rabbitmq.http.client.domain.UpstreamSetDetails;
import com.rabbitmq.http.client.domain.UpstreamSetInfo;
import com.rabbitmq.http.client.domain.UserConnectionInfo;
import com.rabbitmq.http.client.domain.UserInfo;
import com.rabbitmq.http.client.domain.UserLimits;
import com.rabbitmq.http.client.domain.UserPermissions;
import com.rabbitmq.http.client.domain.VhostInfo;
import com.rabbitmq.http.client.domain.VhostLimits;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClientTest {

  static final String DEFAULT_USERNAME = "guest";
  static final String DEFAULT_PASSWORD = "guest";

  Client client;
  String brokerVersion;

  private final ConnectionFactory cf = initializeConnectionFactory();

  protected static ConnectionFactory initializeConnectionFactory() {
    ConnectionFactory cf = new ConnectionFactory();
    cf.setAutomaticRecoveryEnabled(false);
    return cf;
  }

  @BeforeEach
  void setUp() throws Exception {
    client =
        new Client(
            new ClientParameters()
                .url(url())
                .username(DEFAULT_USERNAME)
                .password(DEFAULT_PASSWORD)
                .httpLayerFactory(JdkHttpClientHttpLayer.configure().create()));
    for (ConnectionInfo conn : client.getConnections()) {
      client.closeConnection(conn.getName());
    }
    awaitAllConnectionsClosed(client);
    brokerVersion = client.getOverview().getServerVersion();
  }

  static String url() {
    return "http://127.0.0.1:" + managementPort() + "/api/";
  }

  static int managementPort() {
    return System.getProperty("rabbitmq.management.port") == null
        ? 15672
        : Integer.valueOf(System.getProperty("rabbitmq.management.port"));
  }

  protected Connection openConnection() throws IOException, TimeoutException {
    return this.cf.newConnection();
  }

  protected Connection openConnection(String username, String password)
      throws IOException, TimeoutException {
    ConnectionFactory cf = new ConnectionFactory();
    cf.setUsername(username);
    cf.setPassword(password);
    return cf.newConnection();
  }

  protected Connection openConnection(String clientProvidedName)
      throws IOException, TimeoutException {
    return this.cf.newConnection(clientProvidedName);
  }

  protected static void awaitAllConnectionsClosed(Client client) {
    int n = 0;
    List<ConnectionInfo> result = client.getConnections();
    while (result != null && result.size() > 0 && n < 10000) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      n += 100;
      result = client.getConnections();
    }
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
  }

  protected static void verifyQueueInfo(QueueInfo x) {
    assertThat(x.getName()).isNotNull();
    assertThat(x.isDurable()).isNotNull();
    assertThat(x.isExclusive()).isNotNull();
    assertThat(x.isAutoDelete()).isNotNull();
  }

  protected static void verifyConnectionInfo(ConnectionInfo info) {
    assertThat(info.getPort()).isEqualTo(ConnectionFactory.DEFAULT_AMQP_PORT);
    assertThat(info.isUsesTLS()).isFalse();
  }

  protected static void verifyUserConnectionInfo(UserConnectionInfo info, String username) {
    assertThat(info.getUsername()).isEqualTo(username);
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

  protected static void verifyUpstreamDefinitions(
      String vhost, List<UpstreamInfo> upstreams, String upstreamName) {
    assertThat(upstreams).isNotEmpty();
    UpstreamInfo upstream =
        upstreams.stream().filter(u -> u.getName().equals(upstreamName)).findFirst().orElse(null);
    assertThat(upstream).isNotNull();
    assertThat(upstream.getName()).isEqualTo(upstreamName);
    assertThat(upstream.getVhost()).isEqualTo(vhost);
    assertThat(upstream.getComponent()).isEqualTo("federation-upstream");
    assertThat(upstream.getValue().getUri()).isEqualTo("amqp://localhost:5672");
  }

  protected static void declareUpstream(Client client, String vhost, String upstreamName) {
    UpstreamDetails upstreamDetails = new UpstreamDetails();
    upstreamDetails.setUri("amqp://localhost:5672");
    client.declareUpstream(vhost, upstreamName, upstreamDetails);
  }

  protected static <T> T awaitEventPropagation(Supplier<T> callback) {
    if (callback != null) {
      int n = 0;
      T result = callback.get();
      while (isEmptyResult(result) && n < 10000) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        n += 100;
        result = callback.get();
      }
      assertThat(n).isLessThan(10000);
      return result;
    } else {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return null;
    }
  }

  private static <T> boolean isEmptyResult(T result) {
    if (result == null) return true;
    if (result instanceof Collection) return ((Collection<?>) result).isEmpty();
    if (result.getClass().isArray()) return Array.getLength(result) == 0;
    return false;
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

  protected static int exceptionStatus(Exception e) {
    if (e instanceof HttpClientException) {
      return ((HttpClientException) e).status();
    } else {
      throw new IllegalArgumentException("Unknown exception type: " + e.getClass());
    }
  }

  boolean isVersion37orLater() {
    return TestUtils.isVersion37orLater(brokerVersion);
  }

  boolean isVersion38orLater() {
    return TestUtils.isVersion38orLater(brokerVersion);
  }

  boolean isVersion310orLater() {
    return TestUtils.isVersion310orLater(brokerVersion);
  }

  @Test
  void userInfoDecoding() throws Exception {
    // when: username and password are encoded in the URL
    ClientParameters clientParameters =
        new ClientParameters()
            .url("http://test+user:test%40password@localhost:" + managementPort() + "/api/");

    // then: username and password are decoded
    assertThat(clientParameters.getUsername()).isEqualTo("test user");
    assertThat(clientParameters.getPassword()).isEqualTo("test@password");
  }

  @Test
  void getApiOverview() throws Exception {
    // when: client requests GET /api/overview
    Connection conn = openConnection();
    Channel ch = conn.createChannel();
    for (int i = 0; i < 1000; i++) {
      ch.basicPublish("", "", null, null);
    }

    OverviewResponse res = client.getOverview();
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
  void getApiNodes() {
    // when: client retrieves a list of cluster nodes
    List<NodeInfo> res = client.getNodes();
    NodeInfo node = res.get(0);

    // then: the list is returned
    assertThat(res).hasSizeGreaterThanOrEqualTo(1);
    verifyNode(node);
  }

  @Test
  void getApiNodesWithName() {
    // when: client retrieves a list of cluster nodes
    List<NodeInfo> res = client.getNodes();
    String name = res.get(0).getName();
    NodeInfo node = client.getNode(name);

    // then: the list is returned
    assertThat(res).hasSizeGreaterThanOrEqualTo(1);
    verifyNode(node);
  }

  @Test
  void getApiConnections() throws Exception {
    // given: an open RabbitMQ client connection
    Connection conn = openConnection();

    // when: client retrieves a list of connections
    List<ConnectionInfo> res = awaitEventPropagation(() -> client.getConnections());
    ConnectionInfo fst = res.get(0);

    // then: the list is returned
    assertThat(res).hasSizeGreaterThanOrEqualTo(1);
    verifyConnectionInfo(fst);

    // cleanup
    conn.close();
  }

  @Test
  void getApiConnectionsWithPaging() throws Exception {
    // given: some named RabbitMQ client connections
    List<Connection> connections = new ArrayList<>();
    for (int i = 0; i <= 15; i++) {
      String name = "list-connections-with-paging-test-" + i;
      Connection conn = openConnection(name);
      connections.add(conn);
    }

    // when: client queries the first page of connections
    waitAtMostUntilTrue(10, () -> client.getConnections().size() == connections.size());
    QueryParameters queryParameters = new QueryParameters().pagination().pageSize(10).query();
    Page<ConnectionInfo> page = client.getConnections(queryParameters);

    // then: a list of paged connections is returned
    assertThat(page.getFilteredCount()).isEqualTo(connections.size());
    assertThat(page.getItemCount()).isEqualTo(10);
    assertThat(page.getPageCount()).isEqualTo(2);
    assertThat(page.getTotalCount()).isGreaterThanOrEqualTo(page.getFilteredCount());
    assertThat(page.getPage()).isEqualTo(1);
    verifyConnectionInfo(page.getItemsAsList().get(0));

    // cleanup
    for (Connection conn : connections) {
      conn.close();
    }
  }

  @Test
  void getApiConnectionsWithName() throws Exception {
    // given: an open RabbitMQ client connection
    Connection conn = openConnection();

    // when: client retrieves connection info with the correct name
    List<ConnectionInfo> xs = awaitEventPropagation(() -> client.getConnections());
    ConnectionInfo x = client.getConnection(xs.get(0).getName());

    // then: the info is returned
    verifyConnectionInfo(x);

    // cleanup
    conn.close();
  }

  @Test
  void getApiConnectionsWithNameWithClientProvidedName() throws Exception {
    // given: an open RabbitMQ client connection with client-provided name
    String s = UUID.randomUUID().toString();
    Connection conn = openConnection(s);

    // when: client retrieves connection info with the correct name
    List<ConnectionInfo> xs = awaitEventPropagation(() -> client.getConnections());
    // applying filter as some previous connections can still show up in the management API
    xs =
        xs.stream()
            .filter(ci -> s.equals(ci.getClientProperties().getConnectionName()))
            .collect(Collectors.toList());
    ConnectionInfo x = client.getConnection(xs.get(0).getName());

    // then: the info is returned
    verifyConnectionInfo(x);
    assertThat(x.getClientProperties().getConnectionName()).isEqualTo(s);

    // cleanup
    conn.close();
  }

  @Test
  void getApiConnectionsUsernameWithName() throws Exception {
    if (!isVersion310orLater()) return;

    // given: an open RabbitMQ client connection
    Connection conn = openConnection();
    String username = "guest";

    // when: client retrieves user connection info for the given name
    List<UserConnectionInfo> xs =
        awaitEventPropagation(() -> client.getConnectionsOfUser(username));
    UserConnectionInfo uc = xs.get(0);

    // then: the info is returned
    verifyUserConnectionInfo(uc, username);

    // cleanup
    if (isVersion310orLater()) {
      conn.close();
    }
  }

  @Test
  void deleteApiConnectionsUsernameWithName() throws Exception {
    if (!isVersion310orLater()) return;

    // given: an open RabbitMQ client connection
    CountDownLatch latch = new CountDownLatch(1);
    String s = UUID.randomUUID().toString();
    String username = "guest";
    Connection conn = openConnection(s);
    conn.addShutdownListener(e -> latch.countDown());
    assertThat(conn.isOpen()).isTrue();

    // and: that shows up on connection list of a user
    List<UserConnectionInfo> xs =
        awaitEventPropagation(() -> client.getConnectionsOfUser(username));
    // applying filter as some previous connections can still show up in the management API
    xs = xs.stream().filter(uci -> username.equals(uci.getUsername())).collect(Collectors.toList());
    assertThat(xs).isNotEmpty();

    // when: client closes all connections of the user
    client.closeAllConnectionsOfUser(username);

    // and: some time passes
    assertThat(awaitOn(latch)).isTrue();

    // then: the connection is closed
    assertThat(conn.isOpen()).isFalse();

    // cleanup
    if (isVersion310orLater() && conn.isOpen()) {
      conn.close();
    }
  }

  @Test
  void deleteApiConnectionsWithName() throws Exception {
    // given: an open RabbitMQ client connection
    CountDownLatch latch = new CountDownLatch(1);
    String s = UUID.randomUUID().toString();
    Connection conn = openConnection(s);
    conn.addShutdownListener(e -> latch.countDown());
    assertThat(conn.isOpen()).isTrue();

    // when: client closes the connection
    List<ConnectionInfo> xs = awaitEventPropagation(() -> client.getConnections());
    // applying filter as some previous connections can still show up in the management API
    xs =
        xs.stream()
            .filter(ci -> s.equals(ci.getClientProperties().getConnectionName()))
            .collect(Collectors.toList());
    for (ConnectionInfo ci : xs) {
      client.closeConnection(ci.getName());
    }

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
  void deleteApiConnectionsWithNameWithUserProvidedReason() throws Exception {
    // given: an open RabbitMQ client connection
    CountDownLatch latch = new CountDownLatch(1);
    String s = UUID.randomUUID().toString();
    Connection conn = openConnection(s);
    conn.addShutdownListener(e -> latch.countDown());
    assertThat(conn.isOpen()).isTrue();

    // when: client closes the connection
    List<ConnectionInfo> xs = awaitEventPropagation(() -> client.getConnections());
    // applying filter as some previous connections can still show up in the management API
    xs =
        xs.stream()
            .filter(ci -> s.equals(ci.getClientProperties().getConnectionName()))
            .collect(Collectors.toList());
    for (ConnectionInfo ci : xs) {
      client.closeConnection(ci.getName(), "because reasons!");
    }

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
  void getApiChannels() throws Exception {
    // given: an open RabbitMQ client connection with 1 channel
    String s = UUID.randomUUID().toString();
    Connection conn = openConnection(s);
    Channel ch = conn.createChannel();

    // when: client lists channels
    List<ConnectionInfo> xs = awaitEventPropagation(() -> client.getConnections());
    // applying filter as some previous connections can still show up in the management API
    xs =
        xs.stream()
            .filter(ci -> s.equals(ci.getClientProperties().getConnectionName()))
            .collect(Collectors.toList());
    String cn = xs.get(0).getName();
    List<ChannelInfo> chs = awaitEventPropagation(() -> client.getChannels());
    // channel name starts with the connection name
    chs = chs.stream().filter(chi -> chi.getName().startsWith(cn)).collect(Collectors.toList());
    ChannelInfo chi = chs.get(0);

    // then: the list is returned
    verifyChannelInfo(chi, ch);

    // cleanup
    if (conn.isOpen()) {
      conn.close();
    }
  }

  @Test
  void getApiChannelsWithPaging() throws Exception {
    // given: some AMQP channels
    Connection conn = openConnection();
    List<Channel> channels = new ArrayList<>();
    for (int i = 1; i <= 16; i++) {
      channels.add(conn.createChannel(i));
    }

    // when: client queries the first page of channels
    waitAtMostUntilTrue(10, () -> client.getChannels().size() == channels.size());
    QueryParameters queryParameters = new QueryParameters().pagination().pageSize(10).query();
    Page<ChannelInfo> page = client.getChannels(queryParameters);

    // then: a list of paged channels is returned
    assertThat(page.getFilteredCount()).isEqualTo(channels.size());
    assertThat(page.getItemCount()).isEqualTo(10);
    assertThat(page.getPageCount()).isEqualTo(2);
    assertThat(page.getTotalCount()).isGreaterThanOrEqualTo(page.getFilteredCount());
    assertThat(page.getPage()).isEqualTo(1);
    ChannelInfo channelInfo = page.getItemsAsList().get(0);
    Channel originalChannel =
        channels.stream()
            .filter(ch -> ch.getChannelNumber() == channelInfo.getNumber())
            .findFirst()
            .orElse(null);
    verifyChannelInfo(channelInfo, originalChannel);

    // cleanup
    conn.close();
  }

  @Test
  void getApiConnectionsWithNameChannels() throws Exception {
    // given: an open RabbitMQ client connection with 1 channel
    String s = UUID.randomUUID().toString();
    Connection conn = openConnection(s);
    Channel ch = conn.createChannel();

    // when: client lists channels on that connection
    List<ConnectionInfo> xs = awaitEventPropagation(() -> client.getConnections());
    // applying filter as some previous connections can still show up in the management API
    xs =
        xs.stream()
            .filter(ci -> s.equals(ci.getClientProperties().getConnectionName()))
            .collect(Collectors.toList());
    String cn = xs.get(0).getName();

    List<ChannelInfo> chs = awaitEventPropagation(() -> client.getChannels(cn));
    ChannelInfo chi = chs.get(0);

    // then: the list is returned
    verifyChannelInfo(chi, ch);

    // cleanup
    if (conn.isOpen()) {
      conn.close();
    }
  }

  @Test
  void getApiChannelsWithName() throws Exception {
    // given: an open RabbitMQ client connection with 1 channel
    String s = UUID.randomUUID().toString();
    Connection conn = openConnection(s);
    Channel ch = conn.createChannel();

    // when: client retrieves channel info
    List<ConnectionInfo> xs = awaitEventPropagation(() -> client.getConnections());
    // applying filter as some previous connections can still show up in the management API
    xs =
        xs.stream()
            .filter(ci -> s.equals(ci.getClientProperties().getConnectionName()))
            .collect(Collectors.toList());
    String cn = xs.get(0).getName();
    List<ChannelInfo> chs = awaitEventPropagation(() -> client.getChannels(cn));
    ChannelInfo chi = client.getChannel(chs.get(0).getName());

    // then: the info is returned
    verifyChannelInfo(chi, ch);

    // cleanup
    if (conn.isOpen()) {
      conn.close();
    }
  }

  @Test
  void getApiExchanges() {
    // when: client retrieves the list of exchanges across all vhosts
    List<ExchangeInfo> xs = client.getExchanges();
    ExchangeInfo x = xs.get(0);

    // then: the list is returned
    verifyExchangeInfo(x);
  }

  @Test
  void getApiExchangesWithPaging() {
    // given: at least one exchange was declared

    // when: client lists exchanges
    QueryParameters queryParameters = new QueryParameters().pagination().pageSize(10).query();
    Page<ExchangeInfo> pagedXs = client.getExchanges("/", queryParameters);

    // then: a list of paged exchanges is returned
    ExchangeInfo x =
        pagedXs.getItemsAsList().stream()
            .filter(e -> "amq.fanout".equals(e.getName()))
            .findFirst()
            .orElse(null);
    verifyExchangeInfo(x);
  }

  @Test
  void getApiExchangesWithVhostWhenVhostExists() {
    // when: client retrieves the list of exchanges in a particular vhost
    List<ExchangeInfo> xs = client.getExchanges("/");

    // then: the list is returned
    ExchangeInfo x =
        xs.stream().filter(e -> "amq.fanout".equals(e.getName())).findFirst().orElse(null);
    verifyExchangeInfo(x);
  }

  @Test
  void getApiExchangesWithVhostWhenVhostDoesNotExist() {
    // given: vhost lolwut does not exist
    String v = "lolwut";
    client.deleteVhost(v);

    // when: client retrieves the list of exchanges in that vhost
    List<ExchangeInfo> xs = client.getExchanges(v);

    // then: null is returned
    assertThat(xs).isNull();
  }

  @Test
  void getApiExchangesWithVhostWithNameWhenBothVhostAndExchangeExist() {
    // when: client retrieves exchange amq.fanout in vhost /
    ExchangeInfo x = client.getExchange("/", "amq.fanout");

    // then: exchange info is returned
    assertThat(x).isNotNull();
    assertThat(x.getName()).isEqualTo("amq.fanout");
    assertThat(x.getVhost()).isEqualTo("/");
    verifyExchangeInfo(x);
  }

  @Test
  void putApiExchangesWithVhostWithNameWhenVhostExists() {
    // given: fanout exchange hop.test in vhost /
    String v = "/";
    String s = "hop.test";
    client.declareExchange(v, s, new ExchangeInfo("fanout", false, false));

    // when: client lists exchanges in vhost /
    List<ExchangeInfo> xs = client.getExchanges(v);

    // then: hop.test is listed
    ExchangeInfo x = xs.stream().filter(e -> s.equals(e.getName())).findFirst().orElse(null);
    assertThat(x).isNotNull();
    verifyExchangeInfo(x);

    // cleanup
    client.deleteExchange(v, s);
  }

  @Test
  void deleteApiExchangesWithVhostWithName() {
    // given: fanout exchange hop.test in vhost /
    String v = "/";
    String s = "hop.test";
    client.declareExchange(v, s, new ExchangeInfo("fanout", false, false));

    List<ExchangeInfo> xs = client.getExchanges(v);
    ExchangeInfo x = xs.stream().filter(e -> s.equals(e.getName())).findFirst().orElse(null);
    assertThat(x).isNotNull();
    verifyExchangeInfo(x);

    // when: client deletes exchange hop.test in vhost /
    client.deleteExchange(v, s);

    // and: exchange list in / is reloaded
    xs = client.getExchanges(v);

    // then: hop.test no longer exists
    assertThat(xs.stream().anyMatch(e -> s.equals(e.getName()))).isFalse();
  }

  @Test
  void postApiExchangesWithVhostWithNamePublish() throws Exception {
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
    properties.put("headers", Map.of("header1", "value1"));
    boolean routed =
        client.publish(
            v,
            "amq.direct",
            q,
            new OutboundMessage().payload("Hello world!").utf8Encoded().properties(properties));

    // then: the message is routed to the queue and consumed
    assertThat(routed).isTrue();
    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
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
  void getApiExchangesWithVhostWithNameBindingsSource() throws Exception {
    // given: a queue named hop.queue1
    Connection conn = openConnection();
    Channel ch = conn.createChannel();
    String q = "hop.queue1";
    ch.queueDeclare(q, false, false, false, null);

    // when: client lists bindings of default exchange
    List<BindingInfo> xs = client.getBindingsBySource("/", "");

    // then: there is an automatic binding for hop.queue1
    BindingInfo x =
        xs.stream()
            .filter(
                b ->
                    "".equals(b.getSource())
                        && DestinationType.QUEUE.equals(b.getDestinationType())
                        && q.equals(b.getDestination()))
            .findFirst()
            .orElse(null);
    assertThat(x).isNotNull();

    // cleanup
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiExchangesWithVhostWithNameBindingsDestination() throws Exception {
    // given: an exchange named hop.exchange1 which is bound to amq.fanout
    Connection conn = openConnection();
    Channel ch = conn.createChannel();
    String src = "amq.fanout";
    String dest = "hop.exchange1";
    ch.exchangeDeclare(dest, "fanout");
    ch.exchangeBind(dest, src, "");

    // when: client lists bindings of amq.fanout
    List<BindingInfo> xs = client.getExchangeBindingsByDestination("/", dest);

    // then: there is a binding for hop.exchange1
    BindingInfo x =
        xs.stream()
            .filter(
                b ->
                    src.equals(b.getSource())
                        && DestinationType.EXCHANGE.equals(b.getDestinationType())
                        && dest.equals(b.getDestination()))
            .findFirst()
            .orElse(null);
    assertThat(x).isNotNull();

    // cleanup
    ch.exchangeDelete(dest);
    conn.close();
  }

  @Test
  void getApiQueues() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();
    List<QueueInfo> xs = client.getQueues();
    QueueInfo x = xs.get(0);
    verifyQueueInfo(x);
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiQueuesWithVhostWithNameWithPolicyDefinition() throws Exception {
    String v = "/";
    String s = "hop.test";
    Map<String, Object> pd = Collections.singletonMap("expires", 30000);
    PolicyInfo pi = new PolicyInfo(".*", 1, "queues", pd);
    client.declarePolicy(v, s, pi);
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();
    waitAtMostUntilTrue(
        10,
        () -> {
          QueueInfo qi = client.getQueue(v, q);
          return qi != null && qi.getEffectivePolicyDefinition() != null;
        });
    QueueInfo queueInfo = client.getQueue(v, q);
    assertThat(queueInfo.getEffectivePolicyDefinition()).isEqualTo(pd);
    ch.queueDelete(q);
    conn.close();
    client.deletePolicy(v, s);
  }

  @Test
  void getApiQueuesWithDetails() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.submit(
        () -> {
          while (!Thread.currentThread().isInterrupted()) {
            try {
              ch.basicPublish("", q, null, "".getBytes(StandardCharsets.UTF_8));
              Thread.sleep(10);
            } catch (Exception e) {
              return;
            }
          }
        });
    DetailsParameters detailsParameters =
        new DetailsParameters().messageRates(60, 5).lengths(60, 5);
    Supplier<QueueInfo> request =
        () ->
            client.getQueues(detailsParameters).stream()
                .filter(qi -> qi.getName().equals(q))
                .findFirst()
                .orElse(null);
    waitAtMostUntilTrue(
        10,
        () -> {
          QueueInfo qi = request.get();
          return qi != null
              && qi.getMessagesDetails() != null
              && qi.getMessagesDetails().getRate() > 0;
        });
    QueueInfo x = request.get();
    verifyQueueInfo(x);
    assertThat(x.getMessagesDetails()).isNotNull();
    assertThat(x.getMessagesDetails().getAverage()).isGreaterThan(0);
    executorService.shutdownNow();
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiQueuesWithPaging() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    List<String> queues = new ArrayList<>();
    for (int i = 0; i <= 15; i++) {
      String qn = "queue-for-paging-test-" + i;
      ch.queueDeclare(qn, false, false, false, null);
      queues.add(qn);
    }
    QueryParameters queryParameters =
        new QueryParameters()
            .name("^queue-for-paging-test-*", true)
            .pagination()
            .pageSize(10)
            .query();
    Page<QueueInfo> page = client.getQueues(queryParameters);
    assertThat(page.getFilteredCount()).isEqualTo(queues.size());
    assertThat(page.getItemCount()).isEqualTo(10);
    assertThat(page.getPageCount()).isEqualTo(2);
    for (String queue : queues) ch.queueDelete(queue);
    conn.close();
  }

  @Test
  void getApiQueuesWithPagingAndNavigating() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    List<String> queues = new ArrayList<>();
    for (int i = 0; i <= 15; i++) {
      String qn = "queue-for-paging-and-navigating-test-" + i;
      ch.queueDeclare(qn, false, false, false, null);
      queues.add(qn);
    }
    QueryParameters queryParameters =
        new QueryParameters()
            .name("^queue-for-paging-and-navigating-test-*", true)
            .pagination()
            .pageSize(10)
            .query();
    Page<QueueInfo> page = client.getQueues(queryParameters);
    page = client.getQueues(queryParameters.pagination().nextPage(page).query());
    assertThat(page.getFilteredCount()).isEqualTo(queues.size());
    assertThat(page.getItemCount()).isEqualTo(6);
    assertThat(page.getPage()).isEqualTo(2);
    for (String queue : queues) ch.queueDelete(queue);
    conn.close();
  }

  @Test
  void getApiQueuesWithPagingAndDetails() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    List<String> queues = new ArrayList<>();
    for (int i = 0; i <= 15; i++) {
      String qn = "queue-for-paging-and-details-test-" + i;
      ch.queueDeclare(qn, false, false, false, null);
      ch.queueBind(qn, "amq.fanout", "");
      queues.add(qn);
    }
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.submit(
        () -> {
          while (!Thread.currentThread().isInterrupted()) {
            try {
              ch.basicPublish("amq.fanout", "", null, "".getBytes(StandardCharsets.UTF_8));
              Thread.sleep(10);
            } catch (Exception e) {
              return;
            }
          }
        });
    QueryParameters queryParameters =
        new DetailsParameters()
            .messageRates(60, 5)
            .lengths(60, 5)
            .queryParameters()
            .name("^queue-for-paging-and-details-test-*", true)
            .pagination()
            .pageSize(10)
            .query();
    Supplier<Page<QueueInfo>> request = () -> client.getQueues(queryParameters);
    waitAtMostUntilTrue(
        10,
        () -> {
          Page<QueueInfo> p = request.get();
          return p.getFilteredCount() == queues.size()
              && p.getItems().length > 0
              && p.getItems()[0].getMessagesDetails() != null
              && p.getItems()[0].getMessagesDetails().getRate() > 0;
        });
    Page<QueueInfo> page = request.get();
    assertThat(page.getFilteredCount()).isEqualTo(queues.size());
    assertThat(page.getItemCount()).isEqualTo(10);
    executorService.shutdownNow();
    for (String queue : queues) ch.queueDelete(queue);
    conn.close();
  }

  @Test
  void getApiQueuesWithVhostWhenVhostExists() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();
    List<QueueInfo> xs = client.getQueues("/");
    verifyQueueInfo(xs.get(0));
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiQueuesWithVhostWhenVhostDoesNotExist() {
    String v = "lolwut";
    client.deleteVhost(v);
    List<QueueInfo> xs = client.getQueues(v);
    assertThat(xs).isNull();
  }

  @Test
  void getApiQueuesWithVhostWithNameWhenBothVhostAndQueueExist() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();
    QueueInfo x = client.getQueue("/", q);
    assertThat(x.getVhost()).isEqualTo("/");
    assertThat(x.getName()).isEqualTo(q);
    verifyQueueInfo(x);
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiQueuesWithVhostWithNameWithAnExclusiveQueue() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String s = "hop.q1.exclusive";
    ch.queueDelete(s);
    String q = ch.queueDeclare(s, false, true, false, null).getQueue();
    QueueInfo x = client.getQueue("/", q);
    assertThat(x.isExclusive()).isTrue();
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiQueuesWithNameWithDetails() throws Exception {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();
    executorService.submit(
        () -> {
          while (!Thread.currentThread().isInterrupted()) {
            try {
              ch.basicPublish("", q, null, "".getBytes(StandardCharsets.UTF_8));
              Thread.sleep(10);
            } catch (Exception e) {
              return;
            }
          }
        });
    Supplier<QueueInfo> request =
        () -> client.getQueue("/", q, new DetailsParameters().messageRates(60, 5).lengths(60, 5));
    waitAtMostUntilTrue(
        10,
        () -> {
          QueueInfo qi = request.get();
          return qi.getMessagesDetails() != null && qi.getMessagesDetails().getRate() > 0;
        });
    QueueInfo x = request.get();
    verifyQueueInfo(x);
    assertThat(x.getMessagesDetails()).isNotNull();
    assertThat(x.getMessagesReadyDetails()).isNotNull();
    assertThat(x.getMessageStats()).isNotNull();
    executorService.shutdown();
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiQueuesWithVhostWithNameWhenQueueDoesNotExist() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = "lolwut";
    ch.queueDelete(q);
    QueueInfo x = client.getQueue("/", q);
    assertThat(x).isNull();
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void putApiQueuesWithVhostWithNameWhenVhostExists() {
    String v = "/";
    String s = "hop.test";
    client.declareQueue(v, s, new QueueInfo(false, false, false));
    List<QueueInfo> xs = client.getQueues(v);
    QueueInfo x = xs.stream().filter(qi -> s.equals(qi.getName())).findFirst().orElse(null);
    assertThat(x).isNotNull();
    assertThat(x.isDurable()).isFalse();
    client.deleteQueue(v, s);
  }

  @Test
  void getApiConsumers() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();
    String consumerTag = ch.basicConsume(q, true, (ctag, msg) -> {}, ctag -> {});
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }
    List<ConsumerDetails> cs = client.getConsumers();
    ConsumerDetails cons =
        cs.stream().filter(c -> c.getConsumerTag().equals(consumerTag)).findFirst().orElse(null);
    assertThat(cons).isNotNull();
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiConsumersWithVhost() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();
    String consumerTag = ch.basicConsume(q, true, (ctag, msg) -> {}, ctag -> {});
    waitAtMostUntilTrue(10, () -> client.getConsumers().size() == 1);
    List<ConsumerDetails> cs = client.getConsumers("/");
    ConsumerDetails cons =
        cs.stream().filter(c -> c.getConsumerTag().equals(consumerTag)).findFirst().orElse(null);
    assertThat(cons).isNotNull();
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiConsumersWithVhostWithNoConsumersInVhost() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String q = ch.queueDeclare().getQueue();
    ch.basicConsume(q, true, (ctag, msg) -> {}, ctag -> {});
    waitAtMostUntilTrue(10, () -> client.getConsumers().size() == 1);
    List<ConsumerDetails> cs = client.getConsumers("vh1");
    assertThat(cs).isEmpty();
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void putApiPoliciesWithVhostWithName() {
    String v = "/";
    Map<String, Object> d = new HashMap<>();
    d.put("expires", 30000);
    String s = "hop.test";
    client.declarePolicy(v, s, new PolicyInfo(".*", 1, "queues", d));
    List<PolicyInfo> ps = client.getPolicies(v);
    PolicyInfo p = ps.stream().filter(pi -> s.equals(pi.getName())).findFirst().orElse(null);
    assertThat(p).isNotNull();
    assertThat(p.getVhost()).isEqualTo(v);
    assertThat(p.getName()).isEqualTo(s);
    assertThat(p.getPriority()).isEqualTo(1);
    assertThat(p.getApplyTo()).isEqualTo("queues");
    assertThat(p.getDefinition()).isEqualTo(d);
    client.deletePolicy(v, s);
  }

  @Test
  void putApiOperatorPoliciesWithVhostWithName() {
    String v = "/";
    Map<String, Object> d = new HashMap<>();
    d.put("max-length", 6);
    String s = "hop.test";
    client.declareOperatorPolicy(v, s, new PolicyInfo(".*", 1, null, d));
    List<PolicyInfo> ps = client.getOperatorPolicies(v);
    PolicyInfo p = ps.stream().filter(pi -> s.equals(pi.getName())).findFirst().orElse(null);
    assertThat(p).isNotNull();
    assertThat(p.getApplyTo()).isEqualTo("all");
    client.deleteOperatorPolicy(v, s);
  }

  @Test
  void putApiQueuesWithVhostWithNameWhenVhostDoesNotExist() {
    String v = "lolwut";
    client.deleteVhost(v);
    String s = "hop.test";
    try {
      client.declareQueue(v, s, new QueueInfo(false, false, false));
      Assertions.fail("Expected exception");
    } catch (Exception e) {
      assertThat(exceptionStatus(e)).isEqualTo(404);
    }
  }

  @Test
  void deleteApiQueuesWithVhostWithName() {
    String s = UUID.randomUUID().toString();
    String v = "/";
    client.declareQueue(v, s, new QueueInfo(false, false, false));
    List<QueueInfo> xs = client.getQueues(v);
    QueueInfo x = xs.stream().filter(qi -> s.equals(qi.getName())).findFirst().orElse(null);
    assertThat(x).isNotNull();
    verifyQueueInfo(x);
    client.deleteQueue(v, s);
    xs = client.getQueues(v);
    assertThat(xs.stream().anyMatch(qi -> s.equals(qi.getName()))).isFalse();
  }

  @Test
  void deleteApiQueuesWithVhostWithNameIfEmptyTrue() {
    String queue = UUID.randomUUID().toString();
    String v = "/";
    client.declareQueue(v, queue, new QueueInfo(false, false, false));
    client.publish(v, "amq.default", queue, new OutboundMessage().payload("test"));
    try {
      client.deleteQueue(v, queue, new DeleteQueueParameters(true, false));
      Assertions.fail("Expected exception");
    } catch (Exception e) {
      assertThat(exceptionStatus(e)).isEqualTo(400);
    }
    client.deleteQueue(v, queue);
  }

  @Test
  void getApiBindings() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String x = "amq.fanout";
    String q1 = ch.queueDeclare().getQueue();
    String q2 = ch.queueDeclare().getQueue();
    String q3 = ch.queueDeclare().getQueue();
    ch.queueBind(q1, x, "");
    ch.queueBind(q2, x, "");
    ch.queueBind(q3, x, "");
    List<BindingInfo> xs = client.getBindings();
    long count =
        xs.stream()
            .filter(
                b ->
                    DestinationType.QUEUE.equals(b.getDestinationType()) && x.equals(b.getSource()))
            .count();
    assertThat(count).isGreaterThanOrEqualTo(3);
    ch.queueDelete(q1);
    ch.queueDelete(q2);
    ch.queueDelete(q3);
    conn.close();
  }

  @Test
  void getApiBindingsWithVhost() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String x = "amq.topic";
    String q1 = ch.queueDeclare().getQueue();
    String q2 = ch.queueDeclare().getQueue();
    ch.queueBind(q1, x, "hop.*");
    ch.queueBind(q2, x, "api.test.#");
    List<BindingInfo> xs = client.getBindings("/");
    long count =
        xs.stream()
            .filter(
                b ->
                    DestinationType.QUEUE.equals(b.getDestinationType()) && x.equals(b.getSource()))
            .count();
    assertThat(count).isGreaterThanOrEqualTo(2);
    ch.queueDelete(q1);
    ch.queueDelete(q2);
    conn.close();
  }

  @Test
  void getApiBindingsWithVhostExample2() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String x = "amq.topic";
    String q = "hop.test";
    ch.queueDeclare(q, false, false, false, null);
    ch.queueBind(q, x, "hop.*");
    List<BindingInfo> xs = client.getBindings("/");
    BindingInfo b =
        xs.stream()
            .filter(
                bi ->
                    DestinationType.QUEUE.equals(bi.getDestinationType())
                        && x.equals(bi.getSource())
                        && q.equals(bi.getDestination()))
            .findFirst()
            .orElse(null);
    assertThat(b).isNotNull();
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiQueuesWithVhostWithNameBindings() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String x = "amq.topic";
    String q = "hop.test";
    ch.queueDeclare(q, false, false, false, null);
    ch.queueBind(q, x, "hop.*");
    List<BindingInfo> xs = client.getQueueBindings("/", q);
    BindingInfo b =
        xs.stream()
            .filter(
                bi ->
                    DestinationType.QUEUE.equals(bi.getDestinationType())
                        && x.equals(bi.getSource())
                        && q.equals(bi.getDestination()))
            .findFirst()
            .orElse(null);
    assertThat(b).isNotNull();
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiBindingsWithVhostEWithExchangeQWithQueue() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String x = "amq.topic";
    String q = "hop.test";
    ch.queueDeclare(q, false, false, false, null);
    ch.queueBind(q, x, "hop.*");
    List<BindingInfo> xs = client.getQueueBindingsBetween("/", x, q);
    BindingInfo b = xs.get(0);
    assertThat(xs).hasSize(1);
    assertThat(b.getSource()).isEqualTo(x);
    assertThat(b.getDestination()).isEqualTo(q);
    assertThat(b.getDestinationType()).isEqualTo(DestinationType.QUEUE);
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiBindingsWithVhostEWithSourceEWithDestination() throws Exception {
    Connection conn = cf.newConnection();
    Channel ch = conn.createChannel();
    String s = "amq.fanout";
    String d = "hop.test";
    ch.exchangeDeclare(d, "fanout", false);
    ch.exchangeBind(d, s, "");
    List<BindingInfo> xs = client.getExchangeBindingsBetween("/", s, d);
    BindingInfo b = xs.get(0);
    assertThat(xs).hasSize(1);
    assertThat(b.getSource()).isEqualTo(s);
    assertThat(b.getDestination()).isEqualTo(d);
    assertThat(b.getDestinationType()).isEqualTo(DestinationType.EXCHANGE);
    ch.exchangeDelete(d);
    conn.close();
  }

  @Test
  void postApiBindingsWithVhostEWithSourceEWithDestination() {
    String v = "/";
    String s = "amq.fanout";
    String d = "hop.test";
    client.deleteExchange(v, d);
    client.declareExchange(v, d, new ExchangeInfo("fanout", false, false));
    Map<String, Object> args = new HashMap<>();
    args.put("arg1", "value1");
    args.put("arg2", "value2");
    client.bindExchange(v, d, s, "", args);
    List<BindingInfo> xs = client.getExchangeBindingsBetween(v, s, d);
    BindingInfo b = xs.get(0);
    assertThat(xs).hasSize(1);
    assertThat(b.getArguments()).hasSize(2);
    assertThat(b.getArguments().get("arg1")).isEqualTo("value1");
    client.deleteExchange(v, d);
  }

  @Test
  void postApiBindingsWithVhostEWithExchangeQWithQueue() {
    String v = "/";
    String x = "amq.topic";
    String q = "hop.test";
    client.declareQueue(v, q, new QueueInfo(false, false, false));
    Map<String, Object> args = new HashMap<>();
    args.put("arg1", "value1");
    args.put("arg2", "value2");
    client.bindQueue(v, q, x, "", args);
    List<BindingInfo> xs = client.getQueueBindingsBetween(v, x, q);
    BindingInfo b = xs.get(0);
    assertThat(xs).hasSize(1);
    assertThat(b.getArguments()).hasSize(2);
    client.deleteQueue(v, q);
  }

  @Test
  void postApiQueuesWithVhostWithExchangeGet() throws Exception {
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
    List<InboundMessage> messages =
        client.get(v, q, messageCount, GetAckMode.NACK_REQUEUE_TRUE, GetEncoding.AUTO, -1);
    assertThat(messages).hasSize(messageCount);
    InboundMessage message = messages.get(0);
    assertThat(message.getPayload()).startsWith("payload");
    assertThat(message.getPayloadBytes()).isEqualTo("payload".length() + 1);
    assertThat(message.isRedelivered()).isFalse();
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void postApiQueuesWithVhostWithExchangeGetForOneMessage() throws Exception {
    String v = "/";
    Connection conn = openConnection();
    Channel ch = conn.createChannel();
    String q = "hop.get";
    ch.queueDeclare(q, false, false, false, null);
    ch.confirmSelect();
    ch.basicPublish("", q, null, "payload".getBytes(Charset.forName("UTF-8")));
    ch.waitForConfirms(5_000);
    InboundMessage message = client.get(v, q);
    assertThat(message.getPayload()).isEqualTo("payload");
    assertThat(message.getPayloadBytes()).isEqualTo("payload".length());
    assertThat(message.isRedelivered()).isFalse();
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void deleteApiQueuesWithVhostWithNameContents() throws Exception {
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
    client.purgeQueue("/", q);
    AMQP.Queue.DeclareOk qi2 = ch.queueDeclarePassive(q);
    assertThat(qi2.getMessageCount()).isEqualTo(0);
    ch.queueDelete(q);
    conn.close();
  }

  @Test
  void getApiVhosts() {
    List<VhostInfo> vhs = client.getVhosts();
    VhostInfo vhi = vhs.get(0);
    verifyVhost(vhi, client.getOverview().getServerVersion());
  }

  @Test
  void getApiVhostsWithName() {
    VhostInfo vhi = client.getVhost("/");
    verifyVhost(vhi, client.getOverview().getServerVersion());
  }

  @org.junit.jupiter.api.condition.DisabledOnOs(org.junit.jupiter.api.condition.OS.WINDOWS)
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
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
        "±!@^&#*"
      })
  void putApiVhostsWithName(String name) {
    client.createVhost(name);
    VhostInfo vhi = client.getVhost(name);
    assertThat(vhi.getName()).isEqualTo(name);
    client.deleteVhost(name);
  }

  @Test
  void putApiVhostsWithNameWithMetadata() {
    if (!isVersion38orLater()) return;
    String vhost = "vhost-with-metadata";
    client.deleteVhost(vhost);
    client.createVhost(vhost, true, "vhost description", "production", "application1", "realm1");
    VhostInfo vhi = client.getVhost(vhost);
    assertThat(vhi.getName()).isEqualTo(vhost);
    assertThat(vhi.getDescription()).isEqualTo("vhost description");
    assertThat(vhi.getTags()).hasSize(3);
    assertThat(vhi.getTags()).contains("production", "application1", "realm1");
    assertThat(vhi.isTracing()).isTrue();
    if (isVersion38orLater()) client.deleteVhost(vhost);
  }

  @Test
  void deleteApiVhostsWithNameWhenVhostExists() {
    String s = "hop-test-to-be-deleted";
    client.createVhost(s);
    client.deleteVhost(s);
    assertThat(client.getVhost(s)).isNull();
  }

  @Test
  void deleteApiVhostsWithNameWhenVhostDoesNotExist() {
    String s = "hop-test-to-be-deleted";
    client.deleteVhost(s);
    client.deleteVhost(s);
    assertThat(client.getVhost(s)).isNull();
  }

  @Test
  void getApiVhostsWithNamePermissionsWhenVhostExists() {
    String s = "/";
    List<UserPermissions> xs = client.getPermissionsIn(s);
    UserPermissions x =
        xs.stream().filter(up -> "guest".equals(up.getUser())).findFirst().orElse(null);
    assertThat(x.getRead()).isEqualTo(".*");
  }

  @Test
  void getApiVhostsWithNamePermissionsWhenVhostDoesNotExist() {
    String s = "trololowut";
    List<UserPermissions> xs = client.getPermissionsIn(s);
    assertThat(xs).isNull();
  }

  @Test
  void getApiVhostsWithNameTopicPermissionsWhenVhostExists() {
    if (!isVersion37orLater()) return;
    String s = "/";
    List<TopicPermissions> xs = client.getTopicPermissionsIn(s);
    TopicPermissions x =
        xs.stream().filter(tp -> "guest".equals(tp.getUser())).findFirst().orElse(null);
    assertThat(x.getExchange()).isEqualTo("amq.topic");
    assertThat(x.getRead()).isEqualTo(".*");
  }

  @Test
  void getApiVhostsWithNameTopicPermissionsWhenVhostDoesNotExist() {
    if (!isVersion37orLater()) return;
    String s = "trololowut";
    List<TopicPermissions> xs = client.getTopicPermissionsIn(s);
    assertThat(xs).isNull();
  }

  @Test
  void getApiUsers() {
    List<UserInfo> xs = client.getUsers();
    String version = client.getOverview().getServerVersion();
    UserInfo x = xs.stream().filter(u -> "guest".equals(u.getName())).findFirst().orElse(null);
    assertThat(x.getName()).isEqualTo("guest");
    assertThat(x.getPasswordHash()).isNotNull();
    if (isVersion36orLater(version)) {
      assertThat(x.getHashingAlgorithm()).isNotNull();
    }
    assertThat(x.getTags()).contains("administrator");
  }

  @Test
  void getApiUsersWithNameWhenUserExists() {
    UserInfo x = client.getUser("guest");
    String version = client.getOverview().getServerVersion();
    assertThat(x.getName()).isEqualTo("guest");
    assertThat(x.getPasswordHash()).isNotNull();
    if (isVersion36orLater(version)) {
      assertThat(x.getHashingAlgorithm()).isNotNull();
    }
    assertThat(x.getTags()).contains("administrator");
  }

  @Test
  void getApiUsersWithNameWhenUserDoesNotExist() {
    UserInfo x = client.getUser("lolwut");
    assertThat(x).isNull();
  }

  @Test
  void putApiUsersWithNameUpdatesUserTags() {
    String u = "alt-user";
    client.deleteUser(u);
    client.createUser(u, u.toCharArray(), Arrays.asList("original", "management"));
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }
    client.updateUser(u, u.toCharArray(), Arrays.asList("management", "updated"));
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }
    UserInfo x = client.getUser(u);
    assertThat(x.getTags()).contains("updated");
    assertThat(x.getTags()).doesNotContain("original");
  }

  @Test
  void deleteApiUsersWithName() {
    String u = "alt-user";
    client.deleteUser(u);
    client.createUser(u, u.toCharArray(), Arrays.asList("original", "management"));
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }
    client.deleteUser(u);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }
    UserInfo x = client.getUser(u);
    assertThat(x).isNull();
  }

  @Test
  void getApiUsersWithNamePermissionsWhenUserExists() {
    String s = "guest";
    List<UserPermissions> xs = client.getPermissionsOf(s);
    UserPermissions x =
        xs.stream().filter(up -> "/".equals(up.getVhost())).findFirst().orElse(null);
    assertThat(x.getRead()).isEqualTo(".*");
  }

  @Test
  void getApiUsersWithNamePermissionsWhenUserDoesNotExist() {
    String s = "trololowut";
    List<UserPermissions> xs = client.getPermissionsOf(s);
    assertThat(xs).isNull();
  }

  @Test
  void getApiUsersWithNameTopicPermissionsWhenUserExists() {
    if (!isVersion37orLater()) return;
    String s = "guest";
    List<TopicPermissions> xs = client.getTopicPermissionsOf(s);
    TopicPermissions x =
        xs.stream().filter(tp -> "/".equals(tp.getVhost())).findFirst().orElse(null);
    assertThat(x.getExchange()).isEqualTo("amq.topic");
    assertThat(x.getRead()).isEqualTo(".*");
  }

  @Test
  void getApiUsersWithNameTopicPermissionsWhenUserDoesNotExist() {
    if (!isVersion37orLater()) return;
    String s = "trololowut";
    List<TopicPermissions> xs = client.getTopicPermissionsOf(s);
    assertThat(xs).isNull();
  }

  @Test
  void putApiUsersWithNameWithBlankPasswordHash() throws Exception {
    String u = "alt-user";
    String h = "";
    client.deleteUser(u);
    client.createUserWithPasswordHash(u, h.toCharArray(), Arrays.asList("original", "management"));
    client.updatePermissions("/", u, new UserPermissions(".*", ".*", ".*"));
    try {
      openConnection("alt-user", "alt-user");
      Assertions.fail("Expected exception");
    } catch (Exception e) {
      assertThat(e instanceof AuthenticationFailureException || e instanceof IOException).isTrue();
    }
    client.deleteUser(u);
  }

  @Test
  void getApiWhoami() {
    CurrentUserDetails res = client.whoAmI();
    assertThat(res.getName()).isEqualTo(DEFAULT_USERNAME);
    assertThat(res.getTags()).contains("administrator");
  }

  @Test
  void getApiPermissions() {
    String s = "guest";
    List<UserPermissions> xs = client.getPermissions();
    UserPermissions x =
        xs.stream()
            .filter(up -> "/".equals(up.getVhost()) && s.equals(up.getUser()))
            .findFirst()
            .orElse(null);
    assertThat(x.getRead()).isEqualTo(".*");
  }

  @Test
  void getApiPermissionsWithVhostWithUserWhenBothVhostAndUserExist() {
    String u = "guest";
    String v = "/";
    UserPermissions x = client.getPermissions(v, u);
    assertThat(x.getRead()).isEqualTo(".*");
  }

  @Test
  void getApiPermissionsWithVhostWithUserWhenVhostDoesNotExist() {
    String u = "guest";
    String v = "lolwut";
    UserPermissions x = client.getPermissions(v, u);
    assertThat(x).isNull();
  }

  @Test
  void getApiPermissionsWithVhostWithUserWhenUsernameDoesNotExist() {
    String u = "lolwut";
    String v = "/";
    UserPermissions x = client.getPermissions(v, u);
    assertThat(x).isNull();
  }

  @Test
  void putApiPermissionsWithVhostWithUserWhenBothUserAndVhostExist() {
    String v = "hop-vhost1";
    client.createVhost(v);
    String u = "hop-user1";
    client.createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker"));
    client.updatePermissions(v, u, new UserPermissions("read", "write", "configure"));
    UserPermissions x = client.getPermissions(v, u);
    assertThat(x.getRead()).isEqualTo("read");
    assertThat(x.getWrite()).isEqualTo("write");
    assertThat(x.getConfigure()).isEqualTo("configure");
    client.deleteVhost(v);
    client.deleteUser(u);
  }

  @Test
  void putApiPermissionsWithVhostWithUserWhenVhostDoesNotExist() {
    String v = "hop-vhost1";
    client.deleteVhost(v);
    String u = "hop-user1";
    client.createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker"));
    try {
      client.updatePermissions(v, u, new UserPermissions("read", "write", "configure"));
      Assertions.fail("Expected exception");
    } catch (Exception e) {
      assertThat(exceptionStatus(e)).isEqualTo(400);
    }
    client.deleteUser(u);
  }

  @Test
  void deleteApiPermissionsWithVhostWithUserWhenBothVhostAndUsernameExist() {
    String v = "hop-vhost1";
    client.createVhost(v);
    String u = "hop-user1";
    client.createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker"));
    client.updatePermissions(v, u, new UserPermissions("read", "write", "configure"));
    UserPermissions x = client.getPermissions(v, u);
    assertThat(x.getRead()).isEqualTo("read");
    client.clearPermissions(v, u);
    UserPermissions y = client.getPermissions(v, u);
    assertThat(y).isNull();
    client.deleteVhost(v);
    client.deleteUser(u);
  }

  @Test
  void getApiTopicPermissions() {
    if (!isVersion37orLater()) return;
    String s = "guest";
    List<TopicPermissions> xs = client.getTopicPermissions();
    TopicPermissions x =
        xs.stream()
            .filter(tp -> "/".equals(tp.getVhost()) && s.equals(tp.getUser()))
            .findFirst()
            .orElse(null);
    assertThat(x.getExchange()).isEqualTo("amq.topic");
    assertThat(x.getRead()).isEqualTo(".*");
  }

  @Test
  void getApiTopicPermissionsWithVhostWithUserWhenBothVhostAndUserExist() {
    if (!isVersion37orLater()) return;
    String u = "guest";
    String v = "/";
    List<TopicPermissions> xs = client.getTopicPermissions(v, u);
    TopicPermissions x =
        xs.stream()
            .filter(tp -> v.equals(tp.getVhost()) && u.equals(tp.getUser()))
            .findFirst()
            .orElse(null);
    assertThat(x.getExchange()).isEqualTo("amq.topic");
    assertThat(x.getRead()).isEqualTo(".*");
  }

  @Test
  void getApiTopicPermissionsWithVhostWithUserWhenVhostDoesNotExist() {
    if (!isVersion37orLater()) return;
    String u = "guest";
    String v = "lolwut";
    List<TopicPermissions> xs = client.getTopicPermissions(v, u);
    assertThat(xs).isNull();
  }

  @Test
  void getApiTopicPermissionsWithVhostWithUserWhenUsernameDoesNotExist() {
    if (!isVersion37orLater()) return;
    String u = "lolwut";
    String v = "/";
    List<TopicPermissions> xs = client.getTopicPermissions(v, u);
    assertThat(xs).isNull();
  }

  @Test
  void putApiTopicPermissionsWithVhostWithUserWhenBothUserAndVhostExist() {
    String v = "hop-vhost1";
    client.createVhost(v);
    String u = "hop-user1";
    client.createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker"));
    if (!isVersion37orLater()) return;
    client.updateTopicPermissions(v, u, new TopicPermissions("amq.topic", "read", "write"));
    List<TopicPermissions> xs = client.getTopicPermissions(v, u);
    assertThat(xs).hasSize(1);
    assertThat(xs.get(0).getExchange()).isEqualTo("amq.topic");
    assertThat(xs.get(0).getRead()).isEqualTo("read");
    assertThat(xs.get(0).getWrite()).isEqualTo("write");
    client.deleteVhost(v);
    client.deleteUser(u);
  }

  @Test
  void putApiTopicPermissionsWithVhostWithUserWhenVhostDoesNotExist() {
    String v = "hop-vhost1";
    client.deleteVhost(v);
    String u = "hop-user1";
    client.createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker"));
    if (!isVersion37orLater()) return;
    try {
      client.updateTopicPermissions(v, u, new TopicPermissions("amq.topic", "read", "write"));
      Assertions.fail("Expected exception");
    } catch (Exception e) {
      assertThat(exceptionStatus(e)).isEqualTo(400);
    }
    client.deleteUser(u);
  }

  @Test
  void deleteApiTopicPermissionsWithVhostWithUserWhenBothVhostAndUsernameExist() {
    String v = "hop-vhost1";
    client.createVhost(v);
    String u = "hop-user1";
    client.createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker"));
    if (!isVersion37orLater()) return;
    client.updateTopicPermissions(v, u, new TopicPermissions("amq.topic", "read", "write"));
    List<TopicPermissions> xs = client.getTopicPermissions(v, u);
    assertThat(xs).hasSize(1);
    client.clearTopicPermissions(v, u);
    List<TopicPermissions> ys = client.getTopicPermissions(v, u);
    assertThat(ys).isNull();
    client.deleteVhost(v);
    client.deleteUser(u);
  }

  @Test
  void getApiPolicies() {
    String v = "/";
    String s = "hop.test";
    Map<String, Object> d = new HashMap<>();
    String p = ".*";
    d.put("expires", 30000);
    client.declarePolicy(v, s, new PolicyInfo(p, 0, null, d));
    List<PolicyInfo> xs = awaitEventPropagation(() -> client.getPolicies());
    PolicyInfo x = xs.get(0);
    verifyPolicyInfo(x);
    client.deletePolicy(v, s);
  }

  @Test
  void getApiPoliciesWithVhostWhenVhostExists() {
    String v = "/";
    String s = "hop.test";
    Map<String, Object> d = new HashMap<>();
    String p = ".*";
    d.put("expires", 30000);
    client.declarePolicy(v, s, new PolicyInfo(p, 0, null, d));
    List<PolicyInfo> xs = awaitEventPropagation(() -> client.getPolicies("/"));
    PolicyInfo x = xs.get(0);
    verifyPolicyInfo(x);
    client.deletePolicy(v, s);
  }

  @Test
  void getApiPoliciesWithVhostWhenVhostDoesNotExists() {
    String v = "lolwut";
    client.deleteVhost(v);
    List<PolicyInfo> xs = client.getPolicies(v);
    assertThat(xs).isNull();
  }

  @Test
  void getApiOperatorPolicies() {
    String v = "/";
    String s = "hop.test";
    Map<String, Object> d = new HashMap<>();
    String p = ".*";
    d.put("max-length", 6);
    client.declareOperatorPolicy(v, s, new PolicyInfo(p, 0, null, d));
    List<PolicyInfo> xs = awaitEventPropagation(() -> client.getOperatorPolicies());
    PolicyInfo x = xs.get(0);
    verifyPolicyInfo(x);
    client.deleteOperatorPolicy(v, s);
  }

  @Test
  void getApiOperatorPoliciesWithVhostWhenVhostExists() {
    String v = "/";
    String s = "hop.test";
    Map<String, Object> d = new HashMap<>();
    String p = ".*";
    d.put("max-length", 6);
    client.declareOperatorPolicy(v, s, new PolicyInfo(p, 0, null, d));
    List<PolicyInfo> xs = awaitEventPropagation(() -> client.getOperatorPolicies("/"));
    PolicyInfo x = xs.get(0);
    verifyPolicyInfo(x);
    client.deleteOperatorPolicy(v, s);
  }

  @Test
  void getApiOperatorPoliciesWithVhostWhenVhostDoesNotExists() {
    String v = "lolwut";
    client.deleteVhost(v);
    List<PolicyInfo> xs = client.getOperatorPolicies(v);
    assertThat(xs).isNull();
  }

  @Test
  void getApiAlivenessTestWithVhost() {
    boolean hasSucceeded = client.alivenessTest("/");
    assertThat(hasSucceeded).isTrue();
  }

  @Test
  void getApiClusterName() {
    ClusterId s = client.getClusterName();
    assertThat(s.getName()).isNotNull();
  }

  @Test
  void putApiClusterName() {
    String s = client.getClusterName().getName();
    client.setClusterName("rabbit@warren");
    String x = client.getClusterName().getName();
    assertThat(x).isEqualTo("rabbit@warren");
    client.setClusterName(s);
  }

  @Test
  void getApiExtensions() {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> xs = (List<Map<String, Object>>) (List<?>) client.getExtensions();
    assertThat(xs).isNotEmpty();
  }

  @Test
  void getApiDefinitionsVersionVhostsPermissionsTopicPermissions() {
    Definitions d = client.getDefinitions();
    assertThat(d.getServerVersion()).isNotNull();
    assertThat(d.getServerVersion().trim()).isNotEmpty();
    assertThat(d.getVhosts()).isNotEmpty();
    assertThat(d.getVhosts().get(0).getName()).isNotEmpty();
    assertThat(d.getPermissions()).isNotEmpty();
    assertThat(d.getPermissions().get(0).getUser()).isNotNull().isNotEmpty();
    if (isVersion37orLater()) {
      assertThat(d.getTopicPermissions().get(0).getUser()).isNotNull().isNotEmpty();
    }
  }

  @Test
  void getApiDefinitionsUsers() {
    Definitions d = client.getDefinitions();
    assertThat(d.getUsers()).isNotEmpty();
    assertThat(d.getUsers().get(0).getName()).isNotNull().isNotEmpty();
  }

  @Test
  void getApiDefinitionsQueues() {
    client.declareQueue("/", "queue1", new QueueInfo(false, false, false));
    client.declareQueue("/", "queue2", new QueueInfo(false, false, false));
    client.declareQueue("/", "queue3", new QueueInfo(false, false, false));
    Definitions d = client.getDefinitions();
    assertThat(d.getQueues()).isNotEmpty();
    assertThat(d.getQueues().size()).isGreaterThanOrEqualTo(3);
    QueueInfo q =
        d.getQueues().stream()
            .filter(qi -> "queue1".equals(qi.getName()) && "/".equals(qi.getVhost()))
            .findFirst()
            .orElse(null);
    assertThat(q).isNotNull();
    assertThat(q.isDurable()).isFalse();
    client.deleteQueue("/", "queue1");
    client.deleteQueue("/", "queue2");
    client.deleteQueue("/", "queue3");
  }

  @Test
  void getApiDefinitionsExchanges() {
    client.declareExchange("/", "exchange1", new ExchangeInfo("fanout", false, false));
    client.declareExchange("/", "exchange2", new ExchangeInfo("direct", false, false));
    client.declareExchange("/", "exchange3", new ExchangeInfo("topic", false, false));
    Definitions d = client.getDefinitions();
    assertThat(d.getExchanges()).isNotEmpty();
    assertThat(d.getExchanges().size()).isGreaterThanOrEqualTo(3);
    ExchangeInfo e =
        d.getExchanges().stream()
            .filter(ex -> "exchange1".equals(ex.getName()))
            .findFirst()
            .orElse(null);
    assertThat(e).isNotNull();
    assertThat(e.isDurable()).isFalse();
    client.deleteExchange("/", "exchange1");
    client.deleteExchange("/", "exchange2");
    client.deleteExchange("/", "exchange3");
  }

  @Test
  void getApiDefinitionsBindings() {
    client.declareQueue("/", "queue1", new QueueInfo(false, false, false));
    client.bindQueue("/", "queue1", "amq.fanout", "");
    Definitions d = client.getDefinitions();
    assertThat(d.getBindings()).isNotEmpty();
    assertThat(d.getBindings().size()).isGreaterThanOrEqualTo(1);
    BindingInfo b =
        d.getBindings().stream()
            .filter(
                bi ->
                    "amq.fanout".equals(bi.getSource())
                        && "queue1".equals(bi.getDestination())
                        && DestinationType.QUEUE.equals(bi.getDestinationType()))
            .findFirst()
            .orElse(null);
    assertThat(b).isNotNull();
    client.deleteQueue("/", "queue1");
  }

  @Test
  void getApiDefinitionsParameters() {
    ShovelDetails value =
        new ShovelDetails("amqp://localhost:5672/vh1", "amqp://localhost:5672/vh2", 30, true, null);
    value.setSourceQueue("queue1");
    value.setDestinationExchange("exchange1");
    value.setSourcePrefetchCount(50L);
    value.setSourceDeleteAfter("never");
    value.setDestinationAddTimestampHeader(true);
    client.declareShovel("/", new ShovelInfo("shovel1", value));
    Definitions d = client.getDefinitions();
    List<RuntimeParameter<Object>> parameters = d.getParameters();
    List<RuntimeParameter<Object>> globalParameters = d.getGlobalParameters();
    assertThat(parameters).isNotEmpty();
    RuntimeParameter<Object> parameter =
        parameters.stream().filter(p -> "shovel1".equals(p.getName())).findFirst().orElse(null);
    assertThat(parameter).isNotNull();
    assertThat(parameter.getComponent()).isEqualTo("shovel");
    assertThat(parameter.getValue() instanceof Map).isTrue();
    client.deleteShovel("/", "shovel1");
    client.deleteQueue("/", "queue1");
  }

  @Test
  void getVhostDefinitions() {
    client.declareQueue("/", "test-vhost-def-queue", new QueueInfo(false, false, false));
    Definitions d = client.getDefinitions("/");
    assertThat(d).isNotNull();
    assertThat(d.getQueues()).isNotEmpty();
    QueueInfo q =
        d.getQueues().stream()
            .filter(qi -> "test-vhost-def-queue".equals(qi.getName()))
            .findFirst()
            .orElse(null);
    assertThat(q).isNotNull();
    client.deleteQueue("/", "test-vhost-def-queue");
  }

  @Test
  void getApiParametersShovel() {
    ShovelDetails value =
        new ShovelDetails("amqp://localhost:5672/vh1", "amqp://localhost:5672/vh2", 30, true, null);
    value.setSourceQueue("queue1");
    value.setDestinationExchange("exchange1");
    value.setSourcePrefetchCount(50L);
    value.setSourceDeleteAfter("never");
    value.setDestinationAddTimestampHeader(true);
    client.declareShovel("/", new ShovelInfo("shovel1", value));
    List<ShovelInfo> shovels = awaitEventPropagation(() -> client.getShovels());
    assertThat(shovels).isNotEmpty();
    ShovelInfo s =
        shovels.stream().filter(sh -> "shovel1".equals(sh.getName())).findFirst().orElse(null);
    assertThat(s).isNotNull();
    assertThat(s.getDetails().getSourcePrefetchCount()).isEqualTo(50L);
    assertThat(s.getDetails().isDestinationAddTimestampHeader()).isTrue();
    client.deleteShovel("/", "shovel1");
    client.deleteQueue("/", "queue1");
  }

  @Test
  void getApiParametersShovelWithMultipleUris() {
    ShovelDetails value =
        new ShovelDetails(
            Arrays.asList("amqp://localhost:5672/vh1", "amqp://localhost:5672/vh3"),
            Arrays.asList("amqp://localhost:5672/vh2", "amqp://localhost:5672/vh4"),
            30,
            true,
            null);
    value.setSourceQueue("queue1");
    value.setDestinationExchange("exchange1");
    client.declareShovel("/", new ShovelInfo("shovel2", value));
    List<ShovelInfo> shovels = awaitEventPropagation(() -> client.getShovels());
    ShovelInfo s =
        shovels.stream().filter(sh -> "shovel2".equals(sh.getName())).findFirst().orElse(null);
    assertThat(s).isNotNull();
    assertThat(s.getDetails().getSourceURIs()).hasSize(2);
    client.deleteShovel("/", "shovel2");
    client.deleteQueue("/", "queue1");
  }

  @Test
  void putApiParametersShovelWithAnEmptyPublishPropertiesMap() {
    ShovelDetails value =
        new ShovelDetails(
            "amqp://localhost:5672/vh1",
            "amqp://localhost:5672/vh2",
            30,
            true,
            Collections.emptyMap());
    value.setSourceQueue("queue1");
    value.setDestinationExchange("exchange1");
    try {
      client.declareShovel("/", new ShovelInfo("shovel10", value));
      Assertions.fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // expected
    }
    try {
      client.deleteShovel("/", "shovel1");
    } catch (Exception e) {
    }
    try {
      client.deleteQueue("/", "queue1");
    } catch (Exception e) {
    }
  }

  @Test
  void getApiShovels() {
    ShovelDetails value =
        new ShovelDetails("amqp://localhost:5672/vh1", "amqp://localhost:5672/vh2", 30, true, null);
    value.setSourceQueue("queue1");
    value.setDestinationExchange("exchange1");
    String shovelName = "shovel2";
    client.declareShovel("/", new ShovelInfo(shovelName, value));
    List<ShovelStatus> shovels = awaitEventPropagation(() -> client.getShovelsStatus());
    assertThat(shovels).isNotEmpty();
    ShovelStatus s =
        shovels.stream().filter(sh -> shovelName.equals(sh.getName())).findFirst().orElse(null);
    assertThat(s).isNotNull();
    assertThat(s.getType()).isEqualTo("dynamic");
    waitAtMostUntilTrue(
        30,
        () -> {
          ShovelStatus status =
              client.getShovelsStatus().stream()
                  .filter(sh -> shovelName.equals(sh.getName()))
                  .findFirst()
                  .orElse(null);
          return status != null && "running".equals(status.getState());
        });
    client.deleteShovel("/", shovelName);
  }

  @Test
  void getApiParametersFederationUpstreamDeclareAndGetAtRootVhostWithNonNullAckMode() {
    String vhost = "/";
    String upstreamName = "upstream1";
    UpstreamDetails upstreamDetails = new UpstreamDetails();
    upstreamDetails.setUri("amqp://localhost:5672");
    upstreamDetails.setAckMode(AckMode.ON_CONFIRM);
    client.declareUpstream(vhost, upstreamName, upstreamDetails);
    List<UpstreamInfo> upstreams = awaitEventPropagation(() -> client.getUpstreams());
    verifyUpstreamDefinitions(vhost, upstreams, upstreamName);
    UpstreamInfo upstream =
        upstreams.stream().filter(u -> upstreamName.equals(u.getName())).findFirst().orElse(null);
    assertThat(upstream.getValue().getAckMode()).isEqualTo(AckMode.ON_CONFIRM);
    client.deleteUpstream(vhost, upstreamName);
  }

  @Test
  void getApiParametersFederationUpstreamDeclareAndGetAtRootVhost() {
    String vhost = "/";
    String upstreamName = "upstream1";
    declareUpstream(client, vhost, upstreamName);
    List<UpstreamInfo> upstreams = awaitEventPropagation(() -> client.getUpstreams());
    verifyUpstreamDefinitions(vhost, upstreams, upstreamName);
    client.deleteUpstream(vhost, upstreamName);
  }

  @Test
  void getApiParametersFederationUpstreamDeclareAndGetAtNonRootVhost() {
    String vhost = "foo";
    String upstreamName = "upstream2";
    client.createVhost(vhost);
    declareUpstream(client, vhost, upstreamName);
    List<UpstreamInfo> upstreams = awaitEventPropagation(() -> client.getUpstreams(vhost));
    verifyUpstreamDefinitions(vhost, upstreams, upstreamName);
    client.deleteUpstream(vhost, upstreamName);
    client.deleteVhost(vhost);
  }

  @Test
  void putApiParametersFederationUpstreamWithNullUpstreamUri() {
    UpstreamDetails upstreamDetails = new UpstreamDetails();
    try {
      client.declareUpstream("/", "upstream3", upstreamDetails);
      Assertions.fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  @Test
  void deleteApiParametersFederationUpstreamWithVhostWithName() {
    String vhost = "/";
    String upstreamName = "upstream4";
    declareUpstream(client, vhost, upstreamName);
    List<UpstreamInfo> upstreams = awaitEventPropagation(() -> client.getUpstreams());
    verifyUpstreamDefinitions(vhost, upstreams, upstreamName);
    client.deleteUpstream(vhost, upstreamName);
    upstreams = client.getUpstreams();
    assertThat(upstreams.stream().anyMatch(u -> upstreamName.equals(u.getName()))).isFalse();
  }

  @Test
  void getApiParametersFederationUpstreamSetDeclareAndGet() {
    String vhost = "/";
    String upstreamSetName = "upstream-set-1";
    String upstreamA = "A";
    String upstreamB = "B";
    String policyName = "federation-policy";
    declareUpstream(client, vhost, upstreamA);
    declareUpstream(client, vhost, upstreamB);
    UpstreamSetDetails d1 = new UpstreamSetDetails();
    d1.setUpstream(upstreamA);
    d1.setExchange("exchangeA");
    UpstreamSetDetails d2 = new UpstreamSetDetails();
    d2.setUpstream(upstreamB);
    d2.setExchange("exchangeB");
    List<UpstreamSetDetails> detailsSet = new ArrayList<>();
    detailsSet.add(d1);
    detailsSet.add(d2);
    client.declareUpstreamSet(vhost, upstreamSetName, detailsSet);
    PolicyInfo p = new PolicyInfo();
    p.setApplyTo("exchanges");
    p.setName(policyName);
    p.setPattern("amq\\.topic");
    p.setDefinition(Collections.singletonMap("federation-upstream-set", upstreamSetName));
    client.declarePolicy(vhost, policyName, p);
    List<UpstreamSetInfo> upstreamSets = awaitEventPropagation(() -> client.getUpstreamSets());
    assertThat(upstreamSets).isNotEmpty();
    UpstreamSetInfo upstreamSet =
        upstreamSets.stream()
            .filter(u -> upstreamSetName.equals(u.getName()))
            .findFirst()
            .orElse(null);
    assertThat(upstreamSet).isNotNull();
    assertThat(upstreamSet.getValue()).hasSize(2);
    client.deletePolicy(vhost, policyName);
    client.deleteUpstreamSet(vhost, upstreamSetName);
    client.deleteUpstream(vhost, upstreamA);
    client.deleteUpstream(vhost, upstreamB);
  }

  @Test
  void putApiParametersFederationUpstreamSetWithoutUpstreams() {
    UpstreamSetDetails upstreamSetDetails = new UpstreamSetDetails();
    List<UpstreamSetDetails> detailsSet = new ArrayList<>();
    detailsSet.add(upstreamSetDetails);
    try {
      client.declareUpstreamSet("/", "upstream-set-2", detailsSet);
      Assertions.fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  @Test
  void getApiVhostLimits() {
    String vhost1 = "virtual-host-with-limits-1";
    String vhost2 = "virtual-host-with-limits-2";
    client.createVhost(vhost1);
    client.createVhost(vhost2);
    client.limitMaxNumberOfQueues(vhost1, 100);
    client.limitMaxNumberOfConnections(vhost1, 10);
    client.limitMaxNumberOfQueues(vhost2, 200);
    client.limitMaxNumberOfConnections(vhost2, 20);
    client.limitMaxNumberOfQueues("/", 300);
    client.limitMaxNumberOfConnections("/", 30);
    List<VhostLimits> limits = client.getVhostLimits();
    assertThat(limits).hasSize(3);
    VhostLimits limits1 =
        limits.stream().filter(l -> vhost1.equals(l.getVhost())).findFirst().orElse(null);
    assertThat(limits1.getMaxQueues()).isEqualTo(100);
    assertThat(limits1.getMaxConnections()).isEqualTo(10);
    client.deleteVhost(vhost1);
    client.deleteVhost(vhost2);
    client.clearMaxQueuesLimit("/");
    client.clearMaxConnectionsLimit("/");
  }

  @Test
  void getApiVhostLimitsWithoutLimitsOnAnyHost() {
    List<VhostLimits> limits = client.getVhostLimits();
    assertThat(limits).hasSize(1);
    VhostLimits limits1 =
        limits.stream().filter(l -> "/".equals(l.getVhost())).findFirst().orElse(null);
    assertThat(limits1.getMaxQueues()).isEqualTo(-1);
    assertThat(limits1.getMaxConnections()).isEqualTo(-1);
  }

  @Test
  void getApiGlobalParametersMqttPortToVhostMappingWithoutMqttVhostPortMapping() {
    client.deleteMqttPortToVhostMapping();
    MqttVhostPortInfo mqttPorts = client.getMqttPortToVhostMapping();
    assertThat(mqttPorts).isNull();
  }

  @Test
  void getApiGlobalParametersMqttPortToVhostMappingWithASampleMapping() {
    Map<Integer, String> mqttInputMap = Map.of(2024, "vhost1", 2025, "vhost2");
    client.setMqttPortToVhostMapping(mqttInputMap);
    MqttVhostPortInfo mqttInfo = client.getMqttPortToVhostMapping();
    Map<Integer, String> mqttReturnValues = mqttInfo.getValue();
    assertThat(mqttReturnValues).isEqualTo(mqttInputMap);
    client.deleteMqttPortToVhostMapping();
  }

  @Test
  void putApiGlobalParametersMqttPortToVhostMappingWithABlankVhostValue() {
    Map<Integer, String> mqttInputMap = Map.of(2024, " ", 2025, "vhost2");
    try {
      client.setMqttPortToVhostMapping(mqttInputMap);
      Assertions.fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  @Test
  void getApiVhostLimitsWithVhost() {
    String vhost = "virtual-host-with-limits";
    client.createVhost(vhost);
    client.limitMaxNumberOfQueues(vhost, 100);
    client.limitMaxNumberOfConnections(vhost, 10);
    VhostLimits limits = client.getVhostLimits(vhost);
    assertThat(limits.getMaxQueues()).isEqualTo(100);
    assertThat(limits.getMaxConnections()).isEqualTo(10);
    client.deleteVhost(vhost);
  }

  @Test
  void getApiVhostLimitsWithVhostVhostWithNoLimits() {
    String vhost = "virtual-host-without-limits";
    client.createVhost(vhost);
    VhostLimits limits = client.getVhostLimits(vhost);
    assertThat(limits.getMaxQueues()).isEqualTo(-1);
    assertThat(limits.getMaxConnections()).isEqualTo(-1);
    client.deleteVhost(vhost);
  }

  @Test
  void getApiVhostLimitsWithVhostWithNonExistingVhost() {
    String vhost = "virtual-host-that-does-not-exist";
    VhostLimits limits = client.getVhostLimits(vhost);
    assertThat(limits).isNull();
    client.deleteVhost(vhost);
  }

  @Test
  void deleteApiVhostLimitsWithVhostMaxQueues() {
    String vhost = "virtual-host-max-queues-limit";
    client.createVhost(vhost);
    client.limitMaxNumberOfQueues(vhost, 42);
    client.clearMaxQueuesLimit(vhost);
    assertThat(client.getVhostLimits(vhost).getMaxQueues()).isEqualTo(-1);
    assertThat(client.getVhostLimits(vhost).getMaxConnections()).isEqualTo(-1);
    client.deleteVhost(vhost);
  }

  @Test
  void deleteApiVhostLimitsWithVhostMaxConnections() {
    String vhost = "virtual-host-max-connections-limit";
    client.createVhost(vhost);
    client.limitMaxNumberOfConnections(vhost, 42);
    client.clearMaxConnectionsLimit(vhost);
    assertThat(client.getVhostLimits(vhost).getMaxConnections()).isEqualTo(-1);
    assertThat(client.getVhostLimits(vhost).getMaxQueues()).isEqualTo(-1);
    client.deleteVhost(vhost);
  }

  @Test
  void deleteApiVhostLimitsWithVhostWithOnlyOneLimit() {
    String vhost = "virtual-host-max-queues-connections-limits";
    client.createVhost(vhost);
    client.limitMaxNumberOfQueues(vhost, 314);
    client.limitMaxNumberOfConnections(vhost, 42);
    client.clearMaxQueuesLimit(vhost);
    assertThat(client.getVhostLimits(vhost).getMaxQueues()).isEqualTo(-1);
    assertThat(client.getVhostLimits(vhost).getMaxConnections()).isEqualTo(42);
    client.deleteVhost(vhost);
  }

  @Test
  void getFeatureFlags() {
    List<FeatureFlag> flags = client.getFeatureFlags();
    assertThat(flags).isNotEmpty();
    FeatureFlag flag = flags.get(0);
    assertThat(flag.getName()).isNotNull();
    assertThat(flag.getState()).isNotNull();
    assertThat(flag.getStability()).isNotNull();
  }

  @Test
  void enableFeatureFlag() {
    List<FeatureFlag> flags = client.getFeatureFlags();
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

    client.enableFeatureFlag(disabledFlag.getName());

    List<FeatureFlag> updatedFlags = client.getFeatureFlags();
    FeatureFlag enabledFlag =
        updatedFlags.stream()
            .filter(f -> f.getName().equals(disabledFlag.getName()))
            .findFirst()
            .orElse(null);

    assertThat(enabledFlag).isNotNull();
    assertThat(enabledFlag.getState()).isEqualTo(FeatureFlagState.ENABLED);
  }

  @Test
  void getUserLimitsWithLimits() {
    String username = "user-with-limits";
    client.createUser(username, "password".toCharArray(), Collections.emptyList());
    try {
      client.limitUserMaxConnections(username, 10);
      client.limitUserMaxChannels(username, 100);
      List<UserLimits> limits = client.getUserLimits();
      UserLimits userLimits =
          limits.stream().filter(l -> username.equals(l.getUser())).findFirst().orElse(null);
      assertThat(userLimits).isNotNull();
      assertThat(userLimits.getMaxConnections()).isEqualTo(10);
      assertThat(userLimits.getMaxChannels()).isEqualTo(100);
    } finally {
      client.deleteUser(username);
    }
  }

  @Test
  void getUserLimitsForUser() {
    String username = "user-with-limits-2";
    client.createUser(username, "password".toCharArray(), Collections.emptyList());
    try {
      client.limitUserMaxConnections(username, 20);
      client.limitUserMaxChannels(username, 200);
      UserLimits limits = client.getUserLimits(username);
      assertThat(limits.getMaxConnections()).isEqualTo(20);
      assertThat(limits.getMaxChannels()).isEqualTo(200);
    } finally {
      client.deleteUser(username);
    }
  }

  @Test
  void getUserLimitsForUserWithNoLimits() {
    String username = "user-without-limits";
    client.createUser(username, "password".toCharArray(), Collections.emptyList());
    try {
      UserLimits limits = client.getUserLimits(username);
      assertThat(limits.getMaxConnections()).isEqualTo(-1);
      assertThat(limits.getMaxChannels()).isEqualTo(-1);
    } finally {
      client.deleteUser(username);
    }
  }

  @Test
  void clearUserMaxConnectionsLimit() {
    String username = "user-max-connections-limit";
    client.createUser(username, "password".toCharArray(), Collections.emptyList());
    try {
      client.limitUserMaxConnections(username, 42);
      client.clearUserMaxConnectionsLimit(username);
      assertThat(client.getUserLimits(username).getMaxConnections()).isEqualTo(-1);
    } finally {
      client.deleteUser(username);
    }
  }

  @Test
  void clearUserMaxChannelsLimit() {
    String username = "user-max-channels-limit";
    client.createUser(username, "password".toCharArray(), Collections.emptyList());
    try {
      client.limitUserMaxChannels(username, 42);
      client.clearUserMaxChannelsLimit(username);
      assertThat(client.getUserLimits(username).getMaxChannels()).isEqualTo(-1);
    } finally {
      client.deleteUser(username);
    }
  }

  @Test
  void healthCheckClusterAlarms() {
    client.healthCheckClusterAlarms();
  }

  @Test
  void healthCheckLocalAlarms() {
    client.healthCheckLocalAlarms();
  }

  @Test
  void healthCheckNodeIsQuorumCritical() {
    client.healthCheckNodeIsQuorumCritical();
  }

  @Test
  void healthCheckVirtualHosts() {
    client.healthCheckVirtualHosts();
  }

  @Test
  void healthCheckPortListener() {
    client.healthCheckPortListener(5672);
  }

  @Test
  void healthCheckProtocolListener() {
    client.healthCheckProtocolListener("amqp");
  }

  @Test
  void getDeprecatedFeatures() {
    List<DeprecatedFeature> features = client.getDeprecatedFeatures();
    assertThat(features).isNotNull();
  }

  @Test
  void getDeprecatedFeaturesInUse() {
    List<DeprecatedFeature> features = client.getDeprecatedFeaturesInUse();
    assertThat(features).isNotNull();
  }
}
