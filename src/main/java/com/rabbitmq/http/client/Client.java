/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rabbitmq.http.client.domain.*;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;

public class Client {
  private final RestTemplate rt;
  private final URI rootUri;

  //
  // API
  //

  /**
   * Construct an instance with the provided url and credentials.
   * @param url the url e.g. "http://localhost:15672/api/".
   * @param username the user name.
   * @param password the password
   * @throws MalformedURLException for a badly formed URL.
   * @throws URISyntaxException for a badly formed URL.
   */
  public Client(String url, String username, String password) throws MalformedURLException, URISyntaxException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    this(new URL(url), username, password, false);
  }

  /**
   * Construct an instance with the provided url and credentials.
   * @param url the url e.g. "http://localhost:15672/api/".
   * @param username the user name.
   * @param password the password
   * @throws MalformedURLException for a badly formed URL.
   * @throws URISyntaxException for a badly formed URL.
   */
  public Client(URL url, String username, String password, boolean shouldTrustSelfSignedCertificates) throws MalformedURLException, URISyntaxException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    this.rootUri = url.toURI();

    this.rt = new RestTemplate(getRequestFactory(url, username, password, shouldTrustSelfSignedCertificates));
    this.rt.setMessageConverters(getMessageConverters());
  }

  /**
   * Construct an instance with the provided url and credentials.
   * @param url the url e.g. "http://guest:guest@localhost:15672/api/".
   * @throws MalformedURLException for a badly formed URL.
   * @throws URISyntaxException for a badly formed URL.
   */
  public Client(String url) throws MalformedURLException, URISyntaxException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    this(url, null, null);
  }

  /**
   * Construct an instance with the provided url and credentials.
   * @param url the url e.g. "http://guest:guest@localhost:15672/api/".
   * @throws MalformedURLException for a badly formed URL.
   * @throws URISyntaxException for a badly formed URL.
   */
  public Client(URL url) throws MalformedURLException, URISyntaxException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    this(url, null, null, false);
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

  public void createVhost(String name) throws JsonProcessingException {
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
    if(password == null || password.length == 0) {
      throw new IllegalArgumentException("password cannot be null or empty");
    }
    Map<String, Object> body = new HashMap<String, Object>();
    body.put("password", new String(password));
    body.put("tags", joinStrings(",", tags));

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
    body.put("tags", joinStrings(",", tags));

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

  public void bindQueue(String vhost, String queue, String exchange, String routingKey) {
    bindQueue(vhost, queue, exchange, routingKey, new HashMap<String, Object>());
  }

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
      body.put("args", args);
    }
    body.put("routing_key", routingKey);

    final URI uri = uriWithPath("./bindings/" + encodePathSegment(vhost) +
      "/e/" + encodePathSegment(exchange) + "/q/" + encodePathSegment(queue));
    this.rt.postForLocation(uri, body);
  }

  public void bindExchange(String vhost, String destination, String source, String routingKey) {
    bindExchange(vhost, destination, source, routingKey, new HashMap<String, Object>());
  }

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
      body.put("args", args);
    }
    body.put("routing_key", routingKey);

    final URI uri = uriWithPath("./bindings/" + encodePathSegment(vhost) +
      "/e/" + encodePathSegment(source) + "/e/" + encodePathSegment(destination));
    this.rt.postForLocation(uri, body);
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

  @SuppressWarnings("deprecation")
  private String encodePathSegment(final String vhost) {
    return URLEncoder.encode(vhost);
  }

  private List<HttpMessageConverter<?>> getMessageConverters() {
    List<HttpMessageConverter<?>> xs = new ArrayList<HttpMessageConverter<?>>();
    final Jackson2ObjectMapperBuilder bldr = Jackson2ObjectMapperBuilder
        .json()
        .serializationInclusion(JsonInclude.Include.NON_NULL);
    xs.add(new MappingJackson2HttpMessageConverter(bldr.build()));
    return xs;
  }

  private HttpComponentsClientHttpRequestFactory getRequestFactory(final URL url, final String username, final String password, boolean shouldTrustSelfSignedCertificates) throws MalformedURLException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    String theUser = username;
    String thePassword = password;
    String userInfo = url.getUserInfo();
    if (userInfo != null && theUser == null) {
      String[] userParts = userInfo.split(":");
      if (userParts.length > 0) {
        theUser = userParts[0];
      }
      if (userParts.length > 1) {
        thePassword = userParts[1];
      }
    }
    final HttpClientBuilder bldr = HttpClientBuilder.create().
        setDefaultCredentialsProvider(getCredentialsProvider(url, theUser, thePassword));
    bldr.setDefaultHeaders(Arrays.asList(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json")));
    if (shouldTrustSelfSignedCertificates) {
      SSLContextBuilder builder = new SSLContextBuilder();
      builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
      SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
              builder.build());
      bldr.setSSLSocketFactory(sslsf);
    }

    HttpClient httpClient = bldr.build();

    // RabbitMQ HTTP API currently does not support challenge/response for PUT methods.
    AuthCache authCache = new BasicAuthCache();
    BasicScheme basicScheme = new BasicScheme();
    authCache.put(new HttpHost(rootUri.getHost(), rootUri.getPort(), rootUri.getScheme()), basicScheme);
    final HttpClientContext ctx = HttpClientContext.create();
    ctx.setAuthCache(authCache);
    return new HttpComponentsClientHttpRequestFactory(httpClient) {
      @Override
      protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
        return ctx;
      }
    };
  }

  private CredentialsProvider getCredentialsProvider(final URL url, final String username, final String password) {
    CredentialsProvider cp = new BasicCredentialsProvider();
    cp.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
        new UsernamePasswordCredentials(username, password));

    return cp;
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

  private void deleteIgnoring404(URI uri) {
    try {
      this.rt.delete(uri);
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

  private String joinStrings(String delimiter, List<String> tags) {
    StringBuilder sb = new StringBuilder();
    boolean appendedFirst = false;
    for (String tag : tags) {
      if(!appendedFirst) {
        sb.append(tag);
        appendedFirst = true;
      } else {
        sb.append(delimiter);
        if(tag != null) {
          sb.append(tag);
        }
      }
    }
    return sb.toString();
  }
}
