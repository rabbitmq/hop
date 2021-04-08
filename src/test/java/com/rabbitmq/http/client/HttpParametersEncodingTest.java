package com.rabbitmq.http.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.DefaultUriBuilderFactory;

public class HttpParametersEncodingTest {

  String queryParameterValue = "^outbound-ack|msg-.+$";
  String expectedEncodedValue = "%5Eoutbound-ack%7Cmsg-.%2B%24";

  @Test
  void uriEncodeOfSpecialCharactersWithSpringUriBuilder() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
    factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("name", queryParameterValue);

    assertThrows(
        IllegalArgumentException.class,
        () -> factory.uriString("/exchanges").queryParams(map).build());
    //    assertEquals("/exchanges?name=" + expectedEncodedValue, uri.toASCIIString());
  }

  @Test
  void uriEncodeOfSpecialCharactersWithApacheURIBuilder() throws URISyntaxException {
    URI uri =
        new URIBuilder().setPath("/exchanges").addParameter("name", queryParameterValue).build();
    assertEquals("/exchanges?name=" + expectedEncodedValue, uri.toASCIIString());
  }

  @Test
  void uriEncodeOfSpecialCharactersWithJdkUrlEncoder() {
    assertEquals(expectedEncodedValue, Utils.encodeHttpParameter(queryParameterValue));
  }
}
