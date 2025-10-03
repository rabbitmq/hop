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

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserInfo {
  private String name;
  @JsonProperty("password_hash")
  private String passwordHash;
  @JsonProperty("hashing_algorithm")
  private String hashingAlgorithm;
  private List<String> tags;

  public UserInfo(String name, String passwordHash, String hashingAlgorithm, List<String> tags) {
    this.name = name;
    this.passwordHash = passwordHash;
    this.hashingAlgorithm = hashingAlgorithm;
    this.tags = tags;
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

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public List<String> getTags() {
    return tags;
  }

  @JsonProperty("tags")
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
