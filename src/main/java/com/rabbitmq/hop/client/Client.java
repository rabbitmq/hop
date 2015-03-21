package com.rabbitmq.hop.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.hop.client.domain.*;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client {
  private final RestTemplate rt;
  private final URI rootUri;

  //
  // API
  //

  public Client(String url, String username, String password) throws MalformedURLException, URISyntaxException {
    this(new URL(url), username, password);
  }

  public Client(URL url, String username, String password) throws MalformedURLException, URISyntaxException {
    this.rootUri = url.toURI();

    this.rt = new RestTemplate(getMessageConverters());
    this.rt.setRequestFactory(getRequestFactory(url, username, password));
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
    this.rt.delete(uri);
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
   *
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
    this.rt.delete(uri);
  }

  public List<ExchangeInfo> getExchanges() {
    final URI uri = uriWithPath("./exchanges/");
    return Arrays.asList(this.rt.getForObject(uri, ExchangeInfo[].class));
  }

  public List<ExchangeInfo> getExchanges(String vhost) {
    final URI uri = uriWithPath("./exchanges/" + encodePathSegment(vhost));
    return Arrays.asList(this.rt.getForObject(uri, ExchangeInfo[].class));
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
  public List<BindingInfo> getBindingsByDestination(String vhost, String exchange) {
    final String x = exchange.equals("") ? "amq.default" : exchange;
    final URI uri = uriWithPath("./exchanges/" + encodePathSegment(vhost) +
        "/" + encodePathSegment(x) + "/bindings/destination");
    return Arrays.asList(this.rt.getForObject(uri, BindingInfo[].class));
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
    List<HttpMessageConverter<?>> xs = new ArrayList<>();
    xs.add(new MappingJackson2HttpMessageConverter());
    return xs;
  }

  private ClientHttpRequestFactory getRequestFactory(final URL url, final String username, final String password) throws MalformedURLException {
    final HttpClientBuilder bldr = HttpClientBuilder.create().
        setDefaultCredentialsProvider(getCredentialsProvider(url, username, password));
    bldr.setDefaultHeaders(Arrays.asList(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json")));
    HttpClient httpClient = bldr.build();
    return new HttpComponentsClientHttpRequestFactory(httpClient);
  }

  private CredentialsProvider getCredentialsProvider(final URL url, final String username, final String password) {
    CredentialsProvider cp = new BasicCredentialsProvider();
    cp.setCredentials(new AuthScope(url.getHost(), url.getPort()),
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
}
