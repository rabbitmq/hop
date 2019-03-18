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

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class ExchangeInfo {
  private String name;
  private String vhost;
  private String type;
  private boolean durable;
  @JsonProperty("auto_delete")
  private boolean autoDelete;
  private boolean internal;
  private Map<String, Object> arguments;

  @JsonProperty("message_stats")
  private ExchangeMessageStats messageStats;

  public ExchangeInfo(){}
  public ExchangeInfo(String type, boolean durable, boolean autoDelete) {
    this(type, durable, autoDelete, false, new HashMap<String, Object>());
  }
  public ExchangeInfo(String type, boolean durable, boolean autoDelete, boolean internal, Map<String, Object> arguments) {
    this.type = type;
    this.durable = durable;
    this.autoDelete = autoDelete;
    this.internal = internal;
    this.arguments = arguments;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVhost() {
    return vhost;
  }

  public void setVhost(String vhost) {
    this.vhost = vhost;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isDurable() {
    return durable;
  }

  public void setDurable(boolean durable) {
    this.durable = durable;
  }

  public boolean isAutoDelete() {
    return autoDelete;
  }

  public void setAutoDelete(boolean autoDelete) {
    this.autoDelete = autoDelete;
  }

  public boolean isInternal() {
    return internal;
  }

  public void setInternal(boolean internal) {
    this.internal = internal;
  }

  public Map<String, Object> getArguments() {
    return arguments;
  }

  public void setArguments(Map<String, Object> arguments) {
    this.arguments = arguments;
  }

  @Override
  public String toString() {
    return "ExchangeInfo{" +
            "name='" + name + '\'' +
            ", vhost='" + vhost + '\'' +
            ", type='" + type + '\'' +
            ", durable=" + durable +
            ", autoDelete=" + autoDelete +
            ", internal=" + internal +
            ", arguments=" + arguments +
            ", messageStats=" + messageStats +
            '}';
  }
}
