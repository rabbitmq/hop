@GrabResolver(name = 'spring-staging', root = 'http://repo.spring.io/libs-staging-local/')
@Grab(group = 'com.rabbitmq', module = 'http-client', version = "${version}")
@Grab(group = 'org.springframework', module = 'spring-web', version = "5.1.1.RELEASE")
@Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = "4.5.6")
@Grab(group = 'org.slf4j', module = 'slf4j-simple', version = '1.7.25')

import com.rabbitmq.http.client.Client
import org.slf4j.LoggerFactory

try {
    Client c = new Client("http://127.0.0.1:15672/api/", "guest", "guest");
    c.getOverview()
    LoggerFactory.getLogger("rabbitmq").info("Test succeeded")
    System.exit 0
} catch (Exception e) {
    LoggerFactory.getLogger("rabbitmq").info("Test failed", e)
    System.exit 1
}
