package com.rabbitmq.http.client.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuntimeParameter<T> {
    private String name;
    private String vhost;
    private String component;
    private T value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "RuntimeParameter{" +
                "name='" + name + '\'' +
                ", vhost='" + vhost + '\'' +
                ", component='" + component + '\'' +
                ", value=" + value +
                '}';
    }
}
