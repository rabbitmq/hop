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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// TODO: clarify the meaning of these and support them with sensible field names
@JsonIgnoreProperties({"recv_cnt", "send_cnt", "send_pend"})
@SuppressWarnings("unused")
public class ConnectionInfo {
  private String name;
  private String node;
  private String type;
  private int channels;
  private String state;
  private String user;
  private String vhost;
  private String protocol;
  private int port;
  @JsonProperty("peer_port")
  private int peerPort;
  private String host;
  @JsonProperty("peer_host")
  private String peerHost;
  @JsonProperty("frame_max")
  private int frameMax;
  @JsonProperty("channel_max")
  private int channelMax;
  @JsonProperty("timeout")
  private int heartbeatTimeout;
  @JsonProperty("recv_oct")
  private long octetsReceived;
  @JsonProperty("recv_oct_details")
  private RateDetails octetsReceivedDetails;
  @JsonProperty("send_oct")
  private long octetsSent;
  @JsonProperty("send_oct_details")
  private RateDetails octetsSentDetails;
  @JsonProperty("ssl")
  private boolean usesTLS;
  @JsonProperty("peer_cert_subject")
  private String peerCertificateSubject;
  @JsonProperty("peer_cert_issuer")
  private String peerCertificateIssuer;
  @JsonProperty("peer_cert_validity")
  private String peerCertificateValidity;
  @JsonProperty("auth_mechanism")
  private String authMechanism;
  @JsonProperty("ssl_protocol")
  private String sslProtocol;
  @JsonProperty("ssl_key_exchange")
  private String sslKeyExchange;
  @JsonProperty("ssl_cipher")
  private String sslCipher;
  @JsonProperty("ssl_hash")
  private String sslHash;
  @JsonProperty("client_properties")
  private ClientProperties clientProperties;

  @Override
  public String toString() {
    return "ConnectionInfo{" +
        "name='" + name + '\'' +
        ", node='" + node + '\'' +
        ", type='" + type + '\'' +
        ", channels=" + channels +
        ", state='" + state + '\'' +
        ", user='" + user + '\'' +
        ", vhost='" + vhost + '\'' +
        ", protocol='" + protocol + '\'' +
        ", port=" + port +
        ", peerPort=" + peerPort +
        ", host='" + host + '\'' +
        ", peerHost='" + peerHost + '\'' +
        ", frameMax=" + frameMax +
        ", channelMax=" + channelMax +
        ", heartbeatTimeout=" + heartbeatTimeout +
        ", octetsReceived=" + octetsReceived +
        ", octetsReceivedDetails=" + octetsReceivedDetails +
        ", octetsSent=" + octetsSent +
        ", octetsSentDetails=" + octetsSentDetails +
        ", usesTLS=" + usesTLS +
        ", peerCertificateSubject='" + peerCertificateSubject + '\'' +
        ", peerCertificateIssuer='" + peerCertificateIssuer + '\'' +
        ", peerCertificateValidity='" + peerCertificateValidity + '\'' +
        ", authMechanism='" + authMechanism + '\'' +
        ", sslProtocol='" + sslProtocol + '\'' +
        ", sslKeyExchange='" + sslKeyExchange + '\'' +
        ", sslCipher='" + sslCipher + '\'' +
        ", sslHash='" + sslHash + '\'' +
        ", clientProperties=" + clientProperties +
        ", connectedAt=" + connectedAt +
        '}';
  }

  @JsonProperty("connected_at")
  private long connectedAt;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getNode() {
    return node;
  }

  public void setNode(String node) {
    this.node = node;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public int getChannels() {
    return channels;
  }

  public void setChannels(int channels) {
    this.channels = channels;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
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

  public int getPeerPort() {
    return peerPort;
  }

  public void setPeerPort(int peerPort) {
    this.peerPort = peerPort;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public String getPeerHost() {
    return peerHost;
  }

  public void setPeerHost(String peerHost) {
    this.peerHost = peerHost;
  }

  public int getFrameMax() {
    return frameMax;
  }

  public void setFrameMax(int frameMax) {
    this.frameMax = frameMax;
  }

  public int getChannelMax() {
    return channelMax;
  }

  public void setChannelMax(int channelMax) {
    this.channelMax = channelMax;
  }

  public long getOctetsReceived() {
    return octetsReceived;
  }

  public void setOctetsReceived(long octetsReceived) {
    this.octetsReceived = octetsReceived;
  }

  public RateDetails getOctetsReceivedDetails() {
    return octetsReceivedDetails;
  }

  public void setOctetsReceivedDetails(RateDetails octetsReceivedDetails) {
    this.octetsReceivedDetails = octetsReceivedDetails;
  }

  public long getOctetsSent() {
    return octetsSent;
  }

  public void setOctetsSent(long octetsSent) {
    this.octetsSent = octetsSent;
  }

  public RateDetails getOctetsSentDetails() {
    return octetsSentDetails;
  }

  public void setOctetsSentDetails(RateDetails octetsSentDetails) {
    this.octetsSentDetails = octetsSentDetails;
  }

  public boolean isUsesTLS() {
    return usesTLS;
  }

  public void setUsesTLS(boolean usesTLS) {
    this.usesTLS = usesTLS;
  }

  public String getPeerCertificateSubject() {
    return peerCertificateSubject;
  }

  public void setPeerCertificateSubject(String peerCertificateSubject) {
    this.peerCertificateSubject = peerCertificateSubject;
  }

  public String getPeerCertificateIssuer() {
    return peerCertificateIssuer;
  }

  public void setPeerCertificateIssuer(String peerCertificateIssuer) {
    this.peerCertificateIssuer = peerCertificateIssuer;
  }

  public String getPeerCertificateValidity() {
    return peerCertificateValidity;
  }

  public void setPeerCertificateValidity(String peerCertificateValidity) {
    this.peerCertificateValidity = peerCertificateValidity;
  }

  public String getAuthMechanism() {
    return authMechanism;
  }

  public void setAuthMechanism(String authMechanism) {
    this.authMechanism = authMechanism;
  }

  public String getSslProtocol() {
    return sslProtocol;
  }

  public void setSslProtocol(String sslProtocol) {
    this.sslProtocol = sslProtocol;
  }

  public String getSslKeyExchange() {
    return sslKeyExchange;
  }

  public void setSslKeyExchange(String sslKeyExchange) {
    this.sslKeyExchange = sslKeyExchange;
  }

  public String getSslCipher() {
    return sslCipher;
  }

  public void setSslCipher(String sslCipher) {
    this.sslCipher = sslCipher;
  }

  public String getSslHash() {
    return sslHash;
  }

  public void setSslHash(String sslHash) {
    this.sslHash = sslHash;
  }

  public ClientProperties getClientProperties() {
    return clientProperties;
  }

  public void setClientProperties(ClientProperties clientProperties) {
    this.clientProperties = clientProperties;
  }

  public int getHeartbeatTimeout() {
    return heartbeatTimeout;
  }

  public void setHeartbeatTimeout(int heartbeatTimeout) {
    this.heartbeatTimeout = heartbeatTimeout;
  }

  public long getConnectedAt() {
    return connectedAt;
  }

  public void setConnectedAt(long connectedAt) {
    this.connectedAt = connectedAt;
  }

}
