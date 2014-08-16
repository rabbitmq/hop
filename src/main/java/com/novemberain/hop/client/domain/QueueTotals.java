package com.novemberain.hop.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
public class QueueTotals {
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
}
