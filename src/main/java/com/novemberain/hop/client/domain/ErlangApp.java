package com.novemberain.hop.client.domain;

@SuppressWarnings("unused")
public class ErlangApp {
  private String name;
  private String description;
  private String version;

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

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public String toString() {
    return "ErlangApp{" +
        "name='" + name + '\'' +
        ", description='" + description + '\'' +
        ", version='" + version + '\'' +
        '}';
  }
}
