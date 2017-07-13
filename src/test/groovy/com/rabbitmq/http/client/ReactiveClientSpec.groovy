/*
 * Copyright 2017 the original author or authors.
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
import com.rabbitmq.http.client.domain.ConnectionInfo
import com.rabbitmq.http.client.domain.NodeInfo
import org.springframework.core.codec.DecodingException
import spock.lang.Specification

class ReactiveClientSpec extends Specification {

    protected static final String DEFAULT_USERNAME = "guest"

    protected static final String DEFAULT_PASSWORD = "guest"

    protected ReactiveClient client

    private final ConnectionFactory cf = initializeConnectionFactory()

    protected static ConnectionFactory initializeConnectionFactory() {
        final cf = new ConnectionFactory()
        cf.setAutomaticRecoveryEnabled(false)
        cf
    }

    def setup() {
        client = newLocalhostNodeClient()
    }

    protected static ReactiveClient newLocalhostNodeClient() {
        new ReactiveClient("http://127.0.0.1:15672/api/", DEFAULT_USERNAME, DEFAULT_PASSWORD)
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

    def "GET /api/nodes/{name}"() {
        when: "client retrieves a list of cluster nodes"
        final res = client.getNodes()
        final name = res.blockFirst().name
        final node = client.getNode(name).block()

        then: "the list is returned"
        res.count().block() >= 1
        verifyNode(node)
    }

    def "GET /api/connections"() {
        given: "an open RabbitMQ client connection"
        final conn = openConnection()

        when: "client retrieves a list of connections"

        final res = awaitEventPropagation({ client.getConnections() })
        final fst = res.blockFirst()

        then: "the list is returned"
        res.count().block() >= 1
        verifyConnectionInfo(fst)

        cleanup:
        conn.close()
    }

    protected static void verifyConnectionInfo(ConnectionInfo info) {
        info.port == ConnectionFactory.DEFAULT_AMQP_PORT
        !info.usesTLS
        info.peerHost.equals(info.host)
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
            while (!hasElements && n < 100) {
                Thread.sleep(100)
                result = callback()
                try {
                    // looks like Spring's Jackson2JsonDecoder doesn't like empty JSON array
                    hasElements = result?.hasElements().block()
                } catch(DecodingException e) {

                }
                n++
            }
            assert n < 100
            result
        }
        else {
            Thread.sleep(1000)
            null
        }
    }
}
