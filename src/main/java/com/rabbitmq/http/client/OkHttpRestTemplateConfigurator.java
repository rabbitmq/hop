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
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.net.URL;

/**
 * {@link RestTemplateConfigurator} that sets an {@link OkHttp3ClientHttpRequestFactory} on a {@link RestTemplate}.
 * <p>
 * Use this to use OkHttp to create requests in the {@link Client}'s {@link RestTemplate}.
 * @deprecated use {@link ClientParameters#httpLayerFactory(HttpLayerFactory)} instead
 */
@Deprecated(since = "4.0.0", forRemoval = true)
public class OkHttpRestTemplateConfigurator implements RestTemplateConfigurator {

    private static final OkHttpClientBuilderConfigurator NO_OP_HTTP_CLIENT_BUILDER_CONFIGURATOR = builder -> builder;

    private final OkHttpClientBuilderConfigurator configurator;
    private final SSLSocketFactory sslSocketFactory;
    private final X509TrustManager trustManager;

    /**
     * Create an instance with no TLS configuration and no {@link OkHttpClient.Builder} post-processor.
     */
    public OkHttpRestTemplateConfigurator() {
        this(NO_OP_HTTP_CLIENT_BUILDER_CONFIGURATOR);
    }

    /**
     * Create an instance with a {@link OkHttpClient.Builder} post-processor.
     *
     * @param configurator the post-processing logic to use for the {@link okhttp3.OkHttpClient.Builder}
     */
    public OkHttpRestTemplateConfigurator(OkHttpClientBuilderConfigurator configurator) {
        this(null, null, configurator);
    }

    /**
     * Create an instance with TLS configuration.
     *
     * @param sslSocketFactory the socket factory to use
     * @param trustManager     the trust manager to use
     * @see okhttp3.OkHttpClient.Builder#sslSocketFactory(SSLSocketFactory, X509TrustManager)
     */
    public OkHttpRestTemplateConfigurator(SSLSocketFactory sslSocketFactory,
                                          X509TrustManager trustManager) {
        this(sslSocketFactory, trustManager, NO_OP_HTTP_CLIENT_BUILDER_CONFIGURATOR);
    }

    /**
     * Create an instance with TLS configuration and a {@link OkHttpClient.Builder} post-processor.
     *
     * @param sslSocketFactory the socket factory to use
     * @param trustManager     the trust manager to use
     * @param configurator     the post-processing logic to use for the {@link okhttp3.OkHttpClient.Builder}
     * @see okhttp3.OkHttpClient.Builder#sslSocketFactory(SSLSocketFactory, X509TrustManager)
     */
    public OkHttpRestTemplateConfigurator(SSLSocketFactory sslSocketFactory,
                                          X509TrustManager trustManager,
                                          OkHttpClientBuilderConfigurator configurator) {
        Assert.notNull(configurator, "configurator is required; it must not be null");
        this.sslSocketFactory = sslSocketFactory;
        this.trustManager = trustManager;
        this.configurator = configurator;
    }

    @Override
    public RestTemplate configure(ClientCreationContext context) {
        RestTemplate restTemplate = context.getRestTemplate();
        ClientHttpRequestFactory requestFactory = getRequestFactory(
                context.getClientParameters().getUrl(),
                context.getClientParameters().getUsername(),
                context.getClientParameters().getPassword(),
                sslSocketFactory,
                trustManager,
                configurator
        );
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }

    private ClientHttpRequestFactory getRequestFactory(final URL url,
                                                       final String username, final String password,
                                                       final SSLSocketFactory sslSocketFactory,
                                                       final X509TrustManager trustManager,
                                                       final OkHttpClientBuilderConfigurator configurator) {

        Assert.notNull(configurator, "configurator is required; it must not be null");

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
        final String credentials = Credentials.basic(theUser, thePassword);

        // configure OkHttpClient.Builder essentials
        final OkHttpClient.Builder bldr = new OkHttpClient.Builder()
                .authenticator((route, response) -> response.request().newBuilder()
                        .header("Authorization", credentials)
                        .build());

        if (sslSocketFactory != null && trustManager != null) {
            bldr.sslSocketFactory(sslSocketFactory, trustManager);
        }


        // this lets the user perform non-essential configuration (e.g. timeouts)
        // but reduces the risk of essentials not being set. MK.
        OkHttpClient.Builder b = configurator.configure(bldr);
        OkHttpClient httpClient = b.build();

        return new OkHttp3ClientHttpRequestFactory(httpClient);
    }
}
