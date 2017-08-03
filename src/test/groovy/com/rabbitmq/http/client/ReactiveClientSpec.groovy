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

import com.rabbitmq.client.AuthenticationFailureException
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.http.client.domain.*
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClientException
import spock.lang.IgnoreIf
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

    def "GET /api/connections/{name}"() {
        given: "an open RabbitMQ client connection"
        final conn = openConnection()

        when: "client retrieves connection info with the correct name"

        final xs = awaitEventPropagation({ client.getConnections() })
        final x = client.getConnection(xs.blockFirst().name)

        then: "the info is returned"
        verifyConnectionInfo(x.block())

        cleanup:
        conn.close()
    }

    def "GET /api/connections/{name} with client-provided name"() {
        given: "an open RabbitMQ client connection with client-provided name"
        final s = "client-name"
        final conn = openConnection(s)

        when: "client retrieves connection info with the correct name"

        final xs = awaitEventPropagation({ client.getConnections() })
        final x = client.getConnection(xs.blockFirst().name)

        then: "the info is returned"
        verifyConnectionInfo(x.block())
        x.block().clientProperties.connectionName == s

        cleanup:
        conn.close()
    }

    def "DELETE /api/connections/{name}"() {
        given: "an open RabbitMQ client connection"
        final latch = new CountDownLatch(1)
        final conn = openConnection()
        conn.addShutdownListener({ e -> latch.countDown() })

        assert conn.isOpen()

        when: "client closes the connection"

        final xs = awaitEventPropagation({ client.getConnections() })
        xs.flatMap({ connection -> client.closeConnection(connection.name) })
          .subscribe()

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
        conn.addShutdownListener({ e -> latch.countDown() })
        assert conn.isOpen()

        when: "client closes the connection"

        final xs = awaitEventPropagation({ client.getConnections() })
        xs.flatMap({ connection -> client.closeConnection(connection.name, "because reasons!") })
          .subscribe()

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

        awaitEventPropagation({ client.getConnections() })
        final chs = awaitEventPropagation({ client.getChannels() })
        final chi = chs.blockFirst()

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

        final cn = awaitEventPropagation({ client.getConnections() }).blockFirst().name

        final chs = awaitEventPropagation({ client.getChannels(cn) })
        final chi = chs.blockFirst()

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

        awaitEventPropagation({ client.getConnections() })
        final chs = awaitEventPropagation({ client.getChannels() }).blockFirst()
        final chi = client.getChannel(chs.name).block()

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
        final vhi = vhs.blockFirst()

        then: "the info is returned"
        verifyVhost(vhi)
    }

    def "GET /api/vhosts/{name}"() {
        when: "client retrieves vhost info"
        final vhi = client.getVhost("/").block()

        then: "the info is returned"
        verifyVhost(vhi)
    }

    @IgnoreIf({ os.windows })
    def "PUT /api/vhosts/{name}"(String name) {
        when:
        "client creates a vhost named $name"
        client.createVhost(name).block()

        final vhi = client.getVhost(name).block()

        then: "the vhost is created"
        vhi.name == name

        cleanup:
        client.deleteVhost(name).block()

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
        client.createVhost(s).block()

        when: "the vhost is deleted"
        client.deleteVhost(s).block()
        client.getVhost(s).block()

        then: "it no longer exists"
        thrown(WebClientException.class)
    }

    def "DELETE /api/vhosts/{name} when vhost DOES NOT exist"() {
        given: "no vhost named hop-test-to-be-deleted"
        final s = "hop-test-to-be-deleted"

        when: "the vhost is deleted"
        final response = client.deleteVhost(s).block()

        then: "the response is 404"
        response.statusCode().value() == 404
    }

    def "GET /api/vhosts/{name}/permissions when vhost exists"() {
        when: "permissions for vhost / are listed"
        final s = "/"
        final xs = client.getPermissionsIn(s)

        then: "they include permissions for the guest user"
        UserPermissions x = xs.filter({perm -> perm.user.equals("guest")}).blockFirst()
        x.read == ".*"
    }

    def "GET /api/vhosts/{name}/permissions when vhost DOES NOT exist"() {
        when: "permissions for vhost trololowut are listed"
        final s = "trololowut"
        client.getPermissionsIn(s).blockFirst()

        then: "flux throws an exception"
        thrown(WebClientException.class)
    }

    def "GET /api/users"() {
        when: "users are listed"
        final xs = client.getUsers()
        final version = client.getOverview().block().getRabbitMQVersion()

        then: "a list of users is returned"
        final x = xs.filter( {user -> user.name.equals("guest")} ).blockFirst()
        x.name == "guest"
        x.passwordHash != null
        isVersion36orLater(version) ? x.hashingAlgorithm != null : x.hashingAlgorithm == null
        x.tags.contains("administrator")
    }

    def "GET /api/users/{name} when user exists"() {
        when: "user guest if fetched"
        final x = client.getUser("guest").block()
        final version = client.getOverview().block().getRabbitMQVersion()

        then: "user info returned"
        x.name == "guest"
        x.passwordHash != null
        isVersion36orLater(version) ? x.hashingAlgorithm != null : x.hashingAlgorithm == null
        x.tags.contains("administrator")
    }

    def "GET /api/users/{name} when user DOES NOT exist"() {
        when: "user lolwut if fetched"
        client.getUser("lolwut").block()

        then: "mono throws exception"
        thrown(WebClientException.class)
    }

    def "PUT /api/users/{name} updates user tags"() {
        given: "user alt-user"
        final u = "alt-user"
        client.deleteUser(u).subscribe( { r -> return } , { e -> return})
        client.createUser(u, u.toCharArray(), Arrays.asList("original", "management")).block()
        awaitEventPropagation()

        when: "alt-user's tags are updated"
        client.updateUser(u, u.toCharArray(), Arrays.asList("management", "updated")).block()
        awaitEventPropagation()

        and: "alt-user info is reloaded"
        final x = client.getUser(u).block()

        then: "alt-user has new tags"
        x.tags.contains("updated")
        !x.tags.contains("original")
    }

    def "DELETE /api/users/{name}"() {
        given: "user alt-user"
        final u = "alt-user"
        client.deleteUser(u).subscribe( { r -> return } , { e -> return})
        client.createUser(u, u.toCharArray(), Arrays.asList("original", "management")).block()
        awaitEventPropagation()

        when: "alt-user is deleted"
        client.deleteUser(u).block()
        awaitEventPropagation()

        and: "alt-user info is reloaded"
        client.getUser(u).block()

        // FIXME check status code, should be available in the exception
        then: "deleted user is gone"
        thrown(WebClientException.class)
    }

    def "GET /api/users/{name}/permissions when user exists"() {
        when: "permissions for user guest are listed"
        final s = "guest"
        final xs = client.getPermissionsOf(s)

        then: "they include permissions for the / vhost"
        UserPermissions x = xs.filter( { perm -> perm.user.equals("guest")}).blockFirst()
        x.read == ".*"
    }

    def "GET /api/users/{name}/permissions when user DOES NOT exist"() {
        when: "permissions for user trololowut are listed"
        final s = "trololowut"
        client.getPermissionsOf(s).blockFirst()

        then: "mono throws exception"
        thrown(WebClientException.class)
    }

    def "PUT /api/users/{name} with a blank password hash"() {
        given: "user alt-user with a blank password hash"
        final u = "alt-user"
        // blank password hash means only authentication using alternative
        // authentication mechanisms such as x509 certificates is possible. MK.
        final h = ""
        client.deleteUser(u).subscribe( { r -> return } , { e -> return})
        client.createUserWithPasswordHash(u, h.toCharArray(), Arrays.asList("original", "management")).block()
        client.updatePermissions("/", u, new UserPermissions(".*", ".*", ".*")).block()

        when: "alt-user tries to connect with a blank password"
        openConnection("alt-user", "alt-user")

        then: "connection is refused"
        // it would have a chance of being accepted if the x509 authentication mechanism was used. MK.
        thrown AuthenticationFailureException

        cleanup:
        client.deleteUser(u).block()
    }

    def "GET /api/whoami"() {
        when: "client retrieves active name authentication details"
        final res = client.whoAmI().block()

        then: "the details are returned"
        res.name == DEFAULT_USERNAME
        res.tags ==~ /administrator/
    }

    def "GET /api/permissions"() {
        when: "all permissions are listed"
        final s = "guest"
        final xs = client.getPermissions()

        then: "they include permissions for user guest in vhost /"
        final UserPermissions x = xs
                .filter( { perm -> perm.vhost.equals("/") && perm.user.equals(s)})
                .blockFirst()
        x.read == ".*"
    }

    def "GET /api/permissions/{vhost}/:user when both vhost and user exist"() {
        when: "permissions of user guest in vhost / are listed"
        final u = "guest"
        final v = "/"
        final UserPermissions x = client.getPermissions(v, u).block()

        then: "a single permissions object is returned"
        x.read == ".*"
    }

    def "GET /api/permissions/{vhost}/:user when vhost DOES NOT exist"() {
        when: "permissions of user guest in vhost lolwut are listed"
        final u = "guest"
        final v = "lolwut"
        client.getPermissions(v, u).block()

        then: "mono throws exception"
        thrown(WebClientException.class)
    }

    def "GET /api/permissions/{vhost}/:user when username DOES NOT exist"() {
        when: "permissions of user lolwut in vhost / are listed"
        final u = "lolwut"
        final v = "/"
        client.getPermissions(v, u).block()

        then: "mono throws exception"
        thrown(WebClientException.class)
    }

    def "PUT /api/permissions/{vhost}/:user when both user and vhost exist"() {
        given: "vhost hop-vhost1 exists"
        final v = "hop-vhost1"
        client.createVhost(v).block()
        and: "user hop-user1 exists"
        final u = "hop-user1"
        client.createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker")).block()

        when: "permissions of user guest in vhost / are updated"
        client.updatePermissions(v, u, new UserPermissions("read", "write", "configure"))block()

        and: "permissions are reloaded"
        final UserPermissions x = client.getPermissions(v, u).block()

        then: "a single permissions object is returned"
        x.read == "read"
        x.write == "write"
        x.configure == "configure"

        cleanup:
        client.deleteVhost(v).block()
        client.deleteUser(u).block()
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

    protected Connection openConnection(String clientProvidedName) {
        this.cf.newConnection(clientProvidedName)
    }

    protected Connection openConnection(String username, String password) {
        final cf = new ConnectionFactory()
        cf.setUsername(username)
        cf.setPassword(password)
        cf.newConnection()
    }

    protected static void verifyNode(NodeInfo node) {
        assert node != null
        assert node.name != null
        assert node.socketsUsed <= node.socketsTotal
        assert node.erlangProcessesUsed <= node.erlangProcessesTotal
        assert node.erlangRunQueueLength >= 0
        assert node.memoryUsed <= node.memoryLimit
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
    protected static Object awaitEventPropagation(Closure callback) {
        if (callback) {
            int n = 0
            def result = callback()
            def hasElements = false
            while (!hasElements && n < 100) {
                Thread.sleep(100)
                result = callback()
                hasElements = result?.hasElements().block()
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
