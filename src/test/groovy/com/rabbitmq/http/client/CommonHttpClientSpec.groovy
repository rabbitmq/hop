package com.rabbitmq.http.client

import org.apache.http.HttpRequestInterceptor
import org.apache.http.auth.AuthScope
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.protocol.HttpContext

import java.util.concurrent.atomic.AtomicReference

class CommonHttpClientSpec extends ClientSpec {

    @Override
    protected Client newLocalhostNodeClient() {
        new Client("http://127.0.0.1:" + managementPort() + "/api/", DEFAULT_USERNAME, DEFAULT_PASSWORD)
    }

    @Override
    protected Client newLocalhostNodeClientWithConfiguration() {
        new Client("http://127.0.0.1:" + managementPort() + "/api/", DEFAULT_USERNAME, DEFAULT_PASSWORD, { builder -> builder.setMaxConnTotal(8192) })
    }

    @Override
    protected Client newLocalhostNodeClientWithCredentialsInUrl() {
        new Client("http://" + DEFAULT_USERNAME + ":" + DEFAULT_PASSWORD + "@127.0.0.1:" + managementPort() + "/api/")
    }

    def "user info decoding"() {
        when: "username and password are encoded in the URL"
        def usernamePassword = new AtomicReference<>()
        def localClient = new Client("http://test+user:test%40password@localhost:" + managementPort() + "/api/", { builder ->
            builder.addInterceptorLast(new HttpRequestInterceptor() {
                @Override
                void process(org.apache.http.HttpRequest request, HttpContext context) throws org.apache.http.HttpException, IOException {
                    HttpClientContext httpCtx = (HttpContext) context
                    def credentials = httpCtx.getCredentialsProvider().getCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT))
                    usernamePassword.set(credentials.getUserPrincipal().name + ":" + credentials.getPassword())
                }
            })
            return builder
        }
        )

        try {
            localClient.getOverview()
        } catch (Exception e) {
            // OK
        }

        then: "username and password are decoded before going into the request"
        usernamePassword.get() == "test user:test@password"
    }
}
