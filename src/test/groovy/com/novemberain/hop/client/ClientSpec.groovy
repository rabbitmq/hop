package com.novemberain.hop.client

import spock.lang.Specification

class ClientSpec extends Specification {
  protected static final String DEFAULT_USERNAME = "guest"
  protected static final String DEFAULT_PASSWORD = "guest"

  protected Client client;

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

  def "GET /nodes"() {
    when: "client retrieves a list of cluster nodes"
    final res = client.getNodes()
    final node = res.first()

    then: "the list is returned"
    res.size() == 1
    node.socketsUsed < node.socketsTotal
    node.erlangProcessesUsed < node.erlangProcessesTotal
    node.erlangRunQueueLength >= 0
    node.memoryUsed < node.memoryLimit
    !node.memoryAlarmActive
    node.diskFree > node.diskFreeLimit
    !node.diskAlarmActive
    node.authMechanisms.size() >= 1
    node.erlangApps.size() >= 1
  }
}
