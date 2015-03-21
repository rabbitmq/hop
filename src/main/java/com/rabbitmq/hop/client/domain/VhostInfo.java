package com.rabbitmq.hop.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
public class VhostInfo {
  private String name;
  private boolean tracing;
  @JsonProperty("message_stats")
  private MessageStats messageStats;
  private long messages;
  @JsonProperty("messages_details")
  private RateDetails messagesDetails;
  @JsonProperty("messages_ready")
  private long messagesReady;
  @JsonProperty("messages_ready_details")
  private RateDetails messagesReadyDetails;
  @JsonProperty("messages_unacknowledged")
  private long messagesUnacknowledged;
  @JsonProperty("messages_unacknowledged_details")
  private RateDetails messagesUnacknowledgedDetails;

  @JsonProperty("recv_oct")
  private long octetsReceived;
  @JsonProperty("recv_oct_details")
  private RateDetails octetsReceivedDetails;
  @JsonProperty("send_oct")
  private long octetsSent;
  @JsonProperty("send_oct_details")
  private RateDetails octetsSentDetails;

  @Override
  public String toString() {
    return "VhostInfo{" +
        "name='" + name + '\'' +
        ", tracing=" + tracing +
        ", messageStats=" + messageStats +
        ", messages=" + messages +
        ", messagesDetails=" + messagesDetails +
        ", messagesReady=" + messagesReady +
        ", messagesReadyDetails=" + messagesReadyDetails +
        ", messagesUnacknowledged=" + messagesUnacknowledged +
        ", messagesUnacknowledgedDetails=" + messagesUnacknowledgedDetails +
        ", octetsReceived=" + octetsReceived +
        ", octetsReceivedDetails=" + octetsReceivedDetails +
        ", octetsSent=" + octetsSent +
        ", octetsSentDetails=" + octetsSentDetails +
        '}';
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isTracing() {
    return tracing;
  }

  public void setTracing(boolean tracing) {
    this.tracing = tracing;
  }

  public MessageStats getMessageStats() {
    return messageStats;
  }

  public void setMessageStats(MessageStats messageStats) {
    this.messageStats = messageStats;
  }

  public long getMessages() {
    return messages;
  }

  public void setMessages(long messages) {
    this.messages = messages;
  }

  public RateDetails getMessagesDetails() {
    return messagesDetails;
  }

  public void setMessagesDetails(RateDetails messagesDetails) {
    this.messagesDetails = messagesDetails;
  }

  public long getMessagesReady() {
    return messagesReady;
  }

  public void setMessagesReady(long messagesReady) {
    this.messagesReady = messagesReady;
  }

  public RateDetails getMessagesReadyDetails() {
    return messagesReadyDetails;
  }

  public void setMessagesReadyDetails(RateDetails messagesReadyDetails) {
    this.messagesReadyDetails = messagesReadyDetails;
  }

  public long getMessagesUnacknowledged() {
    return messagesUnacknowledged;
  }

  public void setMessagesUnacknowledged(long messagesUnacknowledged) {
    this.messagesUnacknowledged = messagesUnacknowledged;
  }

  public RateDetails getMessagesUnacknowledgedDetails() {
    return messagesUnacknowledgedDetails;
  }

  public void setMessagesUnacknowledgedDetails(RateDetails messagesUnacknowledgedDetails) {
    this.messagesUnacknowledgedDetails = messagesUnacknowledgedDetails;
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
}
