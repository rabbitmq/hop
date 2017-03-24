# Hop, Java Client for the RabbitMQ HTTP API

Hop is a Java client for the [RabbitMQ HTTP API](https://raw.githack.com/rabbitmq/rabbitmq-management/rabbitmq_v3_5_0/priv/www/api/index.html).


## Polyglot

Hop is designed to be easy to use from other JVM languages, primarily Groovy, Scala,
and Kotlin.

N.B. that Clojure already includes an HTTP API client as part of [Langohr](http://clojurerabbitmq.info),
and you should use Langohr instead.

## Project Maturity

This project is relatively young and not 100% feature complete but the key API operations are covered.
The docs largely don't exist beyond this README. The API may change but radical changes are unlikely.

This section will be updated as the project matures.


## Maven Artifacts

Project artifacts are available from Maven Central and [repo.spring.io](http://repo.spring.io).

### Maven

If you use Maven, add the following repository
definition to your `pom.xml`:

``` xml
<repository>
  <id>spring-releases</id>
  <url>https://repo.spring.io/release</url>
</repository>
```

The most recent release is

``` xml
<dependency>
  <groupId>com.rabbitmq</groupId>
  <artifactId>http-client</artifactId>
  <version>1.2.0.RELEASE</version>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

``` groovy
repositories {
    maven {
        url "https://repo.spring.io/release"
    }
}
```

Current (unpublished) snapshot release is

``` groovy
compile "com.rabbitmq:http-client:1.2.0.RELEASE"
```


## Usage Guide

### Instantiating a Client

Hop faithfully follows RabbitMQ HTTP API conventions in its API. You interact with the server
using a single class, `Client`, which needs an API endpoint and
a pair of credentials to be instantiated:

``` java
import com.rabbitmq.http.Client;

Client c = new Client("http://127.0.0.1:15672/api/", "guest", "guest");
```

### Getting Overview

``` java
c.getOverview();
```


### Node and Cluster Status

``` java
// list cluster nodes
c.getNodes();

// get status and metrics of individual node
c.getNode("rabbit@mercurio.local");
```


### Operations on Connections

``` java
// list client connections
c.getConnections();

// get status and metrics of individual connection
c.getConnection("127.0.0.1:61779 -> 127.0.0.1:5672");

// forcefully close connection
c.closeConnection("127.0.0.1:61779 -> 127.0.0.1:5672");
```

### Operations on Channels

``` java
// list all channels
c.getChannels();

// list channels on individual connection
c.getChannels("127.0.0.1:61779 -> 127.0.0.1:5672");

// list detailed channel info
c.getChannel("127.0.0.1:61779 -> 127.0.0.1:5672 (3)");
```


### Operations on Vhosts

``` java
// get status and metrics of individual vhost
c.getVhost("/");
```


### Managing Users

TBD


### Managing Permissions

TBD


### Operations on Exchanges

TBD


### Operations on Queues

``` java
// list all queues
c.getQueues();

// list all queues in a vhost
c.getQueues();

// declare a queue that's not durable, auto-delete,
// and non-exclusive
c.declareQueue("/", "queue1", new QueueInfo(false, true, false));

// bind a queue
c.bindQueue("/", "queue1", "amq.fanout", "routing-key");

// delete a queue
c.deleteQueue("/", "queue1");
```

### Operations on Bindings

``` java
// list bindings where exchange "an.exchange" is source
// (other things are bound to it)
c.getBindingsBySource("/", "an.exchange");

// list bindings where exchange "an.exchange" is destination
// (it is bound to other exchanges)
c.getBindingsByDestination("/", "an.exchange");
```


## Running Tests

    gradle check

The test suite assumes RabbitMQ is running locally with
stock settings and rabbitmq-management plugin enabled.


## License

[Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).


## Copyright

Michael Klishin, 2014-2016.
Pivotal Software Inc., 2014-current.
