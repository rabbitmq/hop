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

  @Override
  public String toString() {
    return "QueueTotals{" +
        "messages=" + messages +
        ", messagesDetails=" + messagesDetails +
        ", messagesReady=" + messagesReady +
        ", messagesReadyDetails=" + messagesReadyDetails +
        ", messagesUnacknowledged=" + messagesUnacknowledged +
        ", messagesUnacknowledgedDetails=" + messagesUnacknowledgedDetails +
        '}';
  }
}
