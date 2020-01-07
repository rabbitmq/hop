package com.rabbitmq.http.client


import java.util.concurrent.TimeUnit

class OkHttpClientSpec extends ClientSpec {

    @Override
    protected Client newLocalhostNodeClient() {
        new Client(new OkHttpClientConfigurer("http://127.0.0.1:" + managementPort() + "/api/", DEFAULT_USERNAME, DEFAULT_PASSWORD))
    }

    @Override
    protected Client newLocalhostNodeClientWithConfiguration() {
        new Client(new OkHttpClientConfigurer("http://127.0.0.1:" + managementPort() + "/api/", DEFAULT_USERNAME, DEFAULT_PASSWORD, { builder -> builder.readTimeout(10, TimeUnit.SECONDS) }))
    }

    @Override
    protected Client newLocalhostNodeClientWithCredentialsInUrl() {
        new Client(new OkHttpClientConfigurer("http://" + DEFAULT_USERNAME + ":" + DEFAULT_PASSWORD + "@127.0.0.1:" + managementPort() + "/api/"))
    }

}
