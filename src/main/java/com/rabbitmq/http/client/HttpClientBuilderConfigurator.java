/*
 * Copyright 2017-2019 the original author or authors.
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

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

/**
 * Implementations of this interface can perform post-configuration
 * of {@link HttpClientBuilder} used by {@link Client} internally.
 * Note that {@link Client} will configure all essential settings
 * before invoking the configurator.
 * <p>
 * {@link HttpClientBuilderConfigurator} implementations typically
 * customize request interceptors to set specific headers, proxy-related
 * and TLS-related settings.
 *
 * @see HttpClientBuilder#addInterceptorFirst(HttpRequestInterceptor)
 * @see HttpClientBuilder#addInterceptorLast(HttpRequestInterceptor)
 * @see HttpClientBuilder#setProxy(HttpHost)
 * @see HttpClientBuilder#setRoutePlanner(HttpRoutePlanner)
 * @see HttpClientBuilder#setSSLContext(SSLContext)
 * @see HttpClientBuilder#setSSLSocketFactory(LayeredConnectionSocketFactory)
 * @see HttpClientBuilder#setSSLHostnameVerifier(HostnameVerifier)
 */
public interface HttpClientBuilderConfigurator {

    /**
     * Configure the {@link HttpClientBuilder}.
     *
     * @param builder incoming builder
     * @return configured builder, usually same instance as the parameter
     */
    HttpClientBuilder configure(HttpClientBuilder builder);
}
