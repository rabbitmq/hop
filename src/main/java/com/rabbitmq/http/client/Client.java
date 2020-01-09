/*
 * Copyright 2015-2020 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.rabbitmq.http.client.domain.AlivenessTestResult;
import com.rabbitmq.http.client.domain.BindingInfo;
import com.rabbitmq.http.client.domain.ChannelInfo;
import com.rabbitmq.http.client.domain.ClusterId;
import com.rabbitmq.http.client.domain.ConnectionInfo;
import com.rabbitmq.http.client.domain.CurrentUserDetails;
import com.rabbitmq.http.client.domain.Definitions;
import com.rabbitmq.http.client.domain.ExchangeInfo;
import com.rabbitmq.http.client.domain.InboundMessage;
import com.rabbitmq.http.client.domain.NodeInfo;
import com.rabbitmq.http.client.domain.OutboundMessage;
import com.rabbitmq.http.client.domain.OverviewResponse;
import com.rabbitmq.http.client.domain.PolicyInfo;
import com.rabbitmq.http.client.domain.QueueInfo;
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
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import javax.net.ssl.SSLContext;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Client {
  private static final HttpClientBuilderConfigurator NO_OP_HTTP_CLIENT_BUILDER_CONFIGURATOR =
      builder -> builder;

  private final RestTemplate rt;
  private final URI rootUri;

  //
  // API
  //

  /**
   * Construct an instance with the provided url and credentials.
   * @param url the url e.g. "http://localhost:15672/api/".
   * @param username the username.
   * @param password the password
   * @throws MalformedURLException for a badly formed URL.
   * @throws URISyntaxException for a badly formed URL.
   */
  public Client(String url, String username, String password) throws MalformedURLException, URISyntaxException {
    this(new URL(url), username, password);
  }

  /**
   * Construct an instance with the provided url and credentials.
   * @param url the url e.g. "http://localhost:15672/api/".
   * @param username the username.
   * @param password the password
   * @param configurator {@link HttpClientBuilderConfigurator} to use
   * @throws MalformedURLException for a badly formed URL.
   * @throws URISyntaxException for a badly formed URL.
   */
  public Client(String url, String username, String password, HttpClientBuilderConfigurator configurator)
      throws MalformedURLException, URISyntaxException {
    this(new URL(url), username, password, configurator);
  }

  /**
   * Construct an instance with the provided url and credentials.
   * @param url the url e.g. "http://localhost:15672/api/".
   * @param username the username.
   * @param password the password
   * @throws MalformedURLException for a badly formed URL.
   * @throws URISyntaxException for a badly formed URL.
   */
  public Client(URL url, String username, String password) throws MalformedURLException, URISyntaxException {
    this(url, username, password, null, null);
  }

  /**
   * Construct an instance with the provided url and credentials.
   * @param url the url e.g. "http://localhost:15672/api/".
   * @param username the username.
   * @param password the password
   * @param configurator {@link HttpClientBuilderConfigurator} to use
   * @throws MalformedURLException for a badly formed URL.
   * @throws URISyntaxException for a badly formed URL.
   */
  public Client(URL url, String username, String password, HttpClientBuilderConfigurator configurator)
      throws MalformedURLException, URISyntaxException {
    this(url, username, password, null, null, configurator);
  }

  /**
   * Construct an instance with the provided url and credentials.
   * @param url the url e.g. "http://localhost:15672/api/".
   * @param username the username.
   * @param password the password
   * @param sslConnectionSocketFactory ssl connection factory for http client
   * @param sslContext ssl context for http client
   * @throws MalformedURLException for a badly formed URL.
   * @throws URISyntaxException for a badly formed URL.
   */
  private Client(URL url, String username, String password, SSLConnectionSocketFactory sslConnectionSocketFactory, SSLContext sslContext)
      throws MalformedURLException, URISyntaxException {
    this(url, username, password, sslConnectionSocketFactory, sslContext, NO_OP_HTTP_CLIENT_BUILDER_CONFIGURATOR);
  }

  /**
   * Construct an instance with the provided url and credentials.
   * @param url the url e.g. "http://localhost:15672/api/".
   * @param username the username.
   * @param password the password
   * @param sslContext ssl context for http client
   * @throws MalformedURLException for a badly formed URL.
   * @throws URISyntaxException for a badly formed URL.
   */
  public Client(URL url, String username, String password, SSLContext sslContext) throws MalformedURLException, URISyntaxException {
    this(url, username, password, null, sslContext);
  }

  /**
   * Construct an instance with the provided url and credentials.
   * @param url the url e.g. "http://localhost:15672/api/".
   * @param username the username.
   * @param password the password
   * @param sslConnectionSocketFactory ssl connection factory for http client
   * @throws MalformedURLException for a badly formed URL.
   * @throws URISyntaxException for a badly formed URL.
   */
  private Client(URL url, String username, String password, SSLConnectionSocketFactory sslConnectionSocketFactory) throws MalformedURLException, URISyntaxException {
    this(url, username, password, sslConnectionSocketFactory, null);
  }

  /**
   * Construct an instance with the provided url and credentials.
   * @param url the url e.g. "https://guest:guest@localhost:15672/api/".
   * @throws MalformedURLException for a badly formed URL.
   * @throws URISyntaxException for a badly formed URL.
   */
  public Client(String url) throws MalformedURLException, URISyntaxException {
    this(url, NO_OP_HTTP_CLIENT_BUILDER_CONFIGURATOR);
  }

  /**
   * Construct an instance with the provided url and credentials.
   * @param url the url e.g. "https://guest:guest@localhost:15672/api/".
   * @param configurator {@link HttpClientBuilderConfigurator} to use
   * @throws MalformedURLException for a badly formed URL.
   * @throws URISyntaxException for a badly formed URL.
   */
  public Client(String url, HttpClientBuilderConfigurator configurator) throws MalformedURLException, URISyntaxException {
    this(Utils.urlWithoutCredentials(url),
            Utils.extractUsernamePassword(url)[0],
            Utils.extractUsernamePassword(url)[1],
            configurator
    );
  }

  /**
   * Construct an instance with the provided url and credentials.
   * @param url the url e.g. "https://guest:guest@localhost:15672/api/".
   * @throws MalformedURLException for a badly formed URL.
   * @throws URISyntaxException for a badly formed URL.
   */
  public Client(URL url) throws MalformedURLException, URISyntaxException {
    this(url, null, null);
  }

  private Client(URL url, String username, String password, SSLConnectionSocketFactory sslConnectionSocketFactory, SSLContext sslContext, HttpClientBuilderConfigurator configurator) throws MalformedURLException, URISyntaxException {
    this(new ClientParameters().url(url).username(username).password(password)
            .restTemplateConfigurator(new HttpComponentsRestTemplateConfigurator(sslConnectionSocketFactory, sslContext, configurator)));
  }

  /**
   *  Construct an instance with the provided {@link ClientParameters}.
   * @param parameters the client parameters to use
   * @throws URISyntaxException
   * @throws MalformedURLException
   */
  public Client(ClientParameters parameters) throws URISyntaxException, MalformedURLException {
    parameters.validate();
    URL url = parameters.getUrl();
    if (url.toString().endsWith("/")) {
      this.rootUri = url.toURI();
    } else {
      this.rootUri = new URL(url.toString() + "/").toURI();
    }
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setMessageConverters(getMessageConverters());
    RestTemplateConfigurator restTemplateConfigurator = parameters.getRestTemplateConfigurator() == null ?
            new HttpComponentsRestTemplateConfigurator() :
            parameters.getRestTemplateConfigurator();
    this.rt = restTemplateConfigurator.configure(new ClientCreationContext(restTemplate, parameters, this.rootUri));
  }

  /**
   * @return cluster state overview
   */
  public OverviewResponse getOverview() {
    return this.rt.getForObject(uriWithPath("./overview"), OverviewResponse.class);
  }

  /**
   * Performs a basic node aliveness check: declares a queue, publishes a message
   * that routes to it, consumes it, cleans up.
   *
   * @param vhost vhost to use to perform aliveness check in
   * @return true if the check succeeded
   */
  public boolean alivenessTest(String vhost) {
    final URI uri = uriWithPath("./aliveness-test/" + encodePathSegment(vhost));
    return this.rt.getForObject(uri, AlivenessTestResult.class).isSuccessful();
  }

  /**
   * @return information about the user used by this client instance
   */
  public CurrentUserDetails whoAmI() {
    final URI uri = uriWithPath("./whoami/");
    return this.rt.getForObject(uri, CurrentUserDetails.class);
  }

  /**
   * Retrieves state and metrics information for all nodes in the cluster.
   *
   * @return list of nodes in the cluster
   */
  public List<NodeInfo> getNodes() {
    final URI uri = uriWithPath("./nodes/");
    return Arrays.asList(this.rt.getForObject(uri, NodeInfo[].class));
  }

  /**
   * Retrieves state and metrics information for individual node.
   *
   * @param name node name
   * @return node information
   */
  public NodeInfo getNode(String name) {
    final URI uri = uriWithPath("./nodes/" + encodePathSegment(name));
    return this.rt.getForObject(uri, NodeInfo.class);
  }

  /**
   * Retrieves state and metrics information for all client connections across the cluster.
   *
   * @return list of connections across the cluster
   */
  public List<ConnectionInfo> getConnections() {
    final URI uri = uriWithPath("./connections/");
    return Arrays.asList(this.rt.getForObject(uri, ConnectionInfo[].class));
  }

  /**
   * Retrieves state and metrics information for individual client connection.
   *
   * @param name connection name
   * @return connection information
   */
  public ConnectionInfo getConnection(String name) {
    final URI uri = uriWithPath("./connections/" + encodePathSegment(name));
    return this.rt.getForObject(uri, ConnectionInfo.class);
  }

  /**
   * Forcefully closes individual connection.
   * The client will receive a <i>connection.close</i> method frame.
   *
   * @param name connection name
   */
  public void closeConnection(String name) {
    final URI uri = uriWithPath("./connections/" + encodePathSegment(name));
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
    final URI uri = uriWithPath("./connections/" + encodePathSegment(name));

    MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
    headers.put("X-Reason", Collections.singletonList(reason));

    deleteIgnoring404(uri, headers);
  }

  /**
   * Retrieves state and metrics information for all channels across the cluster.
   *
   * @return list of channels across the cluster
   */
  public List<ChannelInfo> getChannels() {
    final URI uri = uriWithPath("./channels/");
    return Arrays.asList(this.rt.getForObject(uri, ChannelInfo[].class));
  }

  /**
   * Retrieves state and metrics information for all channels on individual connection.
   * @param connectionName the connection name to retrieve channels
   * @return list of channels on the connection
   */
  public List<ChannelInfo> getChannels(String connectionName) {
    final URI uri = uriWithPath("./connections/" + encodePathSegment(connectionName) + "/channels/");
    return Arrays.asList(this.rt.getForObject(uri, ChannelInfo[].class));
  }

  /**
   * Retrieves state and metrics information for individual channel.
   *
   * @param name channel name
   * @return channel information
   */
  public ChannelInfo getChannel(String name) {
    final URI uri = uriWithPath("./channels/" + encodePathSegment(name));
    return this.rt.getForObject(uri, ChannelInfo.class);
  }

  public List<VhostInfo> getVhosts() {
    final URI uri = uriWithPath("./vhosts/");
    return Arrays.asList(this.rt.getForObject(uri, VhostInfo[].class));
  }

  public VhostInfo getVhost(String name) {
    final URI uri = uriWithPath("./vhosts/" + encodePathSegment(name));
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

    final URI uri = uriWithPath("./vhosts/" + encodePathSegment(name));
    this.rt.put(uri, body);
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
    final URI uri = uriWithPath("./vhosts/" + encodePathSegment(name));
    this.rt.put(uri, null);
  }

  public void deleteVhost(String name) {
    final URI uri = uriWithPath("./vhosts/" + encodePathSegment(name));
    deleteIgnoring404(uri);
  }

  public List<UserPermissions> getPermissionsIn(String vhost) {
    final URI uri = uriWithPath("./vhosts/" + encodePathSegment(vhost) + "/permissions");
    UserPermissions[] result = this.getForObjectReturningNullOn404(uri, UserPermissions[].class);
    return asListOrNull(result);
  }

  public List<UserPermissions> getPermissionsOf(String username) {
    final URI uri = uriWithPath("./users/" + encodePathSegment(username) + "/permissions");
    UserPermissions[] result = this.getForObjectReturningNullOn404(uri, UserPermissions[].class);
    return asListOrNull(result);
  }

  public List<UserPermissions> getPermissions() {
    final URI uri = uriWithPath("./permissions");
    UserPermissions[] result = this.getForObjectReturningNullOn404(uri, UserPermissions[].class);
    return asListOrNull(result);
  }

  public UserPermissions getPermissions(String vhost, String username) {
    final URI uri = uriWithPath("./permissions/" + encodePathSegment(vhost) + "/" + encodePathSegment(username));
    return this.getForObjectReturningNullOn404(uri, UserPermissions.class);
  }

  public List<TopicPermissions> getTopicPermissionsIn(String vhost) {
    final URI uri = uriWithPath("./vhosts/" + encodePathSegment(vhost) + "/topic-permissions");
    TopicPermissions[] result = this.getForObjectReturningNullOn404(uri, TopicPermissions[].class);
    return asListOrNull(result);
  }

  public List<TopicPermissions> getTopicPermissionsOf(String username) {
    final URI uri = uriWithPath("./users/" + encodePathSegment(username) + "/topic-permissions");
    TopicPermissions[] result = this.getForObjectReturningNullOn404(uri, TopicPermissions[].class);
    return asListOrNull(result);
  }

  public List<TopicPermissions> getTopicPermissions() {
    final URI uri = uriWithPath("./topic-permissions");
    TopicPermissions[] result = this.getForObjectReturningNullOn404(uri, TopicPermissions[].class);
    return asListOrNull(result);
  }

  public List<TopicPermissions> getTopicPermissions(String vhost, String username) {
    final URI uri = uriWithPath("./topic-permissions/" + encodePathSegment(vhost) + "/" + encodePathSegment(username));
    return asListOrNull(this.getForObjectReturningNullOn404(uri, TopicPermissions[].class));
  }

  public List<ExchangeInfo> getExchanges() {
    final URI uri = uriWithPath("./exchanges/");
    return Arrays.asList(this.rt.getForObject(uri, ExchangeInfo[].class));
  }

  public List<ExchangeInfo> getExchanges(String vhost) {
    final URI uri = uriWithPath("./exchanges/" + encodePathSegment(vhost));
    final ExchangeInfo[] result = this.getForObjectReturningNullOn404(uri, ExchangeInfo[].class);
    return asListOrNull(result);
  }

  public ExchangeInfo getExchange(String vhost, String name) {
    final URI uri = uriWithPath("./exchanges/" + encodePathSegment(vhost) + "/" + encodePathSegment(name));
    return this.getForObjectReturningNullOn404(uri, ExchangeInfo.class);
  }

  public void declareExchange(String vhost, String name, ExchangeInfo info) {
    final URI uri = uriWithPath("./exchanges/" + encodePathSegment(vhost) + "/" + encodePathSegment(name));
    this.rt.put(uri, info);
  }

  public void deleteExchange(String vhost, String name) {
    this.deleteIgnoring404(uriWithPath("./exchanges/" + encodePathSegment(vhost) + "/" + encodePathSegment(name)));
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

    final URI uri = uriWithPath("./exchanges/" + encodePathSegment(vhost) + "/" + encodePathSegment(exchange) + "/publish");
    Map<?, ?> response = this.rt.postForObject(uri, body, Map.class);
    Boolean routed = (Boolean) response.get("routed");
    if (routed == null) {
      return false;
    } else {
      return routed.booleanValue();
    }
  }

  public List<QueueInfo> getQueues() {
    final URI uri = uriWithPath("./queues/");
    return Arrays.asList(this.rt.getForObject(uri, QueueInfo[].class));
  }

  public List<QueueInfo> getQueues(String vhost) {
    final URI uri = uriWithPath("./queues/" + encodePathSegment(vhost));
    final QueueInfo[] result = this.getForObjectReturningNullOn404(uri, QueueInfo[].class);
    return asListOrNull(result);
  }

  public QueueInfo getQueue(String vhost, String name) {
    final URI uri = uriWithPath("./queues/" + encodePathSegment(vhost) + "/" + encodePathSegment(name));
    return this.getForObjectReturningNullOn404(uri, QueueInfo.class);
  }

  public void declarePolicy(String vhost, String name, PolicyInfo info) {
    final URI uri = uriWithPath("./policies/" + encodePathSegment(vhost) + "/" + encodePathSegment(name));
    this.rt.put(uri, info);
  }

  public void declareQueue(String vhost, String name, QueueInfo info) {
    final URI uri = uriWithPath("./queues/" + encodePathSegment(vhost) + "/" + encodePathSegment(name));
    this.rt.put(uri, info);
  }

  public void purgeQueue(String vhost, String name) {
    this.deleteIgnoring404(uriWithPath("./queues/" + encodePathSegment(vhost) + "/" + encodePathSegment(name) + "/contents/"));
  }

  public void deleteQueue(String vhost, String name) {
    this.deleteIgnoring404(uriWithPath("./queues/" + encodePathSegment(vhost) + "/" + encodePathSegment(name)));
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

        final URI uri = uriWithPath("./queues/" + encodePathSegment(vhost) + "/" + encodePathSegment(queue) + "/get");
        return Arrays.asList(this.rt.postForObject(uri, body, InboundMessage[].class));
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
    this.deleteIgnoring404(uriWithPath("./policies/" + encodePathSegment(vhost) + "/" + encodePathSegment(name)));
  }

  public List<UserInfo> getUsers() {
    final URI uri = uriWithPath("./users/");
    return Arrays.asList(this.rt.getForObject(uri, UserInfo[].class));
  }

  public UserInfo getUser(String username) {
    final URI uri = uriWithPath("./users/" + encodePathSegment(username));
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

    final URI uri = uriWithPath("./users/" + encodePathSegment(username));
    this.rt.put(uri, body);
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

    final URI uri = uriWithPath("./users/" + encodePathSegment(username));
    this.rt.put(uri, body);
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

    final URI uri = uriWithPath("./users/" + encodePathSegment(username));
    this.rt.put(uri, body);
  }

  public void deleteUser(String username) {
    this.deleteIgnoring404(uriWithPath("./users/" + encodePathSegment(username)));
  }

  public void updatePermissions(String vhost, String username, UserPermissions permissions) {
    final URI uri = uriWithPath("./permissions/" + encodePathSegment(vhost) + "/" + encodePathSegment(username));
    this.rt.put(uri, permissions);
  }

  public void clearPermissions(String vhost, String username) {
    final URI uri = uriWithPath("./permissions/" + encodePathSegment(vhost) + "/" + encodePathSegment(username));
    deleteIgnoring404(uri);
  }

  public void updateTopicPermissions(String vhost, String username, TopicPermissions permissions) {
    final URI uri = uriWithPath("./topic-permissions/" + encodePathSegment(vhost) + "/" + encodePathSegment(username));
    this.rt.put(uri, permissions);
  }

  public void clearTopicPermissions(String vhost, String username) {
    final URI uri = uriWithPath("./topic-permissions/" + encodePathSegment(vhost) + "/" + encodePathSegment(username));
    deleteIgnoring404(uri);
  }

  public List<PolicyInfo> getPolicies() {
    final URI uri = uriWithPath("./policies/");
    return Arrays.asList(this.rt.getForObject(uri, PolicyInfo[].class));
  }

  public List<PolicyInfo> getPolicies(String vhost) {
    final URI uri = uriWithPath("./policies/" + encodePathSegment(vhost));
    final PolicyInfo[] result = this.getForObjectReturningNullOn404(uri, PolicyInfo[].class);
    return asListOrNull(result);
  }

  public List<BindingInfo> getBindings() {
    final URI uri = uriWithPath("./bindings/");
    return Arrays.asList(this.rt.getForObject(uri, BindingInfo[].class));
  }

  public List<BindingInfo> getBindings(String vhost) {
    final URI uri = uriWithPath("./bindings/" + encodePathSegment(vhost));
    return Arrays.asList(this.rt.getForObject(uri, BindingInfo[].class));
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
    final URI uri = uriWithPath("./exchanges/" + encodePathSegment(vhost) +
        "/" + encodePathSegment(x) + "/bindings/source");
    return Arrays.asList(this.rt.getForObject(uri, BindingInfo[].class));
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
    final URI uri = uriWithPath("./exchanges/" + encodePathSegment(vhost) +
        "/" + encodePathSegment(x) + "/bindings/destination");
    final BindingInfo[] result = this.rt.getForObject(uri, BindingInfo[].class);
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
    final URI uri = uriWithPath("./queues/" + encodePathSegment(vhost) +
        "/" + encodePathSegment(queue) + "/bindings");
    final BindingInfo[] result = this.rt.getForObject(uri, BindingInfo[].class);
    return asListOrNull(result);
  }

  public List<BindingInfo> getQueueBindingsBetween(String vhost, String exchange, String queue) {
    final URI uri = uriWithPath("./bindings/" + encodePathSegment(vhost) +
        "/e/" + encodePathSegment(exchange) + "/q/" + encodePathSegment(queue));
    final BindingInfo[] result = this.rt.getForObject(uri, BindingInfo[].class);
    return asListOrNull(result);
  }

  public List<BindingInfo> getExchangeBindingsBetween(String vhost, String source, String destination) {
    final URI uri = uriWithPath("./bindings/" + encodePathSegment(vhost) +
        "/e/" + encodePathSegment(source) + "/e/" + encodePathSegment(destination));
    final BindingInfo[] result = this.rt.getForObject(uri, BindingInfo[].class);
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

    final URI uri = uriWithPath("./bindings/" + encodePathSegment(vhost) +
      "/e/" + encodePathSegment(exchange) + "/q/" + encodePathSegment(queue));
    this.rt.postForLocation(uri, body);
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
    
    this.deleteIgnoring404(uriWithPath("./bindings/" + encodePathSegment(vhost) + "/e/" + encodePathSegment(exchange) + 
      "/q/" + encodePathSegment(queue) + '/' + encodePathSegment(routingKey)));
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

    final URI uri = uriWithPath("./bindings/" + encodePathSegment(vhost) +
      "/e/" + encodePathSegment(source) + "/e/" + encodePathSegment(destination));
    this.rt.postForLocation(uri, body);
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
	    
    this.deleteIgnoring404(uriWithPath("./bindings/" + encodePathSegment(vhost) + "/e/" + encodePathSegment(source) + 
      "/e/" + encodePathSegment(destination) + '/' + encodePathSegment(routingKey)));
  }

  public ClusterId getClusterName() {
    return this.rt.getForObject(uriWithPath("./cluster-name"), ClusterId.class);
  }

  public void setClusterName(String name) {
    if(name== null || name.isEmpty()) {
      throw new IllegalArgumentException("name cannot be null or blank");
    }
    final URI uri = uriWithPath("./cluster-name");
    Map<String, String> m = new HashMap<String, String>();
    m.put("name", name);
    this.rt.put(uri, m);
  }

  @SuppressWarnings({"unchecked","rawtypes"})
  public List<Map> getExtensions() {
    final URI uri = uriWithPath("./extensions/");
    return Arrays.asList(this.rt.getForObject(uri, Map[].class));
  }

  public Definitions getDefinitions() {
    final URI uri = uriWithPath("./definitions/");
    return this.rt.getForObject(uri, Definitions.class);
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
    final URI uri = uriWithPath("./parameters/shovel/" + encodePathSegment(vhost) + "/" + encodePathSegment(info.getName()));
    this.rt.put(uri, info);
  }

  /**
   * Returns virtual host shovels.
   * 
   * @return Shovels.
   */
  public List<ShovelInfo> getShovels() {
    final URI uri = uriWithPath("./parameters/shovel/");
    return Arrays.asList(this.rt.getForObject(uri, ShovelInfo[].class));
  }

  /**
   * Returns virtual host shovels.
   * 
   * @param vhost Virtual host from where search shovels.
   * @return Shovels.
   */
  public List<ShovelInfo> getShovels(String vhost) {
    final URI uri = uriWithPath("./parameters/shovel/" + encodePathSegment(vhost));
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
    return Arrays.asList(this.rt.getForObject(uri, ShovelStatus[].class));
  }

  /**
   * Returns virtual host shovels.
   * 
   * @param vhost Virtual host from where search shovels.
   * @return Shovels.
   */
  public List<ShovelStatus> getShovelsStatus(String vhost) {
    final URI uri = uriWithPath("./shovels/" + encodePathSegment(vhost));
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
	    this.deleteIgnoring404(uriWithPath("./parameters/shovel/" + encodePathSegment(vhost) + "/" + encodePathSegment(shovelname)));
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
    if (StringUtils.isEmpty(details.getUri())) {
      throw new IllegalArgumentException("Upstream uri must not be null or empty");
    }
    final URI uri = uriWithPath("./parameters/federation-upstream/"
            + encodePathSegment(vhost) + "/" + encodePathSegment(name));
    UpstreamInfo body = new UpstreamInfo();
    body.setVhost(vhost);
    body.setName(name);
    body.setValue(details);
    this.rt.put(uri, body);
  }

  /**
   * Deletes an upstream
   * @param vhost virtual host for which to delete the upstream
   * @param name name of the upstream to delete
   */
  public void deleteUpstream(String vhost, String name) {
    this.deleteIgnoring404(uriWithPath("./parameters/federation-upstream/"
            + encodePathSegment(vhost) + "/" + encodePathSegment(name)));
  }

  /**
   * Returns a list of upstreams for "/" virtual host
   *
   * @return upstream info
   */
  public List<UpstreamInfo> getUpstreams() {
    return getParameters("federation-upstream", new ParameterizedTypeReference<List<UpstreamInfo>>() {
    });
  }

  /**
   * Returns a list of upstreams
   *
   * @param vhost virtual host the upstreams are in.
   * @return upstream info
   */
  public List<UpstreamInfo> getUpstreams(String vhost) {
    return getParameters(vhost, "federation-upstream", new ParameterizedTypeReference<List<UpstreamInfo>>() {
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
      if (StringUtils.isEmpty(item.getUpstream())) {
        throw new IllegalArgumentException("Each federation upstream set item must have a non-null and not " +
                "empty upstream name");
      }
    }
    final URI uri = uriWithPath("./parameters/federation-upstream-set/"
            + encodePathSegment(vhost) + "/" + encodePathSegment(name));
    UpstreamSetInfo body = new UpstreamSetInfo();
    body.setVhost(vhost);
    body.setName(name);
    body.setValue(details);
    this.rt.put(uri, body);
  }

  /**
   * Deletes an upstream set
   * @param vhost virtual host for which to delete the upstream set
   * @param name name of the upstream set to delete
   */
  public void deleteUpstreamSet(String vhost, String name) {
    this.deleteIgnoring404(uriWithPath("./parameters/federation-upstream-set/"
            + encodePathSegment(vhost) + "/" + encodePathSegment(name)));
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
   * Returns a ist of upstream sets
   *
   * @param vhost Virtual host from where to get upstreams.
   * @return upstream set info
   */
  public List<UpstreamSetInfo> getUpstreamSets(String vhost) {
    return getParameters(vhost, "federation-upstream-set", new ParameterizedTypeReference<List<UpstreamSetInfo>>() {
    });
  }

  private <T> List<T> getParameters(String component, final ParameterizedTypeReference<List<T>> responseType) {
    final URI uri = uriWithPath("./parameters/" + component + "/");
    return rt.exchange(uri, HttpMethod.GET, null, responseType).getBody();
  }

  private <T> List<T> getParameters(String vhost, String component, final ParameterizedTypeReference<List<T>> responseType) {
    final URI uri = uriWithPath("./parameters/" + component + "/" + encodePathSegment(vhost));
    return getForObjectReturningNullOn404(uri, responseType);
  }

  //
  // Implementation
  //

  /**
   * Produces a URI used to issue HTTP requests to avoid double-escaping of path segments
   * (e.g. vhost names) from {@link RestTemplate#execute}.
   *
   * @param path The path after /api/
   * @return resolved URI
   */
  private URI uriWithPath(final String path) {
    return this.rootUri.resolve(path);
  }

  private String encodePathSegment(final String pathSegment) {
    return UriUtils.encodePathSegment(pathSegment, "UTF-8");
  }

  private List<HttpMessageConverter<?>> getMessageConverters() {
    List<HttpMessageConverter<?>> xs = new ArrayList<HttpMessageConverter<?>>();
    final Jackson2ObjectMapperBuilder bldr = Jackson2ObjectMapperBuilder
        .json()
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        .featuresToEnable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
    xs.add(new MappingJackson2HttpMessageConverter(bldr.build()));
    return xs;
  }

  private <T> T getForObjectReturningNullOn404(final URI uri, final Class<T> klass) {
    try {
      return this.rt.getForObject(uri, klass);
    } catch (final HttpClientErrorException ce) {
      if(ce.getStatusCode() == HttpStatus.NOT_FOUND) {
        return null;
      } else {
        throw ce;
      }
    }
  }

  private <T> T getForObjectReturningNullOn404(final URI uri, final ParameterizedTypeReference<T> responseType) {
    ResponseEntity<T> response = rt.exchange(uri, HttpMethod.GET, null, responseType);
    if (HttpStatus.NOT_FOUND == response.getStatusCode()) {
      return null;
    } else {
      return response.getBody();
    }
  }

  private void deleteIgnoring404(URI uri) {
    try {
      this.rt.delete(uri);
    } catch (final HttpClientErrorException ce) {
      if(!(ce.getStatusCode() == HttpStatus.NOT_FOUND)) {
        throw ce;
      }
    }
  }

  private void deleteIgnoring404(URI uri, MultiValueMap<String, String> headers) {
    try {
      HttpEntity<Object> entity = new HttpEntity<Object>(null, headers);
      this.rt.exchange(uri, HttpMethod.DELETE, entity, Object.class);
    } catch (final HttpClientErrorException ce) {
      if(!(ce.getStatusCode() == HttpStatus.NOT_FOUND)) {
        throw ce;
      }
    }
  }

  private <T> List<T> asListOrNull(T[] result) {
    if(result == null) {
      return null;
    } else {
      return Arrays.asList(result);
    }
  }

}
