package com.novemberain.hop.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

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
  @JsonProperty("get_no_ack")
  private long basicGetNoAck;
  @JsonProperty("get_no_ack_details")
  private RateDetails basicGetNoAckDetails;

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
}
