package com.rabbitmq.http.client;

import com.rabbitmq.http.client.domain.QueryParameters;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals(rootURI.resolve("one/two%2F"), builder.withEncodedPath("one").withPath("two/").get());
        assertEquals(rootURI.resolve("connections/some-name/channels"),
                builder.withEncodedPath("./connections").withPath("some-name").withEncodedPath("channels").get());
        assertEquals(rootURI.resolve("exchanges/one/two"), builder.withEncodedPath("./exchanges").withPath("one").withPath("two").get());
    }

    @Test
    void queryUriWithOnePath() {
        assertEquals(rootURI.resolve("one"), builder.withEncodedPath("one").withQueryParameters(emptyQueryParameters()).get());
    }

    @Test
    void queryUriWithQueryParameterAndOnePath() {
        QueryParameters expectedQueryParams = new QueryParameters().name("some-name").columns().add("name").query();
        URI u = builder.withEncodedPath("one").withQueryParameters(expectedQueryParams).get();

        Map<String, String> actualQueryParams = URLEncodedUtils.parse(u, StandardCharsets.UTF_8).stream()
                .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
        for (Map.Entry<String,String> q : expectedQueryParams.parameters().entrySet()) {
            assertEquals(actualQueryParams.get(q.getKey()), q.getValue());
        }
    }

    @Test
    void queryUriWithMapOfParameterAndOnePath() {
        QueryParameters expectedQueryParams = new QueryParameters().name("some-name").columns().add("name").query();

        URI u = builder.withEncodedPath("one").withQueryParameters(expectedQueryParams.parameters()).get();

        Map<String, String> actualQueryParams = URLEncodedUtils.parse(u, StandardCharsets.UTF_8).stream()
                .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
        for (Map.Entry<String,String> q : expectedQueryParams.parameters().entrySet()) {
            assertEquals(actualQueryParams.get(q.getKey()), q.getValue());
        }
    }

    private QueryParameters emptyQueryParameters() {
        return new QueryParameters();
    }

}