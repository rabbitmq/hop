/*
 * Copyright 2015-2022 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RateDetails {
  private final double average;
  private final double averageRate;
  private final double rate;
  private final List<Sample> samples;

  public RateDetails(double rate) {
    this(0, 0, rate, Collections.emptyList());
  }

  public RateDetails(
      @JsonProperty("avg") double average,
      @JsonProperty("avg_rate") double averageRate,
      @JsonProperty("rate") double rate,
      @JsonProperty("samples") List<Sample> samples
      ) {
    this.average = average;
    this.averageRate = averageRate;
    this.rate = rate;
    this.samples = samples;
  }

  public double getAverage() {
    return average;
  }

  public double getAverageRate() {
    return averageRate;
  }

  public double getRate() {
    return rate;
  }

  public List<Sample> getSamples() {
    return samples;
  }

  @Override
  public String toString() {
    return "RateDetails{" +
        "average=" + average +
        ", averageRate=" + averageRate +
        ", rate=" + rate +
        ", samples=" + samples +
        '}';
  }
}
