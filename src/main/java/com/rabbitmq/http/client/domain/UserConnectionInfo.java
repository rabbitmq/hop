package com.rabbitmq.http.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserConnectionInfo {
    private String name;
    @JsonProperty("user")
    private String username;
    private String vhost;
    private String node;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    @Override
    public String toString() {
        return "UserConnectionInfo{" +
                "name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", vhost='" + vhost + '\'' +
                ", node='" + node + '\'' +
                '}';
    }
}
