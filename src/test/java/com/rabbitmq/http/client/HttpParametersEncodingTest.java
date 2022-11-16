package com.rabbitmq.http.client;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Test;

public class HttpParametersEncodingTest {

  String queryParameterValue = "^outbound-ack|msg-.+$";
  String expectedEncodedValue = "%5Eoutbound-ack%7Cmsg-.%2B%24";

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
