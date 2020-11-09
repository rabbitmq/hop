@GrabResolver(name = 'rabbitmq-bintray', root = 'https://dl.bintray.com/rabbitmq/maven')
@GrabResolver(name = 'rabbitmq-packagecloud-milestones', root = 'https://packagecloud.io/rabbitmq/maven-milestones/maven2')
@Grab(group = 'com.rabbitmq', module = 'http-client', version = "${version}")
@Grab(group = 'org.springframework', module = 'spring-web', version = "5.3.0")
@Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = "4.5.13")
@Grab(group = 'org.slf4j', module = 'slf4j-simple', version = '1.7.30')

import com.rabbitmq.http.client.Client
import org.slf4j.LoggerFactory

try {
    Client c = new Client("http://127.0.0.1:15672/api/", "guest", "guest");
    c.getOverview()
    LoggerFactory.getLogger("rabbitmq").info("Test succeeded with Hop {}", Client.getPackage().getImplementationVersion())
    System.exit 0
} catch (Exception e) {
    LoggerFactory.getLogger("rabbitmq").info("Test failed", e)
    System.exit 1
}
