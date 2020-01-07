package com.rabbitmq.http.client;

import org.springframework.http.client.ClientHttpRequestFactory;

import java.net.URI;

public interface ClientConfigurer {
    ClientHttpRequestFactory getRequestFactory();
    URI getRootUri();
}
