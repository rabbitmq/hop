package com.rabbitmq.http.client.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParameterWrapper<T> {
    private String name;
    private String vhost;
    private String component;
    private T value;

    public String getName() {
        return name;
    }

    public ParameterWrapper<T> setName(String name) {
        this.name = name;
        return this;
    }

    public String getVhost() {
        return vhost;
    }

    public ParameterWrapper<T> setVhost(String vhost) {
        this.vhost = vhost;
        return this;
    }

    public String getComponent() {
        return component;
    }

    public ParameterWrapper<T> setComponent(String component) {
        this.component = component;
        return this;
    }

    public T getValue() {
        return value;
    }

    public ParameterWrapper<T> setValue(T value) {
        this.value = value;
        return this;
    }

    @Override
    public String toString() {
        return "ParameterWrapper{" +
                "name='" + name + '\'' +
                ", vhost='" + vhost + '\'' +
                ", component='" + component + '\'' +
                ", value=" + value +
                '}';
    }
}
