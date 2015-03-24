package com.rabbitmq.http.client.domain;

@SuppressWarnings("unused")
public class QueueDetails {
  private String name;
  private String vhost;

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

  @Override
  public String toString() {
    return "QueueDetails{" +
        "name='" + name + '\'' +
        ", vhost='" + vhost + '\'' +
        '}';
  }
}
