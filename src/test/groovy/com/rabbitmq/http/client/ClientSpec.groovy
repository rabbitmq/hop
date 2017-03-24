/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rabbitmq.http.client

import com.rabbitmq.client.AuthenticationFailureException
import com.rabbitmq.http.client.domain.Definitions
import org.apache.http.impl.client.HttpClientBuilder
import spock.lang.IgnoreIf
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.ShutdownListener
import com.rabbitmq.client.ShutdownSignalException
import com.rabbitmq.http.client.domain.BindingInfo
import com.rabbitmq.http.client.domain.ChannelInfo
import com.rabbitmq.http.client.domain.ClusterId
import com.rabbitmq.http.client.domain.ConnectionInfo
import com.rabbitmq.http.client.domain.ExchangeInfo
import com.rabbitmq.http.client.domain.NodeInfo
import com.rabbitmq.http.client.domain.PolicyInfo
import com.rabbitmq.http.client.domain.QueueInfo
import com.rabbitmq.http.client.domain.UserPermissions
import com.rabbitmq.http.client.domain.VhostInfo

class ClientSpec extends Specification {

  protected static final String DEFAULT_USERNAME = "guest"

  protected static final String DEFAULT_PASSWORD = "guest"

  protected Client client
  private final ConnectionFactory cf = initializeConnectionFactory()

  protected static ConnectionFactory initializeConnectionFactory() {
    final cf = new ConnectionFactory()
    cf.setAutomaticRecoveryEnabled(false)
    cf
  }

  def setup() {
    client = newLocalhostNodeClient()
  }

  protected static Client newLocalhostNodeClient() {
    new Client("http://127.0.0.1:15672/api/", DEFAULT_USERNAME, DEFAULT_PASSWORD)
  }

  protected static Client newLocalhostNodeClient(HttpClientBuilderConfigurator cfg) {
    new Client("http://127.0.0.1:15672/api/", DEFAULT_USERNAME, DEFAULT_PASSWORD, cfg)
  }

  def "GET /api/overview"() {
    when: "client requests GET /api/overview"
    final conn = openConnection()
    final ch = conn.createChannel()
    1000.times { ch.basicPublish("", "", null, null) }

    def res = client.getOverview()
    def xts = res.getExchangeTypes().collect { it.getName() }

    then: "the response is converted successfully"
    res.getNode().startsWith("rabbit@")
    res.getErlangVersion() != null

    final msgStats = res.getMessageStats()
    msgStats.basicPublish >= 0
    msgStats.publisherConfirm >= 0
    msgStats.basicDeliver >= 0
    msgStats.basicReturn >= 0

    final qTotals = res.getQueueTotals()
    qTotals.messages >= 0
    qTotals.messagesReady >= 0
    qTotals.messagesUnacknowledged >= 0

    final oTotals = res.getObjectTotals()
    oTotals.connections >= 0
    oTotals.channels >= 0
    oTotals.exchanges >= 0
    oTotals.queues >= 0
    oTotals.consumers >= 0

    res.listeners.size() >= 1
    res.contexts.size() >= 1

    xts.contains("topic")
    xts.contains("fanout")
    xts.contains("direct")
    xts.contains("headers")

    cleanup:
    if (conn.isOpen()) {
      conn.close()
    }
  }

  def "GET /api/nodes"() {
    when: "client retrieves a list of cluster nodes"
    final res = client.getNodes()
    final node = res.first()

    then: "the list is returned"
    res.size() >= 1
    verifyNode(node)
  }

  def "GET /api/nodes with a user-provided HTTP builder configurator"() {
    when: "a user-provided HTTP builder configurator is set"
    final cfg = new HttpClientBuilderConfigurator() {
      @Override
      HttpClientBuilder configure(HttpClientBuilder builder) {
        // this number has no particular meaning
        // but it should be enough connections for this test suite
        // and then some. MK.
        builder.setMaxConnTotal(8192)
        return builder
      }
    }
    final client = newLocalhostNodeClient(cfg)

    and: "client retrieves a list of cluster nodes"
    final res = client.getNodes()
    final node = res.first()

    then: "the list is returned"
    res.size() >= 1
    verifyNode(node)
  }

  def "GET /api/nodes/{name}"() {
    when: "client retrieves a list of cluster nodes"
    final res = client.getNodes()
    final name = res.first().name
    final node = client.getNode(name)

    then: "the list is returned"
    res.size() >= 1
    verifyNode(node)
  }

  def "GET /api/connections"() {
    given: "an open RabbitMQ client connection"
    final conn = openConnection()

    when: "client retrieves a list of connections"
    awaitEventPropagation()
    final res = client.getConnections()
    final fst = res.first()

    then: "the list is returned"
    res.size() >= 1
    verifyConnectionInfo(fst)

    cleanup:
    conn.close()
  }

  def "GET /api/connections/{name}"() {
    given: "an open RabbitMQ client connection"
    final conn = openConnection()

    when: "client retrieves connection info with the correct name"
    awaitEventPropagation()
    final xs = client.getConnections()
    final x = client.getConnection(xs.first().name)

    then: "the info is returned"
    verifyConnectionInfo(x)

    cleanup:
    conn.close()
  }

  def "GET /api/connections/{name} with client-provided name"() {
    given: "an open RabbitMQ client connection with client-provided name"
    final s = "client-name"
    final conn = openConnection(s)

    when: "client retrieves connection info with the correct name"
    awaitEventPropagation()
    final xs = client.getConnections()
    final x = client.getConnection(xs.first().name)

    then: "the info is returned"
    verifyConnectionInfo(x)
    x.clientProperties.connectionName == s

    cleanup:
    conn.close()
  }

  def "DELETE /api/connections/{name}"() {
    given: "an open RabbitMQ client connection"
    final latch = new CountDownLatch(1)
    final conn = openConnection()
    conn.addShutdownListener(new ShutdownListener() {
      @Override
      void shutdownCompleted(ShutdownSignalException e) {
        latch.countDown()
      }
    })
    assert conn.isOpen()

    when: "client closes the connection"
    awaitEventPropagation()
    final xs = client.getConnections()
    xs.each({ client.closeConnection(it.name) })

    and: "some time passes"
    assert awaitOn(latch)

    then: "the connection is closed"
    !conn.isOpen()

    cleanup:
    if (conn.isOpen()) {
      conn.close()
    }
  }

  def "DELETE /api/connections/{name} with a user-provided reason"() {
    given: "an open RabbitMQ client connection"
    final latch = new CountDownLatch(1)
    final conn = openConnection()
    conn.addShutdownListener(new ShutdownListener() {
      @Override
      void shutdownCompleted(ShutdownSignalException e) {
        latch.countDown()
      }
    })
    assert conn.isOpen()

    when: "client closes the connection"
    awaitEventPropagation()
    final xs = client.getConnections()
    xs.each({ client.closeConnection(it.name, "because reasons!") })

    and: "some time passes"
    assert awaitOn(latch)

    then: "the connection is closed"
    !conn.isOpen()

    cleanup:
    if (conn.isOpen()) {
      conn.close()
    }
  }

  def "GET /api/channels"() {
    given: "an open RabbitMQ client connection with 1 channel"
    final conn = openConnection()
    final ch = conn.createChannel()

    when: "client lists channels"
    awaitEventPropagation()
    final chs = client.getChannels()
    final chi = chs.first()

    then: "the list is returned"
    verifyChannelInfo(chi, ch)

    cleanup:
    if (conn.isOpen()) {
      conn.close()
    }
  }

  def "GET /api/connections/{name}/channels/"() {
    given: "an open RabbitMQ client connection with 1 channel"
    final conn = openConnection()
    final ch = conn.createChannel()

    when: "client lists channels on that connection"
    awaitEventPropagation()
    final cn = client.getConnections().first().name
    final chs = client.getChannels(cn)
    final chi = chs.first()

    then: "the list is returned"
    verifyChannelInfo(chi, ch)

    cleanup:
    if (conn.isOpen()) {
      conn.close()
    }
  }

  def "GET /api/channels/{name}"() {
    given: "an open RabbitMQ client connection with 1 channel"
    final conn = openConnection()
    final ch = conn.createChannel()

    when: "client retrieves channel info"
    awaitEventPropagation()
    final chs = client.getChannels()
    final chi = client.getChannel(chs.first().name)

    then: "the info is returned"
    verifyChannelInfo(chi, ch)

    cleanup:
    if (conn.isOpen()) {
      conn.close()
    }
  }

  def "GET /api/exchanges"() {
    when: "client retrieves the list of exchanges across all vhosts"
    final xs = client.getExchanges()
    final x = xs.first()

    then: "the list is returned"
    verifyExchangeInfo(x)
  }

  def "GET /api/exchanges/{vhost} when vhost exists"() {
    when: "client retrieves the list of exchanges in a particular vhost"
    final xs = client.getExchanges("/")

    then: "the list is returned"
    final x = xs.find { (it.name == "amq.fanout") }
    verifyExchangeInfo(x)
  }

  def "GET /api/exchanges/{vhost} when vhost DOES NOT exist"() {
    given: "vhost lolwut does not exist"
    final v = "lolwut"
    client.deleteVhost(v)

    when: "client retrieves the list of exchanges in that vhost"
    final xs = client.getExchanges(v)

    then: "null is returned"
    xs == null
  }

  def "GET /api/exchanges/{vhost}/{name} when both vhost and exchange exist"() {
    when: "client retrieves exchange amq.fanout in vhost /"
    final xs = client.getExchange("/", "amq.fanout")

    then: "exchange info is returned"
    final ExchangeInfo x = (ExchangeInfo)xs.find { it.name == "amq.fanout" && it.vhost == "/" }
    verifyExchangeInfo(x)
  }

  def "PUT /api/exchanges/{vhost}/{name} when vhost exists"() {
    given: "fanout exchange hop.test in vhost /"
    final v = "/"
    final s = "hop.test"
    client.declareExchange(v, s, new ExchangeInfo("fanout", false, false))

    when: "client lists exchanges in vhost /"
    List<ExchangeInfo> xs = client.getExchanges(v)

    then: "hop.test is listed"
    ExchangeInfo x = xs.find { (it.name == s) }
    x != null
    verifyExchangeInfo(x)

    cleanup:
    client.deleteExchange(v, s)
  }

  def "DELETE /api/exchanges/{vhost}/{name}"() {
    given: "fanout exchange hop.test in vhost /"
    final v = "/"
    final s = "hop.test"
    client.declareExchange(v, s, new ExchangeInfo("fanout", false, false))

    List<ExchangeInfo> xs = client.getExchanges(v)
    ExchangeInfo x = xs.find { (it.name == s) }
    x != null
    verifyExchangeInfo(x)

    when: "client deletes exchange hop.test in vhost /"
    client.deleteExchange(v, s)

    and: "exchange list in / is reloaded"
    xs = client.getExchanges(v)

    then: "hop.test no longer exists"
    xs.find { (it.name == s) } == null
  }

  def "POST /api/exchanges/{vhost}/{name}/publish"() {
    // TODO
  }

  def "GET /api/exchanges/{vhost}/{name}/bindings/source"() {
    given: "a queue named hop.queue1"
    final conn = openConnection()
    final ch = conn.createChannel()
    final q = "hop.queue1"
    ch.queueDeclare(q, false, false, false, null)

    when: "client lists bindings of default exchange"
    final xs = client.getBindingsBySource("/", "")

    then: "there is an automatic binding for hop.queue1"
    final x = xs.find { it.source == "" && it.destinationType == "queue" && it.destination == q }
    x != null

    cleanup:
    ch.queueDelete(q)
    conn.close()
  }

  def "GET /api/exchanges/{vhost}/{name}/bindings/destination"() {
    given: "an exchange named hop.exchange1 which is bound to amq.fanout"
    final conn = openConnection()
    final ch = conn.createChannel()
    final src = "amq.fanout"
    final dest = "hop.exchange1"
    ch.exchangeDeclare(dest, "fanout")
    ch.exchangeBind(dest, src, "")

    when: "client lists bindings of amq.fanout"
    final xs = client.getExchangeBindingsByDestination("/", dest)

    then: "there is a binding for hop.exchange1"
    final x = xs.find { it.source == src &&
        it.destinationType == "exchange" &&
        it.destination == dest
    }
    x != null

    cleanup:
    ch.exchangeDelete(dest)
    conn.close()
  }

  def "GET /api/queues"() {
    given: "at least one queue was declared"
    final Connection conn = cf.newConnection()
    final Channel ch = conn.createChannel()
    final String q = ch.queueDeclare().queue

    when: "client lists queues"
    final xs = client.getQueues()

    then: "a list of queues is returned"
    final x = xs.first()
    verifyQueueInfo(x)

    cleanup:
    ch.queueDelete(q)
    conn.close()
  }

  def "GET /api/queues/{vhost} when vhost exists"() {
    given: "at least one queue was declared in vhost /"
    final Connection conn = cf.newConnection()
    final Channel ch = conn.createChannel()
    final String q = ch.queueDeclare().queue

    when: "client lists queues"
    final xs = client.getQueues("/")

    then: "a list of queues is returned"
    final x = xs.first()
    verifyQueueInfo(x)

    cleanup:
    ch.queueDelete(q)
    conn.close()
  }

  def "GET /api/queues/{vhost} when vhost DOES NOT exist"() {
    given: "vhost lolwut DOES not exist"
    final v = "lolwut"
    client.deleteVhost(v)

    when: "client lists queues"
    final xs = client.getQueues(v)

    then: "null is returned"
    xs == null
  }

  def "GET /api/queues/{vhost}/{name} when both vhost and queue exist"() {
    given: "a queue was declared in vhost /"
    final Connection conn = cf.newConnection()
    final Channel ch = conn.createChannel()
    final String q = ch.queueDeclare().queue

    when: "client fetches info of the queue"
    final x = client.getQueue("/", q)

    then: "the info is returned"
    x.vhost == "/"
    x.name == q
    verifyQueueInfo(x)

    cleanup:
    ch.queueDelete(q)
    conn.close()
  }

  def "GET /api/queues/{vhost}/{name} with an exclusive queue"() {
    given: "an exclusive queue named hop.q1.exclusive"
    final Connection conn = cf.newConnection()
    final Channel ch = conn.createChannel()
    String s = "hop.q1.exclusive"
    ch.queueDelete(s)
    final String q = ch.queueDeclare(s, false, true, false, null).queue

    when: "client fetches info of the queue"
    final x = client.getQueue("/", q)

    then: "the queue is exclusive according to the response"
    x.exclusive

    cleanup:
    ch.queueDelete(q)
    conn.close()
  }

  def "GET /api/queues/{vhost}/{name} when queue DOES NOT exist"() {
    given: "queue lolwut does not exist in vhost /"
    final Connection conn = cf.newConnection()
    final Channel ch = conn.createChannel()
    final String q = "lolwut"
    ch.queueDelete(q)

    when: "client fetches info of the queue"
    final x = client.getQueue("/", q)

    then: "null is returned"
    x == null

    cleanup:
    ch.queueDelete(q)
    conn.close()
  }

  def "PUT /api/queues/{vhost}/{name} when vhost exists"() {
    given: "vhost /"
    final v = "/"

    when: "client declares a queue hop.test"
    final s = "hop.test"
    client.declareQueue(v, s, new QueueInfo(false, false, false))

    and: "client lists queues in vhost /"
    List<QueueInfo> xs = client.getQueues(v)

    then: "hop.test is listed"
    QueueInfo x = xs.find { it.name.equals(s) }
    x != null
    x.vhost.equals(v)
    x.name.equals(s)
    !x.durable
    !x.exclusive
    !x.autoDelete

    cleanup:
    client.deleteQueue(v, s)
  }

  def "PUT /api/policies/{vhost}/{name}"() {
    given: "vhost / and definition"
    final v = "/"
    final d = new HashMap<String, Object>()
    d.put("ha-mode", "all")

    when: "client declares a policy hop.test"
    final s = "hop.test"
    client.declarePolicy(v, s, new PolicyInfo(".*", 1, null, d))

    and: "client lists policies in vhost /"
    List<PolicyInfo> ps = client.getPolicies(v)

    then: "hop.test is listed"
    PolicyInfo p = ps.find { it.name.equals(s) }
    p != null
    p.vhost.equals(v)
    p.name.equals(s)
    p.priority.equals(1)
    p.applyTo.equals("all")
    p.definition.equals(d)

    cleanup:
    client.deletePolicy(v, s)
  }

  def "PUT /api/queues/{vhost}/{name} when vhost DOES NOT exist"() {
    given: "vhost lolwut which does not exist"
    final v = "lolwut"
    client.deleteVhost(v)

    when: "client declares a queue hop.test"
    final s = "hop.test"
    client.declareQueue(v, s, new QueueInfo(false, false, false))

    then: "an exception is thrown"
    final e = thrown(HttpClientErrorException)
    e.getStatusCode() == HttpStatus.NOT_FOUND

  }

  def "DELETE /api/queues/{vhost}/{name}"() {
    final String s = UUID.randomUUID().toString()
    given: "queue ${s} in vhost /"
    final v = "/"
    client.declareQueue(v, s, new QueueInfo(false, false, false))

    List<QueueInfo> xs = client.getQueues(v)
    QueueInfo x = xs.find { it.name.equals(s) }
    x != null
    verifyQueueInfo(x)

    when: "client deletes queue ${s} in vhost /"
    client.deleteQueue(v, s)

    and: "queue list in / is reloaded"
    xs = client.getQueues(v)

    then: "${s} no longer exists"
    xs.find { it.name.equals(s) } == null
  }

  def "GET /api/bindings"() {
    given: "3 queues bound to amq.fanout"
    final Connection conn = cf.newConnection()
    final Channel ch = conn.createChannel()
    final String x  = 'amq.fanout'
    final String q1 = ch.queueDeclare().queue
    final String q2 = ch.queueDeclare().queue
    final String q3 = ch.queueDeclare().queue
    ch.queueBind(q1, x, "")
    ch.queueBind(q2, x, "")
    ch.queueBind(q3, x, "")

    when: "all queue bindings are listed"
    final List<BindingInfo> xs = client.getBindings()

    then: "amq.fanout bindings are listed"
    xs.findAll { it.destinationType.equals("queue") && it.source.equals(x) }
      .size() >= 3

    cleanup:
    ch.queueDelete(q1)
    ch.queueDelete(q2)
    ch.queueDelete(q3)
    conn.close()
  }

  def "GET /api/bindings/{vhost}"() {
    given: "2 queues bound to amq.topic in vhost /"
    final Connection conn = cf.newConnection()
    final Channel ch = conn.createChannel()
    final String x  = 'amq.topic'
    final String q1 = ch.queueDeclare().queue
    final String q2 = ch.queueDeclare().queue
    ch.queueBind(q1, x, "hop.*")
    ch.queueBind(q2, x, "api.test.#")

    when: "all queue bindings are listed"
    final List<BindingInfo> xs = client.getBindings("/")

    then: "amq.fanout bindings are listed"
    xs.findAll { it.destinationType.equals("queue") && it.source.equals(x) }
      .size() >= 2

    cleanup:
    ch.queueDelete(q1)
    ch.queueDelete(q2)
    conn.close()
  }

  def "GET /api/bindings/{vhost} example 2"() {
    given: "queues hop.test bound to amq.topic in vhost /"
    final Connection conn = cf.newConnection()
    final Channel ch = conn.createChannel()
    final String x  = 'amq.topic'
    final String q  = "hop.test"
    ch.queueDeclare(q, false, false, false, null)
    ch.queueBind(q, x, "hop.*")

    when: "all queue bindings are listed"
    final List<BindingInfo> xs = client.getBindings("/")

    then: "the amq.fanout binding is listed"
    xs.find { it.destinationType.equals("queue") && it.source.equals(x) && it.destination.equals(q) }

    cleanup:
    ch.queueDelete(q)
    conn.close()
  }

  def "GET /api/queues/{vhost}/{name}/bindings"() {
    given: "queues hop.test bound to amq.topic in vhost /"
    final Connection conn = cf.newConnection()
    final Channel ch = conn.createChannel()
    final String x  = 'amq.topic'
    final String q  = "hop.test"
    ch.queueDeclare(q, false, false, false, null)
    ch.queueBind(q, x, "hop.*")

    when: "all queue bindings are listed"
    final List<BindingInfo> xs = client.getQueueBindings("/", q)

    then: "the amq.fanout binding is listed"
    xs.find { it.destinationType.equals("queue") && it.source.equals(x) && it.destination.equals(q) }

    cleanup:
    ch.queueDelete(q)
    conn.close()
  }

  def "GET /api/bindings/{vhost}/e/:exchange/q/:queue"() {
    given: "queues hop.test bound to amq.topic in vhost /"
    final Connection conn = cf.newConnection()
    final Channel ch = conn.createChannel()
    final String x  = 'amq.topic'
    final String q  = "hop.test"
    ch.queueDeclare(q, false, false, false, null)
    ch.queueBind(q, x, "hop.*")

    when: "bindings between hop.test and amq.topic are listed"
    final List<BindingInfo> xs = client.getQueueBindingsBetween("/", x, q)

    then: "the amq.fanout binding is listed"
    final b = xs.find()
    xs.size() == 1
    b.source.equals(x)
    b.destination.equals(q)
    b.destinationType.equals("queue")

    cleanup:
    ch.queueDelete(q)
    conn.close()
  }

  def "GET /api/bindings/{vhost}/e/:source/e/:destination"() {
    given: "fanout exchange hop.test bound to amq.fanout in vhost /"
    final Connection conn = cf.newConnection()
    final Channel ch = conn.createChannel()
    final String s  = 'amq.fanout'
    final String d  = "hop.test"
    ch.exchangeDeclare(d, "fanout", false)
    ch.exchangeBind(d, s, "")

    when: "bindings between hop.test and amq.topic are listed"
    final List<BindingInfo> xs = client.getExchangeBindingsBetween("/", s, d)

    then: "the amq.topic binding is listed"
    final b = xs.find()
    xs.size() == 1
    b.source.equals(s)
    b.destination.equals(d)
    b.destinationType.equals("exchange")

    cleanup:
    ch.exchangeDelete(d)
    conn.close()
  }

  def "POST /api/bindings/{vhost}/e/:source/e/:destination"() {
    given: "fanout hop.test bound to amq.fanout in vhost /"
    final v = "/"
    final String s  = 'amq.fanout'
    final String d  = "hop.test"
    client.deleteExchange(v, d)
    client.declareExchange(v, d, new ExchangeInfo("fanout", false, false))
    client.bindExchange(v, d, s, "")

    when: "bindings between hop.test and amq.fanout are listed"
    final List<BindingInfo> xs = client.getExchangeBindingsBetween(v, s, d)

    then: "the amq.fanout binding is listed"
    final b = xs.find()
    xs.size() == 1
    b.source.equals(s)
    b.destination.equals(d)
    b.destinationType.equals("exchange")

    cleanup:
    client.deleteExchange(v, d)
  }

  def "POST /api/bindings/{vhost}/e/:exchange/q/:queue"() {
    given: "queues hop.test bound to amq.topic in vhost /"
    final v = "/"
    final String x  = 'amq.topic'
    final String q  = "hop.test"
    client.declareQueue(v, q, new QueueInfo(false, false, false))
    client.bindQueue(v, q, x, "")

    when: "bindings between hop.test and amq.topic are listed"
    final List<BindingInfo> xs = client.getQueueBindingsBetween(v, x, q)

    then: "the amq.fanout binding is listed"
    final b = xs.find()
    xs.size() == 1
    b.source.equals(x)
    b.destination.equals(q)
    b.destinationType.equals("queue")

    cleanup:
    client.deleteQueue(v, q)
  }

  def "GET /api/bindings/{vhost}/e/:exchange/q/:queue/props"() {
    // TODO
  }

  def "DELETE /api/bindings/{vhost}/e/:exchange/q/:queue/props"() {
    // TODO
  }

  def "DELETE /api/queues/{vhost}/{name}/contents"() {
    given: "queue hop.test with 10 messages"
    final Connection conn = cf.newConnection()
    final Channel ch = conn.createChannel()
    final q = "hop.test"
    ch.queueDelete(q)
    ch.queueDeclare(q, false, false, false, null)
    ch.queueBind(q, "amq.fanout", "")
    ch.confirmSelect()
    100.times { ch.basicPublish("amq.fanout", "", null, "msg".getBytes()) }
    assert ch.waitForConfirms()
    final qi1 = ch.queueDeclarePassive(q)
    qi1.messageCount == 100

    when: "client purges the queue"
    client.purgeQueue("/", q)

    then: "the queue becomes empty"
    final qi2 = ch.queueDeclarePassive(q)
    qi2.messageCount == 0

    cleanup:
    ch.queueDelete(q)
    conn.close()
  }

  def "POST /api/queues/{vhost}/{name}/get"() {
    // TODO
  }

  def "GET /api/vhosts"() {
    when: "client retrieves a list of vhosts"
    final vhs = client.getVhosts()
    final vhi = vhs.first()

    then: "the info is returned"
    verifyVhost(vhi)
  }

  def "GET /api/vhosts/{name}"() {
    when: "client retrieves vhost info"
    final vhi = client.getVhost("/")

    then: "the info is returned"
    verifyVhost(vhi)
  }

  @IgnoreIf({ os.windows })
  def "PUT /api/vhosts/{name}"(String name) {
    when:
    "client creates a vhost named $name"
    client.createVhost(name)
    final vhi = client.getVhost(name)

    then: "the vhost is created"
    vhi.name == name

    cleanup:
    client.deleteVhost(name)

    where:
    name << [
        "http-created",
        "http-created2",
        "http_created",
        "http created",
        "создан по хатэтэпэ",
        "creado a través de HTTP",
        "通过http",
        "HTTP를 통해 생성",
        "HTTPを介して作成",
        "created over http?",
        "created @ http API",
        "erstellt über http",
        "http पर बनाया",
        "ถูกสร้างขึ้นผ่าน HTTP",
        "±!@^&#*"
    ]
  }

  def "DELETE /api/vhosts/{name} when vhost exists"() {
    given: "a vhost named hop-test-to-be-deleted"
    final s = "hop-test-to-be-deleted"
    client.createVhost(s)

    when: "the vhost is deleted"
    client.deleteVhost(s)

    then: "it no longer exists"
    client.getVhost(s) == null
  }

  def "DELETE /api/vhosts/{name} when vhost DOES NOT exist"() {
    given: "no vhost named hop-test-to-be-deleted"
    final s = "hop-test-to-be-deleted"
    client.deleteVhost(s)

    when: "the vhost is deleted"
    client.deleteVhost(s)

    then: "it is a no-op"
    client.getVhost(s) == null
  }

  def "GET /api/vhosts/{name}/permissions when vhost exists"() {
    when: "permissions for vhost / are listed"
    final s = "/"
    final xs = client.getPermissionsIn(s)

    then: "they include permissions for the guest user"
    UserPermissions x = xs.find { it.user.equals("guest") }
    x.read == ".*"
  }

  def "GET /api/vhosts/{name}/permissions when vhost DOES NOT exist"() {
    when: "permissions for vhost trololowut are listed"
    final s = "trololowut"
    final xs = client.getPermissionsIn(s)

    then: "method returns null"
    xs == null
  }

  def "GET /api/users"() {
    when: "users are listed"
    final xs = client.getUsers()
    final version = client.getOverview().getRabbitMQVersion()

    then: "a list of users is returned"
    final x = xs.find { it.name.equals("guest") }
    x.name == "guest"
    x.passwordHash != null
    isVersion36orLater(version) ? x.hashingAlgorithm != null : x.hashingAlgorithm == null
    x.tags.contains("administrator")
  }

  def "GET /api/users/{name} when user exists"() {
    when: "user guest if fetched"
    final x = client.getUser("guest")
    final version = client.getOverview().getRabbitMQVersion()

    then: "user info returned"
    x.name == "guest"
    x.passwordHash != null
    isVersion36orLater(version) ? x.hashingAlgorithm != null : x.hashingAlgorithm == null
    x.tags.contains("administrator")
  }

  def "GET /api/users/{name} when user DOES NOT exist"() {
    when: "user lolwut if fetched"
    final x = client.getUser("lolwut")

    then: "null is returned"
    x == null
  }

  def "PUT /api/users/{name} updates user tags"() {
    given: "user alt-user"
    final u = "alt-user"
    client.deleteUser(u)
    client.createUser(u, u.toCharArray(), Arrays.asList("original", "management"))
    awaitEventPropagation()

    when: "alt-user's tags are updated"
    client.updateUser(u, u.toCharArray(), Arrays.asList("management", "updated"))
    awaitEventPropagation()

    and: "alt-user info is reloaded"
    final x = client.getUser(u)

    then: "alt-user has new tags"
    x.tags.contains("updated")
    !x.tags.contains("original")
  }

  def "DELETE /api/users/{name}"() {
    given: "user alt-user"
    final u = "alt-user"
    client.deleteUser(u)
    client.createUser(u, u.toCharArray(), Arrays.asList("original", "management"))
    awaitEventPropagation()

    when: "alt-user is deleted"
    client.deleteUser(u)
    awaitEventPropagation()

    and: "alt-user info is reloaded"
    final x = client.getUser(u)

    then: "deleted user is gone"
    x == null
  }

  def "GET /api/users/{name}/permissions when user exists"() {
    when: "permissions for user guest are listed"
    final s = "guest"
    final xs = client.getPermissionsOf(s)

    then: "they include permissions for the / vhost"
    UserPermissions x = xs.find { it.vhost.equals("/") }
    x.read == ".*"
  }

  def "GET /api/users/{name}/permissions when users DOES NOT exist"() {
    when: "permissions for user trololowut are listed"
    final s = "trololowut"
    final xs = client.getPermissionsOf(s)

    then: "method returns null"
    xs == null
  }

  def "PUT /api/users/{name} with a blank password hash"() {
    given: "user alt-user with a blank password hash"
    final u = "alt-user"
    // blank password hash means only authentication using alternative
    // authentication mechanisms such as x509 certificates is possible. MK.
    final h = ""
    client.deleteUser(u)
    client.createUserWithPasswordHash(u, h.toCharArray(), Arrays.asList("original", "management"))
    client.updatePermissions("/", u, new UserPermissions(".*", ".*", ".*"))

    when: "alt-user tries to connect with a blank password"
    final conn = openConnection("alt-user", "alt-user")

    then: "connection is refused"
    // it would have a chance of being accepted if the x509 authentication mechanism was used. MK.
    thrown AuthenticationFailureException

    cleanup:
    client.deleteUser(u)
  }

  def "GET /api/whoami"() {
    when: "client retrieves active name authentication details"
    final res = client.whoAmI()

    then: "the details are returned"
    res.name == DEFAULT_USERNAME
    res.tags ==~ /administrator/
  }

  def "GET /api/permissions"() {
    when: "all permissions are listed"
    final s = "guest"
    final xs = client.getPermissions()

    then: "they include permissions for user guest in vhost /"
    final UserPermissions x = xs.find { it.vhost.equals("/") && it.user.equals(s) }
    x.read == ".*"
  }

  def "GET /api/permissions/{vhost}/:user when both vhost and user exist"() {
    when: "permissions of user guest in vhost / are listed"
    final u = "guest"
    final v = "/"
    final UserPermissions x = client.getPermissions(v, u)

    then: "a single permissions object is returned"
    x.read == ".*"
  }

  def "GET /api/permissions/{vhost}/:user when vhost DOES NOT exist"() {
    when: "permissions of user guest in vhost lolwut are listed"
    final u = "guest"
    final v = "lolwut"
    final UserPermissions x = client.getPermissions(v, u)

    then: "null is returned"
    x == null
  }

  def "GET /api/permissions/{vhost}/:user when username DOES NOT exist"() {
    when: "permissions of user lolwut in vhost / are listed"
    final u = "lolwut"
    final v = "/"
    final UserPermissions x = client.getPermissions(v, u)

    then: "null is returned"
    x == null
  }

  def "PUT /api/permissions/{vhost}/:user when both user and vhost exist"() {
    given: "vhost hop-vhost1 exists"
    final v = "hop-vhost1"
    client.createVhost(v)
    and: "user hop-user1 exists"
    final u = "hop-user1"
    client.createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker"))

    when: "permissions of user guest in vhost / are updated"
    client.updatePermissions(v, u, new UserPermissions("read", "write", "configure"))

    and: "permissions are reloaded"
    final UserPermissions x = client.getPermissions(v, u)

    then: "a single permissions object is returned"
    x.read == "read"
    x.write == "write"
    x.configure == "configure"

    cleanup:
    client.deleteVhost(v)
    client.deleteUser(u)
  }

  def "PUT /api/permissions/{vhost}/:user when vhost DOES NOT exist"() {
    given: "vhost hop-vhost1 DOES NOT exist"
    final v = "hop-vhost1"
    client.deleteVhost(v)
    and: "user hop-user1 exists"
    final u = "hop-user1"
    client.createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker"))

    when: "permissions of user guest in vhost / are updated"
    client.updatePermissions(v, u, new UserPermissions("read", "write", "configure"))

    then: "an exception is thrown"
    final e = thrown(HttpClientErrorException)
    e.getStatusCode() == HttpStatus.BAD_REQUEST

    cleanup:
    client.deleteUser(u)
  }

  def "DELETE /api/permissions/{vhost}/:user when both vhost and username exist"() {
    given: "vhost hop-vhost1 exists"
    final v = "hop-vhost1"
    client.createVhost(v)
    and: "user hop-user1 exists"
    final u = "hop-user1"
    client.createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker"))

    and: "permissions of user guest in vhost / are set"
    client.updatePermissions(v, u, new UserPermissions("read", "write", "configure"))
    final UserPermissions x = client.getPermissions(v, u)
    x.read == "read"

    when: "permissions are cleared"
    client.clearPermissions(v, u)

    then: "no permissions are returned on reload"
    final UserPermissions y = client.getPermissions(v, u)
    y == null

    cleanup:
    client.deleteVhost(v)
    client.deleteUser(u)
  }

  def "GET /api/parameters"() {
    // TODO
  }

  def "GET /api/policies"() {
    given: "at least one policy was declared"
    final v = "/"
    final s = "hop.test"
    final d = new HashMap<String, Object>()
    final p = ".*"
    d.put("ha-mode", "all")
    client.declarePolicy(v, s, new PolicyInfo(p, 0, null, d))
    awaitEventPropagation()

    when: "client lists policies"
    final xs = client.getPolicies()

    then: "a list of policies is returned"
    final x = xs.first()
    verifyPolicyInfo(x)

    cleanup:
    client.deletePolicy(v, s)
  }

  def "GET /api/policies/{vhost} when vhost exists"() {
    given: "at least one policy was declared in vhost /"
    final v = "/"
    final s = "hop.test"
    final d = new HashMap<String, Object>()
    final p = ".*"
    d.put("ha-mode", "all")
    client.declarePolicy(v, s, new PolicyInfo(p, 0, null, d))
    awaitEventPropagation()

    when: "client lists policies"
    final xs = client.getPolicies("/")

    then: "a list of queues is returned"
    final x = xs.first()
    verifyPolicyInfo(x)

    cleanup:
    client.deletePolicy(v, s)
  }

  def "GET /api/policies/{vhost} when vhost DOES NOT exists"() {
    given: "vhost lolwut DOES not exist"
    final v = "lolwut"
    client.deleteVhost(v)
    awaitEventPropagation()

    when: "client lists policies"
    final xs = client.getPolicies(v)

    then: "null is returned"
    xs == null
  }

  def "GET /api/aliveness-test/{vhost}"() {
    when: "client performs aliveness check for the / vhost"
    final hasSucceeded = client.alivenessTest("/")

    then: "the check succeeds"
    hasSucceeded
  }

  def "GET /api/cluster-name"() {
    when: "client fetches cluster name"
    final ClusterId s = client.getClusterName()

    then: "cluster name is returned"
    s.getName() != null
  }

  def "PUT /api/cluster-name"() {
    given: "cluster name"
    final String s = client.getClusterName().name

    when: "cluster name is set to rabbit@warren"
    client.setClusterName("rabbit@warren")

    and: "cluster name is reloaded"
    final String x = client.getClusterName().name

    then: "the name is updated"
    x.equals("rabbit@warren")

    cleanup:
    client.setClusterName(s)
  }

  def "GET /api/extensions"() {
    given: "a node with the management plugin enabled"
    when: "client requests a list of (plugin) extensions"
    List<Map<String, Object>> xs = client.getExtensions()

    then: "a list of extensions is returned"
    !xs.isEmpty()
  }

  def "GET /api/definitions (version, vhosts, users, permissions)"() {
    when: "client requests the definitions"
    Definitions d = client.getDefinitions()

    then: "broker definitions are returned"
    d.getRabbitMQVersion() != null
    !d.getRabbitMQVersion().trim().isEmpty()
    !d.getVhosts().isEmpty()
    !d.getVhosts().get(0).getName().isEmpty()
    !d.getVhosts().isEmpty()
    d.getVhosts().get(0).getName() != null
    !d.getVhosts().get(0).getName().isEmpty()
    !d.getUsers().isEmpty()
    d.getUsers().get(0).getName() != null
    !d.getUsers().get(0).getName().isEmpty()
    !d.getPermissions().isEmpty()
    d.getPermissions().get(0).getUser() != null
    !d.getPermissions().get(0).getUser().isEmpty()
  }

  def "GET /api/definitions (queues)"() {
    given: "a basic topology"
    client.declareQueue("/","queue1",new QueueInfo(false,false,false))
    client.declareQueue("/","queue2",new QueueInfo(false,false,false))
    client.declareQueue("/","queue3",new QueueInfo(false,false,false))
    when: "client requests the definitions"
    Definitions d = client.getDefinitions()

    then: "broker definitions are returned"
    !d.getQueues().isEmpty()
    d.getQueues().size() >= 3
    QueueInfo q = d.getQueues().find { it.name.equals("queue1") }
    q != null
    q.vhost.equals("/")
    q.name.equals("queue1")
    !q.durable
    !q.exclusive
    !q.autoDelete

    cleanup:
    client.deleteQueue("/","queue1")
    client.deleteQueue("/","queue2")
    client.deleteQueue("/","queue3")
  }

  def "GET /api/definitions (exchanges)"() {
    given: "a basic topology"
    client.declareExchange("/", "exchange1", new ExchangeInfo("fanout", false, false))
    client.declareExchange("/", "exchange2", new ExchangeInfo("direct", false, false))
    client.declareExchange("/", "exchange3", new ExchangeInfo("topic", false, false))
    when: "client requests the definitions"
    Definitions d = client.getDefinitions()

    then: "broker definitions are returned"
    !d.getExchanges().isEmpty()
    d.getExchanges().size() >= 3
    ExchangeInfo e = d.getExchanges().find { it.name.equals("exchange1") }
    e != null
    e.vhost.equals("/")
    e.name.equals("exchange1")
    !e.durable
    !e.internal
    !e.autoDelete

    cleanup:
    client.deleteExchange("/","exchange1")
    client.deleteExchange("/","exchange2")
    client.deleteExchange("/","exchange3")
  }

  def "GET /api/definitions (bindings)"() {
    given: "a basic topology"
    client.declareQueue("/", "queue1", new QueueInfo(false, false, false))
    client.bindQueue("/", "queue1", "amq.fanout", "")
    when: "client requests the definitions"
    Definitions d = client.getDefinitions()

    then: "broker definitions are returned"
    !d.getBindings().isEmpty()
    d.getBindings().size() >= 1
    BindingInfo b = d.getBindings().find {
      it.source.equals("amq.fanout") && it.destination.equals("queue1") && it.destinationType.equals("queue")
    }
    b != null
    b.vhost.equals("/")
    b.source.equals("amq.fanout")
    b.destination.equals("queue1")
    b.destinationType.equals("queue")

    cleanup:
    client.deleteQueue("/","queue1")
  }

  protected static boolean awaitOn(CountDownLatch latch) {
    latch.await(5, TimeUnit.SECONDS)
  }

  protected static void verifyConnectionInfo(ConnectionInfo info) {
    info.port == ConnectionFactory.DEFAULT_AMQP_PORT
    !info.usesTLS
    info.peerHost.equals(info.host)
  }

  protected static void verifyChannelInfo(ChannelInfo chi, Channel ch) {
    chi.getConsumerCount() == 0
    chi.number == ch.getChannelNumber()
    chi.node.startsWith("rabbit@")
    chi.state == "running"
    !chi.usesPublisherConfirms()
    !chi.transactional
  }

  protected static void verifyVhost(VhostInfo vhi) {
    vhi.name == "/"
    !vhi.tracing
  }

  protected Connection openConnection() {
    this.cf.newConnection()
  }

  protected Connection openConnection(String username, String password) {
    final cf = new ConnectionFactory()
    cf.setUsername(username)
    cf.setPassword(password)
    cf.newConnection()
  }

  protected Connection openConnection(String clientProvidedName) {
    this.cf.newConnection(clientProvidedName)
  }

  protected static void verifyNode(NodeInfo node) {
    assert node != null
    assert node.name != null
    assert node.socketsUsed <= node.socketsTotal
    assert node.erlangProcessesUsed <= node.erlangProcessesTotal
    assert node.erlangRunQueueLength >= 0
    assert node.memoryUsed <= node.memoryLimit
  }

  protected static void verifyExchangeInfo(ExchangeInfo x) {
    assert x.type != null
    assert x.durable != null
    assert x.name != null
    assert x.autoDelete != null
  }

  protected static void verifyPolicyInfo(PolicyInfo x) {
    assert x.name != null
    assert x.vhost != null
    assert x.pattern != null
    assert x.definition != null
    assert x.applyTo != null
  }

  protected static void verifyQueueInfo(QueueInfo x) {
    assert x.name != null
    assert x.durable != null
    assert x.exclusive != null
    assert x.autoDelete != null
  }

  boolean isVersion36orLater(String currentVersion) {
    String v = currentVersion.replaceAll("\\+.*\$", "");
    v == "0.0.0" ? true : compareVersions(v, "3.6.0") >= 0
  }

  /**
   * http://stackoverflow.com/questions/6701948/efficient-way-to-compare-version-strings-in-java
   *
   */
  Integer compareVersions(String str1, String str2) {
    String[] vals1 = str1.split("\\.")
    String[] vals2 = str2.split("\\.")
    int i = 0
    // set index to first non-equal ordinal or length of shortest version string
    while (i < vals1.length && i < vals2.length && vals1[i] == vals2[i]) {
      i++
    }
    // compare first non-equal ordinal number
    if (i < vals1.length && i < vals2.length) {
      if(vals1[i].indexOf('-') != -1) {
        vals1[i] = vals1[i].substring(0,vals1[i].indexOf('-'))
      }
      if(vals2[i].indexOf('-') != -1) {
        vals2[i] = vals2[i].substring(0,vals2[i].indexOf('-'))
      }
      int diff = Integer.valueOf(vals1[i]) <=> Integer.valueOf(vals2[i])
      return Integer.signum(diff)
    }
    // the strings are equal or one string is a substring of the other
    // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
    else {
      return Integer.signum(vals1.length - vals2.length)
    }
  }

  /**
   * Statistics tables in the server are updated asynchronously,
   * in particular starting with rabbitmq/rabbitmq-management#236,
   * so in some cases we need to wait before GET'ing e.g. a newly opened connection.
   */
  protected static void awaitEventPropagation() {
    // same number as used in rabbit-hole test suite. Works OK.
    Thread.sleep(1000)
  }
}
