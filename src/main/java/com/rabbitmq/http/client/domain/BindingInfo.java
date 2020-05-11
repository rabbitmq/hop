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
public class BindingInfo {
  private String vhost;
  private String source;
  private String destination;
  @JsonProperty("destination_type")
  private DestinationType destinationType;
  @JsonProperty("routing_key")
  private String routingKey;
  private Map<String, Object> arguments;
  @JsonProperty("properties_key")
  private String propertiesKey;

  public String getVhost() {
    return vhost;
  }

  public void setVhost(String vhost) {
    this.vhost = vhost;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public String getDestinationType() {
    if(destinationType != null) {
      return destinationType.name();
    }
    return null;
  }

  public void setDestinationType(DestinationType destinationType){
    this.destinationType = destinationType;
  }

  public void setDestinationType(String destinationType) {
    if(destinationType != null) {
       this.destinationType = DestinationType.valueOf(destinationType.toUpperCase());
    }
  }

  public DestinationType getDestinationTypeAsEnum(){
    return destinationType;
  }


  public String getRoutingKey() {
    return routingKey;
  }

  public void setRoutingKey(String routingKey) {
    this.routingKey = routingKey;
  }

  public Map<String, Object> getArguments() {
    return arguments;
  }

  public void setArguments(Map<String, Object> arguments) {
    this.arguments = arguments;
  }

  public String getPropertiesKey() {
    return propertiesKey;
  }

  public void setPropertiesKey(String propertiesKey) {
    this.propertiesKey = propertiesKey;
  }

  @Override
  public String toString() {
    return "BindingInfo{" +
            "vhost='" + vhost + '\'' +
            ", source='" + source + '\'' +
            ", destination='" + destination + '\'' +
            ", destinationType='" + destinationType + '\'' +
            ", routingKey='" + routingKey + '\'' +
            ", arguments=" + arguments +
            ", propertiesKey='" + propertiesKey + '\'' +
            '}';
  }
}
