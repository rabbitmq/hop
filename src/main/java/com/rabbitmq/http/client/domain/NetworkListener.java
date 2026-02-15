/*
 * Copyright 2015 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.rabbitmq.http.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
public class NetworkListener {
  @JsonProperty("ip_address")
  private String ipAddress;
  private String node;
  private String protocol;
  private int port;

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public String getNode() {
    return node;
  }

  public void setNode(String node) {
    this.node = node;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getHumanFriendlyName() {
    if (protocol == null) {
      return null;
    }
    switch (protocol) {
      case "amqp":
        return "AMQP 1.0 and 0-9-1";
      case "amqp/ssl":
        return "AMQP 1.0 and 0-9-1 with TLS";
      case "mqtt":
        return "MQTT";
      case "mqtt/ssl":
        return "MQTT with TLS";
      case "stomp":
        return "STOMP";
      case "stomp/ssl":
        return "STOMP with TLS";
      case "stream":
        return "Stream Protocol";
      case "stream/ssl":
        return "Stream Protocol with TLS";
      case "http/web-mqtt":
        return "MQTT over WebSockets";
      case "https/web-mqtt":
        return "MQTT over WebSockets with TLS";
      case "http/web-stomp":
        return "STOMP over WebSockets";
      case "https/web-stomp":
        return "STOMP over WebSockets with TLS";
      case "http/web-amqp":
        return "AMQP 1.0 over WebSockets";
      case "https/web-amqp":
        return "AMQP 1.0 over WebSockets with TLS";
      case "http/prometheus":
        return "Prometheus";
      case "https/prometheus":
        return "Prometheus with TLS";
      case "http":
        return "HTTP API";
      case "https":
        return "HTTP API with TLS";
      case "clustering":
        return "Inter-node and CLI Tool Communication";
      default:
        return protocol;
    }
  }

  public boolean hasTlsEnabled() {
    if (protocol == null) {
      return false;
    }
    switch (protocol) {
      case "amqp/ssl":
      case "mqtt/ssl":
      case "stomp/ssl":
      case "stream/ssl":
      case "https/web-mqtt":
      case "https/web-stomp":
      case "https/web-amqp":
      case "https/prometheus":
      case "https":
        return true;
      default:
        return false;
    }
  }

  @Override
  public String toString() {
    return "NetworkListener{" +
        "ipAddress='" + ipAddress + '\'' +
        ", node='" + node + '\'' +
        ", protocol='" + protocol + '\'' +
        ", port=" + port +
        '}';
  }
}
