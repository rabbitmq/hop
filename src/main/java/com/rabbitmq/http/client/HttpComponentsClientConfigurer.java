package com.rabbitmq.http.client;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
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

import javax.net.ssl.SSLContext;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class HttpComponentsClientConfigurer implements ClientConfigurer {
    private static final HttpClientBuilderConfigurator NO_OP_HTTP_CLIENT_BUILDER_CONFIGURATOR =
            builder -> builder;
    private final URI rootUri;
    private final ClientHttpRequestFactory rf;

    public HttpComponentsClientConfigurer(String url, String username, String password) throws MalformedURLException, URISyntaxException {
        this(new URL(url), username, password);
    }

    public HttpComponentsClientConfigurer(String url, String username, String password, HttpClientBuilderConfigurator configurator)
            throws MalformedURLException, URISyntaxException {
        this(new URL(url), username, password, configurator);
    }

    public HttpComponentsClientConfigurer(URL url, String username, String password) throws MalformedURLException, URISyntaxException {
        this(url, username, password, null, null);
    }

    public HttpComponentsClientConfigurer(URL url, String username, String password, HttpClientBuilderConfigurator configurator)
            throws MalformedURLException, URISyntaxException {
        this(url, username, password, null, null, configurator);
    }

    private HttpComponentsClientConfigurer(URL url, String username, String password, SSLConnectionSocketFactory sslConnectionSocketFactory, SSLContext sslContext)
            throws MalformedURLException, URISyntaxException {
        this(url, username, password, sslConnectionSocketFactory, sslContext, NO_OP_HTTP_CLIENT_BUILDER_CONFIGURATOR);
    }

    public HttpComponentsClientConfigurer(URL url, String username, String password, SSLContext sslContext) throws MalformedURLException, URISyntaxException {
        this(url, username, password, null, sslContext);
    }

    private HttpComponentsClientConfigurer(URL url, String username, String password, SSLConnectionSocketFactory sslConnectionSocketFactory) throws MalformedURLException, URISyntaxException {
        this(url, username, password, sslConnectionSocketFactory, null);
    }

    public HttpComponentsClientConfigurer(String url) throws MalformedURLException, URISyntaxException {
        this(url, NO_OP_HTTP_CLIENT_BUILDER_CONFIGURATOR);
    }

    public HttpComponentsClientConfigurer(String url, HttpClientBuilderConfigurator configurator) throws MalformedURLException, URISyntaxException {
        this(Utils.urlWithoutCredentials(url),
                Utils.extractUsernamePassword(url)[0],
                Utils.extractUsernamePassword(url)[1],
                configurator
        );
    }

    public HttpComponentsClientConfigurer(URL url) throws MalformedURLException, URISyntaxException {
        this(url, null, null);
    }

    HttpComponentsClientConfigurer(URL url, String username, String password, SSLConnectionSocketFactory sslConnectionSocketFactory,
                                   SSLContext sslContext,
                                   HttpClientBuilderConfigurator configurator) throws URISyntaxException, MalformedURLException {
        Assert.notNull(url, "URL is required; it must not be null");
        Assert.notNull(username, "username is required; it must not be null");
        Assert.notNull(password, "password is required; it must not be null");
        Assert.notNull(configurator, "configurator is required; it must not be null");

        if (url.toString().endsWith("/")) {
            this.rootUri = url.toURI();
        } else {
            this.rootUri = new URL(url.toString() + "/").toURI();
        }

        this.rf = getRequestFactory(url, username, password,
                sslConnectionSocketFactory, sslContext, configurator);
    }


    private HttpComponentsClientHttpRequestFactory getRequestFactory(final URL url,
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

    @Override
    public ClientHttpRequestFactory getRequestFactory() {
        return rf;
    }

    @Override
    public URI getRootUri() {
        return rootUri;
    }
}
