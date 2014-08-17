# Hop, Java Client for the RabbitMQ HTTP API

Hop is a Java client for the [RabbitMQ HTTP API](http://hg.rabbitmq.com/rabbitmq-management/raw-file/rabbitmq_v3_3_5/priv/www/api/index.html).


## Polyglot

Hop is designed to be easy to use from other JVM languages, primarily Groovy, Scala,
and Kotlin. 

N.B. that Clojure already includes an HTTP API client as part of [Langohr](http://clojurerabbitmq.info),
and you should use Langohr instead.

## Project Maturity

This project is extremely young and under heavy development. The API can radically
change without an upfront notice.

This section will be updated as the API matures.


## Maven Artifacts

Project artifacts are released to [Clojars](http://clojars.org).

### Maven

If you use Maven, add the following repository
definition to your `pom.xml`:

``` xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

The most recent release is

``` xml
<dependency>
  <groupId>com.novemberain.hop</groupId>
  <artifactId>hop</artifactId>
  <version>1.0.0-beta2</version>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

``` groovy
repositories {
    maven {
        url "http://clojars.org/repo"
    }
}
```

The most recent release is

``` groovy
compile "com.novemberain.hop:hop:1.0.0-beta2"
```


## Usage Guide

### Instantiating a Client

Hop faithfully follows RabbitMQ HTTP API conventions in its API. You interact with the server
using a single class, `com.novemberain.hop.client.Client`, which needs an API endpoint and
a pair of credentials to be instantiated:

``` java
import com.novemberain.hop.client.Client;

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

TBD


### Operations on Vhosts

TBD


### Managing Users

TBD


### Managing Permissions

TBD


### Operations on Exchanges

TBD


### Operations on Queues

TBD



## License

[Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).


## Copyright

Michael Klishin, 2014.
