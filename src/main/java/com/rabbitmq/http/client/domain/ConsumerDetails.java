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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

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
