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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * A class to gather parameters on <code>_details</code> objects.
 *
 * <p>Parameters can be set to get extra information on how count fields have changed (messages sent
 * and received, queue lengths).
 *
 * <p>A {@link DetailsParameters} instance can create a {@link QueryParameters} instance with its
 * own parameters applied on it, see {@link #queryParameters()}. This way {@link DetailsParameters}
 * can be "injected" in methods like {@link
 * com.rabbitmq.http.client.Client#getQueues(DetailsParameters)}.
 *
 * @since 4.1.0
 * @see com.rabbitmq.http.client.Client#getQueues(DetailsParameters)
 * @see com.rabbitmq.http.client.Client#getQueues(QueryParameters)
 * @see com.rabbitmq.http.client.Client#getQueue(String, String, DetailsParameters)
 * @see com.rabbitmq.http.client.ReactorNettyClient#getQueues(String, DetailsParameters)
 * @see com.rabbitmq.http.client.ReactorNettyClient#getQueue(String, String, DetailsParameters)
 */
public class DetailsParameters {

  private final Map<String, Object> parameters = new HashMap<>();
  private QueryParameters queryParameters;

  private static void checkGreaterThanZero(int value, String field) {
    if (value <= 0) {
      throw new IllegalArgumentException(String.format("'%s' must be greater than 0", field));
    }
  }

  private static void checkGreaterThanZero(Duration value, String field) {
    if (value == null) {
      throw new IllegalArgumentException(String.format("'%s' cannot be null", field));
    }
    if (value.toSeconds() <= 0) {
      throw new IllegalArgumentException(String.format("'%s' must be greater than 0", field));
    }
  }

  public DetailsParameters messageRates(int ageSeconds, int incrementSeconds) {
    checkGreaterThanZero(ageSeconds, "age");
    checkGreaterThanZero(incrementSeconds, "increment");
    this.parameters.put("msg_rates_age", String.valueOf(ageSeconds));
    this.parameters.put("msg_rates_incr", String.valueOf(incrementSeconds));
    return this;
  }

  public DetailsParameters messageRates(Duration age, Duration increment) {
    checkGreaterThanZero(age, "age");
    checkGreaterThanZero(increment, "increment");
    return this.messageRates((int) age.toSeconds(), (int) increment.toSeconds());
  }

  public DetailsParameters lengths(int ageSeconds, int incrementSeconds) {
    checkGreaterThanZero(ageSeconds, "age");
    checkGreaterThanZero(incrementSeconds, "increment");
    this.parameters.put("lengths_age", String.valueOf(ageSeconds));
    this.parameters.put("lengths_incr", String.valueOf(incrementSeconds));
    return this;
  }

  public DetailsParameters lengths(Duration age, Duration increment) {
    checkGreaterThanZero(age, "age");
    checkGreaterThanZero(increment, "increment");
    return this.lengths((int) age.toSeconds(), (int) increment.toSeconds());
  }

  public Map<String, String> parameters() {
    return this.parameters.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().toString()));
  }

  public QueryParameters queryParameters() {
    if (this.queryParameters == null) {
      this.queryParameters = new QueryParameters(this.parameters);
    }
    return this.queryParameters;
  }
}
