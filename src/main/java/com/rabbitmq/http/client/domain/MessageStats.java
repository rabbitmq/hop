/*
 * Copyright 2015 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
public class MessageStats {
  @JsonProperty("publish")
  private long basicPublish;
  @JsonProperty("publish_details")
  private RateDetails basicPublishDetails;
  @JsonProperty("confirm")
  private long publisherConfirm;
  @JsonProperty("confirm_details")
  private RateDetails publisherConfirmDetails;
  @JsonProperty("return_unroutable")
  private long basicReturn;
  @JsonProperty("return_unroutable_details")
  private RateDetails basicReturnDetails;
  @JsonProperty("deliver")
  private long basicDeliver;
  @JsonProperty("deliver_get")
  private long basicGet;
  @JsonProperty("deliver_get_details")
  private RateDetails basicGetDetails;
  @JsonProperty("deliver_details")
  private RateDetails basicDeliverDetails;
  @JsonProperty("deliver_no_ack")
  private long basicDeliverNoAck;
  @JsonProperty("deliver_no_ack_details")
  private RateDetails basicDeliverNoAckDetails;
  @JsonProperty("redeliver")
  private long redeliver;

  public long getRedeliver() {
    return redeliver;
  }

  public void setRedeliver(long redeliver) {
    this.redeliver = redeliver;
  }

  public RateDetails getRedeliverDetails() {
    return redeliverDetails;
  }

  public void setRedeliverDetails(RateDetails redeliverDetails) {
    this.redeliverDetails = redeliverDetails;
  }

  @JsonProperty("redeliver_details")
  private RateDetails redeliverDetails;
  @JsonProperty("get_no_ack")
  private long basicGetNoAck;
  @JsonProperty("get_no_ack_details")
  private RateDetails basicGetNoAckDetails;
  @JsonProperty("ack")
  private long ack;
  @JsonProperty("ack_details")
  private RateDetails ackDetails;
  @JsonProperty("get")
  private long getCounter;
  @JsonProperty("get_details")
  private RateDetails getDetails;

  public long getBasicPublish() {
    return basicPublish;
  }

  public void setBasicPublish(long basicPublish) {
    this.basicPublish = basicPublish;
  }

  public RateDetails getBasicPublishDetails() {
    return basicPublishDetails;
  }

  public void setBasicPublishDetails(RateDetails basicPublishDetails) {
    this.basicPublishDetails = basicPublishDetails;
  }

  public long getPublisherConfirm() {
    return publisherConfirm;
  }

  public void setPublisherConfirm(long publisherConfirm) {
    this.publisherConfirm = publisherConfirm;
  }

  public RateDetails getPublisherConfirmDetails() {
    return publisherConfirmDetails;
  }

  public void setPublisherConfirmDetails(RateDetails publisherConfirmDetails) {
    this.publisherConfirmDetails = publisherConfirmDetails;
  }

  public long getBasicReturn() {
    return basicReturn;
  }

  public void setBasicReturn(long basicReturn) {
    this.basicReturn = basicReturn;
  }

  public RateDetails getBasicReturnDetails() {
    return basicReturnDetails;
  }

  public void setBasicReturnDetails(RateDetails basicReturnDetails) {
    this.basicReturnDetails = basicReturnDetails;
  }

  public long getBasicDeliver() {
    return basicDeliver;
  }

  public void setBasicDeliver(long basicDeliver) {
    this.basicDeliver = basicDeliver;
  }

  public long getBasicGet() {
    return basicGet;
  }

  public void setBasicGet(long basicGet) {
    this.basicGet = basicGet;
  }

  public RateDetails getBasicGetDetails() {
    return basicGetDetails;
  }

  public void setBasicGetDetails(RateDetails basicGetDetails) {
    this.basicGetDetails = basicGetDetails;
  }

  public RateDetails getBasicDeliverDetails() {
    return basicDeliverDetails;
  }

  public void setBasicDeliverDetails(RateDetails basicDeliverDetails) {
    this.basicDeliverDetails = basicDeliverDetails;
  }

  public long getBasicDeliverNoAck() {
    return basicDeliverNoAck;
  }

  public void setBasicDeliverNoAck(long basicDeliverNoAck) {
    this.basicDeliverNoAck = basicDeliverNoAck;
  }

  public RateDetails getBasicDeliverNoAckDetails() {
    return basicDeliverNoAckDetails;
  }

  public void setBasicDeliverNoAckDetails(RateDetails basicDeliverNoAckDetails) {
    this.basicDeliverNoAckDetails = basicDeliverNoAckDetails;
  }

  public long getBasicGetNoAck() {
    return basicGetNoAck;
  }

  public void setBasicGetNoAck(long basicGetNoAck) {
    this.basicGetNoAck = basicGetNoAck;
  }

  public RateDetails getBasicGetNoAckDetails() {
    return basicGetNoAckDetails;
  }

  public void setBasicGetNoAckDetails(RateDetails basicGetNoAckDetails) {
    this.basicGetNoAckDetails = basicGetNoAckDetails;
  }

  public long getAck() {
    return ack;
  }

  public void setAck(long ack) {
    this.ack = ack;
  }

  public RateDetails getAckDetails() {
    return ackDetails;
  }

  public void setAckDetails(RateDetails ackDetails) {
    this.ackDetails = ackDetails;
  }

  public long getGetCounter() {
    return getCounter;
  }

  public void setGetCounter(long getCounter) {
    this.getCounter = getCounter;
  }

  public RateDetails getGetDetails() {
    return getDetails;
  }

  public void setGetDetails(RateDetails getDetails) {
    this.getDetails = getDetails;
  }

  @Override
  public String toString() {
    return "MessageStats{" +
        "basicPublish=" + basicPublish +
        ", basicPublishDetails=" + basicPublishDetails +
        ", publisherConfirm=" + publisherConfirm +
        ", publisherConfirmDetails=" + publisherConfirmDetails +
        ", basicReturn=" + basicReturn +
        ", basicReturnDetails=" + basicReturnDetails +
        ", basicDeliver=" + basicDeliver +
        ", basicGet=" + basicGet +
        ", basicGetDetails=" + basicGetDetails +
        ", basicDeliverDetails=" + basicDeliverDetails +
        ", basicDeliverNoAck=" + basicDeliverNoAck +
        ", basicDeliverNoAckDetails=" + basicDeliverNoAckDetails +
        ", basicGetNoAck=" + basicGetNoAck +
        ", basicGetNoAckDetails=" + basicGetNoAckDetails +
        ", ack=" + ack +
        ", ackDetails=" + ackDetails +
        ", getCounter=" + getCounter +
        ", getDetails=" + getDetails +
        '}';
  }
}
