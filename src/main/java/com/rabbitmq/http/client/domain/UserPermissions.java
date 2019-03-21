/*
 * Copyright 2015 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.rabbitmq.http.client.domain;

/**
 * Represents permissions of a user in a vhost.
 */
public class UserPermissions {
  private String user;
  private String vhost;
  private String read;
  private String write;
  private String configure;

  public UserPermissions() {}
  public UserPermissions(String read, String write, String configure) {
    this.read = read;
    this.write = write;
    this.configure = configure;
  }

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

  public static UserPermissions FULL = fullPermissions();

  private static UserPermissions fullPermissions() {
    UserPermissions p = new UserPermissions();
    p.setConfigure(".*");
    p.setRead(".*");
    p.setWrite(".*");
    return p;
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
