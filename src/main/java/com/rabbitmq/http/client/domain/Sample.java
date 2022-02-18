/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rabbitmq.http.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class Sample {

  private final long sample;
  private final long timestamp;

  public Sample(@JsonProperty("sample") long sample,
      @JsonProperty("timestamp") long timestamp) {
    this.sample = sample;
    this.timestamp = timestamp;
  }

  public long getSample() {
    return sample;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "Sample{" +
        "sample=" + sample +
        ", timestamp=" + timestamp +
        '}';
  }
}
