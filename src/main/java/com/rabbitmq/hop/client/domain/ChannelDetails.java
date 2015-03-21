package com.rabbitmq.hop.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("ununsed")
public class ChannelDetails {
  @JsonProperty("connection_name")
  private String connectionName;
  private String name;
  private int number;
  @JsonProperty("peer_host")
  private String peerHost;
  @JsonProperty("peer_port")
  private int peerPort;

  public String getConnectionName() {
    return connectionName;
  }

  public void setConnectionName(String connectionName) {
    this.connectionName = connectionName;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    this.number = number;
  }

  public String getPeerHost() {
    return peerHost;
  }

  public void setPeerHost(String peerHost) {
    this.peerHost = peerHost;
  }

  public int getPeerPort() {
    return peerPort;
  }

  public void setPeerPort(int peerPort) {
    this.peerPort = peerPort;
  }
}
