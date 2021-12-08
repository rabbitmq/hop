@GrabResolver(name = 'ossrh-staging', root = 'https://oss.sonatype.org/content/groups/staging/')
@GrabResolver(name = 'rabbitmq-packagecloud-milestones', root = 'https://packagecloud.io/rabbitmq/maven-milestones/maven2')
@Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = "4.5.13")
@Grab(group = 'com.rabbitmq', module = 'http-client', version = "${version}")
@Grab(group = 'org.slf4j', module = 'slf4j-simple', version = '1.7.32')

// the Apache HttpClient dependency needs to stay because of Groovy's classloading mechanism
// (the Client class depends on 1 Apache HttpClient's class, that does not matter
// with regular Java classloading, but it does with Groovy's)
import com.rabbitmq.http.client.Client
import com.rabbitmq.http.client.ClientParameters
import org.slf4j.LoggerFactory

try {
    Client c = new Client(new ClientParameters()
            .url("http://127.0.0.1:15672/api/")
            .username("guest")
            .password("guest"))
    c.getOverview()
    LoggerFactory.getLogger("rabbitmq").info("Test succeeded with Hop {}", Client.getPackage().getImplementationVersion())
    System.exit 0
} catch (Exception e) {
    LoggerFactory.getLogger("rabbitmq").info("Test failed", e)
    System.exit 1
}
