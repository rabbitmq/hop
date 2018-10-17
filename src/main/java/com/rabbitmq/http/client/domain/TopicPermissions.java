package com.rabbitmq.http.client.domain;

/**
 * Represents topic permissions of a user in a vhost for a given topic exchange.
 *
 * @since 3.0.0
 */
public class TopicPermissions {

    private String user;
    private String vhost;
    private String exchange;
    private String read;
    private String write;

    public TopicPermissions() {

    }

    public TopicPermissions(String exchange, String read, String write) {
        this.exchange = exchange;
        this.read = read;
        this.write = write;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getRead() {
        return read;
    }

    public void setRead(String read) {
        this.read = read;
    }

    public String getWrite() {
        return write;
    }

    public void setWrite(String write) {
        this.write = write;
    }
}
