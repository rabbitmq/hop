package com.novemberain.hop.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RateDetails {
  private double rate;

  public RateDetails(@JsonProperty("rate") double rate) {
    this.rate = rate;
  }

  public double getRate() {
    return rate;
  }

  @Override
  public String toString() {
    return "RateDetails{" +
        "rate=" + rate +
        '}';
  }
}
