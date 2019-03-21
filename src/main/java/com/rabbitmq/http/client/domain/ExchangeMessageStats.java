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

public class ExchangeMessageStats {
  @JsonProperty("publish_in")
  private long publishIn;
  @JsonProperty("publish_in_details")
  private RateDetails publishInDetails;
  @JsonProperty("publish_out")
  private long publishOut;
  @JsonProperty("publish_out_details")
  private RateDetails publishOutDetails;

  @JsonProperty("confirm")
  private long confirm;
  @JsonProperty("confirm_details")
  private RateDetails confirmDetails;

  public long getPublishIn() {
    return publishIn;
  }

  public void setPublishIn(long publishIn) {
    this.publishIn = publishIn;
  }

  public RateDetails getPublishInDetails() {
    return publishInDetails;
  }

  public void setPublishInDetails(RateDetails publishInDetails) {
    this.publishInDetails = publishInDetails;
  }

  public long getPublishOut() {
    return publishOut;
  }

  public void setPublishOut(long publishOut) {
    this.publishOut = publishOut;
  }

  public RateDetails getPublishOutDetails() {
    return publishOutDetails;
  }

  public void setPublishOutDetails(RateDetails publishOutDetails) {
    this.publishOutDetails = publishOutDetails;
  }

  @Override
  public String toString() {
    return "ExchangeMessageStats{" +
        "publishIn=" + publishIn +
        ", publishInDetails=" + publishInDetails +
        ", publishOut=" + publishOut +
        ", publishOutDetails=" + publishOutDetails +
        ", confirm=" + confirm +
        ", confirmDetails=" + confirmDetails +
        '}';
  }

  public long getConfirm() {
    return confirm;
  }

  public void setConfirm(long confirm) {
    this.confirm = confirm;
  }

  public RateDetails getConfirmDetails() {
    return confirmDetails;
  }

  public void setConfirmDetails(RateDetails confirmDetails) {
    this.confirmDetails = confirmDetails;
  }
}
