/*
 * Copyright 2018 the original author or authors.
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

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.http.client.domain.NodeInfo
import com.rabbitmq.http.client.domain.PolicyInfo
import spock.lang.Specification

class ReactorNettyClientSpec extends Specification {

    protected ReactorNettyClient client

    private final ConnectionFactory cf = initializeConnectionFactory()

    protected static ConnectionFactory initializeConnectionFactory() {
        final cf = new ConnectionFactory()
        cf.setAutomaticRecoveryEnabled(false)
        cf
    }

    def setup() {
        client = newLocalhostNodeClient()
    }

    protected static ReactorNettyClient newLocalhostNodeClient() {
        new ReactorNettyClient()
    }

    def "GET /api/overview"() {
        when: "client requests GET /api/overview"
        final conn = openConnection()
        final ch = conn.createChannel()
        1000.times { ch.basicPublish("", "", null, null) }

        def res = client.getOverview().block()
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
        final node = res.blockFirst()

        then: "the list is returned"
        res.count().block() >= 1
        verifyNode(node)
    }

    def "GET /api/policies"() {
        given: "at least one policy was declared"
        final v = "/"
        final s = "hop.test"
        final d = new HashMap<String, Object>()
        final p = ".*"
        d.put("ha-mode", "all")
        client.declarePolicy(v, s, new PolicyInfo(p, 0, null, d)).block()

        when: "client lists policies"
        final xs = awaitEventPropagation({ client.getPolicies() })

        then: "a list of policies is returned"
        final x = xs.blockFirst()
        verifyPolicyInfo(x)

        cleanup:
        client.deletePolicy(v, s).block()
    }

    protected Connection openConnection() {
        this.cf.newConnection()
    }

    protected static void verifyNode(NodeInfo node) {
        assert node != null
        assert node.name != null
        assert node.socketsUsed <= node.socketsTotal
        assert node.erlangProcessesUsed <= node.erlangProcessesTotal
        assert node.erlangRunQueueLength >= 0
        assert node.memoryUsed <= node.memoryLimit
    }

    protected static void verifyPolicyInfo(PolicyInfo x) {
        assert x.name != null
        assert x.vhost != null
        assert x.pattern != null
        assert x.definition != null
        assert x.applyTo != null
    }

    /**
     * Statistics tables in the server are updated asynchronously,
     * in particular starting with rabbitmq/rabbitmq-management#236,
     * so in some cases we need to wait before GET'ing e.g. a newly opened connection.
     */
    protected static Object awaitEventPropagation(Closure callback) {
        if (callback) {
            int n = 0
            def result = callback()
            def hasElements = false
            while (!hasElements && n < 10000) {
                Thread.sleep(100)
                n += 100
                result = callback()
                hasElements = result?.hasElements().block()
            }
            assert n < 10000
            result
        } else {
            Thread.sleep(1000)
            null
        }
    }

}
