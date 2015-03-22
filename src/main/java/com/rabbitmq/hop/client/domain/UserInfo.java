package com.rabbitmq.hop.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;

public class UserInfo {
  private String name;
  @JsonProperty("password_hash")
  private String passwordHash;
  private List<String> tags;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public List<String> getTags() {
    return tags;
  }

  @JsonIgnore
  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  @JsonProperty("tags")
  public void setTags(String tags) {
    this.tags = Arrays.asList(tags.split(","));
  }

  @Override
  public String toString() {
    return "UserInfo{" +
        "name='" + name + '\'' +
        ", passwordHash='" + passwordHash + '\'' +
        ", tags=" + tags +
        '}';
  }
}
