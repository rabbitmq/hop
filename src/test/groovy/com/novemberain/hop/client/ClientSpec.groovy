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

    xts.contains("topic")
    xts.contains("fanout")
    xts.contains("direct")
    xts.contains("headers")
  }
}
