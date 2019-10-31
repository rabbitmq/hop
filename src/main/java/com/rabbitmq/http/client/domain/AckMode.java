/*
 * Copyright 2019 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum AckMode {
    /**
     * Consumes in manual acknowledgement mode.
     * Messages are acknowledged to the upstream broker after they have been confirmed downstream.
     * <p>
     * This is the safest option that offers lowest throughput.
     *
     * @see <a href="https://www.rabbitmq.com/confirms.html">Confirms</a>
     */
    ON_CONFIRM("on-confirm"),

    /**
     * Consumes in manual acknowledgement mode.
     * Messages are acknowledged to the upstream broker after they have been published downstream
     * without waiting for publisher confirms. In other words, this uses fire-and-forget publishing.
     * <p>
     * This is a moderately safe safe option that does not handle downstream node failures.
     *
     * @see <a href="https://www.rabbitmq.com/confirms.html">Confirms</a>
     */
    ON_PUBLISH("on-publish"),

    /**
     * Consumes in automatic acknowledgement mode.
     * <p>
     * Unsafe, offers best throughput.
     *
     * @see <a href="https://www.rabbitmq.com/confirms.html">Confirms</a>
     */
    AUTOMATIC("no-ack");

    private final String value;

    AckMode(String value) {
        this.value = value;
    }

    @JsonCreator
    static AckMode fromValue(String value) {
        return Arrays.stream(AckMode.values()).filter(e -> e.value.equals(value))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Unexpected value for mode: " + value));
    }

    @JsonValue
    public String toValue() {
        return value;
    }
}
