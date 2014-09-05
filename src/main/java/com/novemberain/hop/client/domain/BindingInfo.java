package com.novemberain.hop.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@SuppressWarnings("unused")
public class BindingInfo {
  private String vhost;
  private String source;
  private String destination;
  @JsonProperty("destination_type")
  private String destinationType;
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
    return destinationType;
  }

  public void setDestinationType(String destinationType) {
    this.destinationType = destinationType;
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
}
