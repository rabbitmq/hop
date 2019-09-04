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
package com.rabbitmq.http.client;

/**
 * The type of expected encoding when consuming messages.
 *
 * @see Client#get(String, String, int, GetAckMode, GetEncoding, int) 
 * @see ReactorNettyClient#get(String, String, int, GetAckMode, GetEncoding, int) 
 * @since 3.4.0
 */
public enum GetEncoding {

    /**
     * The payload will be returned as a string if it is valid UTF-8,
     * and base64 encoded otherwise
     */
    AUTO("auto"),

    /**
     * The payload will be base64 encoded
     */
    BASE64("base64");

    final String encoding;

    GetEncoding(String encoding) {
        this.encoding = encoding;
    }
}
