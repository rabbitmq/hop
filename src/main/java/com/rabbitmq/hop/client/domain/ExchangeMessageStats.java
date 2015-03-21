package com.rabbitmq.hop.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExchangeMessageStats {
  @JsonProperty("publish_in")
  private int publishIn;
  @JsonProperty("publish_in_details")
  private RateDetails publishInDetails;
  @JsonProperty("publish_out")
  private int publishOut;
  @JsonProperty("publish_out_details")
  private RateDetails publishOutDetails;

  public int getPublishIn() {
    return publishIn;
  }

  public void setPublishIn(int publishIn) {
    this.publishIn = publishIn;
  }

  public RateDetails getPublishInDetails() {
    return publishInDetails;
  }

  public void setPublishInDetails(RateDetails publishInDetails) {
    this.publishInDetails = publishInDetails;
  }

  public int getPublishOut() {
    return publishOut;
  }

  public void setPublishOut(int publishOut) {
    this.publishOut = publishOut;
  }

  public RateDetails getPublishOutDetails() {
    return publishOutDetails;
  }

  public void setPublishOutDetails(RateDetails publishOutDetails) {
    this.publishOutDetails = publishOutDetails;
  }
}
