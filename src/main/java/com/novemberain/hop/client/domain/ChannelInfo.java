package com.novemberain.hop.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

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
}
