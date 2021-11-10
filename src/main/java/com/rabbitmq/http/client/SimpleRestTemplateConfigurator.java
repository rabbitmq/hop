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
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link RestTemplateConfigurator} that sets an {@link SimpleClientHttpRequestFactory} on a {@link RestTemplate}.
 * <p>
 * Use this to use standard JDK facilities to create requests in the {@link Client}'s {@link RestTemplate}.
 *
 * @since 3.6.0
 * @deprecated use {@link ClientParameters#httpLayerFactory(HttpLayerFactory)} instead
 */
@Deprecated(since = "4.0.0", forRemoval = true)
public class SimpleRestTemplateConfigurator implements RestTemplateConfigurator {

    private static final HttpConnectionConfigurator NO_OP_HTTP_CONNECTION_CONFIGURATOR = c -> {
    };

    private final HttpConnectionConfigurator configurator;
    private final HostnameVerifier hostnameVerifier;
    private final SSLSocketFactory sslSocketFactory;

    /**
     * Create an instance with no TLS configuration and no {@link HttpURLConnection} post-processor.
     */
    public SimpleRestTemplateConfigurator() {
        this(null, null, NO_OP_HTTP_CONNECTION_CONFIGURATOR);
    }

    /**
     * Create an instance with a {@link HttpURLConnection} post-processor.
     *
     * @param configurator the post-processing logic to use for the {@link HttpURLConnection}
     */
    public SimpleRestTemplateConfigurator(HttpConnectionConfigurator configurator) {
        this(null, null, configurator);
    }

    /**
     * Create an instance with TLS configuration.
     *
     * @param sslSocketFactory the socket factory to use
     */
    public SimpleRestTemplateConfigurator(SSLSocketFactory sslSocketFactory) {
        this(sslSocketFactory, null, NO_OP_HTTP_CONNECTION_CONFIGURATOR);
    }

    /**
     * Create an instance with TLS configuration and a {@link HttpURLConnection} post-processor.
     *
     * @param sslSocketFactory the socket factory to use
     * @param configurator     the post-processing logic to use for the {@link HttpURLConnection}
     */
    public SimpleRestTemplateConfigurator(SSLSocketFactory sslSocketFactory, HttpConnectionConfigurator configurator) {
        this(sslSocketFactory, null, configurator);
    }

    /**
     * Create an instance with TLS configuration, including a custom hostname verifier.
     * <p>
     * Note {@link HttpsURLConnection} sets a default hostname verifier, so setting a custom
     * one is only needed for specific cases.
     *
     * @param sslSocketFactory the socket factory to use
     * @param hostnameVerifier the hostname verifier to use
     */
    public SimpleRestTemplateConfigurator(SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier) {
        this(sslSocketFactory, hostnameVerifier, NO_OP_HTTP_CONNECTION_CONFIGURATOR);
    }

    /**
     * Create an instance with TLS configuration, a custom hostname verifier, and a {@link HttpURLConnection} post-processor.
     *
     * @param sslSocketFactory the socket factory to use
     * @param hostnameVerifier the hostname verifier to use
     * @param configurator     the post-processing logic to use for the {@link HttpURLConnection}
     */
    public SimpleRestTemplateConfigurator(SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier, HttpConnectionConfigurator configurator) {
        Assert.notNull(configurator, "configurator is required; it must not be null");
        this.configurator = configurator;
        this.hostnameVerifier = hostnameVerifier;
        this.sslSocketFactory = sslSocketFactory;
    }

    @Override
    public RestTemplate configure(ClientCreationContext context) {
        RestTemplate restTemplate = context.getRestTemplate();
        ClientHttpRequestInterceptor authenticationInterceptor = new BasicAuthenticationInterceptor(
                context.getClientParameters().getUsername(), context.getClientParameters().getPassword()
        );
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        if (interceptors == null || interceptors.isEmpty()) {
            restTemplate.setInterceptors(Collections.singletonList(authenticationInterceptor));
        } else {
            interceptors = new ArrayList<>(interceptors);
            interceptors.add(authenticationInterceptor);
            restTemplate.setInterceptors(Collections.unmodifiableList(interceptors));
        }
        restTemplate.setRequestFactory(getRequestFactory());
        return restTemplate;
    }

    private ClientHttpRequestFactory getRequestFactory() {
        return new InterceptableSimpleClientRequestFactory(sslSocketFactory, hostnameVerifier, configurator);
    }

    private static class InterceptableSimpleClientRequestFactory extends SimpleClientHttpRequestFactory {

        private final HttpConnectionConfigurator configurator;
        private final SSLSocketFactory sslSocketFactory;
        private final HostnameVerifier hostnameVerifier;

        private InterceptableSimpleClientRequestFactory(SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier, HttpConnectionConfigurator configurator) {
            this.configurator = configurator;
            this.sslSocketFactory = sslSocketFactory;
            this.hostnameVerifier = hostnameVerifier;
        }

        @Override
        protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
            super.prepareConnection(connection, httpMethod);
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection securedConnection = (HttpsURLConnection) connection;
                if (this.hostnameVerifier != null) {
                    securedConnection.setHostnameVerifier(this.hostnameVerifier);
                }
                if (this.sslSocketFactory != null) {
                    securedConnection.setSSLSocketFactory(this.sslSocketFactory);
                }
            }
            configurator.configure(connection);
        }
    }

}
