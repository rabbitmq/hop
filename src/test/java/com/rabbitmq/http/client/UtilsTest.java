/*
 * Copyright 2025 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.rabbitmq.http.client.domain.QueryParameters;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UtilsTest {

  URI rootURI = URI.create("http://localhost:80/api/");
  Utils.URIBuilder builder;

  @BeforeEach
  void setUp() {
    builder = new Utils.URIBuilder(rootURI);
  }

  @Test
  void uriWithOnePath() {
    assertEquals(rootURI.resolve("one"), builder.withEncodedPath("./one").get());
    assertEquals(rootURI.resolve("one"), builder.withEncodedPath("one").get());
  }

  @Test
  void uriWithTwoPathComponents() {
    assertEquals(
        rootURI.resolve("one/two%2F"), builder.withEncodedPath("one").withPath("two/").get());
    assertEquals(
        rootURI.resolve("connections/some-name/channels"),
        builder
            .withEncodedPath("./connections")
            .withPath("some-name")
            .withEncodedPath("channels")
            .get());
    assertEquals(
        rootURI.resolve("exchanges/one/two"),
        builder.withEncodedPath("./exchanges").withPath("one").withPath("two").get());
  }

  @Test
  void queryUriWithOnePath() {
    assertEquals(
        rootURI.resolve("one"),
        builder.withEncodedPath("one").withQueryParameters(emptyQueryParameters()).get());
  }

  @Test
  void queryUriWithQueryParameterAndOnePath() {
    QueryParameters expectedQueryParams =
        new QueryParameters().name("some-name").columns().add("name").query();
    URI u = builder.withEncodedPath("one").withQueryParameters(expectedQueryParams).get();

    Map<String, String> actualQueryParams =
        URLEncodedUtils.parse(u, StandardCharsets.UTF_8).stream()
            .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
    for (Map.Entry<String, String> q : expectedQueryParams.parameters().entrySet()) {
      assertEquals(actualQueryParams.get(q.getKey()), q.getValue());
    }
  }

  @Test
  void queryUriWithMapOfParameterAndOnePath() {
    QueryParameters expectedQueryParams =
        new QueryParameters().name("some-name").columns().add("name").query();

    URI u =
        builder.withEncodedPath("one").withQueryParameters(expectedQueryParams.parameters()).get();

    Map<String, String> actualQueryParams =
        URLEncodedUtils.parse(u, StandardCharsets.UTF_8).stream()
            .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
    for (Map.Entry<String, String> q : expectedQueryParams.parameters().entrySet()) {
      assertEquals(actualQueryParams.get(q.getKey()), q.getValue());
    }
  }

  @Test
  void extractUsernamePasswordNoDecodingNeeded() {
    // when: "username and password are extracted from http://mylogin:mypassword@localhost:15672"
    String[] usernamePassword =
        Utils.extractUsernamePassword("http://mylogin:mypassword@localhost:15672");

    // then: "username and password are extracted properly"
    assertThat(usernamePassword).hasSize(2).containsExactly("mylogin", "mypassword");
  }

  @Test
  void extractUsernamePasswordNoUserInfoInUrl() {
    // when: "there is no user info in the URL"
    // then: "an exception is thrown"
    assertThatThrownBy(
            () -> {
              Utils.extractUsernamePassword("http://localhost:15672");
            })
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void extractUsernamePasswordDecodingNeeded() {
    // when: "username and password are extracted from
    // https://test+user:test%40password@myrabbithost/api/"
    String[] usernamePassword =
        Utils.extractUsernamePassword("https://test+user:test%40password@myrabbithost/api/");

    // then: "username and password are extracted and decoded properly"
    assertThat(usernamePassword).hasSize(2).containsExactly("test user", "test@password");
  }

  @Test
  void urlWithoutCredentialsNotEncoded() {
    // when: "credentials do not need encoding in the URL"
    String urlWithoutCredentials =
        Utils.urlWithoutCredentials("http://mylogin:mypassword@localhost:15672");

    // then: "credentials are properly removed from the URL"
    assertThat(urlWithoutCredentials).isEqualTo("http://localhost:15672");
  }

  @Test
  void urlWithoutCredentialsNeedToBeEncoded() {
    // when: "credentials need encoding in the URL"
    String urlWithoutCredentials =
        Utils.urlWithoutCredentials("https://test+user:test%40password@myrabbithost/api/");

    // then: "credentials are properly removed from the URL"
    assertThat(urlWithoutCredentials).isEqualTo("https://myrabbithost/api/");
  }

  private QueryParameters emptyQueryParameters() {
    return new QueryParameters();
  }
}
