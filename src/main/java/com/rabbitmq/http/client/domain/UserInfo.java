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

import java.util.Arrays;
import java.util.List;

public class UserInfo {
  private String name;

  @Deprecated
  private String passwordHash;

  private String hashingAlgorithm;
  private List<String> tags;
  private boolean hasPassword;

  /**
   * Deprecated as of RabbitMQ 4.4.
   * @deprecated RabbitMQ APIs no longer return password hashes.
   * Use {@link #UserInfo(String, String, List, boolean)} instead.
   */
  @Deprecated
  public UserInfo(String name, String passwordHash, String hashingAlgorithm, List<String> tags) {
    this(name, hashingAlgorithm, tags, resolveLegacyHasPassword(passwordHash));
    this.passwordHash = passwordHash;
  }

  /**
   *
   * @param name
   * @param hashingAlgorithm
   * @param tags
   * @param hasPassword
   * @since 5.8.0
   */
  public UserInfo(String name, String hashingAlgorithm, List<String> tags, boolean hasPassword) {
    this.name = name;
    this.hashingAlgorithm = hashingAlgorithm;
    this.tags = tags;
    this.hasPassword = hasPassword;
  }

  private static boolean resolveLegacyHasPassword(String passwordHash) {
    return passwordHash != null;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getHashingAlgorithm() {
    return hashingAlgorithm;
  }

  public void setHashingAlgorithm(String hashingAlgorithm) {
    this.hashingAlgorithm = hashingAlgorithm;
  }

  /**
   * Deprecated as of RabbitMQ 4.4.
   * @deprecated RabbitMQ APIs no longer return password hashes.
   * This will return null on newer brokers. Use {@link #hasPassword()} instead.
   */
  @Deprecated
  public String getPasswordHash() {
    return passwordHash;
  }

  /**
   * Deprecated as of RabbitMQ 4.4.
   * @deprecated RabbitMQ APIs no longer return password hashes.
   */
  @Deprecated
  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  /**
   * Whether the user has a password or not.
   * @return whether the user has a password or not
   * @since 5.8.0
   */
  public boolean hasPassword() {
    return hasPassword;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public void setTags(String tags) {
    this.tags = Arrays.asList(tags.split(","));
  }

  public boolean canAccessHttpApi() {
    if (tags == null) {
      return false;
    }
    return tags.contains("management") || tags.contains("monitoring")
        || tags.contains("policymaker") || tags.contains("administrator");
  }

  public boolean isAdministrator() {
    if (tags == null) {
      return false;
    }
    return tags.contains("administrator");
  }

  public boolean canAccessMonitoringEndpoints() {
    if (tags == null) {
      return false;
    }
    return tags.contains("monitoring") || tags.contains("administrator");
  }

  @Override
  public String toString() {
    return "UserInfo{" +
        "name='" + name + '\'' +
        ", passwordHash='" + passwordHash + '\'' +
        ", hasPassword=" + hasPassword +
        ", tags=" + tags +
        '}';
  }
}