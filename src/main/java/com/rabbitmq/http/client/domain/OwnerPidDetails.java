package com.rabbitmq.http.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
    "owner_pid_details": {
        "name": "127.0.0.1:57130 -> 127.0.0.1:5672",
        "peer_host": "127.0.0.1",
        "peer_port": 57130
    },
    "pol
 */
public class OwnerPidDetails {
  private String name;
  private String peerHost;
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
    return "OwnerPidDetails{" +
        "name='" + name + '\'' +
        ", peerHost='" + peerHost + '\'' +
        ", peerPort=" + peerPort +
        '}';
  }
}
