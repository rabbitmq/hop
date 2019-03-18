/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rabbitmq.http.client.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
// TODO
@JsonIgnoreProperties({"publishes", "deliveries"})
public class ChannelInfo {
  private String vhost;
  private String user;
  private int number;
  private String name;
  private String node;
  private String state;

  @JsonProperty("global_prefetch_count")
  private int globalPrefetchCount;
  @JsonProperty("prefetch_count")
  private int prefetchCount;
  @JsonProperty("acks_uncommitted")
  private int acksUncommitted;
  @JsonProperty("messages_uncommitted")
  private int messagesUncommitted;
  @JsonProperty("messages_unconfirmed")
  private int messagesUnconfirmed;
  @JsonProperty("messages_unacknowledged")
  private int messagesUnacknowledged;

  @JsonProperty("consumer_count")
  private int consumerCount;
  @JsonProperty("confirm")
  private boolean publisherConfirms;
  private boolean transactional;

  @JsonProperty("idle_since")
  private String idleSince;
  @JsonProperty("connection_details")
  private ConnectionDetails connectionDetails;
  @JsonProperty("message_stats")
  private MessageStats messageStats;

  @JsonProperty("consumer_details")
  private List<ConsumerDetails> consumerDetails;

  @JsonProperty("client_flow_blocked")
  private boolean clientFlowBlocked;

  public String getVhost() {
    return vhost;
  }

  public void setVhost(String vhost) {
    this.vhost = vhost;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    this.number = number;
  }

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

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public int getGlobalPrefetchCount() {
    return globalPrefetchCount;
  }

  public void setGlobalPrefetchCount(int globalPrefetchCount) {
    this.globalPrefetchCount = globalPrefetchCount;
  }

  public int getPrefetchCount() {
    return prefetchCount;
  }

  public void setPrefetchCount(int prefetchCount) {
    this.prefetchCount = prefetchCount;
  }

  public int getAcksUncommitted() {
    return acksUncommitted;
  }

  public void setAcksUncommitted(int acksUncommitted) {
    this.acksUncommitted = acksUncommitted;
  }

  public int getMessagesUncommitted() {
    return messagesUncommitted;
  }

  public void setMessagesUncommitted(int messagesUncommitted) {
    this.messagesUncommitted = messagesUncommitted;
  }

  public int getMessagesUnconfirmed() {
    return messagesUnconfirmed;
  }

  public void setMessagesUnconfirmed(int messagesUnconfirmed) {
    this.messagesUnconfirmed = messagesUnconfirmed;
  }

  public int getMessagesUnacknowledged() {
    return messagesUnacknowledged;
  }

  public void setMessagesUnacknowledged(int messagesUnacknowledged) {
    this.messagesUnacknowledged = messagesUnacknowledged;
  }

  public int getConsumerCount() {
    return consumerCount;
  }

  public void setConsumerCount(int consumerCount) {
    this.consumerCount = consumerCount;
  }

  public boolean usesPublisherConfirms() {
    return publisherConfirms;
  }

  public void setPublisherConfirms(boolean publisherConfirms) {
    this.publisherConfirms = publisherConfirms;
  }

  public boolean isTransactional() {
    return transactional;
  }

  public void setTransactional(boolean transactional) {
    this.transactional = transactional;
  }

  public String getIdleSince() {
    return idleSince;
  }

  public void setIdleSince(String idleSince) {
    this.idleSince = idleSince;
  }

  public ConnectionDetails getConnectionDetails() {
    return connectionDetails;
  }

  public void setConnectionDetails(ConnectionDetails connectionDetails) {
    this.connectionDetails = connectionDetails;
  }

  public boolean isClientFlowBlocked() {
    return clientFlowBlocked;
  }

  public void setClientFlowBlocked(boolean clientFlowBlocked) {
    this.clientFlowBlocked = clientFlowBlocked;
  }

  public MessageStats getMessageStats() {
    return messageStats;
  }

  public void setMessageStats(MessageStats messageStats) {
    this.messageStats = messageStats;
  }
  
  public List<ConsumerDetails> getConsumerDetails() {
      return consumerDetails;
  }
  
  public void setConsumerDetails(List<ConsumerDetails> consumerDetails) {
      this.consumerDetails = consumerDetails;
    }

  @Override
  public String toString() {
    return "ChannelInfo{" +
            "vhost='" + vhost + '\'' +
            ", user='" + user + '\'' +
            ", number=" + number +
            ", name='" + name + '\'' +
            ", node='" + node + '\'' +
            ", state='" + state + '\'' +
            ", globalPrefetchCount=" + globalPrefetchCount +
            ", prefetchCount=" + prefetchCount +
            ", acksUncommitted=" + acksUncommitted +
            ", messagesUncommitted=" + messagesUncommitted +
            ", messagesUnconfirmed=" + messagesUnconfirmed +
            ", messagesUnacknowledged=" + messagesUnacknowledged +
            ", consumerCount=" + consumerCount +
            ", publisherConfirms=" + publisherConfirms +
            ", transactional=" + transactional +
            ", idleSince='" + idleSince + '\'' +
            ", connectionDetails=" + connectionDetails +
            ", messageStats=" + messageStats +
            ", consumerDetails=" + consumerDetails +
            ", clientFlowBlocked=" + clientFlowBlocked +
            '}';
  }
}
