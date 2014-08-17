package com.novemberain.hop.client;

import com.novemberain.hop.client.domain.ConnectionInfo;
import com.novemberain.hop.client.domain.CurrentUserDetails;
import com.novemberain.hop.client.domain.NodeInfo;
import com.novemberain.hop.client.domain.OverviewResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
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
    return this.rt.getForObject(uri, AlivenessTestResponse.class).isSuccessful();
  }

  public CurrentUserDetails whoAmI() {
    final URI uri = uriWithPath("./whoami/");
    return this.rt.getForObject(uri, CurrentUserDetails.class);
  }

  public List<NodeInfo> getNodes() {
    final URI uri = uriWithPath("./nodes/");
    return Arrays.asList(this.rt.getForObject(uri, NodeInfo[].class));
  }

  public NodeInfo getNode(String name) {
    final URI uri = uriWithPath("./nodes/" + encodePathSegment(name));
    return this.rt.getForObject(uri, NodeInfo.class);
  }

  public List<ConnectionInfo> getConnections() {
    final URI uri = uriWithPath("./connections/");
    return Arrays.asList(this.rt.getForObject(uri, ConnectionInfo[].class));
  }

  public ConnectionInfo getConnection(String name) {
    final URI uri = uriWithPath("./connections/" + encodePathSegment(name));
    return this.rt.getForObject(uri, ConnectionInfo.class);
  }

  public void closeConnection(String name) {
    final URI uri = uriWithPath("./connections/" + encodePathSegment(name));
    this.rt.delete(uri);
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
  private URI uriWithPath(String path) {
    return this.rootUri.resolve(path);
  }

  @SuppressWarnings("deprecation")
  private String encodePathSegment(String vhost) {
    return URLEncoder.encode(vhost);
  }

  private List<HttpMessageConverter<?>> getMessageConverters() {
    List<HttpMessageConverter<?>> xs = new ArrayList<>();
    xs.add(new MappingJackson2HttpMessageConverter());
    return xs;
  }

  private ClientHttpRequestFactory getRequestFactory(URL url, String username, String password) throws MalformedURLException {
    HttpClient httpClient = HttpClientBuilder.create().
        setDefaultCredentialsProvider(getCredentialsProvider(url, username, password)).
        build();
    return new HttpComponentsClientHttpRequestFactory(httpClient);
  }

  private CredentialsProvider getCredentialsProvider(URL url, String username, String password) {
    CredentialsProvider cp = new BasicCredentialsProvider();
    cp.setCredentials(new AuthScope(url.getHost(), url.getPort()),
        new UsernamePasswordCredentials(username, password));

    return cp;
  }
}
