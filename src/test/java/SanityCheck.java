///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.rabbitmq:http-client:${version}
//DEPS org.slf4j:slf4j-simple:1.7.36

import org.slf4j.LoggerFactory;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.ClientParameters;

public class SanityCheck {

  public static void main(String[] args) {
    try {
      Client c =
          new Client(
              new ClientParameters()
                  .url("http://127.0.0.1:15672/api/")
                  .username("guest")
                  .password("guest"));
      c.getOverview();
      LoggerFactory.getLogger("rabbitmq")
          .info("Test succeeded with Hop {}", Client.class.getPackage().getImplementationVersion());
      System.exit(0);
    } catch (Exception e) {
      LoggerFactory.getLogger("rabbitmq").info("Test failed", e);
      System.exit(1);
    }
  }
}
