package com.rabbitmq.http.client;

import com.rabbitmq.http.client.domain.VhostInfo;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class OkHttpSmokeClientTest {

    static final String DEFAULT_USERNAME = "guest";
    static final String DEFAULT_PASSWORD = "guest";

    @Test
    public void should_connect_to_rabbit_using_okhttp() throws MalformedURLException, URISyntaxException {
        String url = "http://" + DEFAULT_USERNAME + ":" + DEFAULT_PASSWORD + "@127.0.0.1:" + managementPort() + "/api/";

        Client client = new Client(new ClientParameters().url(url)
                .restTemplateConfigurator(new OkHttpRestTemplateConfigurator()));
        List<VhostInfo> vhosts = client.getVhosts();
        assertThat(vhosts, not(emptyList()));
        assertThat(vhosts.get(0).getName(), equalTo("/"));

    }

    static int managementPort() {
        return System.getProperty("rabbitmq.management.port") == null ?
                15672 :
                Integer.valueOf(System.getProperty("rabbitmq.management.port"));
    }

}
