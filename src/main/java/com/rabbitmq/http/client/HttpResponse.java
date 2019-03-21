/*
 * Copyright 2018 the original author or authors.
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

import java.util.Collections;
import java.util.Map;

/**
 * Representation of an HTTP response.
 *
 * @since 2.1.0
 */
public class HttpResponse {

    private final int status;

    private final String reason;

    private final Map<String, String> headers;

    public HttpResponse(int status, String reason, Map<String, String> headers) {
        this.status = status;
        this.reason = reason;
        this.headers = Collections.unmodifiableMap(headers);
    }

    public int getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String toString() {
        return "HttpResponse{" +
            "status=" + status +
            ", reason='" + reason + '\'' +
            ", headers=" + headers +
            '}';
    }
}
