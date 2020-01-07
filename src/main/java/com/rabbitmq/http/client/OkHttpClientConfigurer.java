package com.rabbitmq.http.client;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.util.Assert;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class OkHttpClientConfigurer implements ClientConfigurer {
    private static final OkHttpClientBuilderConfigurator NO_OP_HTTP_CLIENT_BUILDER_CONFIGURATOR =
            builder -> builder;
    private final URI rootUri;
    private final ClientHttpRequestFactory rf;

    /**
     * Construct an instance with the provided url and credentials.
     * @param url the url e.g. "http://localhost:15672/api/".
     * @param username the username.
     * @param password the password
     * @throws MalformedURLException for a badly formed URL.
     * @throws URISyntaxException for a badly formed URL.
     */
    public OkHttpClientConfigurer(String url, String username, String password) throws MalformedURLException, URISyntaxException {
        this(new URL(url), username, password);
    }

    /**
     * Construct an instance with the provided url and credentials.
     * @param url the url e.g. "http://localhost:15672/api/".
     * @param username the username.
     * @param password the password
     * @param configurator {@link OkHttpClientBuilderConfigurator} to use
     * @throws MalformedURLException for a badly formed URL.
     * @throws URISyntaxException for a badly formed URL.
     */
    public OkHttpClientConfigurer(String url, String username, String password, OkHttpClientBuilderConfigurator configurator)
            throws MalformedURLException, URISyntaxException {
        this(new URL(url), username, password, configurator);
    }

    /**
     * Construct an instance with the provided url and credentials.
     * @param url the url e.g. "http://localhost:15672/api/".
     * @param username the username.
     * @param password the password
     * @throws MalformedURLException for a badly formed URL.
     * @throws URISyntaxException for a badly formed URL.
     */
    public OkHttpClientConfigurer(URL url, String username, String password) throws MalformedURLException, URISyntaxException {
        this(url, username, password, null, null);
    }

    /**
     * Construct an instance with the provided url and credentials.
     * @param url the url e.g. "http://localhost:15672/api/".
     * @param username the username.
     * @param password the password
     * @param configurator {@link OkHttpClientBuilderConfigurator} to use
     * @throws MalformedURLException for a badly formed URL.
     * @throws URISyntaxException for a badly formed URL.
     */
    public OkHttpClientConfigurer(URL url, String username, String password, OkHttpClientBuilderConfigurator configurator)
            throws MalformedURLException, URISyntaxException {
        this(url, username, password, null, null, configurator);
    }

    /**
     * Construct an instance with the provided url and credentials.
     * @param url the url e.g. "http://localhost:15672/api/".
     * @param username the username.
     * @param password the password
     * @param sslSocketFactory ssl connection factory for http client
     * @param trustManager X509 certificates may be used to authenticate the remote side of a secure socket
     * @throws MalformedURLException for a badly formed URL.
     * @throws URISyntaxException for a badly formed URL.
     */
    private OkHttpClientConfigurer(URL url, String username, String password, SSLSocketFactory sslSocketFactory, X509TrustManager trustManager)
            throws MalformedURLException, URISyntaxException {
        this(url, username, password, sslSocketFactory, trustManager, NO_OP_HTTP_CLIENT_BUILDER_CONFIGURATOR);
    }

    /**
     * Construct an instance with the provided url and credentials.
     * @param url the url e.g. "https://guest:guest@localhost:15672/api/".
     * @throws MalformedURLException for a badly formed URL.
     * @throws URISyntaxException for a badly formed URL.
     */
    public OkHttpClientConfigurer(String url) throws MalformedURLException, URISyntaxException {
        this(url, NO_OP_HTTP_CLIENT_BUILDER_CONFIGURATOR);
    }

    /**
     * Construct an instance with the provided url and credentials.
     * @param url the url e.g. "https://guest:guest@localhost:15672/api/".
     * @param configurator {@link OkHttpClientBuilderConfigurator} to use
     * @throws MalformedURLException for a badly formed URL.
     * @throws URISyntaxException for a badly formed URL.
     */
    public OkHttpClientConfigurer(String url, OkHttpClientBuilderConfigurator configurator) throws MalformedURLException, URISyntaxException {
        this(Utils.urlWithoutCredentials(url),
                Utils.extractUsernamePassword(url)[0],
                Utils.extractUsernamePassword(url)[1],
                configurator
        );
    }

    /**
     * Construct an instance with the provided url and credentials.
     * @param url the url e.g. "https://guest:guest@localhost:15672/api/".
     * @throws MalformedURLException for a badly formed URL.
     * @throws URISyntaxException for a badly formed URL.
     */
    public OkHttpClientConfigurer(URL url) throws MalformedURLException, URISyntaxException {
        this(url, null, null);
    }

    private OkHttpClientConfigurer(URL url, String username, String password, SSLSocketFactory sslSocketFactory,
                                   X509TrustManager trustManager,
                                   OkHttpClientBuilderConfigurator configurator) throws URISyntaxException, MalformedURLException {
        Assert.notNull(url, "URL is required; it must not be null");
        Assert.notNull(username, "username is required; it must not be null");
        Assert.notNull(password, "password is required; it must not be null");
        Assert.notNull(configurator, "configurator is required; it must not be null");

        if (url.toString().endsWith("/")) {
            this.rootUri = url.toURI();
        } else {
            this.rootUri = new URL(url.toString() + "/").toURI();
        }

        this.rf = getRequestFactory(url, username, password, sslSocketFactory, trustManager, configurator);
    }


    private OkHttp3ClientHttpRequestFactory getRequestFactory(final URL url,
                                                              final String username, final String password,
                                                              final SSLSocketFactory sslSocketFactory,
                                                              final X509TrustManager trustManager,
                                                              final OkHttpClientBuilderConfigurator configurator) {
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

    @Override
    public ClientHttpRequestFactory getRequestFactory() {
        return rf;
    }

    @Override
    public URI getRootUri() {
        return rootUri;
    }
}
