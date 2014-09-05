package com.novemberain.hop.client

import com.novemberain.hop.client.domain.*
import com.rabbitmq.client.*
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ClientSpec extends Specification {
  protected static final String DEFAULT_USERNAME = "guest"
  protected static final String DEFAULT_PASSWORD = "guest"

  protected Client client
  private final ConnectionFactory cf = initializeConnectionFactory()

  protected ConnectionFactory initializeConnectionFactory() {
    final cf = new ConnectionFactory()
    cf.setAutomaticRecoveryEnabled(false)
    cf
  }

  def setup() {
    client = new Client("http://127.0.0.1:15672/api/", DEFAULT_USERNAME, DEFAULT_PASSWORD)
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
    res.getStatisticsDbNode().startsWith("rabbit@")

    final msgStats = res.getMessageStats()
    msgStats.basicPublish >= 0
    msgStats.publisherConfirm >= 0
    msgStats.basicDeliver >= 0
    msgStats.basicReturn >= 0

    final qTotals = res.getQueueTotals()
    qTotals.messages >= 0
    qTotals.messagesReady >= 0
    qTotals.messagesUnacknowledged >= 0

    final oTotals = res.getObjectTotals();
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

  def "GET /api/aliveness-test/{vhost}"() {
    when: "client performs aliveness check for the / vhost"
    final hasSucceeded = client.alivenessTest("/")

    then: "the check succeeds"
    hasSucceeded
  }

  def "GET /api/whoami"() {
    when: "client retrieves active name authentication details"
    final res = client.whoAmI()

    then: "the details are returned"
    res.name == DEFAULT_USERNAME
    res.tags ==~ /administrator/
  }

  def "GET /api/nodes"() {
    when: "client retrieves a list of cluster nodes"
    final res = client.getNodes()
    final node = res.first()

    then: "the list is returned"
    res.size() == 1
    verifyNode(node)
  }

  def "GET /api/nodes/{name}"() {
    when: "client retrieves a list of cluster nodes"
    final res = client.getNodes()
    final name = res.first().name
    final node = client.getNode(name)

    then: "the list is returned"
    res.size() == 1
    verifyNode(node)
  }

  def "GET /api/connections"() {
    given: "an open RabbitMQ client connection"
    final conn = openConnection()

    when: "client retrieves a list of connections"
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
    final xs = client.getConnections()
    final x = client.getConnection(xs.first().name)

    then: "the info is returned"
    verifyConnectionInfo(x)

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

  def "GET /api/channels"() {
    given: "an open RabbitMQ client connection with 1 channel"
    final conn = openConnection()
    final ch = conn.createChannel()

    when: "client lists channels"
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
    final chs = client.getChannels()
    final chi = client.getChannel(chs.first().name)

    then: "the info is returned"
    verifyChannelInfo(chi, ch)

    cleanup:
    if (conn.isOpen()) {
      conn.close()
    }
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

  def "GET /api/exchanges"() {
    when: "client retrieves the list of exchanges across all vhosts"
    final xs = client.getExchanges()
    final x = xs.first()

    then: "the list is returned"
    verifyExchangeInfo(x)
  }

  def "GET /api/exchanges/{vhost}"() {
    when: "client retrieves the list of exchanges in a particular vhost"
    final xs = client.getExchanges("/")

    then: "the list is returned"
    final x = xs.find { it.name == "amq.fanout" }
    verifyExchangeInfo(x)
  }

  protected boolean awaitOn(CountDownLatch latch) {
    latch.await(5, TimeUnit.SECONDS)
  }

  protected void verifyConnectionInfo(ConnectionInfo info) {
    info.port == ConnectionFactory.DEFAULT_AMQP_PORT
    !info.usesTLS
    info.peerHost.equals(info.host)
  }

  protected void verifyChannelInfo(ChannelInfo chi, Channel ch) {
    chi.getConsumerCount() == 0
    chi.number == ch.getChannelNumber()
    chi.node.startsWith("rabbit@")
    chi.state == "running"
    !chi.usesPublisherConfirms()
    !chi.transactional
  }

  protected void verifyVhost(VhostInfo vhi) {
    vhi.name == "/"
    !vhi.tracing
  }

  protected Connection openConnection() {
    this.cf.newConnection()
  }

  protected void verifyNode(NodeInfo node) {
    assert node.name != null
    assert node.type == "disc"
    assert node.isDiskNode()
    assert node.socketsUsed < node.socketsTotal
    assert node.erlangProcessesUsed < node.erlangProcessesTotal
    assert node.erlangRunQueueLength >= 0
    assert node.memoryUsed < node.memoryLimit
    assert !node.memoryAlarmActive
    assert node.diskFree > node.diskFreeLimit
    assert !node.diskAlarmActive
    assert node.authMechanisms.size() >= 1
    assert node.erlangApps.size() >= 1
  }

  protected void verifyExchangeInfo(ExchangeInfo x) {
    assert x.type != null
    assert x.durable != null
    assert x.name != null
    assert x.autoDelete != null
  }
}
