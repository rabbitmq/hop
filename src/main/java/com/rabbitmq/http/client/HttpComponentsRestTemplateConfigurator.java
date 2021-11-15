/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rabbitmq.http.client;

import com.rabbitmq.http.client.HttpLayer.HttpLayerFactory;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URL;

/**
 * {@link RestTemplateConfigurator} that sets an {@link HttpComponentsClientHttpRequestFactory} on a {@link RestTemplate}.
 * <p>
 * Use this to use Apache HttpComponents HTTP Client to create requests in the {@link Client}'s {@link RestTemplate}.
 *
 * @see HttpComponentsClientHttpRequestFactory
 * @since 3.6.0
 * @deprecated use {@link ClientParameters#httpLayerFactory(HttpLayerFactory)} instead
 */
@Deprecated(since = "4.0.0", forRemoval = true)
public class HttpComponentsRestTemplateConfigurator implements RestTemplateConfigurator {

    private static final HttpClientBuilderConfigurator NO_OP_HTTP_CLIENT_BUILDER_CONFIGURATOR = builder -> builder;

    private final HttpClientBuilderConfigurator configurator;
    private final SSLConnectionSocketFactory sslConnectionSocketFactory;
    private final SSLContext sslContext;

    /**
     * Create an instance with no TLS configuration and no {@link HttpClientBuilder} post-processor.
     */
    public HttpComponentsRestTemplateConfigurator() {
        this(null, null, NO_OP_HTTP_CLIENT_BUILDER_CONFIGURATOR);
    }

    /**
     * Create an instance with a {@link HttpClientBuilder} post-processor.
     *
     * @param configurator the post-processing logic to use for the {@link HttpClientBuilder}
     */
    public HttpComponentsRestTemplateConfigurator(HttpClientBuilderConfigurator configurator) {
        this(null, null, configurator);
    }

    /**
     * Create an instance with TLS configuration.
     *
     * @param sslConnectionSocketFactory socket factory to use
     * @param sslContext                 SSL context to use
     * @see HttpClientBuilder#setSSLSocketFactory(LayeredConnectionSocketFactory)
     * @see HttpClientBuilder#setSSLContext(SSLContext)
     */
    public HttpComponentsRestTemplateConfigurator(SSLConnectionSocketFactory sslConnectionSocketFactory, SSLContext sslContext) {
        this(sslConnectionSocketFactory, sslContext, NO_OP_HTTP_CLIENT_BUILDER_CONFIGURATOR);
    }

    /**
     * Create an instance with TLS configuration and a {@link HttpClientBuilder} post-processor.
     *
     * @param sslConnectionSocketFactory socket factory to use
     * @param sslContext                 SSL context to use
     * @param configurator               the post-processing logic to use for the {@link HttpClientBuilder}
     * @see HttpClientBuilder#setSSLSocketFactory(LayeredConnectionSocketFactory)
     * @see HttpClientBuilder#setSSLContext(SSLContext)
     */
    public HttpComponentsRestTemplateConfigurator(SSLConnectionSocketFactory sslConnectionSocketFactory, SSLContext sslContext, HttpClientBuilderConfigurator configurator) {
        Assert.notNull(configurator, "configurator is required; it must not be null");
        this.sslConnectionSocketFactory = sslConnectionSocketFactory;
        this.sslContext = sslContext;
        this.configurator = configurator;
    }

    @Override
    public RestTemplate configure(ClientCreationContext context) {
        RestTemplate restTemplate = context.getRestTemplate();
        ClientHttpRequestFactory requestFactory = getRequestFactory(
                context.getClientParameters().getUrl(),
                context.getRootUri(),
                context.getClientParameters().getUsername(),
                context.getClientParameters().getPassword(),
                this.sslConnectionSocketFactory,
                this.sslContext,
                configurator
        );
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }

    private ClientHttpRequestFactory getRequestFactory(final URL url,
                                                       final URI rootUri,
                                                       final String username, final String password,
                                                       final SSLConnectionSocketFactory sslConnectionSocketFactory,
                                                       final SSLContext sslContext,
                                                       final HttpClientBuilderConfigurator configurator) {
        String theUser = username;
        String thePassword = password;
        String userInfo = url.getUserInfo();
        if (userInfo != null && theUser == null) {
            String[] userParts = userInfo.split(":");
            if (userParts.length > 0) {
                theUser = Utils.decode(userParts[0]);
            }
            if (userParts.length > 1) {
                thePassword = Utils.decode(userParts[1]);
            }
        }

        // configure HttpClientBuilder essentials
        final HttpClientBuilder bldr = HttpClientBuilder.create().
                setDefaultCredentialsProvider(getCredentialsProvider(theUser, thePassword));
        if (sslConnectionSocketFactory != null) {
            bldr.setSSLSocketFactory(sslConnectionSocketFactory);
        }
        if (sslContext != null) {
            bldr.setSSLContext(sslContext);
        }

        HttpClient httpClient;
        // this lets the user perform non-essential configuration (e.g. timeouts)
        // but reduces the risk of essentials not being set. MK.
        HttpClientBuilder b = configurator.configure(bldr);
        httpClient = b.build();

        // RabbitMQ HTTP API currently does not support challenge/response for PUT methods.
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicScheme = new BasicScheme();
        authCache.put(new HttpHost(rootUri.getHost(), rootUri.getPort(), rootUri.getScheme()), basicScheme);
        final HttpClientContext ctx = HttpClientContext.create();
        ctx.setAuthCache(authCache);
        return new HttpComponentsClientHttpRequestFactory(httpClient) {
            @Override
            protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
                return ctx;
            }
        };
    }

    private CredentialsProvider getCredentialsProvider(final String username, final String password) {
        CredentialsProvider cp = new BasicCredentialsProvider();
        cp.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(username, password));

        return cp;
    }

}
