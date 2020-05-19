/*
 * Copyright 2020 the original author or authors.
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
 * Virtual host limit (max queues and connections).
 *
 * @since 3.7.0
 */
public class VhostLimits {

    private final String vhost;

    private final int maxQueues;

    private final int maxConnections;

    public VhostLimits(String vhost, int maxQueues, int maxConnections) {
        this.vhost = vhost;
        this.maxQueues = maxQueues;
        this.maxConnections = maxConnections;
    }

    public String getVhost() {
        return vhost;
    }

    public int getMaxQueues() {
        return maxQueues;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    @Override
    public String toString() {
        return "VhostLimit{" +
                "vhost='" + vhost + '\'' +
                ", maxQueues=" + maxQueues +
                ", maxConnections=" + maxConnections +
                '}';
    }
}
