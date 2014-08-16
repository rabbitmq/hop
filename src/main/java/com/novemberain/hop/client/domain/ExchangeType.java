package com.novemberain.hop.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExchangeType {
  private String name;
  private String description;
  @JsonProperty("internal_purpose")
  private String internalPurpose;
  private boolean enabled;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getInternalPurpose() {
    return internalPurpose;
  }

  public void setInternalPurpose(String internalPurpose) {
    this.internalPurpose = internalPurpose;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
