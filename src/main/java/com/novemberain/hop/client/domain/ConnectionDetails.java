package com.novemberain.hop.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
public class ConnectionDetails {
  private String name;
  @JsonProperty("peer_host")
  private String peerHost;
  @JsonProperty("peer_port")
  private int peerPort;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  @Override
  public String toString() {
    return "ConnectionDetails{" +
        "name='" + name + '\'' +
        ", peerHost='" + peerHost + '\'' +
        ", peerPort=" + peerPort +
        '}';
  }
}
