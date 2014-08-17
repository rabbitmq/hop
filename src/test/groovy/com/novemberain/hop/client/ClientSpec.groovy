package com.novemberain.hop.client

import com.novemberain.hop.client.domain.NodeInfo
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import spock.lang.Specification

class ClientSpec extends Specification {
  protected static final String DEFAULT_USERNAME = "guest"
  protected static final String DEFAULT_PASSWORD = "guest"

  protected Client client
  private final ConnectionFactory cf = new ConnectionFactory()

  def setup() {
    client = new Client("http://127.0.0.1:15672/api/", DEFAULT_USERNAME, DEFAULT_PASSWORD)
  }

  def "GET /api/overview"() {
    when: "client requests GET /api/overview"
    def res = client.getOverview()
    def xts = res.getExchangeTypes().collect { it.getName() }

    then: "the response is converted successfully"
    res.getNode().startsWith("rabbit@")
    res.getErlangVersion() != null
    res.getStatisticsDbNode().startsWith("rabbit@")

    final msgStats = res.getMessageStats()
    msgStats.basicPublish >= 0
    msgStats.basicPublishDetails.rate >= 0.0
    msgStats.publisherConfirm >= 0
    msgStats.publisherConfirmDetails.rate >= 0.0
    msgStats.basicDeliver >= 0
    msgStats.basicDeliverDetails.rate >= 0.0
    msgStats.basicReturn >= 0
    msgStats.basicReturnDetails.rate >= 0.0

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
    fst.port == ConnectionFactory.DEFAULT_AMQP_PORT
    !fst.usesTLS
    fst.peerHost.equals(fst.host)

    cleanup:
    conn.close()
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
}
