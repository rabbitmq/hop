/*
 * Copyright 2026 the original author or authors.
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

/**
 * User limits (max connections and channels).
 *
 * @since 5.5.0
 */
public class UserLimits {

    private final String user;

    private final int maxConnections;

    private final int maxChannels;

    public UserLimits(String user, int maxConnections, int maxChannels) {
        this.user = user;
        this.maxConnections = maxConnections;
        this.maxChannels = maxChannels;
    }

    public String getUser() {
        return user;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getMaxChannels() {
        return maxChannels;
    }

    @Override
    public String toString() {
        return "UserLimits{" +
                "user='" + user + '\'' +
                ", maxConnections=" + maxConnections +
                ", maxChannels=" + maxChannels +
                '}';
    }
}
