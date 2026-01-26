/*
 * Copyright 2024-2025 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents authentication attempt statistics for a specific protocol on a node.
 *
 * @since 5.5.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticationAttemptStatistics {

    private String protocol;

    @JsonProperty("auth_attempts")
    private long allAttemptCount;

    @JsonProperty("auth_attempts_failed")
    private long failureCount;

    @JsonProperty("auth_attempts_succeeded")
    private long successCount;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public long getAllAttemptCount() {
        return allAttemptCount;
    }

    public void setAllAttemptCount(long allAttemptCount) {
        this.allAttemptCount = allAttemptCount;
    }

    public long getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(long failureCount) {
        this.failureCount = failureCount;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    @Override
    public String toString() {
        return "AuthenticationAttemptStatistics{" +
                "protocol='" + protocol + '\'' +
                ", allAttemptCount=" + allAttemptCount +
                ", failureCount=" + failureCount +
                ", successCount=" + successCount +
                '}';
    }
}
