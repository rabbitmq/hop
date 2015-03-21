package com.rabbitmq.hop.client.domain;

import java.util.Map;

@SuppressWarnings("unused")
public class ClientProperties {
  private Map<String, Object> capabilities;
  private String product;
  private String platform;
  private String version;
  private String information;
  private String copyright;

  public Map<String, Object> getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(Map<String, Object> capabilities) {
    this.capabilities = capabilities;
  }

  public String getProduct() {
    return product;
  }

  public void setProduct(String product) {
    this.product = product;
  }

  public String getPlatform() {
    return platform;
  }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getInformation() {
    return information;
  }

  public void setInformation(String information) {
    this.information = information;
  }

  public String getCopyright() {
    return copyright;
  }

  public void setCopyright(String copyright) {
    this.copyright = copyright;
  }

  @Override
  public String toString() {
    return "ClientProperties{" +
        "capabilities=" + capabilities +
        ", product='" + product + '\'' +
        ", platform='" + platform + '\'' +
        ", version='" + version + '\'' +
        ", information='" + information + '\'' +
        ", copyright='" + copyright + '\'' +
        '}';
  }
}
