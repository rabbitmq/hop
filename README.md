# Hop, Java Client for the RabbitMQ HTTP API

[![Travis CI](https://travis-ci.org/rabbitmq/hop.svg?branch=master)](https://travis-ci.org/rabbitmq/hop)

Hop is a Java client for the [RabbitMQ HTTP API](https://raw.githack.com/rabbitmq/rabbitmq-management/v3.7.5/priv/www/api/index.html).


## Polyglot

Hop is designed to be easy to use from other JVM languages, primarily Groovy, Scala,
and Kotlin.

N.B. that Clojure already includes an HTTP API client as part of [Langohr](http://clojurerabbitmq.info),
and you should use Langohr instead.

## Reactive

As of Hop 2.1.0, a new reactive, non-blocking IO client based on [Reactor Netty](https://projectreactor.io/) is available. Note
the original blocking IO client remains available.

## Project Maturity

This project is relatively young and not 100% feature complete but the key API operations are covered.
The docs largely don't exist beyond this README. The API may change but radical changes are unlikely.

This section will be updated as the project matures.


## Maven Artifacts

### Stable

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.rabbitmq/http-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.rabbitmq/http-client)

Project artifacts are available from Maven Central and [repo.spring.io](https://repo.spring.io).

#### Maven

If you want to use the **blocking IO client**, add the following dependencies:

```xml
<dependency>
  <groupId>com.rabbitmq</groupId>
  <artifactId>http-client</artifactId>
  <version>3.4.0.RELEASE</version>
</dependency>
<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-web</artifactId>
  <version>5.1.9.RELEASE</version>
</dependency>
<dependency>
  <groupId>org.apache.httpcomponents</groupId>
  <artifactId>httpclient</artifactId>
  <version>4.5.9</version>
</dependency>
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-databind</artifactId>
  <version>2.9.10</version>
</dependency>
```

If you want to use the **reactive, non-blocking IO client**, add the following dependencies:

```xml
<dependency>
  <groupId>com.rabbitmq</groupId>
  <artifactId>http-client</artifactId>
  <version>3.4.0.RELEASE</version>
</dependency>
<dependency>
  <groupId>io.projectreactor.netty</groupId>
  <artifactId>reactor-netty</artifactId>
  <version>0.8.11.RELEASE</version>
</dependency>
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-databind</artifactId>
  <version>2.9.10</version>
</dependency>
```

#### Gradle

If you want to use the **blocking IO client**, add the following dependencies:

```groovy
compile "com.rabbitmq:http-client:3.4.0.RELEASE"
compile "org.springframework:spring-web:5.1.9.RELEASE"
compile "org.apache.httpcomponents:httpclient:4.5.9"
compile "com.fasterxml.jackson.core:jackson-databind:2.9.10"
```

If you want to use the **reactive, non-blocking IO client**, add the following dependencies:

```groovy
compile "com.rabbitmq:http-client:3.4.0.RELEASE"
compile "io.projectreactor.netty:reactor-netty:0.8.11.RELEASE"
compile "com.fasterxml.jackson.core:jackson-databind:2.9.10"
```

## Usage Guide

### Instantiating a Client

Hop faithfully follows RabbitMQ HTTP API conventions in its API. You interact with the server
using a single class, `Client`, which needs an API endpoint and
a pair of credentials to be instantiated:

``` java
import com.rabbitmq.http.client.Client;

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

To run the suite against a specific RabbitMQ node, export `HOP_RABBITMQCTL` and `HOP_RABBITMQ_PLUGINS` 
to point at `rabbitmqctl` and `rabbitmq-plugins` from the installation.

Then set up the node that is assumed to be running:

``` sh
./bin/before_build.sh
```

This will enable several plugins used by the test suite and configure the node
to use a much shorter event refresh interval so that HTTP API reflects system state
changes with less of a delay.

To run the tests:

``` sh
./gradlew check
```

The test suite assumes RabbitMQ is running locally with
stock settings and a few plugins are enabled:

 * `rabbitmq_management`
 * `rabbitmq_shovel_management`
 * `rabbitmq_federation_management`  

[Travis file](https://github.com/rabbitmq/hop/blob/master/.travis.yml) contains instructions to install and configure 
local Erlang and RabbitMQ

## License

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).


## Copyright

Michael Klishin, 2014-2016.
Pivotal Software Inc., 2014-current.
