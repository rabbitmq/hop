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

  @Override
  public String toString() {
    return "ExchangeType{" +
        "name='" + name + '\'' +
        ", description='" + description + '\'' +
        ", internalPurpose='" + internalPurpose + '\'' +
        ", enabled=" + enabled +
        '}';
  }
}
