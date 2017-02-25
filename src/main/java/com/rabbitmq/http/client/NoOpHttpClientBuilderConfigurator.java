package com.rabbitmq.http.client;

import org.apache.http.impl.client.HttpClientBuilder;

/**
 * This implementation of {@link HttpClientBuilderConfigurator} performs
 * no configuration (does nothing).
 */
public class NoOpHttpClientBuilderConfigurator implements HttpClientBuilderConfigurator {
  @Override
  public HttpClientBuilder configure(HttpClientBuilder builder) {
    return builder;
  }
}
