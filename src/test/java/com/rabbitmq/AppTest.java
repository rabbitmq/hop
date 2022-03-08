package com.rabbitmq;

import static com.rabbitmq.http.client.JdkHttpClientHttpLayer.authorization;

import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.ClientParameters;
import com.rabbitmq.http.client.HttpLayer.HttpLayerFactory;
import com.rabbitmq.http.client.JdkHttpClientHttpLayer;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class AppTest {

  @Test
  void createUseClient() throws Exception {
    HttpLayerFactory httpLayerFactory =
        JdkHttpClientHttpLayer.configure()
            .clientBuilderConsumer(
                clientBuilder -> clientBuilder.connectTimeout(Duration.ofSeconds(10)))
            .requestBuilderConsumer(
                requestBuilder ->
                    requestBuilder
                        .timeout(Duration.ofSeconds(10))
                        .setHeader("Authorization", authorization("guest", "guest")))
            .create();

    Client c =
        new Client(
            new ClientParameters()
                .url("http://127.0.0.1:15672/api/")
                .username("guest")
                .password("guest")
                .httpLayerFactory(httpLayerFactory));

    c.getOverview();
  }

}
