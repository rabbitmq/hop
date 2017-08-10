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
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClientException
import reactor.core.publisher.Flux
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
        client.getConnections().subscribe( { c -> client.closeConnection(c.name) })
    }

    protected static ReactiveClient newLocalhostNodeClient() {
        new ReactiveClient(
                String.format("http://%s:%s@127.0.0.1:15672/api", DEFAULT_USERNAME, DEFAULT_PASSWORD)
        )
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

    def "GET /api/exchanges"() {
        when: "client retrieves the list of exchanges across all vhosts"
        final xs = client.getExchanges()
        final x = xs.blockFirst()

        then: "the list is returned"
        verifyExchangeInfo(x)
    }

    def "GET /api/exchanges/{vhost} when vhost exists"() {
        when: "client retrieves the list of exchanges in a particular vhost"
        final xs = client.getExchanges("/")

        then: "the list is returned"
        final x = xs.filter( { e -> e.name == "amq.fanout" } )
        verifyExchangeInfo(x.blockFirst())
    }

    def "GET /api/exchanges/{vhost} when vhost DOES NOT exist"() {
        given: "vhost lolwut does not exist"
        final v = "lolwut"
        client.deleteVhost(v).block()

        when: "client retrieves the list of exchanges in that vhost"
        client.getExchanges(v).blockFirst()

        then: "exception is thrown"
        // FIXME check status code
        thrown(WebClientException.class)
    }

    def "GET /api/exchanges/{vhost}/{name} when both vhost and exchange exist"() {
        when: "client retrieves exchange amq.fanout in vhost /"
        final xs = client.getExchange("/", "amq.fanout")

        then: "exchange info is returned"
        final x = xs.filter( { e -> e.name == "amq.fanout" && e.vhost == "/" })
        verifyExchangeInfo(x.blockFirst())
    }

    def "PUT /api/exchanges/{vhost}/{name} when vhost exists"() {
        given: "fanout exchange hop.test in vhost /"
        final v = "/"
        final s = "hop.test"
        client.declareExchange(v, s, new ExchangeInfo("fanout", false, false)).block()

        when: "client lists exchanges in vhost /"
        final xs = client.getExchanges(v)

        then: "hop.test is listed"
        final x = xs.filter( { e -> e.name == s } )
        verifyExchangeInfo(x.blockFirst())

        cleanup:
        client.deleteExchange(v, s).block()
    }

    def "DELETE /api/exchanges/{vhost}/{name}"() {
        given: "fanout exchange hop.test in vhost /"
        final v = "/"
        final s = "hop.test"
        client.declareExchange(v, s, new ExchangeInfo("fanout", false, false)).block()

        final xs = client.getExchanges(v)
        final x = xs.filter( { e -> e.name == s } )
        verifyExchangeInfo(x.blockFirst())

        when: "client deletes exchange hop.test in vhost /"
        client.deleteExchange(v, s).block()

        and: "exchange list in / is reloaded"
        xs = client.getExchanges(v)

        then: "hop.test no longer exists"
        !xs.filter( { e -> e.name == s } ).hasElements().block()
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
        final xs = client.getExchangeBindingsBySource("/", "")

        then: "there is an automatic binding for hop.queue1"
        final x = xs.filter( { b -> b.source == "" && b.destinationType == "queue" && b.destination == q } )
        x.hasElements().block()

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
        final x = xs.filter( { b -> b.source == src &&
                b.destinationType == "exchange" &&
                b.destination == dest
        } )
        x.hasElements().block()

        cleanup:
        ch.exchangeDelete(dest)
        conn.close()
    }

    def "GET /api/vhosts"() {
        when: "client retrieves a list of vhosts"
        final vhs = client.getVhosts()
        final vhi = vhs.blockFirst()

        then: "the info is returned"
        verifyVhost(vhi, client.getOverview().block().getRabbitMQVersion())
    }

    def "GET /api/vhosts/{name}"() {
        when: "client retrieves vhost info"
        final vhi = client.getVhost("/").block()

        then: "the info is returned"
        verifyVhost(vhi, client.getOverview().block().getRabbitMQVersion())
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
        client.updatePermissions(v, u, new UserPermissions("read", "write", "configure")).block()

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

    def "PUT /api/permissions/{vhost}/:user when vhost DOES NOT exist"() {
        given: "vhost hop-vhost1 DOES NOT exist"
        final v = "hop-vhost1"
        client.deleteVhost(v).block()
        and: "user hop-user1 exists"
        final u = "hop-user1"
        client.createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker")).block()

        when: "permissions of user guest in vhost / are updated"
        final r = client.updatePermissions(v, u, new UserPermissions("read", "write", "configure")).block()

        then: "HTTP status is 400 BAD REQUEST"
        r.statusCode() == HttpStatus.BAD_REQUEST

        cleanup:
        client.deleteUser(u).block()
    }

    def "DELETE /api/permissions/{vhost}/:user when both vhost and username exist"() {
        given: "vhost hop-vhost1 exists"
        final v = "hop-vhost1"
        client.createVhost(v).block()
        and: "user hop-user1 exists"
        final u = "hop-user1"
        client.createUser(u, "test".toCharArray(), Arrays.asList("management", "http", "policymaker")).block()

        and: "permissions of user guest in vhost / are set"
        client.updatePermissions(v, u, new UserPermissions("read", "write", "configure")).block()
        final UserPermissions x = client.getPermissions(v, u).block()
        x.read == "read"

        when: "permissions are cleared"
        client.clearPermissions(v, u).block()

        client.getPermissions(v, u).block()

        then: "an exception is thrown on reload"
        // FIXME check status
        thrown(WebClientException.class)

        cleanup:
        client.deleteVhost(v).block()
        client.deleteUser(u).block()
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
        client.declarePolicy(v, s, new PolicyInfo(p, 0, null, d)).block()

        when: "client lists policies"
        final xs = awaitEventPropagation({ client.getPolicies() })

        then: "a list of policies is returned"
        final x = xs.blockFirst()
        verifyPolicyInfo(x)

        cleanup:
        client.deletePolicy(v, s).block()
    }

    def "GET /api/policies/{vhost} when vhost exists"() {
        given: "at least one policy was declared in vhost /"
        final v = "/"
        final s = "hop.test"
        final d = new HashMap<String, Object>()
        final p = ".*"
        d.put("ha-mode", "all")
        client.declarePolicy(v, s, new PolicyInfo(p, 0, null, d)).block()

        when: "client lists policies"
        final xs = awaitEventPropagation({ client.getPolicies("/") })

        then: "a list of queues is returned"
        final x = xs.blockFirst()
        verifyPolicyInfo(x)

        cleanup:
        client.deletePolicy(v, s).block()
    }

    def "GET /api/policies/{vhost} when vhost DOES NOT exists"() {
        given: "vhost lolwut DOES not exist"
        final v = "lolwut"
        client.deleteVhost(v).block()

        when: "client lists policies"
        awaitEventPropagation({ client.getPolicies(v) })

        then: "exception is thrown"
        // FIXME check status is 404
        thrown(WebClientException.class)
    }

    def "GET /api/aliveness-test/{vhost}"() {
        when: "client performs aliveness check for the / vhost"
        final hasSucceeded = client.alivenessTest("/").block().isSuccessful()

        then: "the check succeeds"
        hasSucceeded
    }

    def "GET /api/cluster-name"() {
        when: "client fetches cluster name"
        final ClusterId s = client.getClusterName().block()

        then: "cluster name is returned"
        s.getName() != null
    }

    def "PUT /api/cluster-name"() {
        given: "cluster name"
        final String s = client.getClusterName().block().name

        when: "cluster name is set to rabbit@warren"
        client.setClusterName("rabbit@warren").block()

        and: "cluster name is reloaded"
        final String x = client.getClusterName().block().name

        then: "the name is updated"
        x.equals("rabbit@warren")

        cleanup:
        client.setClusterName(s).block()
    }

    def "GET /api/extensions"() {
        given: "a node with the management plugin enabled"
        when: "client requests a list of (plugin) extensions"
        Flux<Map<String, Object>> xs = client.getExtensions()

        then: "a list of extensions is returned"
        xs.hasElements().block()
    }

    def "GET /api/definitions (version, vhosts, users, permissions)"() {
        when: "client requests the definitions"
        Definitions d = client.getDefinitions().block()

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

    def "GET /api/queues"() {
        given: "at least one queue was declared"
        final Connection conn = cf.newConnection()
        final Channel ch = conn.createChannel()
        final String q = ch.queueDeclare().queue

        when: "client lists queues"
        final xs = client.getQueues()

        then: "a list of queues is returned"
        final x = xs.blockFirst()
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
        final x = xs.blockFirst()
        verifyQueueInfo(x)

        cleanup:
        ch.queueDelete(q)
        conn.close()
    }

    def "GET /api/queues/{vhost} when vhost DOES NOT exist"() {
        given: "vhost lolwut DOES not exist"
        final v = "lolwut"
        client.deleteVhost(v).block()

        when: "client lists queues"
        client.getQueues(v).blockFirst()

        then: "exception is thrown"
        // FIXME check status code is 404
        thrown(WebClientException.class)
    }

    def "GET /api/queues/{vhost}/{name} when both vhost and queue exist"() {
        given: "a queue was declared in vhost /"
        final Connection conn = cf.newConnection()
        final Channel ch = conn.createChannel()
        final String q = ch.queueDeclare().queue

        when: "client fetches info of the queue"
        final x = client.getQueue("/", q).block()

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
        final x = client.getQueue("/", q).block()

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
        final x = client.getQueue("/", q).block()

        then: "exception is thrown"
        // FIXME check status code is 404
        thrown(WebClientException.class)

        cleanup:
        ch.queueDelete(q)
        conn.close()
    }

    def "PUT /api/queues/{vhost}/{name} when vhost exists"() {
        given: "vhost /"
        final v = "/"

        when: "client declares a queue hop.test"
        final s = "hop.test"
        client.declareQueue(v, s, new QueueInfo(false, false, false)).block()

        and: "client lists queues in vhost /"
        Flux<QueueInfo> xs = client.getQueues(v)

        then: "hop.test is listed"
        QueueInfo x = xs.filter( { q -> q.name.equals(s) } ).blockFirst()
        x != null
        x.vhost.equals(v)
        x.name.equals(s)
        !x.durable
        !x.exclusive
        !x.autoDelete

        cleanup:
        client.deleteQueue(v, s).block()
    }

    def "PUT /api/policies/{vhost}/{name}"() {
        given: "vhost / and definition"
        final v = "/"
        final d = new HashMap<String, Object>()
        d.put("ha-mode", "all")

        when: "client declares a policy hop.test"
        final s = "hop.test"
        client.declarePolicy(v, s, new PolicyInfo(".*", 1, null, d)).block()

        and: "client lists policies in vhost /"
        Flux<PolicyInfo> ps = client.getPolicies(v)

        then: "hop.test is listed"
        PolicyInfo p = ps.filter( { p -> p.name.equals(s) } ).blockFirst()
        p != null
        p.vhost.equals(v)
        p.name.equals(s)
        p.priority.equals(1)
        p.applyTo.equals("all")
        p.definition.equals(d)

        cleanup:
        client.deletePolicy(v, s).block()
    }

    def "PUT /api/queues/{vhost}/{name} when vhost DOES NOT exist"() {
        given: "vhost lolwut which does not exist"
        final v = "lolwut"
        client.deleteVhost(v).block()

        when: "client declares a queue hop.test"
        final s = "hop.test"
        ClientResponse r = client.declareQueue(v, s, new QueueInfo(false, false, false)).block()

        then: "status code is 404"
        r.statusCode() == HttpStatus.NOT_FOUND
    }

    def "DELETE /api/queues/{vhost}/{name}"() {
        final String s = UUID.randomUUID().toString()
        given: "queue ${s} in vhost /"
        final v = "/"
        client.declareQueue(v, s, new QueueInfo(false, false, false)).block()

        Flux<QueueInfo> xs = client.getQueues(v)
        QueueInfo x = xs.filter( { q -> q.name.equals(s) } ).blockFirst()
        x != null
        verifyQueueInfo(x)

        when: "client deletes queue ${s} in vhost /"
        client.deleteQueue(v, s).block()

        and: "queue list in / is reloaded"
        xs = client.getQueues(v)

        then: "${s} no longer exists"
        !xs.filter( { q -> q.name.equals(s) } ).hasElements().block()
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
        final Flux<BindingInfo> xs = client.getBindings()

        then: "amq.fanout bindings are listed"
        xs.filter( { b -> b.destinationType.equals("queue") && b.source.equals(x) } )
                .toStream().count() >= 3

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
        final Flux<BindingInfo> xs = client.getBindings("/")

        then: "amq.fanout bindings are listed"
        xs.filter( { b -> b.destinationType.equals("queue") && b.source.equals(x) } )
                .count().block() >= 2

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
        final Flux<BindingInfo> xs = client.getBindings("/")

        then: "the amq.fanout binding is listed"
        xs.filter( { b -> b.destinationType.equals("queue") &&
                             b.source.equals(x) &&
                             b.destination.equals(q) } ).count().block() == 1

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
        final Flux<BindingInfo> xs = client.getQueueBindings("/", q)

        then: "the amq.fanout binding is listed"
        xs.filter( { b-> b.destinationType.equals("queue") &&
                         b.source.equals(x) &&
                         b.destination.equals(q) } ).count().block() == 1

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
        final Flux<BindingInfo> xs = client.getQueueBindingsBetween("/", x, q)

        then: "the amq.fanout binding is listed"
        final b = xs.blockFirst()
        xs.count().block() == 1
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
        final Flux<BindingInfo> xs = client.getExchangeBindingsBetween("/", s, d)

        then: "the amq.topic binding is listed"
        final b = xs.blockFirst()
        xs.count().block() == 1
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
        client.deleteExchange(v, d).block()
        client.declareExchange(v, d, new ExchangeInfo("fanout", false, false)).block()
        client.bindExchange(v, d, s, "").block()

        when: "bindings between hop.test and amq.fanout are listed"
        final Flux<BindingInfo> xs = client.getExchangeBindingsBetween(v, s, d)

        then: "the amq.fanout binding is listed"
        final b = xs.blockFirst()
        xs.count().block() == 1
        b.source.equals(s)
        b.destination.equals(d)
        b.destinationType.equals("exchange")

        cleanup:
        client.deleteExchange(v, d).block()
    }

    def "POST /api/bindings/{vhost}/e/:exchange/q/:queue"() {
        given: "queues hop.test bound to amq.topic in vhost /"
        final v = "/"
        final String x  = 'amq.topic'
        final String q  = "hop.test"
        client.declareQueue(v, q, new QueueInfo(false, false, false)).block()
        client.bindQueue(v, q, x, "").block()

        when: "bindings between hop.test and amq.topic are listed"
        final Flux<BindingInfo> xs = client.getQueueBindingsBetween(v, x, q)

        then: "the amq.fanout binding is listed"
        final b = xs.blockFirst()
        xs.count().block() == 1
        b.source.equals(x)
        b.destination.equals(q)
        b.destinationType.equals("queue")

        cleanup:
        client.deleteQueue(v, q).block()
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
        client.purgeQueue("/", q).block()

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

    def "GET /api/definitions (queues)"() {
        given: "a basic topology"
        client.declareQueue("/","queue1",new QueueInfo(false,false,false)).block()
        client.declareQueue("/","queue2",new QueueInfo(false,false,false)).block()
        client.declareQueue("/","queue3",new QueueInfo(false,false,false)).block()
        when: "client requests the definitions"
        Definitions d = client.getDefinitions().block()

        then: "broker definitions are returned"
        !d.getQueues().isEmpty()
        d.getQueues().size() >= 3
        QueueInfo q = d.getQueues().find { it.name.equals("queue1") && it.vhost.equals("/") }
        q != null
        q.vhost.equals("/")
        q.name.equals("queue1")
        !q.durable
        !q.exclusive
        !q.autoDelete

        cleanup:
        client.deleteQueue("/","queue1").block()
        client.deleteQueue("/","queue2").block()
        client.deleteQueue("/","queue3").block()
    }

    def "GET /api/definitions (exchanges)"() {
        given: "a basic topology"
        client.declareExchange("/", "exchange1", new ExchangeInfo("fanout", false, false)).block()
        client.declareExchange("/", "exchange2", new ExchangeInfo("direct", false, false)).block()
        client.declareExchange("/", "exchange3", new ExchangeInfo("topic", false, false)).block()
        when: "client requests the definitions"
        Definitions d = client.getDefinitions().block()

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
        client.deleteExchange("/","exchange1").block()
        client.deleteExchange("/","exchange2").block()
        client.deleteExchange("/","exchange3").block()
    }

    def "GET /api/definitions (bindings)"() {
        given: "a basic topology"
        client.declareQueue("/", "queue1", new QueueInfo(false, false, false)).block()
        client.bindQueue("/", "queue1", "amq.fanout", "").block()
        when: "client requests the definitions"
        Definitions d = client.getDefinitions().block()

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
        client.deleteQueue("/","queue1").block()
    }

    def "GET /api/parameters/shovel"() {
        given: "a basic topology"
        ShovelDetails value = new ShovelDetails("amqp://localhost:5672/vh1", "amqp://localhost:5672/vh2", 30, true, null);
        value.setSourceQueue("queue1");
        value.setDestinationExchange("exchange1");
        client.declareShovel("/", new ShovelInfo("shovel1", value)).block()
        when: "client requests the shovels"
        final shovels = awaitEventPropagation { client.getShovels() }

        then: "broker definitions are returned"
        shovels.hasElements().block()
        ShovelInfo s = shovels.filter( { s -> s.name.equals("shovel1") } ).blockFirst()
        s != null
        s.name.equals("shovel1")
        s.virtualHost.equals("/")
        s.details.sourceURI.equals("amqp://localhost:5672/vh1")
        s.details.sourceExchange == null
        s.details.sourceQueue.equals("queue1")
        s.details.destinationURI.equals("amqp://localhost:5672/vh2")
        s.details.destinationExchange.equals("exchange1")
        s.details.destinationQueue == null
        s.details.reconnectDelay == 30
        s.details.addForwardHeaders
        s.details.publishProperties == null

        cleanup:
        client.deleteShovel("/","shovel1").block()
    }

    def "GET /api/shovels"() {
        given: "a basic topology"
        ShovelDetails value = new ShovelDetails("amqp://localhost:5672/vh1", "amqp://localhost:5672/vh2", 30, true, null);
        value.setSourceQueue("queue1");
        value.setDestinationExchange("exchange1");
        client.declareShovel("/", new ShovelInfo("shovel1", value)).block()
        when: "client requests the shovels status"
        final shovels = awaitEventPropagation { client.getShovelsStatus() }

        then: "shovels status are returned"
        shovels.hasElements().block()
        ShovelStatus s = shovels.filter( { s -> s.name.equals("shovel1") } ).blockFirst()
        s != null
        s.name.equals("shovel1")
        s.virtualHost.equals("/")
        s.type.equals("dynamic")
        s.state != null
        s.sourceURI == null
        s.destinationURI == null

        cleanup:
        client.deleteShovel("/","shovel1").block()
    }

    protected static boolean awaitOn(CountDownLatch latch) {
        latch.await(5, TimeUnit.SECONDS)
    }

    protected static void verifyConnectionInfo(ConnectionInfo info) {
        assert info.port == ConnectionFactory.DEFAULT_AMQP_PORT
        assert !info.usesTLS
        assert info.peerHost.equals(info.host)
    }

    protected static void verifyChannelInfo(ChannelInfo chi, Channel ch) {
        assert chi.getConsumerCount() == 0
        assert chi.number == ch.getChannelNumber()
        assert chi.node.startsWith("rabbit@")
        assert chi.state == "running"
        assert !chi.usesPublisherConfirms()
        assert !chi.transactional
    }

    protected static void verifyVhost(VhostInfo vhi, String version) {
        assert vhi.name == "/"
        assert !vhi.tracing
        assert isVersion37orLater(version) ? vhi.clusterState != null : vhi.clusterState == null
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

    static boolean isVersion36orLater(String currentVersion) {
        String v = currentVersion.replaceAll("\\+.*\$", "");
        v == "0.0.0" ? true : compareVersions(v, "3.6.0") >= 0
    }

    static boolean isVersion37orLater(String currentVersion) {
        String v = currentVersion.replaceAll("\\+.*\$", "");
        v == "0.0.0" ? true : compareVersions(v, "3.7.0") >= 0
    }

    /**
     * http://stackoverflow.com/questions/6701948/efficient-way-to-compare-version-strings-in-java
     *
     */
    static Integer compareVersions(String str1, String str2) {
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
