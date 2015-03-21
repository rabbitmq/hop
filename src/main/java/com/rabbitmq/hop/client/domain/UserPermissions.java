package com.rabbitmq.hop.client.domain;

/**
 * Represents permissions of a user in a vhost.
 */
public class UserPermissions {
  private String user;
  private String vhost;
  private String read;
  private String write;
  private String configure;

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getVhost() {
    return vhost;
  }

  public void setVhost(String vhost) {
    this.vhost = vhost;
  }

  public String getRead() {
    return read;
  }

  public void setRead(String read) {
    this.read = read;
  }

  public String getWrite() {
    return write;
  }

  public void setWrite(String write) {
    this.write = write;
  }

  public String getConfigure() {
    return configure;
  }

  public void setConfigure(String configure) {
    this.configure = configure;
  }

  @Override
  public String toString() {
    return "UserPermissions{" +
        "user='" + user + '\'' +
        ", vhost='" + vhost + '\'' +
        ", read='" + read + '\'' +
        ", write='" + write + '\'' +
        ", configure='" + configure + '\'' +
        '}';
  }
}
