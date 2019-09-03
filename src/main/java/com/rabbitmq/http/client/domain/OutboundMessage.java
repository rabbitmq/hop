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

import java.util.Collections;
import java.util.Map;

/**
 * Representation for an outbound message.
 *
 * @see com.rabbitmq.http.client.Client#publish(String, String, String, OutboundMessage)
 * @see com.rabbitmq.http.client.ReactorNettyClient#publish(String, String, String, OutboundMessage)
 * @since 3.4.0
 */
public class OutboundMessage {

    private static final String PAYLOAD_UTF8 = "string";
    private static final String PAYLOAD_BASE64 = "base64";

    private String payload;
    private Map<String, Object> properties = Collections.emptyMap();
    private String payloadEncoding = PAYLOAD_UTF8;

    public OutboundMessage payload(String payload) {
        this.payload = payload;
        return this;
    }

    public OutboundMessage properties(Map<String, Object> properties) {
        this.properties = properties;
        return this;
    }

    public OutboundMessage utf8Encoded() {
        this.payloadEncoding = PAYLOAD_UTF8;
        return this;
    }

    public OutboundMessage base64Encoded() {
        this.payloadEncoding = PAYLOAD_BASE64;
        return this;
    }

    public String getPayload() {
        return payload;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getPayloadEncoding() {
        return payloadEncoding;
    }
}
