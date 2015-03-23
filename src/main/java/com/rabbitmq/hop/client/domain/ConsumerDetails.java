package com.rabbitmq.hop.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@SuppressWarnings("unused")
public class ConsumerDetails {
  @JsonProperty("consumer_tag")
  private String consumerTag;
  @JsonProperty("prefetch_count")
  private int prefetchCount;
  @JsonProperty("channel_details")
  private ChannelDetails channelDetails;
  private boolean exclusive;
  private Map<String, Object> arguments;
  private QueueDetails queueDetails;

  public String getConsumerTag() {
    return consumerTag;
  }

  public void setConsumerTag(String consumerTag) {
    this.consumerTag = consumerTag;
  }

  public int getPrefetchCount() {
    return prefetchCount;
  }

  public void setPrefetchCount(int prefetchCount) {
    this.prefetchCount = prefetchCount;
  }

  public ChannelDetails getChannelDetails() {
    return channelDetails;
  }

  public void setChannelDetails(ChannelDetails channelDetails) {
    this.channelDetails = channelDetails;
  }

  public boolean isExclusive() {
    return exclusive;
  }

  public void setExclusive(boolean exclusive) {
    this.exclusive = exclusive;
  }

  public Map<String, Object> getArguments() {
    return arguments;
  }

  public void setArguments(Map<String, Object> arguments) {
    this.arguments = arguments;
  }

  public QueueDetails getQueueDetails() {
    return queueDetails;
  }

  public void setQueueDetails(QueueDetails queueDetails) {
    this.queueDetails = queueDetails;
  }

  @Override
  public String toString() {
    return "ConsumerDetails{" +
        "consumerTag='" + consumerTag + '\'' +
        ", prefetchCount=" + prefetchCount +
        ", channelDetails=" + channelDetails +
        ", exclusive=" + exclusive +
        ", arguments=" + arguments +
        ", queueDetails=" + queueDetails +
        '}';
  }
}
