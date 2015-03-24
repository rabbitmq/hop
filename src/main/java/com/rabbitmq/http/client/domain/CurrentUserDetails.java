package com.rabbitmq.http.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
public class CurrentUserDetails {
  private String name;
  private String tags;
  @JsonProperty("auth_backend")
  private String authBackend;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTags() {
    return tags;
  }

  public void setTags(String tags) {
    this.tags = tags;
  }

  public String getAuthBackend() {
    return authBackend;
  }

  public void setAuthBackend(String authBackend) {
    this.authBackend = authBackend;
  }

  @Override
  public String toString() {
    return "CurrentUserDetails{" +
        "name='" + name + '\'' +
        ", tags='" + tags + '\'' +
        ", authBackend='" + authBackend + '\'' +
        '}';
  }
}
