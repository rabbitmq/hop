package com.novemberain.hop.client;

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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Client {
  private final URL url;
  private final String username;
  private final String password;
  private final RestTemplate rt;

  public Client(String url, String username, String password) throws MalformedURLException {
    this(new URL(url), username, password);
  }

  public Client(URL url, String username, String password) throws MalformedURLException {
    this.url = url;
    this.username = username;
    this.password = password;

    this.rt = new RestTemplate(getMessageConverters());
    this.rt.setRequestFactory(getRequestFactory(url, username, password));
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

  public OverviewResponse getOverview() {
    return rt.getForObject(url + "/overview", OverviewResponse.class);
  }

  //
  // Implementation
  //

  private List<HttpMessageConverter<?>> getMessageConverters() {
    List<HttpMessageConverter<?>> xs = new ArrayList<>();
    xs.add(new MappingJackson2HttpMessageConverter());
    return xs;
  }
}
