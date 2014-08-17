package com.novemberain.hop.client.domain;

@SuppressWarnings("unused")
public class AlivenessTestResponse {
  private static final String SUCCESS = "ok";
  private String status;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public boolean isSuccessful() {
    return status.equals(SUCCESS);
  }

  @Override
  public String toString() {
    return "AlivenessTestResponse{" +
        "status='" + status + '\'' +
        '}';
  }
}
