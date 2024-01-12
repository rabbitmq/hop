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

import com.rabbitmq.http.client.HttpLayer.HttpLayerFactory;
import com.rabbitmq.http.client.domain.*;

import javax.net.ssl.SSLContext;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class Client {

  private final HttpLayer httpLayer;
  protected final URI rootUri;

  //
  // API
  //

  /**
   * Construct an instance with the provided url and credentials.
   * <p>
   *
   * @param url      the url e.g. "http://localhost:15672/api/".
   * @param username the username.
   * @param password the password
   * @throws MalformedURLException for a badly formed URL.
   * @throws URISyntaxException    for a badly formed URL.
   */
  public Client(URL url, String username, String password) throws MalformedURLException, URISyntaxException {
    this(new ClientParameters().url(url).username(username).password(password));
  }

  /**
   * Construct an instance with the provided url and credentials.
   * <p>
   *
   * @param url        the url e.g. "http://localhost:15672/api/".
   * @param username   the username.
   * @param password   the password
   * @param sslContext ssl context for http client
   * @throws MalformedURLException for a badly formed URL.
   * @throws URISyntaxException    for a badly formed URL.
   */
  public Client(URL url, String username, String password, SSLContext sslContext)
      throws MalformedURLException, URISyntaxException {
    this(new ClientParameters()
        .url(url)
        .username(username)
        .password(password)
        .httpLayerFactory(JdkHttpClientHttpLayer.configure()
            .clientBuilderConsumer(builder -> builder.sslContext(sslContext))
            .create())
    );
  }

  /**
   * Construct an instance with the provided url and credentials.
   * <p>
   *
   * @param url the url e.g. "https://guest:guest@localhost:15672/api/".
   * @throws MalformedURLException for a badly formed URL.
   * @throws URISyntaxException    for a badly formed URL.
   */
  public Client(String url) throws MalformedURLException, URISyntaxException {
    this(new ClientParameters().url(url));
  }

  /**
   * Construct an instance with the provided {@link ClientParameters}.
   * <p>
   * The instance will use Java 11's {@link java.net.http.HttpClient} internally.
   *
   * @param parameters the client parameters to use
   * @throws URISyntaxException    for a badly formed URL.
   * @throws MalformedURLException for a badly formed URL.
   * @since 3.6.0
   */
  public Client(ClientParameters parameters) throws URISyntaxException, MalformedURLException {
    parameters.validate();
    URL url = parameters.getUrl();
    this.rootUri = Utils.rootUri(url);
    HttpLayerFactory httpLayerFactory = parameters.getHttpLayerFactory() == null ?
          JdkHttpClientHttpLayer.configure().create() :
          parameters.getHttpLayerFactory();
    this.httpLayer = httpLayerFactory.create(parameters);
  }

  /**
   * @return cluster state overview
   */
  public OverviewResponse getOverview() {
    return this.httpLayer.get(uriWithPath("./overview"), OverviewResponse.class);
  }

  /**
   * Performs a basic node aliveness check: declares a queue, publishes a message
   * that routes to it, consumes it, cleans up.
   *
   * @param vhost vhost to use to perform aliveness check in
   * @return true if the check succeeded
   */
  public boolean alivenessTest(String vhost) {
    final URI uri = uri().withEncodedPath("./aliveness-test").withPath(vhost).get();
    return this.httpLayer.get(uri, AlivenessTestResult.class).isSuccessful();
  }

  /**
   * @return information about the user used by this client instance
   */
  public CurrentUserDetails whoAmI() {
    final URI uri = uriWithPath("./whoami/");
    return this.httpLayer.get(uri, CurrentUserDetails.class);
  }

  /**
   * Retrieves state and metrics information for all nodes in the cluster.
   *
   * @return list of nodes in the cluster
   */
  public List<NodeInfo> getNodes() {
    final URI uri = uriWithPath("./nodes/");
    return Arrays.asList(this.httpLayer.get(uri, NodeInfo[].class));
  }

  /**
   * Retrieves state and metrics information for individual node.
   *
   * @param name node name
   * @return node information
   */
  public NodeInfo getNode(String name) {
    final URI uri = uri().withEncodedPath("./nodes").withPath(name).get();
    return this.httpLayer.get(uri, NodeInfo.class);
  }

  /**
   * Retrieves state and metrics information for all client connections across the cluster.
   *
   * @return list of connections across the cluster
   */
  public List<ConnectionInfo> getConnections() {
    final URI uri = uri().withEncodedPath("./connections/").get();
    return Arrays.asList(this.httpLayer.get(uri, ConnectionInfo[].class));
  }

  /**
   * Retrieves state and metrics information for all client connections across the cluster
   * using query parameters
   *
   * @param queryParameters
   * @return list of connections across the cluster
   */
  @SuppressWarnings("unchecked")
  public Page<ConnectionInfo> getConnections(QueryParameters queryParameters) {
    final URI uri = uri().withEncodedPath("./connections/").withQueryParameters(queryParameters).get();
    ParameterizedTypeReference<Page<ConnectionInfo>> type = new ParameterizedTypeReference<>() {
    };
    if (queryParameters.pagination().hasAny()) {
      return this.httpLayer.get(uri, type);
    } else {
      return new Page(this.httpLayer.get(uri, ConnectionInfo[].class));
    }
  }

  /**
   * Retrieves state and metrics information for individual client connection.
   *
   * @param name connection name
   * @return connection information
   */
  public ConnectionInfo getConnection(String name) {
    final URI uri = uri().withEncodedPath("./connections").withPath(name).get();
    return this.httpLayer.get(uri, ConnectionInfo.class);
  }

  /**
   * Lists connection that belong to a specific user (used the provided username
   * during authentication).
   * @param username username
   */
  public List<UserConnectionInfo> getConnectionsOfUser(String username) {
    final URI uri = uri().withEncodedPath("./connections/username/").withPath(username).get();
    return Arrays.asList(this.httpLayer.get(uri, UserConnectionInfo[].class));
  }

  /**
   * Forcefully closes individual connection.
   * The client will receive a <i>connection.close</i> method frame.
   *
   * @param name connection name
   */
  public void closeConnection(String name) {
    final URI uri = uri().withEncodedPath("./connections").withPath(name).get();
    deleteIgnoring404(uri);
  }

  /**
   * Forcefully closes all connections that belong to a specific user
   * (used the provided username during authentication).
   * The client will receive a <i>connection.close</i> method frame.
   *
   * @param username username
   */
  public void closeAllConnectionsOfUser(String username) {
    final URI uri = uri().withEncodedPath("./connections/username/").withPath(username).get();
    deleteIgnoring404(uri);
  }

  /**
   * Forcefully closes individual connection with a user-provided message.
   * The client will receive a <i>connection.close</i> method frame.
   *
   * @param name connection name
   * @param reason the reason of closing
   */
  public void closeConnection(String name, String reason) {
    final URI uri = uri().withEncodedPath("./connections").withPath(name).get();

    Map<String, String> headers = new HashMap<>();
    headers.put("X-Reason", reason);

    deleteIgnoring404(uri, headers);
  }

  /**
   * Retrieves state and metrics information for all consumers across the cluster.
   *
   * @return list of consumers across the cluster
   */
  public List<ConsumerDetails> getConsumers() {
    final URI uri = uriWithPath("./consumers/");
    return Arrays.asList(this.httpLayer.get(uri, ConsumerDetails[].class));
  }

  /**
   * Retrieves state and metrics information for all consumers across an individual virtual host.
   *
   * @param vhost name of the virtual host
   * @return list of consumers in the virtual host (across all nodes)
   */
  public List<ConsumerDetails> getConsumers(String vhost) {
    final URI uri = uri().withEncodedPath("./consumers").withPath(vhost).get();
    return Arrays.asList(this.httpLayer.get(uri, ConsumerDetails[].class));
  }

  /**
   * Retrieves state and metrics information for all channels across the cluster.
   *
   * @return list of channels across the cluster
   */
  public List<ChannelInfo> getChannels() {
    final URI uri = uriWithPath("./channels/");
    return Arrays.asList(this.httpLayer.get(uri, ChannelInfo[].class));
  }

  /**
   * Retrieves state and metrics information for all channels across the cluster.
   * using query parameters
   *
   * @param queryParameters
   * @return list of channels across the cluster
   */
  @SuppressWarnings("unchecked")
  public Page<ChannelInfo> getChannels(QueryParameters queryParameters) {
    final URI uri = uri().withEncodedPath("./channels").withQueryParameters(queryParameters).get();
    ParameterizedTypeReference<Page<ChannelInfo>> type = new ParameterizedTypeReference<>() {
    };
    if (queryParameters.pagination().hasAny()) {
      return this.httpLayer.get(uri, type);
    } else {
      return new Page(this.httpLayer.get(uri, ChannelInfo[].class));
    }
  }

  /**
   * Retrieves state and metrics information for all channels on individual connection.
   * @param connectionName the connection name to retrieve channels
   * @return list of channels on the connection
   */
  public List<ChannelInfo> getChannels(String connectionName) {
    final URI uri = uri().withEncodedPath("./connections").withPath(connectionName).withEncodedPath("channels").get();
    return Arrays.asList(this.httpLayer.get(uri, ChannelInfo[].class));
  }

  /**
   * Retrieves state and metrics information for individual channel.
   *
   * @param name channel name
   * @return channel information
   */
  public ChannelInfo getChannel(String name) {
    final URI uri = uri().withEncodedPath("./channels").withPath(name).get();
    return this.httpLayer.get(uri, ChannelInfo.class);
  }

  public List<VhostInfo> getVhosts() {
    final URI uri = uriWithPath("./vhosts/");
    return Arrays.asList(this.httpLayer.get(uri, VhostInfo[].class));
  }

  public VhostInfo getVhost(String name) {
    final URI uri = uri().withEncodedPath("./vhosts").withPath(name).get();
    return getForObjectReturningNullOn404(uri, VhostInfo.class);
  }

  /**
   * Create a virtual host with name, tracing flag, and metadata.
   * Note metadata (description and tags) are supported as of RabbitMQ 3.8.
   *
   * @param name        name of the virtual host
   * @param tracing     whether tracing is enabled or not
   * @param description virtual host description (requires RabbitMQ 3.8 or more)
   * @param tags        virtual host tags (requires RabbitMQ 3.8 or more)
   * @since 3.4.0
   */
  public void createVhost(String name, boolean tracing, String description, String... tags) {
    Map<String, Object> body = new HashMap<String, Object>();
    body.put("tracing", tracing);

    if (description != null && !description.isEmpty()) {
      body.put("description", description);
    }

    if (tags != null && tags.length > 0) {
      body.put("tags", String.join(",", tags));
    }

    final URI uri = uri().withEncodedPath("./vhosts").withPath(name).get();
    this.httpLayer.put(uri, body);
    this.httpLayer.put(uri, body);
  }

  /**
   * Create a virtual host with name and metadata.
   * Note metadata (description and tags) are supported as of RabbitMQ 3.8.
   *
   * @param name        name of the virtual host
   * @param description virtual host description (requires RabbitMQ 3.8 or more)
   * @param tags        virtual host tags (requires RabbitMQ 3.8 or more)
   * @since 3.4.0
   */
  public void createVhost(String name, String description, String... tags) {
    this.createVhost(name, false, description, tags);
  }

  /**
   * Create a virtual host with name and tracing flag.
   *
   * @param name    name of the virtual host
   * @param tracing whether tracing is enabled or not
   * @since 3.4.0
   */
  public void createVhost(String name, boolean tracing) {
    this.createVhost(name, tracing, null);
  }

  public void createVhost(String name) {
    final URI uri = uri().withEncodedPath("./vhosts/").withPath(name).get();
    this.httpLayer.put(uri, null);
  }

  public void deleteVhost(String name) {
    final URI uri = uri().withEncodedPath("./vhosts/").withPath(name).get();
    deleteIgnoring404(uri);
  }

  public List<UserPermissions> getPermissionsIn(String vhost) {
    final URI uri = uri().withEncodedPath("./vhosts/").withPath(vhost).withEncodedPath("/permissions").get();
    UserPermissions[] result = this.getForObjectReturningNullOn404(uri, UserPermissions[].class);
    return asListOrNull(result);
  }

  public List<UserPermissions> getPermissionsOf(String username) {
    final URI uri = uri().withEncodedPath("./users/").withPath(username).withEncodedPath("/permissions").get();
    UserPermissions[] result = this.getForObjectReturningNullOn404(uri, UserPermissions[].class);
    return asListOrNull(result);
  }

  public List<UserPermissions> getPermissions() {
    final URI uri = uri().withEncodedPath("./permissions").get();
    UserPermissions[] result = this.getForObjectReturningNullOn404(uri, UserPermissions[].class);
    return asListOrNull(result);
  }

  public UserPermissions getPermissions(String vhost, String username) {
    final URI uri = uri().withEncodedPath("./permissions").withPath(vhost).withPath(username).get();
    return this.getForObjectReturningNullOn404(uri, UserPermissions.class);
  }

  public List<TopicPermissions> getTopicPermissionsIn(String vhost) {
    final URI uri = uri().withEncodedPath("./vhosts").withPath(vhost).withEncodedPath("topic-permissions").get();
    TopicPermissions[] result = this.getForObjectReturningNullOn404(uri, TopicPermissions[].class);
    return asListOrNull(result);
  }

  public List<TopicPermissions> getTopicPermissionsOf(String username) {
    final URI uri = uri().withEncodedPath("./users").withPath(username).withEncodedPath("topic-permissions").get();
    TopicPermissions[] result = this.getForObjectReturningNullOn404(uri, TopicPermissions[].class);
    return asListOrNull(result);
  }

  public List<TopicPermissions> getTopicPermissions() {
    final URI uri = uriWithPath("./topic-permissions");
    TopicPermissions[] result = this.getForObjectReturningNullOn404(uri, TopicPermissions[].class);
    return asListOrNull(result);
  }

  public List<TopicPermissions> getTopicPermissions(String vhost, String username) {
    final URI uri = uri().withEncodedPath("./topic-permissions").withPath(vhost).withPath(username).get();
    return asListOrNull(this.getForObjectReturningNullOn404(uri, TopicPermissions[].class));
  }

  public List<ExchangeInfo> getExchanges() {
    final URI uri = uriWithPath("./exchanges/");
    return Arrays.asList(this.httpLayer.get(uri, ExchangeInfo[].class));
  }

  @SuppressWarnings("unchecked")
  public Page<ExchangeInfo> getExchanges(QueryParameters queryParameters) {
    final URI uri = uri().withEncodedPath("./exchanges").withQueryParameters(queryParameters).get();
    ParameterizedTypeReference<Page<ExchangeInfo>> type = new ParameterizedTypeReference<>() {
    };
    if (queryParameters.pagination().hasAny()) {
      return this.httpLayer.get(uri, type);
    } else {
      return new Page(this.httpLayer.get(uri, ExchangeInfo[].class));
    }
  }

  public List<ExchangeInfo> getExchanges(String vhost) {
    final URI uri = uri().withEncodedPath("./exchanges").withPath(vhost).get();
    final ExchangeInfo[] result = this.getForObjectReturningNullOn404(uri, ExchangeInfo[].class);
    return asListOrNull(result);
  }

  @SuppressWarnings("unchecked")
  public Page<ExchangeInfo> getExchanges(String vhost, QueryParameters queryParameters) {
    final URI uri = uri().withEncodedPath("./exchanges").withPath(vhost).withQueryParameters(queryParameters).get();
    ParameterizedTypeReference<Page<ExchangeInfo>> type = new ParameterizedTypeReference<>() {
    };
    if (queryParameters.pagination().hasAny()) {
      return this.httpLayer.get(uri, type);
    } else {
      return new Page(this.httpLayer.get(uri, ExchangeInfo[].class));
    }
  }

  public ExchangeInfo getExchange(String vhost, String name) {
    final URI uri = uri().withEncodedPath("./exchanges").withPath(vhost).withPath(name).get();
    return this.getForObjectReturningNullOn404(uri, ExchangeInfo.class);
  }

  public void declareExchange(String vhost, String name, ExchangeInfo info) {
    final URI uri = uri().withEncodedPath("./exchanges").withPath(vhost).withPath(name).get();
    this.httpLayer.put(uri, info);
  }

  public void deleteExchange(String vhost, String name) {
    this.deleteIgnoring404(uri().withEncodedPath("./exchanges").withPath(vhost).withPath(name).get());
  }

  /**
   * Publishes a message to an exchange.
   * <p>
   * <b>DO NOT USE THIS METHOD IN PRODUCTION</b>. The HTTP API has to create a new TCP
   * connection for each message published, which is highly suboptimal.
   * <p>
   * Use this method for test or development code only.
   * In production, use AMQP 0-9-1 or any other messaging protocol that uses a long-lived connection.
   *
   * @param vhost the virtual host to use
   * @param exchange the target exchange
   * @param routingKey the routing key to use
   * @param outboundMessage the message to publish
   * @return true if message has been routed to at least a queue, false otherwise
   * @since 3.4.0
   */
  public boolean publish(String vhost, String exchange, String routingKey, OutboundMessage outboundMessage) {
    if (vhost == null || vhost.isEmpty()) {
      throw new IllegalArgumentException("vhost cannot be null or blank");
    }
    if (exchange == null || exchange.isEmpty()) {
      throw new IllegalArgumentException("exchange cannot be null or blank");
    }

    Map<String, Object> body = Utils.bodyForPublish(routingKey, outboundMessage);

    final URI uri = uri().withEncodedPath("./exchanges").withPath(vhost).withPath(exchange).withEncodedPath("/publish").get();
    Map<?, ?> response = this.httpLayer.post(uri, body, Map.class);
    Boolean routed = (Boolean) response.get("routed");
    if (routed == null) {
      return false;
    } else {
      return routed.booleanValue();
    }
  }

  public List<QueueInfo> getQueues() {
    return this.getQueues((DetailsParameters) null);
  }

  public List<QueueInfo> getQueues(DetailsParameters detailsParameters) {
    final URI uri = uri().withEncodedPath("./queues")
        .withQueryParameters(detailsParameters == null ? Collections.emptyMap() :
            detailsParameters.parameters())
        .get();
    return Arrays.asList(this.httpLayer.get(uri, QueueInfo[].class));
  }

  public List<QueueInfo> getQueues(String vhost) {
    return this.getQueues(vhost, (DetailsParameters) null);
  }

  public List<QueueInfo> getQueues(String vhost, DetailsParameters detailsParameters) {
    final URI uri = uri().withEncodedPath("./queues")
        .withPath(vhost)
        .withQueryParameters(detailsParameters == null ? Collections.emptyMap() :
            detailsParameters.parameters())
        .get();
    final QueueInfo[] result = this.getForObjectReturningNullOn404(uri, QueueInfo[].class);
    return asListOrNull(result);
  }

  @SuppressWarnings("unchecked")
  public Page<QueueInfo> getQueues(String vhost, QueryParameters queryParameters) {
    final URI uri = uri().withEncodedPath("./queues").withPath(vhost).withQueryParameters(queryParameters).get();
    ParameterizedTypeReference<Page<QueueInfo>> type = new ParameterizedTypeReference<>() {
    };
    if (queryParameters.pagination().hasAny()) {
      return this.httpLayer.get(uri, type);
    } else {
      return new Page(this.httpLayer.get(uri, QueueInfo[].class));
    }
  }

  public QueueInfo getQueue(String vhost, String name, DetailsParameters detailsParameters) {
    final URI uri = uri().withEncodedPath("./queues")
        .withPath(vhost)
        .withPath(name)
        .withQueryParameters(detailsParameters == null ? Collections.emptyMap() :
            detailsParameters.parameters())
        .get();
    return this.getForObjectReturningNullOn404(uri, QueueInfo.class);
  }

  public QueueInfo getQueue(String vhost, String name) {
    return getQueue(vhost, name, null);
  }

  @SuppressWarnings("unchecked")
  public Page<QueueInfo> getQueues(QueryParameters queryParameters) {
    final URI uri = uri().withEncodedPath("./queues").withQueryParameters(queryParameters).get();
    ParameterizedTypeReference<Page<QueueInfo>> type = new ParameterizedTypeReference<>() {
    };
    if (queryParameters.pagination().hasAny()) {
      return this.httpLayer.get(uri, type);
    } else {
      return new Page(this.httpLayer.get(uri, QueueInfo[].class));
    }
  }

  public void declarePolicy(String vhost, String name, PolicyInfo info) {
    final URI uri = uri().withEncodedPath("./policies").withPath(vhost).withPath(name).get();
    this.httpLayer.put(uri, info);
  }

  public void declareOperatorPolicy(String vhost, String name, PolicyInfo info) {
    final URI uri = uri().withEncodedPath("./operator-policies").withPath(vhost).withPath(name).get();
    this.httpLayer.put(uri, info);
  }

  public void declareQueue(String vhost, String name, QueueInfo info) {
    final URI uri = uri().withEncodedPath("./queues").withPath(vhost).withPath(name).get();
    this.httpLayer.put(uri, info);
  }

  public void purgeQueue(String vhost, String name) {
    this.deleteIgnoring404(uri().withEncodedPath("./queues").withPath(vhost).withPath(name)
            .withEncodedPath("contents/").get());
  }

  public void deleteQueue(String vhost, String name) {
    this.deleteIgnoring404(uri().withEncodedPath("./queues").withPath(vhost).withPath(name).get());
  }

  public void deleteQueue(String vhost, String name, DeleteQueueParameters deleteInfo) {
    this.deleteIgnoring404(uri().withEncodedPath("./queues").withPath(vhost).withPath(name)
            .withQueryParameters(deleteInfo.getAsQueryParams()).get());
  }


  /**
     * Get messages from a queue.
     *
     * <b>DO NOT USE THIS METHOD IN PRODUCTION</b>. Getting messages with the HTTP API
     * is intended for diagnostics or tests. It does not implement reliable delivery
     * and so should be treated as a sysadmin's tool rather than a general API for messaging.
     *
     * @param vhost    the virtual host the target queue is in
     * @param queue    the queue to consume from
     * @param count    the maximum number of messages to get
     * @param ackMode  determines whether the messages will be removed from the queue
     * @param encoding the expected encoding of the message payload
     * @param truncate to truncate the message payload if it is larger than the size given (in bytes), -1 means no truncation
     * @return the list of messages
     * @see GetAckMode
     * @see GetEncoding
     * @since 3.4.0
     */
    public List<InboundMessage> get(String vhost, String queue,
                                    int count, GetAckMode ackMode, GetEncoding encoding, int truncate) {
        if (vhost == null || vhost.isEmpty()) {
            throw new IllegalArgumentException("vhost cannot be null or blank");
        }
        if (queue == null || queue.isEmpty()) {
            throw new IllegalArgumentException("queue cannot be null or blank");
        }
        Map<String, Object> body = Utils.bodyForGet(count, ackMode, encoding, truncate);

        final URI uri = uri().withEncodedPath("./queues").withPath(vhost).withPath(queue).withEncodedPath("get").get();
        return Arrays.asList(this.httpLayer.post(uri, body, InboundMessage[].class));
    }

    /**
     * Get messages from a queue, with no limit for message payload truncation.
     *
     * <b>DO NOT USE THIS METHOD IN PRODUCTION</b>. Getting messages with the HTTP API
     * is intended for diagnostics or tests. It does not implement reliable delivery
     * and so should be treated as a sysadmin's tool rather than a general API for messaging.
     *
     * @param vhost    the virtual host the target queue is in
     * @param queue    the queue to consume from
     * @param count    the maximum number of messages to get
     * @param ackMode  determines whether the messages will be removed from the queue
     * @param encoding the expected encoding of the message payload
     * @return the list of messages
     * @see GetAckMode
     * @see GetEncoding
     * @since 3.4.0
     */
    public List<InboundMessage> get(String vhost, String queue,
                                    int count, GetAckMode ackMode, GetEncoding encoding) {
        return get(vhost, queue, count, ackMode, encoding, -1);
    }

    /**
     * Get one message from a queue and requeue it.
     *
     * <b>DO NOT USE THIS METHOD IN PRODUCTION</b>. Getting messages with the HTTP API
     * is intended for diagnostics or tests. It does not implement reliable delivery
     * and so should be treated as a sysadmin's tool rather than a general API for messaging.
     *
     * @param vhost the virtual host the target queue is in
     * @param queue the queue to consume from
     * @return the message, null if the queue is empty
     * @see GetAckMode
     * @see GetEncoding
     * @since 3.4.0
     */
    public InboundMessage get(String vhost, String queue) {
        List<InboundMessage> inboundMessages = get(vhost, queue, 1, GetAckMode.NACK_REQUEUE_TRUE, GetEncoding.AUTO, 50000);
        if (inboundMessages != null && inboundMessages.size() == 1) {
            return inboundMessages.get(0);
        } else {
            return null;
        }
    }

  public void deletePolicy(String vhost, String name) {
    this.deleteIgnoring404(uri().withEncodedPath("./policies").withPath(vhost).withPath(name).get());
  }

  public void deleteOperatorPolicy(String vhost, String name) {
    this.deleteIgnoring404(uri().withEncodedPath("./operator-policies").withPath(vhost).withPath(name).get());
  }

  public List<UserInfo> getUsers() {
    final URI uri = uriWithPath("./users/");
    return Arrays.asList(this.httpLayer.get(uri, UserInfo[].class));
  }

  public UserInfo getUser(String username) {
    final URI uri = uri().withEncodedPath("./users").withPath(username).get();
    return this.getForObjectReturningNullOn404(uri, UserInfo.class);
  }

  public void createUser(String username, char[] password, List<String> tags) {
    if(username == null) {
      throw new IllegalArgumentException("username cannot be null");
    }
    if(password == null) {
      throw new IllegalArgumentException("password cannot be null or empty. If you need to create a user that "
            + "will only authenticate using an x509 certificate, use createUserWithPasswordHash with a blank hash.");
    }
    Map<String, Object> body = new HashMap<String, Object>();
    body.put("password", new String(password));
    body.put("tags", String.join(",", tags));

    final URI uri = uri().withEncodedPath("./users").withPath(username).get();
    this.httpLayer.put(uri, body);
  }

  public void createUserWithPasswordHash(String username, char[] passwordHash, List<String> tags) {
    if(username == null) {
      throw new IllegalArgumentException("username cannot be null");
    }
    // passwordless authentication is a thing. See
    // https://github.com/rabbitmq/hop/issues/94 and https://www.rabbitmq.com/authentication.html. MK.
    if(passwordHash == null) {
      passwordHash = "".toCharArray();
    }
    Map<String, Object> body = new HashMap<String, Object>();
    body.put("password_hash", String.valueOf(passwordHash));
    body.put("tags", String.join(",", tags));

    final URI uri = uri().withEncodedPath("./users").withPath(username).get();
    this.httpLayer.put(uri, body);
  }

  public void updateUser(String username, char[] password, List<String> tags) {
    if(username == null) {
      throw new IllegalArgumentException("username cannot be null");
    }
    Map<String, Object> body = new HashMap<String, Object>();
    // only update password if provided
    if(password != null) {
      body.put("password", new String(password));
    }
    body.put("tags", String.join(",", tags));

    final URI uri = uri().withEncodedPath("./users").withPath(username).get();
    this.httpLayer.put(uri, body);
  }

  public void deleteUser(String username) {
    this.deleteIgnoring404(uri().withEncodedPath("./users").withPath(username).get());
  }

  public void updatePermissions(String vhost, String username, UserPermissions permissions) {
    final URI uri = uri().withEncodedPath("./permissions").withPath(vhost).withPath(username).get();
    this.httpLayer.put(uri, permissions);
  }

  public void clearPermissions(String vhost, String username) {
    final URI uri = uri().withEncodedPath("./permissions").withPath(vhost).withPath(username).get();
    deleteIgnoring404(uri);
  }

  public void updateTopicPermissions(String vhost, String username, TopicPermissions permissions) {
    final URI uri = uri().withEncodedPath("./topic-permissions").withPath(vhost).withPath(username).get();
    this.httpLayer.put(uri, permissions);
  }

  public void clearTopicPermissions(String vhost, String username) {
    final URI uri = uri().withEncodedPath("./topic-permissions").withPath(vhost).withPath(username).get();
    deleteIgnoring404(uri);
  }

  public List<PolicyInfo> getPolicies() {
    final URI uri = uriWithPath("./policies/");
    return Arrays.asList(this.httpLayer.get(uri, PolicyInfo[].class));
  }

  public List<PolicyInfo> getPolicies(String vhost) {
    final URI uri = uri().withEncodedPath("./policies").withPath(vhost).get();
    final PolicyInfo[] result = this.getForObjectReturningNullOn404(uri, PolicyInfo[].class);
    return asListOrNull(result);
  }

  public List<PolicyInfo> getOperatorPolicies() {
    final URI uri = uriWithPath("./operator-policies/");
    return Arrays.asList(this.httpLayer.get(uri, PolicyInfo[].class));
  }

  public List<PolicyInfo> getOperatorPolicies(String vhost) {
    final URI uri = uri().withEncodedPath("./operator-policies").withPath(vhost).get();
    final PolicyInfo[] result = this.getForObjectReturningNullOn404(uri, PolicyInfo[].class);
    return asListOrNull(result);
  }

  public List<BindingInfo> getBindings() {
    final URI uri = uriWithPath("./bindings/");
    return Arrays.asList(this.httpLayer.get(uri, BindingInfo[].class));
  }

  public List<BindingInfo> getBindings(String vhost) {
    final URI uri = uri().withEncodedPath("./bindings").withPath(vhost).get();
    return Arrays.asList(this.httpLayer.get(uri, BindingInfo[].class));
  }

  /**
   * Returns a list of bindings where provided exchange is the source (other things are
   * bound to it).
   *
   * @param vhost    vhost of the exchange
   * @param exchange source exchange name
   * @return list of bindings
   */
  public List<BindingInfo> getBindingsBySource(String vhost, String exchange) {
    final String x = exchange.equals("") ? "amq.default" : exchange;
    final URI uri = uri().withEncodedPath("./exchanges").withPath(vhost).withPath(x).withEncodedPath("bindings/source").get();
    return Arrays.asList(this.httpLayer.get(uri, BindingInfo[].class));
  }

  /**
   * Returns a list of bindings where provided exchange is the destination (it is
   * bound to another exchange).
   *
   * @param vhost    vhost of the exchange
   * @param exchange destination exchange name
   * @return list of bindings
   */
  public List<BindingInfo> getExchangeBindingsByDestination(String vhost, String exchange) {
    final String x = exchange.equals("") ? "amq.default" : exchange;
    final URI uri = uri().withEncodedPath("./exchanges").withPath(vhost).withPath(x).withEncodedPath("bindings/destination").get();
    final BindingInfo[] result = this.httpLayer.get(uri, BindingInfo[].class);
    return asListOrNull(result);
  }

  /**
   * Returns a list of bindings where provided queue is the destination.
   *
   * @param vhost    vhost of the exchange
   * @param queue    destination queue name
   * @return list of bindings
   */
  public List<BindingInfo> getQueueBindings(String vhost, String queue) {
    final URI uri = uri().withEncodedPath("./queues").withPath(vhost).withPath(queue).withEncodedPath("bindings").get();
    final BindingInfo[] result = this.httpLayer.get(uri, BindingInfo[].class);
    return asListOrNull(result);
  }

  public List<BindingInfo> getQueueBindingsBetween(String vhost, String exchange, String queue) {
    final URI uri = uri().withEncodedPath("./bindings").withPath(vhost).withEncodedPath("e").withPath(exchange)
            .withEncodedPath("q").withPath(queue).get();
    final BindingInfo[] result = this.httpLayer.get(uri, BindingInfo[].class);
    return asListOrNull(result);
  }

  public List<BindingInfo> getExchangeBindingsBetween(String vhost, String source, String destination) {
    final URI uri = uri().withEncodedPath("./bindings").withPath(vhost).withEncodedPath("e").withPath(source)
            .withEncodedPath("e").withPath(destination).get();
    final BindingInfo[] result = this.httpLayer.get(uri, BindingInfo[].class);
    return asListOrNull(result);
  }

  /**
   * Binds a queue to an exchange.
   * @param vhost virtual host the queue and exchange are in
   * @param queue the queue name
   * @param exchange the exchange name
   * @param routingKey the routing key to use
   */
  public void bindQueue(String vhost, String queue, String exchange, String routingKey) {
    bindQueue(vhost, queue, exchange, routingKey, new HashMap<String, Object>());
  }

  /**
   * Binds a queue to an exchange.
   * @param vhost virtual host the queue and exchange are in
   * @param queue the queue name
   * @param exchange the exchange name
   * @param routingKey the routing key to use
   * @param args additional map of arguments (used by some exchange types)
   */
  public void bindQueue(String vhost, String queue, String exchange, String routingKey, Map<String, Object> args) {
    if(vhost == null || vhost.isEmpty()) {
      throw new IllegalArgumentException("vhost cannot be null or blank");
    }
    if(queue == null || queue.isEmpty()) {
      throw new IllegalArgumentException("queue cannot be null or blank");
    }
    if(exchange == null || exchange.isEmpty()) {
      throw new IllegalArgumentException("exchange cannot be null or blank");
    }
    Map<String, Object> body = new HashMap<String, Object>();
    if(!(args == null)) {
      body.put("arguments", args);
    }
    body.put("routing_key", routingKey);

    final URI uri = uri().withEncodedPath("./bindings").withPath(vhost).withEncodedPath("e").withPath(exchange)
            .withEncodedPath("q").withPath(queue).get();
    this.httpLayer.post(uri, body, null);
  }

  /**
   * Unbinds a queue from an exchange.
   * @param vhost virtual host the queue and exchange are in
   * @param queue the queue name
   * @param exchange the exchange name
   * @param routingKey the routing key used when binding
   */
  public void unbindQueue(String vhost, String queue, String exchange, String routingKey) {
    if(vhost == null || vhost.isEmpty()) {
      throw new IllegalArgumentException("vhost cannot be null or blank");
    }
    if(queue == null || queue.isEmpty()) {
      throw new IllegalArgumentException("queue cannot be null or blank");
    }
    if(exchange == null || exchange.isEmpty()) {
      throw new IllegalArgumentException("exchange cannot be null or blank");
    }
    
    this.deleteIgnoring404(uri().withEncodedPath("./bindings").withPath(vhost).withEncodedPath("e").withPath(exchange)
            .withEncodedPath("q").withPath(queue).withPath(routingKey).get());
  }

  /**
   * Binds a destination exchange to a source one.
   * @param vhost virtual host the exchanges are in
   * @param destination the destination exchange name
   * @param source the source exchange name
   * @param routingKey the routing key to use
   */
  public void bindExchange(String vhost, String destination, String source, String routingKey) {
    bindExchange(vhost, destination, source, routingKey, new HashMap<String, Object>());
  }

  /**
   * Binds a destination exchange to a source one.
   * @param vhost virtual host the exchanges are in
   * @param destination the destination exchange name
   * @param source the source exchange name
   * @param routingKey the routing key to use
   * @param args additional map of arguments (used by some exchange types)
   */
  public void bindExchange(String vhost, String destination, String source, String routingKey, Map<String, Object> args) {
    if(vhost == null || vhost.isEmpty()) {
      throw new IllegalArgumentException("vhost cannot be null or blank");
    }
    if(destination == null || destination.isEmpty()) {
      throw new IllegalArgumentException("destination cannot be null or blank");
    }
    if(source == null || source.isEmpty()) {
      throw new IllegalArgumentException("source cannot be null or blank");
    }
    Map<String, Object> body = new HashMap<String, Object>();
    if(!(args == null)) {
      body.put("arguments", args);
    }
    body.put("routing_key", routingKey);

    final URI uri = uri().withEncodedPath("./bindings").withPath(vhost).withEncodedPath("e").withPath(source)
            .withEncodedPath("e").withPath(destination).get();
    this.httpLayer.post(uri, body, null);
  }

  /**
   * Unbinds a destination exchange from a source one.
   * @param vhost virtual host the exchanges are in
   * @param destination the destination exchange name
   * @param source the source exchange name
   * @param routingKey the routing key used when binding
   */
  public void unbindExchange(String vhost, String destination, String source, String routingKey) {
    if(vhost == null || vhost.isEmpty()) {
      throw new IllegalArgumentException("vhost cannot be null or blank");
    }
    if(destination == null || destination.isEmpty()) {
      throw new IllegalArgumentException("destination cannot be null or blank");
    }
    if(source == null || source.isEmpty()) {
      throw new IllegalArgumentException("source cannot be null or blank");
    }
	    
    this.deleteIgnoring404(uri().withEncodedPath("./bindings").withPath(vhost).withEncodedPath("e").withPath(source)
            .withEncodedPath("e").withPath(destination).withPath(routingKey).get());
  }

  public ClusterId getClusterName() {
    return this.httpLayer.get(uriWithPath("./cluster-name"), ClusterId.class);
  }

  public void setClusterName(String name) {
    if(name== null || name.isEmpty()) {
      throw new IllegalArgumentException("name cannot be null or blank");
    }
    final URI uri = uriWithPath("./cluster-name");
    Map<String, String> m = new HashMap<String, String>();
    m.put("name", name);
    this.httpLayer.put(uri, m);
  }

  @SuppressWarnings({"unchecked","rawtypes"})
  public List<Map> getExtensions() {
    final URI uri = uriWithPath("./extensions/");
    return Arrays.asList(this.httpLayer.get(uri, Map[].class));
  }

  public Definitions getDefinitions() {
    final URI uri = uriWithPath("./definitions/");
    return this.httpLayer.get(uri, Definitions.class);
  }
  
  //
  // Shovel support
  //

  /**
   * Declares a shovel.
   * 
   * @param vhost virtual host where to declare the shovel
   * @param info Shovel info. 
   */
  public void declareShovel(String vhost, ShovelInfo info) {
    Map<String, Object> props = info.getDetails().getPublishProperties();
    if(props != null && props.isEmpty()) {
      throw new IllegalArgumentException("Shovel publish properties must be a non-empty map or null");
    }
    final URI uri = uri().withEncodedPath("./parameters/shovel").withPath(vhost).withPath(info.getName()).get();
    this.httpLayer.put(uri, info);
  }

  /**
   * Returns virtual host shovels.
   * 
   * @return Shovels.
   */
  public List<ShovelInfo> getShovels() {
    final URI uri = uriWithPath("./parameters/shovel/");
    return Arrays.asList(this.httpLayer.get(uri, ShovelInfo[].class));
  }

  /**
   * Returns virtual host shovels.
   * 
   * @param vhost Virtual host from where search shovels.
   * @return Shovels.
   */
  public List<ShovelInfo> getShovels(String vhost) {
    final URI uri = uri().withEncodedPath("./parameters/shovel").withPath(vhost).get();
    final ShovelInfo[] result = this.getForObjectReturningNullOn404(uri, ShovelInfo[].class);
    return asListOrNull(result);
  }

  /**
   * Returns virtual host shovels.
   * 
   * @return Shovels.
   */
  public List<ShovelStatus> getShovelsStatus() {
    final URI uri = uriWithPath("./shovels/");
    return Arrays.asList(this.httpLayer.get(uri, ShovelStatus[].class));
  }

  /**
   * Returns virtual host shovels.
   * 
   * @param vhost Virtual host from where search shovels.
   * @return Shovels.
   */
  public List<ShovelStatus> getShovelsStatus(String vhost) {
    final URI uri = uri().withEncodedPath("./shovels").withPath(vhost).get();
    final ShovelStatus[] result = this.getForObjectReturningNullOn404(uri, ShovelStatus[].class);
    return asListOrNull(result);
  }

  /**
   * Deletes the specified shovel from specified virtual host.
   * 
   * @param vhost virtual host from where to delete the shovel
   * @param shovelname Shovel to be deleted.
   */
  public void deleteShovel(String vhost, String shovelname) {
	    this.deleteIgnoring404(uri().withEncodedPath("./parameters/shovel").withPath(vhost).withPath(shovelname).get());
  }

  //
  // Federation support
  //

  /**
   * Declares an upstream
   * @param vhost virtual host for which to declare the upstream
   * @param name name of the upstream to declare
   * @param details upstream arguments
   */
  public void declareUpstream(String vhost, String name, UpstreamDetails details) {
    if (details.getUri() == null || details.getUri().isBlank()) {
      throw new IllegalArgumentException("Upstream uri must not be null or empty");
    }
    final URI uri = uri().withEncodedPath("./parameters/federation-upstream").withPath(vhost).withPath(name).get();
    UpstreamInfo body = new UpstreamInfo();
    body.setVhost(vhost);
    body.setName(name);
    body.setValue(details);
    this.httpLayer.put(uri, body);
  }

  /**
   * Deletes an upstream
   * @param vhost virtual host for which to delete the upstream
   * @param name name of the upstream to delete
   */
  public void deleteUpstream(String vhost, String name) {
    this.deleteIgnoring404(uri().withEncodedPath("./parameters/federation-upstream").withPath(vhost).withPath(name).get());
  }

  /**
   * Returns a list of upstreams for "/" virtual host
   *
   * @return upstream info
   */
  public List<UpstreamInfo> getUpstreams() {
    return getParameters("federation-upstream", new ParameterizedTypeReference<>() {
    });
  }

  /**
   * Returns a list of upstreams
   *
   * @param vhost virtual host the upstreams are in.
   * @return upstream info
   */
  public List<UpstreamInfo> getUpstreams(String vhost) {
    return getParameters(vhost, "federation-upstream", new ParameterizedTypeReference<>() {
    });
  }

  /**
   * Declares an upstream set.
   * @param vhost virtual host for which to declare the upstream set
   * @param name name of the upstream set to declare
   * @param details upstream set arguments
   */
  public void declareUpstreamSet(String vhost, String name, List<UpstreamSetDetails> details) {
    for (UpstreamSetDetails item : details) {
      if (item.getUpstream() == null || item.getUpstream().isBlank()) {
        throw new IllegalArgumentException("Each federation upstream set item must have a non-null and not " +
                "empty upstream name");
      }
    }
    final URI uri = uri().withEncodedPath("./parameters/federation-upstream-set").withPath(vhost).withPath(name).get();
    UpstreamSetInfo body = new UpstreamSetInfo();
    body.setVhost(vhost);
    body.setName(name);
    body.setValue(details);
    this.httpLayer.put(uri, body);
  }

  /**
   * Deletes an upstream set
   * @param vhost virtual host for which to delete the upstream set
   * @param name name of the upstream set to delete
   */
  public void deleteUpstreamSet(String vhost, String name) {
    this.deleteIgnoring404(uri().withEncodedPath("./parameters/federation-upstream-set").withPath(vhost).withPath(name).get());
  }

  /**
   * Returns a list of upstream sets for "/" virtual host
   *
   * @return upstream info
   */
  public List<UpstreamSetInfo> getUpstreamSets() {
    return getParameters("federation-upstream-set", new ParameterizedTypeReference<List<UpstreamSetInfo>>() {
    });
  }

  /**
   * Returns a list of upstream sets.
   *
   * @param vhost Virtual host from where to get upstreams.
   * @return upstream set info
   */
  public List<UpstreamSetInfo> getUpstreamSets(String vhost) {
    return getParameters(vhost, "federation-upstream-set", new ParameterizedTypeReference<List<UpstreamSetInfo>>() {
    });
  }

  /**
   * Returns the limits (max queues and connections) for all virtual hosts.
   *
   * @return the limits
   * @since 3.7.0
   */
  public List<VhostLimits> getVhostLimits() {
    final URI uri = uriWithPath("./vhost-limits/");
    return asListOrNull(getForObjectReturningNullOn404(uri, VhostLimits[].class));
  }

  /**
   * Returns the limits (max queues and connections) for a given virtual host.
   *
   * @param vhost the virtual host
   * @return the limits for this virtual host
   * @since 3.7.0
   */
  public VhostLimits getVhostLimits(String vhost) {
    final URI uri = uri().withEncodedPath("./vhost-limits").withPath(vhost).get();
    VhostLimits limits = this.httpLayer.get(uri, VhostLimits.class);
    if (limits != null && limits.getVhost() == null) {
      limits = new VhostLimits(vhost, -1, -1);
    }
    return limits;
  }

  /**
   * Sets the max number (limit) of connections for a virtual host.
   *
   * @param vhost the virtual host
   * @param limit the max number of connections allowed
   * @since 3.7.0
   */
  public void limitMaxNumberOfConnections(String vhost, int limit) {
    final URI uri = uri().withEncodedPath("./vhost-limits").withPath(vhost).withEncodedPath("max-connections").get();
    this.httpLayer.put(uri, Collections.singletonMap("value", limit));
  }

  /**
   * Sets the max number (limit) of queues for a virtual host.
   *
   * @param vhost the virtual host
   * @param limit the max number of queues allowed
   * @since 3.7.0
   */
  public void limitMaxNumberOfQueues(String vhost, int limit) {
    final URI uri = uri().withEncodedPath("./vhost-limits").withPath(vhost).withEncodedPath("max-queues").get();
    this.httpLayer.put(uri, Collections.singletonMap("value", limit));
  }

  /**
   * Clears the connection limit for a virtual host.
   *
   * @param vhost the virtual host
   * @since 3.7.0
   */
  public void clearMaxConnectionsLimit(String vhost) {
    final URI uri = uri().withEncodedPath("./vhost-limits").withPath(vhost).withEncodedPath("max-connections").get();
    this.deleteIgnoring404(uri);
  }

  /**
   * Clears the queue limit for a virtual host.
   *
   * @param vhost the virtual host
   * @since 3.7.0
   */
  public void clearMaxQueuesLimit(String vhost) {
    final URI uri = uri().withEncodedPath("./vhost-limits").withPath(vhost).withEncodedPath("max-queues").get();
    this.deleteIgnoring404(uri);
  }

  public MqttVhostPortInfo getMqttVhostPorts(){
    return getGlobalParameters("mqtt_port_to_vhost_mapping", new ParameterizedTypeReference<>() {});
  }

  public void deleteMqttVhostPorts() {
    this.deleteIgnoring404(uri().withEncodedPath("./global-parameters/mqtt_port_to_vhost_mapping").get());
  }

  public void setMqttVhostPorts(Map<Integer, String> portMappings) {
    for (String vhost : portMappings.values()){
      if (vhost.isBlank()) {
        throw new IllegalArgumentException("Map with undefined vhosts provided!");
      }
    }

    final URI uri = uri().withEncodedPath("./global-parameters/mqtt_port_to_vhost_mapping").get();

    MqttVhostPortInfo body = new MqttVhostPortInfo();
    body.setValue(portMappings);

    this.httpLayer.put(uri, body);
  }

  private <T> List<T> getParameters(String component, final ParameterizedTypeReference<List<T>> responseType) {
    final URI uri = uri().withEncodedPath("./parameters").withEncodedPath(component).withPathSeparator().get();
    return this.httpLayer.get(uri, responseType);
  }

  private <T> List<T> getParameters(String vhost, String component, final ParameterizedTypeReference<List<T>> responseType) {
    final URI uri = uri().withEncodedPath("./parameters").withEncodedPath(component).withPath(vhost).get();
    return getForObjectReturningNullOn404(uri, responseType);
  }

  private <T> T getGlobalParameters(String name, final ParameterizedTypeReference<T> responseType) {
    final URI uri = uri().withEncodedPath("./global-parameters").withEncodedPath(name).withPathSeparator().get();
    return getForObjectReturningNullOn404(uri, responseType);
  }

  //
  // Implementation
  //

  /**
   * Produces a URI used to issue HTTP requests to avoid double-escaping of path segments
   * (e.g. vhost names).
   *
   * @param path The path after /api/
   * @return resolved URI
   */
  private URI uriWithPath(final String path) {
    return this.rootUri.resolve(path);
  }

  private Utils.URIBuilder uri() {
    return new Utils.URIBuilder(rootUri);
  }


  private <T> T getForObjectReturningNullOn404(final URI uri, final Class<T> klass) {
    return this.httpLayer.get(uri, klass);
  }

  private <T> T getForObjectReturningNullOn404(final URI uri, final ParameterizedTypeReference<T> responseType) {
    return this.httpLayer.get(uri, responseType);
  }

  private void deleteIgnoring404(URI uri) {
    this.httpLayer.delete(uri, null);
  }

  private void deleteIgnoring404(URI uri, Map<String, String> headers) {
    this.httpLayer.delete(uri, headers);
  }

  private <T> List<T> asListOrNull(T[] result) {
    if(result == null) {
      return null;
    } else {
      return Arrays.asList(result);
    }
  }

}
