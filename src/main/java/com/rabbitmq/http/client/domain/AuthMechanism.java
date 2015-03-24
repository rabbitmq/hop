package com.rabbitmq.http.client.domain;

@SuppressWarnings("unused")
public class AuthMechanism {
  private String name;
  private String description;
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

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public String toString() {
    return "AuthMechanism{" +
        "name='" + name + '\'' +
        ", description='" + description + '\'' +
        ", enabled=" + enabled +
        '}';
  }
}
