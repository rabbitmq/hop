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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import com.rabbitmq.http.client.HttpLayer.HttpLayerFactory;
import com.rabbitmq.http.client.domain.AlivenessTestResult;
import com.rabbitmq.http.client.domain.AuthenticationAttemptStatistics;
import com.rabbitmq.http.client.domain.BindingInfo;
import com.rabbitmq.http.client.domain.ChannelInfo;
import com.rabbitmq.http.client.domain.ClusterId;
import com.rabbitmq.http.client.domain.ConnectionInfo;
import com.rabbitmq.http.client.domain.ConsumerDetails;
import com.rabbitmq.http.client.domain.CurrentUserDetails;
import com.rabbitmq.http.client.domain.Definitions;
import com.rabbitmq.http.client.domain.DeleteQueueParameters;
import com.rabbitmq.http.client.domain.DeprecatedFeature;
import com.rabbitmq.http.client.domain.DetailsParameters;
import com.rabbitmq.http.client.domain.ExchangeInfo;
import com.rabbitmq.http.client.domain.FeatureFlag;
import com.rabbitmq.http.client.domain.FeatureFlagStability;
import com.rabbitmq.http.client.domain.FeatureFlagState;
import com.rabbitmq.http.client.domain.OAuthConfiguration;
import com.rabbitmq.http.client.domain.GlobalRuntimeParameter;
import com.rabbitmq.http.client.domain.InboundMessage;
import com.rabbitmq.http.client.domain.MqttVhostPortInfo;
import com.rabbitmq.http.client.domain.NodeInfo;
import com.rabbitmq.http.client.domain.OutboundMessage;
import com.rabbitmq.http.client.domain.OverviewResponse;
import com.rabbitmq.http.client.domain.Page;
import com.rabbitmq.http.client.domain.PolicyInfo;
import com.rabbitmq.http.client.domain.QueryParameters;
import com.rabbitmq.http.client.domain.QueueInfo;
import com.rabbitmq.http.client.domain.ShovelInfo;
import com.rabbitmq.http.client.domain.ShovelStatus;
import com.rabbitmq.http.client.domain.StreamConsumer;
import com.rabbitmq.http.client.domain.StreamPublisher;
import com.rabbitmq.http.client.domain.TopicPermissions;
import com.rabbitmq.http.client.domain.UserLimits;
import com.rabbitmq.http.client.domain.UpstreamDetails;
import com.rabbitmq.http.client.domain.UpstreamInfo;
import com.rabbitmq.http.client.domain.UpstreamSetDetails;
import com.rabbitmq.http.client.domain.UpstreamSetInfo;
import com.rabbitmq.http.client.domain.UserConnectionInfo;
import com.rabbitmq.http.client.domain.UserInfo;
import com.rabbitmq.http.client.domain.UserPermissions;
import com.rabbitmq.http.client.domain.VhostInfo;
import com.rabbitmq.http.client.domain.VhostLimits;

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
   * Provides an overview of the most commonly used cluster metrics.
   *
   * <p>Requires the {@code management} user tag. Does not modify state.
   *
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
   * @deprecated This endpoint is a no-op since RabbitMQ 3.12.4 and has been removed
   * in RabbitMQ 4.0. Use the health check endpoints instead.
   * @see #healthCheckClusterAlarms()
   * @see #healthCheckLocalAlarms()
   */
  @Deprecated
  public boolean alivenessTest(String vhost) {
    final URI uri = uri().withEncodedPath("./aliveness-test").withPath(vhost).get();
    return this.httpLayer.get(uri, AlivenessTestResult.class).isSuccessful();
  }

  /**
   * Returns information about the authenticated user.
   *
   * <p>Requires the {@code management} user tag. Does not modify state.
   *
   * @return information about the user used by this client instance
   */
  public CurrentUserDetails whoAmI() {
    final URI uri = uriWithPath("./whoami/");
    return this.httpLayer.get(uri, CurrentUserDetails.class);
  }

  /**
   * Retrieves state and metrics information for all nodes in the cluster.
   *
   * <p>Requires the {@code monitoring} user tag. Does not modify state.
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
   * <p>Requires the {@code monitoring} user tag. Does not modify state.
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
   * <p>Requires the {@code monitoring} user tag for all connections, or {@code management}
   * for own connections only. Does not modify state.
   *
   * @return list of connections across the cluster
   */
  public List<ConnectionInfo> getConnections() {
    final URI uri = uri().withEncodedPath("./connections/").get();
    return Arrays.asList(this.httpLayer.get(uri, ConnectionInfo[].class));
  }

  /**
   * Retrieves state and metrics information for all client connections across the cluster
   * using query parameters.
   *
   * <p>Requires the {@code monitoring} user tag for all connections, or {@code management}
   * for own connections only. Does not modify state.
   *
   * @param queryParameters pagination and filtering parameters
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
   * <p>Requires the {@code monitoring} user tag. Does not modify state.
   *
   * @param name connection name
   * @return connection information
   */
  public ConnectionInfo getConnection(String name) {
    final URI uri = uri().withEncodedPath("./connections").withPath(name).get();
    return this.httpLayer.get(uri, ConnectionInfo.class);
  }

  /**
   * Lists connections that belong to a specific user (used the provided username
   * during authentication).
   *
   * <p>Requires the {@code monitoring} user tag. Does not modify state.
   *
   * @param username username
   * @return list of connections for the user
   */
  public List<UserConnectionInfo> getConnectionsOfUser(String username) {
    final URI uri = uri().withEncodedPath("./connections/username/").withPath(username).get();
    return Arrays.asList(this.httpLayer.get(uri, UserConnectionInfo[].class));
  }

  /**
   * Forcefully closes individual connection.
   * The client will receive a <i>connection.close</i> method frame.
   *
   * <p>Requires the {@code administrator} user tag for other users' connections,
   * or {@code management} for own connections.
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
   * <p>Requires the {@code administrator} user tag.
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
   * <p>Requires the {@code administrator} user tag for other users' connections,
   * or {@code management} for own connections.
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
   * <p>Requires the {@code management} user tag and {@code read} permissions.
   * Does not modify state.
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
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
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
   * <p>Requires the {@code monitoring} user tag for all channels, or {@code management}
   * for own channels only. Does not modify state.
   *
   * @return list of channels across the cluster
   */
  public List<ChannelInfo> getChannels() {
    final URI uri = uriWithPath("./channels/");
    return Arrays.asList(this.httpLayer.get(uri, ChannelInfo[].class));
  }

  /**
   * Retrieves state and metrics information for all channels across the cluster
   * using query parameters.
   *
   * <p>Requires the {@code monitoring} user tag for all channels, or {@code management}
   * for own channels only. Does not modify state.
   *
   * @param queryParameters pagination and filtering parameters
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
   *
   * <p>Requires the {@code monitoring} user tag. Does not modify state.
   *
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
   * <p>Requires the {@code monitoring} user tag. Does not modify state.
   *
   * @param name channel name
   * @return channel information
   */
  public ChannelInfo getChannel(String name) {
    final URI uri = uri().withEncodedPath("./channels").withPath(name).get();
    return this.httpLayer.get(uri, ChannelInfo.class);
  }

  /**
   * Lists virtual hosts in the cluster.
   *
   * <p>Requires the {@code monitoring} user tag for all vhosts, or {@code management}
   * for accessible vhosts only. Does not modify state.
   *
   * @return list of virtual hosts
   */
  public List<VhostInfo> getVhosts() {
    final URI uri = uriWithPath("./vhosts/");
    return Arrays.asList(this.httpLayer.get(uri, VhostInfo[].class));
  }

  /**
   * Returns information about a virtual host.
   *
   * <p>Requires the {@code management} user tag. Does not modify state.
   *
   * @param name virtual host name
   * @return virtual host information
   */
  public VhostInfo getVhost(String name) {
    final URI uri = uri().withEncodedPath("./vhosts").withPath(name).get();
    return getForObjectReturningNullOn404(uri, VhostInfo.class);
  }

  /**
   * Create a virtual host with name, tracing flag, and metadata.
   * Note metadata (description and tags) are supported as of RabbitMQ 3.8.
   *
   * <p>Requires the {@code administrator} user tag.
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
   * <p>Requires the {@code administrator} user tag.
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
   * <p>Requires the {@code administrator} user tag.
   *
   * @param name    name of the virtual host
   * @param tracing whether tracing is enabled or not
   * @since 3.4.0
   */
  public void createVhost(String name, boolean tracing) {
    this.createVhost(name, tracing, null);
  }

  /**
   * Creates a virtual host with the given name.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param name name of the virtual host
   */
  public void createVhost(String name) {
    final URI uri = uri().withEncodedPath("./vhosts/").withPath(name).get();
    this.httpLayer.put(uri, null);
  }

  /**
   * Deletes a virtual host.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param name name of the virtual host to delete
   */
  public void deleteVhost(String name) {
    final URI uri = uri().withEncodedPath("./vhosts/").withPath(name).get();
    deleteIgnoring404(uri);
  }

  /**
   * Enables deletion protection for a virtual host.
   * When enabled, the virtual host cannot be deleted until protection is disabled.
   *
   * <p>Requires RabbitMQ 4.1.0 or later.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param name the virtual host name
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/vhosts">Virtual Hosts</a>
   */
  public void enableVhostDeletionProtection(String name) {
    final URI uri = uri().withEncodedPath("./vhosts").withPath(name).withEncodedPath("deletion/protection").get();
    this.httpLayer.post(uri, Collections.emptyMap(), null);
  }

  /**
   * Disables deletion protection for a virtual host.
   *
   * <p>Requires RabbitMQ 4.1.0 or later.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param name the virtual host name
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/vhosts">Virtual Hosts</a>
   */
  public void disableVhostDeletionProtection(String name) {
    final URI uri = uri().withEncodedPath("./vhosts").withPath(name).withEncodedPath("deletion/protection").get();
    this.httpLayer.delete(uri, null);
  }

  /**
   * Lists permissions in a virtual host.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param vhost virtual host name
   * @return list of permissions in the virtual host
   */
  public List<UserPermissions> getPermissionsIn(String vhost) {
    final URI uri = uri().withEncodedPath("./vhosts/").withPath(vhost).withEncodedPath("/permissions").get();
    UserPermissions[] result = this.getForObjectReturningNullOn404(uri, UserPermissions[].class);
    return asListOrNull(result);
  }

  /**
   * Lists permissions for a specific user.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param username username
   * @return list of permissions for the user
   */
  public List<UserPermissions> getPermissionsOf(String username) {
    final URI uri = uri().withEncodedPath("./users/").withPath(username).withEncodedPath("/permissions").get();
    UserPermissions[] result = this.getForObjectReturningNullOn404(uri, UserPermissions[].class);
    return asListOrNull(result);
  }

  /**
   * Lists all permissions in the cluster.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @return list of permissions
   */
  public List<UserPermissions> getPermissions() {
    final URI uri = uri().withEncodedPath("./permissions").get();
    UserPermissions[] result = this.getForObjectReturningNullOn404(uri, UserPermissions[].class);
    return asListOrNull(result);
  }

  /**
   * Gets permissions for a user in a virtual host.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param vhost virtual host name
   * @param username username
   * @return permissions for the user in the virtual host
   */
  public UserPermissions getPermissions(String vhost, String username) {
    final URI uri = uri().withEncodedPath("./permissions").withPath(vhost).withPath(username).get();
    return this.getForObjectReturningNullOn404(uri, UserPermissions.class);
  }

  /**
   * Lists all topic permissions in a virtual host.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param vhost virtual host name
   * @return list of topic permissions in the virtual host
   */
  public List<TopicPermissions> getTopicPermissionsIn(String vhost) {
    final URI uri = uri().withEncodedPath("./vhosts").withPath(vhost).withEncodedPath("topic-permissions").get();
    TopicPermissions[] result = this.getForObjectReturningNullOn404(uri, TopicPermissions[].class);
    return asListOrNull(result);
  }

  /**
   * Lists all topic permissions of a user.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param username username
   * @return list of topic permissions for the user
   */
  public List<TopicPermissions> getTopicPermissionsOf(String username) {
    final URI uri = uri().withEncodedPath("./users").withPath(username).withEncodedPath("topic-permissions").get();
    TopicPermissions[] result = this.getForObjectReturningNullOn404(uri, TopicPermissions[].class);
    return asListOrNull(result);
  }

  /**
   * Lists all topic permissions in the cluster.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @return list of all topic permissions
   */
  public List<TopicPermissions> getTopicPermissions() {
    final URI uri = uriWithPath("./topic-permissions");
    TopicPermissions[] result = this.getForObjectReturningNullOn404(uri, TopicPermissions[].class);
    return asListOrNull(result);
  }

  /**
   * Gets topic permissions for a user in a specific virtual host.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param vhost virtual host name
   * @param username username
   * @return list of topic permissions for the user in the virtual host
   */
  public List<TopicPermissions> getTopicPermissions(String vhost, String username) {
    final URI uri = uri().withEncodedPath("./topic-permissions").withPath(vhost).withPath(username).get();
    return asListOrNull(this.getForObjectReturningNullOn404(uri, TopicPermissions[].class));
  }

  /**
   * Lists all exchanges across the cluster.
   *
   * <p>Requires the {@code monitoring} user tag for all vhosts, or {@code management}
   * for accessible vhosts only. Does not modify state.
   *
   * @return list of exchanges
   */
  public List<ExchangeInfo> getExchanges() {
    final URI uri = uriWithPath("./exchanges/");
    return Arrays.asList(this.httpLayer.get(uri, ExchangeInfo[].class));
  }

  /**
   * Lists all exchanges across the cluster with pagination.
   *
   * <p>Requires the {@code monitoring} user tag for all vhosts, or {@code management}
   * for accessible vhosts only. Does not modify state.
   *
   * @param queryParameters pagination and filtering parameters
   * @return page of exchanges
   */
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

  /**
   * Lists all exchanges in the given virtual host.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost virtual host name
   * @return list of exchanges in the virtual host
   */
  public List<ExchangeInfo> getExchanges(String vhost) {
    final URI uri = uri().withEncodedPath("./exchanges").withPath(vhost).get();
    final ExchangeInfo[] result = this.getForObjectReturningNullOn404(uri, ExchangeInfo[].class);
    return asListOrNull(result);
  }

  /**
   * Lists exchanges in the given virtual host with pagination.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost virtual host name
   * @param queryParameters pagination and filtering parameters
   * @return page of exchanges in the virtual host
   */
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

  /**
   * Returns information about an exchange.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost virtual host name
   * @param name exchange name
   * @return exchange information
   */
  public ExchangeInfo getExchange(String vhost, String name) {
    final URI uri = uri().withEncodedPath("./exchanges").withPath(vhost).withPath(name).get();
    return this.getForObjectReturningNullOn404(uri, ExchangeInfo.class);
  }

  /**
   * Declares an exchange.
   *
   * <p>Requires the {@code management} user tag and {@code configure} permissions on the vhost.
   *
   * @param vhost virtual host name
   * @param name exchange name
   * @param info exchange information
   */
  public void declareExchange(String vhost, String name, ExchangeInfo info) {
    final URI uri = uri().withEncodedPath("./exchanges").withPath(vhost).withPath(name).get();
    this.httpLayer.put(uri, info);
  }

  /**
   * Deletes an exchange.
   *
   * <p>Requires the {@code management} user tag and {@code configure} permissions on the vhost.
   *
   * @param vhost virtual host name
   * @param name exchange name
   */
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
   * <p>Requires the {@code management} user tag and {@code write} permissions on the exchange.
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

  /**
   * Lists all queues and streams across the cluster.
   *
   * <p>Requires the {@code monitoring} user tag for all vhosts, or {@code management}
   * for accessible vhosts only. Does not modify state.
   *
   * @return list of queues
   */
  public List<QueueInfo> getQueues() {
    return this.getQueues((DetailsParameters) null);
  }

  /**
   * Lists all queues and streams across the cluster with details parameters.
   *
   * <p>Requires the {@code monitoring} user tag for all vhosts, or {@code management}
   * for accessible vhosts only. Does not modify state.
   *
   * @param detailsParameters optional details parameters
   * @return list of queues
   */
  public List<QueueInfo> getQueues(DetailsParameters detailsParameters) {
    final URI uri = uri().withEncodedPath("./queues")
        .withQueryParameters(detailsParameters == null ? Collections.emptyMap() :
            detailsParameters.parameters())
        .get();
    return Arrays.asList(this.httpLayer.get(uri, QueueInfo[].class));
  }

  /**
   * Lists all queues and streams in the given virtual host.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost virtual host name
   * @return list of queues in the virtual host
   */
  public List<QueueInfo> getQueues(String vhost) {
    return this.getQueues(vhost, (DetailsParameters) null);
  }

  /**
   * Lists all queues and streams in the given virtual host with details parameters.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost virtual host name
   * @param detailsParameters optional details parameters
   * @return list of queues in the virtual host
   */
  public List<QueueInfo> getQueues(String vhost, DetailsParameters detailsParameters) {
    final URI uri = uri().withEncodedPath("./queues")
        .withPath(vhost)
        .withQueryParameters(detailsParameters == null ? Collections.emptyMap() :
            detailsParameters.parameters())
        .get();
    final QueueInfo[] result = this.getForObjectReturningNullOn404(uri, QueueInfo[].class);
    return asListOrNull(result);
  }

  /**
   * Lists queues and streams in the given virtual host with pagination.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost virtual host name
   * @param queryParameters pagination and filtering parameters
   * @return page of queues in the virtual host
   */
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

  /**
   * Returns information about a queue or stream.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost virtual host name
   * @param name queue name
   * @param detailsParameters optional details parameters
   * @return queue information
   */
  public QueueInfo getQueue(String vhost, String name, DetailsParameters detailsParameters) {
    final URI uri = uri().withEncodedPath("./queues")
        .withPath(vhost)
        .withPath(name)
        .withQueryParameters(detailsParameters == null ? Collections.emptyMap() :
            detailsParameters.parameters())
        .get();
    return this.getForObjectReturningNullOn404(uri, QueueInfo.class);
  }

  /**
   * Returns information about a queue or stream.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost virtual host name
   * @param name queue name
   * @return queue information
   */
  public QueueInfo getQueue(String vhost, String name) {
    return getQueue(vhost, name, null);
  }

  /**
   * Lists all queues and streams across the cluster with pagination.
   *
   * <p>Requires the {@code monitoring} user tag for all vhosts, or {@code management}
   * for accessible vhosts only. Does not modify state.
   *
   * @param queryParameters pagination and filtering parameters
   * @return page of queues
   */
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

  /**
   * Declares a policy.
   *
   * <p>Requires the {@code policymaker} user tag.
   *
   * @param vhost virtual host name
   * @param name policy name
   * @param info policy information
   */
  public void declarePolicy(String vhost, String name, PolicyInfo info) {
    final URI uri = uri().withEncodedPath("./policies").withPath(vhost).withPath(name).get();
    this.httpLayer.put(uri, info);
  }

  /**
   * Declares an operator policy.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param vhost virtual host name
   * @param name operator policy name
   * @param info policy information
   */
  public void declareOperatorPolicy(String vhost, String name, PolicyInfo info) {
    final URI uri = uri().withEncodedPath("./operator-policies").withPath(vhost).withPath(name).get();
    this.httpLayer.put(uri, info);
  }

  /**
   * Declares a queue.
   *
   * <p>Requires the {@code management} user tag and {@code configure} permissions on the vhost.
   *
   * @param vhost virtual host name
   * @param name queue name
   * @param info queue information
   */
  public void declareQueue(String vhost, String name, QueueInfo info) {
    final URI uri = uri().withEncodedPath("./queues").withPath(vhost).withPath(name).get();
    this.httpLayer.put(uri, info);
  }

  /**
   * Purges a queue, removing all messages.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   *
   * @param vhost virtual host name
   * @param name queue name
   */
  public void purgeQueue(String vhost, String name) {
    this.deleteIgnoring404(uri().withEncodedPath("./queues").withPath(vhost).withPath(name)
            .withEncodedPath("contents/").get());
  }

  /**
   * Deletes a queue.
   *
   * <p>Requires the {@code management} user tag and {@code configure} permissions on the vhost.
   *
   * @param vhost virtual host name
   * @param name queue name
   */
  public void deleteQueue(String vhost, String name) {
    this.deleteIgnoring404(uri().withEncodedPath("./queues").withPath(vhost).withPath(name).get());
  }

  /**
   * Deletes a queue with additional parameters.
   *
   * <p>Requires the {@code management} user tag and {@code configure} permissions on the vhost.
   *
   * @param vhost virtual host name
   * @param name queue name
   * @param deleteInfo additional delete parameters
   */
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
     * <p>Requires the {@code management} user tag and {@code read} permissions on the queue.
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
     * <p>Requires the {@code management} user tag and {@code read} permissions on the queue.
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
     * <p>Requires the {@code management} user tag and {@code read} permissions on the queue.
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

  /**
   * Deletes a policy.
   *
   * <p>Requires the {@code policymaker} user tag.
   *
   * @param vhost virtual host name
   * @param name policy name
   */
  public void deletePolicy(String vhost, String name) {
    this.deleteIgnoring404(uri().withEncodedPath("./policies").withPath(vhost).withPath(name).get());
  }

  /**
   * Deletes an operator policy.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param vhost virtual host name
   * @param name operator policy name
   */
  public void deleteOperatorPolicy(String vhost, String name) {
    this.deleteIgnoring404(uri().withEncodedPath("./operator-policies").withPath(vhost).withPath(name).get());
  }

  /**
   * Lists users in the internal database.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @return list of users
   */
  public List<UserInfo> getUsers() {
    final URI uri = uriWithPath("./users/");
    return Arrays.asList(this.httpLayer.get(uri, UserInfo[].class));
  }

  /**
   * Returns information about a user in the internal database.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param username username
   * @return user information
   */
  public UserInfo getUser(String username) {
    final URI uri = uri().withEncodedPath("./users").withPath(username).get();
    return this.getForObjectReturningNullOn404(uri, UserInfo.class);
  }

  /**
   * Adds a user to the internal database.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param username username
   * @param password password
   * @param tags user tags
   */
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

  /**
   * Adds a user to the internal database with a pre-hashed password.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param username username
   * @param passwordHash pre-hashed password
   * @param tags user tags
   */
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

  /**
   * Updates a user in the internal database.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param username username
   * @param password password (null to keep existing)
   * @param tags user tags
   */
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

  /**
   * Deletes a user from the internal database.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param username username
   */
  public void deleteUser(String username) {
    this.deleteIgnoring404(uri().withEncodedPath("./users").withPath(username).get());
  }

  /**
   * Lists users in the internal database that do not have access to any virtual hosts.
   * This is useful for finding users that may need permissions granted, or are not used
   * and should be cleaned up.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @return list of users without permissions
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/access-control">Access Control</a>
   */
  public List<UserInfo> getUsersWithoutPermissions() {
    final URI uri = uriWithPath("./users/without-permissions");
    return Arrays.asList(this.httpLayer.get(uri, UserInfo[].class));
  }

  /**
   * Declares permissions for a user in a virtual host.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param vhost virtual host name
   * @param username username
   * @param permissions permissions to grant
   */
  public void updatePermissions(String vhost, String username, UserPermissions permissions) {
    final URI uri = uri().withEncodedPath("./permissions").withPath(vhost).withPath(username).get();
    this.httpLayer.put(uri, permissions);
  }

  /**
   * Revokes user permissions in a specific virtual host.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param vhost virtual host name
   * @param username username
   */
  public void clearPermissions(String vhost, String username) {
    final URI uri = uri().withEncodedPath("./permissions").withPath(vhost).withPath(username).get();
    deleteIgnoring404(uri);
  }

  /**
   * Sets topic permissions in a specific virtual host.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param vhost virtual host name
   * @param username username
   * @param permissions topic permissions to set
   */
  public void updateTopicPermissions(String vhost, String username, TopicPermissions permissions) {
    final URI uri = uri().withEncodedPath("./topic-permissions").withPath(vhost).withPath(username).get();
    this.httpLayer.put(uri, permissions);
  }

  /**
   * Clears topic permissions for a user in a specific virtual host.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param vhost virtual host name
   * @param username username
   */
  public void clearTopicPermissions(String vhost, String username) {
    final URI uri = uri().withEncodedPath("./topic-permissions").withPath(vhost).withPath(username).get();
    deleteIgnoring404(uri);
  }

  /**
   * Lists all policies in the cluster.
   *
   * <p>Requires the {@code policymaker} user tag. Does not modify state.
   *
   * @return list of policies
   */
  public List<PolicyInfo> getPolicies() {
    final URI uri = uriWithPath("./policies/");
    return Arrays.asList(this.httpLayer.get(uri, PolicyInfo[].class));
  }

  /**
   * Lists policies in a virtual host.
   *
   * <p>Requires the {@code policymaker} user tag. Does not modify state.
   *
   * @param vhost virtual host name
   * @return list of policies in the virtual host
   */
  public List<PolicyInfo> getPolicies(String vhost) {
    final URI uri = uri().withEncodedPath("./policies").withPath(vhost).get();
    final PolicyInfo[] result = this.getForObjectReturningNullOn404(uri, PolicyInfo[].class);
    return asListOrNull(result);
  }

  /**
   * Lists all operator policies in the cluster.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @return list of operator policies
   */
  public List<PolicyInfo> getOperatorPolicies() {
    final URI uri = uriWithPath("./operator-policies/");
    return Arrays.asList(this.httpLayer.get(uri, PolicyInfo[].class));
  }

  /**
   * Lists operator policies in a virtual host.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param vhost virtual host name
   * @return list of operator policies in the virtual host
   */
  public List<PolicyInfo> getOperatorPolicies(String vhost) {
    final URI uri = uri().withEncodedPath("./operator-policies").withPath(vhost).get();
    final PolicyInfo[] result = this.getForObjectReturningNullOn404(uri, PolicyInfo[].class);
    return asListOrNull(result);
  }

  /**
   * Lists all bindings across the cluster.
   *
   * <p>Requires the {@code monitoring} user tag for all vhosts, or {@code management}
   * for accessible vhosts only. Does not modify state.
   *
   * @return list of bindings
   */
  public List<BindingInfo> getBindings() {
    final URI uri = uriWithPath("./bindings/");
    return Arrays.asList(this.httpLayer.get(uri, BindingInfo[].class));
  }

  /**
   * Lists all bindings in the given virtual host.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost virtual host name
   * @return list of bindings in the virtual host
   */
  public List<BindingInfo> getBindings(String vhost) {
    final URI uri = uri().withEncodedPath("./bindings").withPath(vhost).get();
    return Arrays.asList(this.httpLayer.get(uri, BindingInfo[].class));
  }

  /**
   * Returns a list of bindings where provided exchange is the source (other things are
   * bound to it).
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
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
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
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
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
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

  /**
   * Lists bindings between an exchange and a queue.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost virtual host name
   * @param exchange exchange name
   * @param queue queue name
   * @return list of bindings between the exchange and queue
   */
  public List<BindingInfo> getQueueBindingsBetween(String vhost, String exchange, String queue) {
    final URI uri = uri().withEncodedPath("./bindings").withPath(vhost).withEncodedPath("e").withPath(exchange)
            .withEncodedPath("q").withPath(queue).get();
    final BindingInfo[] result = this.httpLayer.get(uri, BindingInfo[].class);
    return asListOrNull(result);
  }

  /**
   * Lists bindings between two exchanges.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost virtual host name
   * @param source source exchange name
   * @param destination destination exchange name
   * @return list of bindings between the exchanges
   */
  public List<BindingInfo> getExchangeBindingsBetween(String vhost, String source, String destination) {
    final URI uri = uri().withEncodedPath("./bindings").withPath(vhost).withEncodedPath("e").withPath(source)
            .withEncodedPath("e").withPath(destination).get();
    final BindingInfo[] result = this.httpLayer.get(uri, BindingInfo[].class);
    return asListOrNull(result);
  }

  /**
   * Binds a queue to an exchange.
   *
   * <p>Requires the {@code management} user tag and {@code write} permissions on the queue
   * and {@code read} permissions on the exchange.
   *
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
   *
   * <p>Requires the {@code management} user tag and {@code write} permissions on the queue
   * and {@code read} permissions on the exchange.
   *
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
   *
   * <p>Requires the {@code management} user tag and {@code write} permissions on the queue
   * and {@code read} permissions on the exchange.
   *
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
   *
   * <p>Requires the {@code management} user tag and {@code write} permissions on the destination
   * and {@code read} permissions on the source.
   *
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
   *
   * <p>Requires the {@code management} user tag and {@code write} permissions on the destination
   * and {@code read} permissions on the source.
   *
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
   *
   * <p>Requires the {@code management} user tag and {@code write} permissions on the destination
   * and {@code read} permissions on the source.
   *
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

  /**
   * Gets cluster name (identifier).
   *
   * <p>Requires the {@code management} user tag. Does not modify state.
   *
   * @return cluster identity
   */
  public ClusterId getClusterName() {
    return this.httpLayer.get(uriWithPath("./cluster-name"), ClusterId.class);
  }

  /**
   * Sets cluster name (identifier).
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param name the new cluster name
   */
  public void setClusterName(String name) {
    if(name== null || name.isEmpty()) {
      throw new IllegalArgumentException("name cannot be null or blank");
    }
    final URI uri = uriWithPath("./cluster-name");
    Map<String, String> m = new HashMap<String, String>();
    m.put("name", name);
    this.httpLayer.put(uri, m);
  }

  /**
   * Returns cluster metadata tags.
   *
   * <p>Requires the {@code policymaker} user tag. Does not modify state.
   *
   * @return map of cluster tags
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/parameters">Parameters and Policies</a>
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> getClusterTags() {
    try {
      GlobalRuntimeParameter<?> param = getGlobalParameter("cluster_tags");
      if (param != null && param.getValue() instanceof Map) {
        return (Map<String, Object>) param.getValue();
      }
    } catch (HttpClientException e) {
      if (e.status() == 404) {
        return Collections.emptyMap();
      }
      throw e;
    }
    return Collections.emptyMap();
  }

  /**
   * Sets cluster metadata tags.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param tags the tags to set
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/parameters">Parameters and Policies</a>
   */
  public void setClusterTags(Map<String, Object> tags) {
    setGlobalParameter("cluster_tags", tags);
  }

  /**
   * Clears all cluster metadata tags.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/parameters">Parameters and Policies</a>
   */
  public void clearClusterTags() {
    deleteGlobalParameter("cluster_tags");
  }

  /**
   * Lists extensions available in the cluster.
   *
   * <p>Requires the {@code management} user tag. Does not modify state.
   *
   * @return list of extensions
   */
  @SuppressWarnings({"unchecked","rawtypes"})
  public List<Map> getExtensions() {
    final URI uri = uriWithPath("./extensions/");
    return Arrays.asList(this.httpLayer.get(uri, Map[].class));
  }

  /**
   * Exports cluster-wide definitions.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @return cluster definitions
   */
  public Definitions getDefinitions() {
    final URI uri = uriWithPath("./definitions/");
    return this.httpLayer.get(uri, Definitions.class);
  }

  /**
   * Returns definitions for a specific virtual host.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param vhost the virtual host name
   * @return definitions for the virtual host
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/definitions">Definition Export and Import</a>
   */
  public Definitions getDefinitions(String vhost) {
    final URI uri = uri().withEncodedPath("./definitions").withPath(vhost).get();
    return this.httpLayer.get(uri, Definitions.class);
  }

  //
  // Feature flags
  //

  /**
   * Returns all feature flags.
   *
   * <p>Requires the {@code monitoring} user tag. Does not modify state.
   *
   * @return list of feature flags
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/feature-flags">Feature Flags</a>
   */
  public List<FeatureFlag> getFeatureFlags() {
    final URI uri = uriWithPath("./feature-flags/");
    return Arrays.asList(this.httpLayer.get(uri, FeatureFlag[].class));
  }

  /**
   * Enables a feature flag.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param name the name of the feature flag to enable
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/feature-flags">Feature Flags</a>
   */
  public void enableFeatureFlag(String name) {
    final URI uri = uri().withEncodedPath("./feature-flags").withPath(name).withEncodedPath("enable").get();
    this.httpLayer.put(uri, Collections.emptyMap());
  }

  /**
   * Enables all stable feature flags.
   * This iterates through all feature flags and enables those that are stable and disabled.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/feature-flags">Feature Flags</a>
   */
  public void enableAllStableFeatureFlags() {
    List<FeatureFlag> flags = getFeatureFlags();
    for (FeatureFlag flag : flags) {
      if (flag.getState() == FeatureFlagState.DISABLED
          && flag.getStability() == FeatureFlagStability.STABLE) {
        enableFeatureFlag(flag.getName());
      }
    }
  }

  //
  // Health checks
  //

  /**
   * Performs a cluster-wide health check for any active resource alarms.
   * Throws {@link HttpServerException} with status 503 if the check fails.
   *
   * <p>No authentication required. Does not modify state.
   *
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/monitoring#health-checks">Health Checks</a>
   */
  public void healthCheckClusterAlarms() {
    final URI uri = uriWithPath("./health/checks/alarms");
    this.httpLayer.get(uri, Object.class);
  }

  /**
   * Performs a health check for alarms on the local node only.
   * Throws {@link HttpServerException} with status 503 if the check fails.
   *
   * <p>No authentication required. Does not modify state.
   *
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/monitoring#health-checks">Health Checks</a>
   */
  public void healthCheckLocalAlarms() {
    final URI uri = uriWithPath("./health/checks/local-alarms");
    this.httpLayer.get(uri, Object.class);
  }

  /**
   * Checks if a specific port has an active listener.
   * Throws {@link HttpServerException} with status 503 if the check fails.
   *
   * <p>No authentication required. Does not modify state.
   *
   * @param port the port to check
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/monitoring#health-checks">Health Checks</a>
   */
  public void healthCheckPortListener(int port) {
    final URI uri = uri().withEncodedPath("./health/checks/port-listener").withEncodedPath(String.valueOf(port)).get();
    this.httpLayer.get(uri, Object.class);
  }

  /**
   * Checks if a specific protocol listener is active.
   * Throws {@link HttpServerException} with status 503 if the check fails.
   *
   * <p>No authentication required. Does not modify state.
   *
   * @param protocol the protocol to check (e.g., "amqp", "mqtt", "stomp")
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/monitoring#health-checks">Health Checks</a>
   */
  public void healthCheckProtocolListener(String protocol) {
    final URI uri = uri().withEncodedPath("./health/checks/protocol-listener").withPath(protocol).get();
    this.httpLayer.get(uri, Object.class);
  }

  /**
   * Checks if the target node is critical for maintaining quorum.
   * Throws {@link HttpServerException} with status 503 if the check fails.
   *
   * <p>No authentication required. Does not modify state.
   *
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/monitoring#health-checks">Health Checks</a>
   */
  public void healthCheckNodeIsQuorumCritical() {
    final URI uri = uriWithPath("./health/checks/node-is-quorum-critical");
    this.httpLayer.get(uri, Object.class);
  }

  /**
   * Checks if all virtual hosts are running.
   * Throws {@link HttpServerException} with status 503 if the check fails.
   *
   * <p>No authentication required. Does not modify state.
   *
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/monitoring#health-checks">Health Checks</a>
   */
  public void healthCheckVirtualHosts() {
    final URI uri = uriWithPath("./health/checks/virtual-hosts");
    this.httpLayer.get(uri, Object.class);
  }

  //
  // Authentication
  //

  /**
   * Returns the current OAuth 2.0 configuration.
   *
   * <p>Requires the {@code management} user tag. Does not modify state.
   *
   * @return OAuth configuration
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/oauth2">OAuth 2 Guide</a>
   */
  public OAuthConfiguration getOAuthConfiguration() {
    final URI uri = uriWithPath("./auth");
    return this.httpLayer.get(uri, OAuthConfiguration.class);
  }

  /**
   * Returns authentication attempt statistics for a given node.
   *
   * <p>Requires the {@code monitoring} user tag. Does not modify state.
   *
   * @param nodeName the name of the node
   * @return list of authentication attempt statistics per protocol
   * @since 5.5.0
   */
  public List<AuthenticationAttemptStatistics> getAuthAttemptStatistics(String nodeName) {
    final URI uri = uri().withEncodedPath("./auth/attempts").withPath(nodeName).get();
    return Arrays.asList(this.httpLayer.get(uri, AuthenticationAttemptStatistics[].class));
  }

  //
  // Deprecated features
  //

  /**
   * Returns all deprecated features.
   *
   * <p>Requires RabbitMQ 3.13.0 or later.
   *
   * <p>Requires the {@code monitoring} user tag. Does not modify state.
   *
   * @return list of deprecated features
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/deprecated">Deprecated Features</a>
   */
  public List<DeprecatedFeature> getDeprecatedFeatures() {
    final URI uri = uriWithPath("./deprecated-features/");
    return Arrays.asList(this.httpLayer.get(uri, DeprecatedFeature[].class));
  }

  /**
   * Returns deprecated features that are currently in use.
   *
   * <p>Requires RabbitMQ 3.13.0 or later.
   *
   * <p>Requires the {@code monitoring} user tag. Does not modify state.
   *
   * @return list of deprecated features in use
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/deprecated">Deprecated Features</a>
   */
  public List<DeprecatedFeature> getDeprecatedFeaturesInUse() {
    final URI uri = uriWithPath("./deprecated-features/used");
    return Arrays.asList(this.httpLayer.get(uri, DeprecatedFeature[].class));
  }

  //
  // Queue rebalancing
  //

  /**
   * Triggers queue leader rebalancing across the cluster.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/clustering#rebalancing">Queue Leader Rebalancing</a>
   */
  public void rebalanceQueueLeaders() {
    final URI uri = uriWithPath("./rebalance/queues");
    this.httpLayer.post(uri, Collections.emptyMap(), null);
  }

  //
  // Stream protocol support
  //

  /**
   * Returns all stream connections.
   *
   * <p>Requires the {@code monitoring} user tag. Does not modify state.
   *
   * @return list of stream connections
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
   */
  public List<ConnectionInfo> getStreamConnections() {
    final URI uri = uriWithPath("./stream/connections/");
    return Arrays.asList(this.httpLayer.get(uri, ConnectionInfo[].class));
  }

  /**
   * Returns stream connections in a specific virtual host.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost the virtual host name
   * @return list of stream connections
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
   */
  public List<ConnectionInfo> getStreamConnections(String vhost) {
    final URI uri = uri().withEncodedPath("./stream/connections").withPath(vhost).get();
    return Arrays.asList(this.httpLayer.get(uri, ConnectionInfo[].class));
  }

  /**
   * Returns information about a specific stream connection.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost the virtual host name
   * @param name the connection name
   * @return stream connection info
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
   */
  public ConnectionInfo getStreamConnection(String vhost, String name) {
    final URI uri = uri().withEncodedPath("./stream/connections").withPath(vhost).withPath(name).get();
    return this.httpLayer.get(uri, ConnectionInfo.class);
  }

  /**
   * Closes a stream connection.
   *
   * <p>Requires the {@code administrator} user tag for other users' connections,
   * or {@code management} for own connections.
   *
   * @param vhost the virtual host name
   * @param name the connection name
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
   */
  public void closeStreamConnection(String vhost, String name) {
    final URI uri = uri().withEncodedPath("./stream/connections").withPath(vhost).withPath(name).get();
    this.httpLayer.delete(uri, null);
  }

  /**
   * Returns all stream publishers.
   *
   * <p>Requires the {@code monitoring} user tag. Does not modify state.
   *
   * @return list of stream publishers
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
   */
  public List<StreamPublisher> getStreamPublishers() {
    final URI uri = uriWithPath("./stream/publishers/");
    return Arrays.asList(this.httpLayer.get(uri, StreamPublisher[].class));
  }

  /**
   * Returns stream publishers in a specific virtual host.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost the virtual host name
   * @return list of stream publishers
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
   */
  public List<StreamPublisher> getStreamPublishers(String vhost) {
    final URI uri = uri().withEncodedPath("./stream/publishers").withPath(vhost).get();
    return Arrays.asList(this.httpLayer.get(uri, StreamPublisher[].class));
  }

  /**
   * Returns stream publishers for a specific stream.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost the virtual host name
   * @param stream the stream name
   * @return list of stream publishers
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
   */
  public List<StreamPublisher> getStreamPublishers(String vhost, String stream) {
    final URI uri = uri().withEncodedPath("./stream/publishers").withPath(vhost).withPath(stream).get();
    return Arrays.asList(this.httpLayer.get(uri, StreamPublisher[].class));
  }

  /**
   * Returns all stream consumers.
   *
   * <p>Requires the {@code monitoring} user tag. Does not modify state.
   *
   * @return list of stream consumers
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
   */
  public List<StreamConsumer> getStreamConsumers() {
    final URI uri = uriWithPath("./stream/consumers/");
    return Arrays.asList(this.httpLayer.get(uri, StreamConsumer[].class));
  }

  /**
   * Returns stream consumers in a specific virtual host.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost the virtual host name
   * @return list of stream consumers
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
   */
  public List<StreamConsumer> getStreamConsumers(String vhost) {
    final URI uri = uri().withEncodedPath("./stream/consumers").withPath(vhost).get();
    return Arrays.asList(this.httpLayer.get(uri, StreamConsumer[].class));
  }

  /**
   * Returns stream consumers for a specific stream.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost the virtual host name
   * @param stream the stream name
   * @return list of stream consumers
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
   */
  public List<StreamConsumer> getStreamConsumers(String vhost, String stream) {
    final URI uri = uri().withEncodedPath("./stream/consumers").withPath(vhost).withPath(stream).get();
    return Arrays.asList(this.httpLayer.get(uri, StreamConsumer[].class));
  }

  /**
   * Returns stream publishers on a specific stream connection.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost the virtual host name
   * @param connectionName the connection name
   * @return list of stream publishers on the connection
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
   */
  public List<StreamPublisher> getStreamPublishersOnConnection(String vhost, String connectionName) {
    final URI uri = uri().withEncodedPath("./stream/connections").withPath(vhost).withPath(connectionName).withEncodedPath("publishers").get();
    return Arrays.asList(this.httpLayer.get(uri, StreamPublisher[].class));
  }

  /**
   * Returns stream consumers on a specific stream connection.
   *
   * <p>Requires the {@code management} user tag and {@code read} permissions on the vhost.
   * Does not modify state.
   *
   * @param vhost the virtual host name
   * @param connectionName the connection name
   * @return list of stream consumers on the connection
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/streams">RabbitMQ Streams</a>
   */
  public List<StreamConsumer> getStreamConsumersOnConnection(String vhost, String connectionName) {
    final URI uri = uri().withEncodedPath("./stream/connections").withPath(vhost).withPath(connectionName).withEncodedPath("consumers").get();
    return Arrays.asList(this.httpLayer.get(uri, StreamConsumer[].class));
  }

  //
  // Federation links
  //

  /**
   * Returns all federation links.
   *
   * <p>Requires the {@code monitoring} user tag. Does not modify state.
   *
   * @return list of federation links as untyped maps
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/federation">Federation Plugin</a>
   */
  @SuppressWarnings("rawtypes")
  public List<Map> getFederationLinks() {
    final URI uri = uriWithPath("./federation-links/");
    return Arrays.asList(this.httpLayer.get(uri, Map[].class));
  }

  /**
   * Returns federation links in a specific virtual host.
   *
   * <p>Requires the {@code monitoring} user tag. Does not modify state.
   *
   * @param vhost the virtual host name
   * @return list of federation links as untyped maps
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/federation">Federation Plugin</a>
   */
  @SuppressWarnings("rawtypes")
  public List<Map> getFederationLinks(String vhost) {
    final URI uri = uri().withEncodedPath("./federation-links").withPath(vhost).get();
    return Arrays.asList(this.httpLayer.get(uri, Map[].class));
  }

  //
  // Global parameters
  //

  /**
   * Returns all global runtime parameters.
   *
   * <p>Requires the {@code policymaker} user tag. Does not modify state.
   *
   * @return list of global parameters
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/parameters">Parameters and Policies</a>
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public List<GlobalRuntimeParameter<?>> getGlobalParameters() {
    final URI uri = uriWithPath("./global-parameters/");
    return Arrays.asList(this.httpLayer.get(uri, GlobalRuntimeParameter[].class));
  }

  /**
   * Returns a specific global runtime parameter.
   *
   * <p>Requires the {@code policymaker} user tag. Does not modify state.
   *
   * @param name the parameter name
   * @return the global parameter
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/parameters">Parameters and Policies</a>
   */
  @SuppressWarnings("rawtypes")
  public GlobalRuntimeParameter<?> getGlobalParameter(String name) {
    final URI uri = uri().withEncodedPath("./global-parameters").withPath(name).get();
    return this.httpLayer.get(uri, GlobalRuntimeParameter.class);
  }

  /**
   * Sets a global runtime parameter.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param name the parameter name
   * @param value the parameter value
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/parameters">Parameters and Policies</a>
   */
  public void setGlobalParameter(String name, Object value) {
    final URI uri = uri().withEncodedPath("./global-parameters").withPath(name).get();
    GlobalRuntimeParameter<Object> param = new GlobalRuntimeParameter<>();
    param.setName(name);
    param.setValue(value);
    this.httpLayer.put(uri, param);
  }

  /**
   * Deletes a global runtime parameter.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param name the parameter name
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/parameters">Parameters and Policies</a>
   */
  public void deleteGlobalParameter(String name) {
    final URI uri = uri().withEncodedPath("./global-parameters").withPath(name).get();
    this.httpLayer.delete(uri, null);
  }

  //
  // Bulk operations
  //

  /**
   * Deletes multiple users in a single operation.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param usernames the list of usernames to delete
   * @since 5.5.0
   * @see <a href="https://www.rabbitmq.com/docs/access-control">Access Control</a>
   */
  public void deleteUsers(List<String> usernames) {
    final URI uri = uriWithPath("./users/bulk-delete");
    Map<String, Object> body = Collections.singletonMap("users", usernames);
    this.httpLayer.post(uri, body, null);
  }

  //
  // Shovel support
  //

  /**
   * Declares a shovel.
   *
   * <p>Requires the {@code policymaker} user tag.
   *
   * @param vhost virtual host where to declare the shovel
   * @param info Shovel info
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
   * Returns all shovel definitions.
   *
   * <p>Requires the {@code policymaker} user tag. Does not modify state.
   *
   * @return list of shovels
   */
  public List<ShovelInfo> getShovels() {
    final URI uri = uriWithPath("./parameters/shovel/");
    return Arrays.asList(this.httpLayer.get(uri, ShovelInfo[].class));
  }

  /**
   * Returns shovel definitions in a specific virtual host.
   *
   * <p>Requires the {@code policymaker} user tag. Does not modify state.
   *
   * @param vhost virtual host name
   * @return list of shovels
   */
  public List<ShovelInfo> getShovels(String vhost) {
    final URI uri = uri().withEncodedPath("./parameters/shovel").withPath(vhost).get();
    final ShovelInfo[] result = this.getForObjectReturningNullOn404(uri, ShovelInfo[].class);
    return asListOrNull(result);
  }

  /**
   * Returns status of all shovels.
   *
   * <p>Requires the {@code monitoring} user tag. Does not modify state.
   *
   * @return list of shovel statuses
   */
  public List<ShovelStatus> getShovelsStatus() {
    final URI uri = uriWithPath("./shovels/");
    return Arrays.asList(this.httpLayer.get(uri, ShovelStatus[].class));
  }

  /**
   * Returns status of shovels in a specific virtual host.
   *
   * <p>Requires the {@code monitoring} user tag. Does not modify state.
   *
   * @param vhost virtual host name
   * @return list of shovel statuses
   */
  public List<ShovelStatus> getShovelsStatus(String vhost) {
    final URI uri = uri().withEncodedPath("./shovels").withPath(vhost).get();
    final ShovelStatus[] result = this.getForObjectReturningNullOn404(uri, ShovelStatus[].class);
    return asListOrNull(result);
  }

  /**
   * Deletes the specified shovel from specified virtual host.
   *
   * <p>Requires the {@code policymaker} user tag.
   *
   * @param vhost virtual host from where to delete the shovel
   * @param shovelname shovel to be deleted
   */
  public void deleteShovel(String vhost, String shovelname) {
	    this.deleteIgnoring404(uri().withEncodedPath("./parameters/shovel").withPath(vhost).withPath(shovelname).get());
  }

  //
  // Federation support
  //

  /**
   * Declares a federation upstream.
   *
   * <p>Requires the {@code policymaker} user tag.
   *
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
   * Deletes a federation upstream.
   *
   * <p>Requires the {@code policymaker} user tag.
   *
   * @param vhost virtual host for which to delete the upstream
   * @param name name of the upstream to delete
   */
  public void deleteUpstream(String vhost, String name) {
    this.deleteIgnoring404(uri().withEncodedPath("./parameters/federation-upstream").withPath(vhost).withPath(name).get());
  }

  /**
   * Returns a list of federation upstreams for "/" virtual host.
   *
   * <p>Requires the {@code policymaker} user tag. Does not modify state.
   *
   * @return upstream info
   */
  public List<UpstreamInfo> getUpstreams() {
    return getParameters("federation-upstream", new ParameterizedTypeReference<>() {
    });
  }

  /**
   * Returns a list of federation upstreams in a virtual host.
   *
   * <p>Requires the {@code policymaker} user tag. Does not modify state.
   *
   * @param vhost virtual host the upstreams are in
   * @return upstream info
   */
  public List<UpstreamInfo> getUpstreams(String vhost) {
    return getParameters(vhost, "federation-upstream", new ParameterizedTypeReference<>() {
    });
  }

  /**
   * Declares a federation upstream set.
   *
   * <p>Requires the {@code policymaker} user tag.
   *
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
   * Deletes a federation upstream set.
   *
   * <p>Requires the {@code policymaker} user tag.
   *
   * @param vhost virtual host for which to delete the upstream set
   * @param name name of the upstream set to delete
   */
  public void deleteUpstreamSet(String vhost, String name) {
    this.deleteIgnoring404(uri().withEncodedPath("./parameters/federation-upstream-set").withPath(vhost).withPath(name).get());
  }

  /**
   * Returns a list of federation upstream sets for "/" virtual host.
   *
   * <p>Requires the {@code policymaker} user tag. Does not modify state.
   *
   * @return upstream set info
   */
  public List<UpstreamSetInfo> getUpstreamSets() {
    return getParameters("federation-upstream-set", new ParameterizedTypeReference<List<UpstreamSetInfo>>() {
    });
  }

  /**
   * Returns a list of federation upstream sets in a virtual host.
   *
   * <p>Requires the {@code policymaker} user tag. Does not modify state.
   *
   * @param vhost virtual host name
   * @return upstream set info
   */
  public List<UpstreamSetInfo> getUpstreamSets(String vhost) {
    return getParameters(vhost, "federation-upstream-set", new ParameterizedTypeReference<List<UpstreamSetInfo>>() {
    });
  }

  /**
   * Returns the limits (max queues and connections) for all virtual hosts.
   *
   * <p>Requires the {@code administrator} user tag.
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
   * <p>Requires the {@code administrator} user tag.
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
   * <p>Requires the {@code administrator} user tag.
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
   * <p>Requires the {@code administrator} user tag.
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
   * <p>Requires the {@code administrator} user tag.
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
   * <p>Requires the {@code administrator} user tag.
   *
   * @param vhost the virtual host
   * @since 3.7.0
   */
  public void clearMaxQueuesLimit(String vhost) {
    final URI uri = uri().withEncodedPath("./vhost-limits").withPath(vhost).withEncodedPath("max-queues").get();
    this.deleteIgnoring404(uri);
  }

  /**
   * Returns limits for all users that have limits set.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @return list of user limits
   * @since 5.5.0
   */
  public List<UserLimits> getUserLimits() {
    final URI uri = uriWithPath("./user-limits/");
    return asListOrNull(getForObjectReturningNullOn404(uri, UserLimits[].class));
  }

  /**
   * Returns the limits (max connections and channels) for a given user.
   *
   * <p>Requires the {@code administrator} user tag. Does not modify state.
   *
   * @param username the username
   * @return the limits for this user
   * @since 5.5.0
   */
  public UserLimits getUserLimits(String username) {
    final URI uri = uri().withEncodedPath("./user-limits").withPath(username).get();
    UserLimits limits = this.httpLayer.get(uri, UserLimits.class);
    if (limits != null && limits.getUser() == null) {
      limits = new UserLimits(username, -1, -1);
    }
    return limits;
  }

  /**
   * Sets the max number (limit) of connections for a user.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param username the username
   * @param limit the max number of connections allowed
   * @since 5.5.0
   */
  public void limitUserMaxConnections(String username, int limit) {
    final URI uri = uri().withEncodedPath("./user-limits").withPath(username).withEncodedPath("max-connections").get();
    this.httpLayer.put(uri, Collections.singletonMap("value", limit));
  }

  /**
   * Sets the max number (limit) of channels for a user.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param username the username
   * @param limit the max number of channels allowed
   * @since 5.5.0
   */
  public void limitUserMaxChannels(String username, int limit) {
    final URI uri = uri().withEncodedPath("./user-limits").withPath(username).withEncodedPath("max-channels").get();
    this.httpLayer.put(uri, Collections.singletonMap("value", limit));
  }

  /**
   * Clears the connection limit for a user.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param username the username
   * @since 5.5.0
   */
  public void clearUserMaxConnectionsLimit(String username) {
    final URI uri = uri().withEncodedPath("./user-limits").withPath(username).withEncodedPath("max-connections").get();
    this.deleteIgnoring404(uri);
  }

  /**
   * Clears the channel limit for a user.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param username the username
   * @since 5.5.0
   */
  public void clearUserMaxChannelsLimit(String username) {
    final URI uri = uri().withEncodedPath("./user-limits").withPath(username).withEncodedPath("max-channels").get();
    this.deleteIgnoring404(uri);
  }

  /**
   * Returns the MQTT port to virtual host mapping.
   *
   * <p>Requires the {@code administrator} user tag. Does not modify state.
   *
   * @return the MQTT port to virtual host mapping, or null if not set
   */
  public MqttVhostPortInfo getMqttPortToVhostMapping(){
    return getGlobalParameters("mqtt_port_to_vhost_mapping", new ParameterizedTypeReference<>() {});
  }

  /**
   * Deletes the MQTT port to virtual host mapping.
   *
   * <p>Requires the {@code administrator} user tag.
   */
  public void deleteMqttPortToVhostMapping() {
    this.deleteIgnoring404(uri().withEncodedPath("./global-parameters/mqtt_port_to_vhost_mapping").get());
  }

  /**
   * Sets the MQTT port to virtual host mapping.
   *
   * <p>Requires the {@code administrator} user tag.
   *
   * @param portMappings a map of TCP port numbers to virtual host names
   * @throws IllegalArgumentException if any virtual host name is blank
   */
  public void setMqttPortToVhostMapping(Map<Integer, String> portMappings) {
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
