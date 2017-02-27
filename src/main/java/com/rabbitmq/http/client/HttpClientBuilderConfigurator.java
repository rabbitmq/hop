package com.rabbitmq.http.client;

import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Implementations of this interface can perform post-configuration
 * of {@link HttpClientBuilder} used by {@link Client} internally.
 * Note that {@link Client} will configure all essential settings
 * before invoking the configurator.
 *
 * @see Client#setHttpClientBuilderConfigurator(HttpClientBuilderConfigurator)
 * @see NoOpHttpClientBuilderConfigurator
 */
public interface HttpClientBuilderConfigurator {
  HttpClientBuilder configure(HttpClientBuilder builder);
}
