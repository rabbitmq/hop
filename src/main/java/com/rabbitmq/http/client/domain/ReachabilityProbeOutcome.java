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

import java.time.Duration;

public class ReachabilityProbeOutcome {
  private final CurrentUserDetails currentUser;
  private final Duration duration;
  private final Exception error;

  private ReachabilityProbeOutcome(
      CurrentUserDetails currentUser, Duration duration, Exception error) {
    this.currentUser = currentUser;
    this.duration = duration;
    this.error = error;
  }

  public static ReachabilityProbeOutcome reached(CurrentUserDetails currentUser, Duration duration) {
    return new ReachabilityProbeOutcome(currentUser, duration, null);
  }

  public static ReachabilityProbeOutcome unreachable(Exception error) {
    return new ReachabilityProbeOutcome(null, null, error);
  }

  public boolean isReached() {
    return error == null;
  }

  public CurrentUserDetails getCurrentUser() {
    return currentUser;
  }

  public Duration getDuration() {
    return duration;
  }

  public Exception getError() {
    return error;
  }
}
