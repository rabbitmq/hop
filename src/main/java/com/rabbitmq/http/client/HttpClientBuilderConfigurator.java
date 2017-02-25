package com.rabbitmq.http.client;

import org.apache.http.impl.client.HttpClientBuilder;

public interface HttpClientBuilderConfigurator {
  public HttpClientBuilder configure(HttpClientBuilder builder);
}
