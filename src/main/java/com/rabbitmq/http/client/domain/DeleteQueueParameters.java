/*
 * Copyright 2020 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;

public class DeleteQueueParameters {
    private final boolean ifEmpty;
    private final boolean ifUnused;

    public DeleteQueueParameters(boolean ifEmpty, boolean ifUnused) {
        this.ifEmpty = ifEmpty;
        this.ifUnused = ifUnused;
    }

    public boolean isIfEmpty() {
        return ifEmpty;
    }

    public boolean isIfUnused() {
        return ifUnused;
    }

    public Map<String, String> getAsQueryParams() {
        Map<String, String> params = new LinkedHashMap<>();
        if (ifEmpty) {
            params.put("if-empty", Boolean.TRUE.toString());
        }
        if (ifUnused) {
            params.put("if-unused", Boolean.TRUE.toString());
        }
        return params;
    }

    @Override
    public String toString() {
        return "QueueDeleteInfo{" +
                "ifEmpty=" + ifEmpty +
                ", ifUnused=" + ifUnused +
                '}';
    }
}
