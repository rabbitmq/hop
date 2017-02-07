package com.rabbitmq.http.client.domain;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PolicyInfo {
  private String name;
  private String vhost;
  private String pattern;
  private Map<String, Object> definition;
  private int priority;
  @JsonProperty("apply-to")
  private String applyTo;

  public PolicyInfo() {
  }

  public PolicyInfo(String pattern, int priority, String applyTo, Map<String, Object> definition) {
    this.pattern = pattern;
    this.priority = priority;
    this.applyTo = applyTo;
    this.definition = definition;
  }

  public String getVhost() {
    return vhost;
  }

  public void setVhost(String vhost) {
    this.vhost = vhost;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public Map<String, Object> getDefinition() {
    return definition;
  }

  public void setDefinition(Map<String, Object> definition) {
    this.definition = definition;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public String getApplyTo() {
    return applyTo;
  }

  public void setApplyTo(String applyTo) {
    this.applyTo = applyTo;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
