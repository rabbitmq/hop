/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rabbitmq.http.client.domain;

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
  
  @Override
  public String toString() {
    return "ChannelDetails{" +
        "connectionName='" + connectionName + '\'' +
        ", name='" + name + '\'' +
        ", number=" + number +
        ", peerHost='" + peerHost + '\'' +
        ", peerPort=" + peerPort +
        '}';
  }
}
