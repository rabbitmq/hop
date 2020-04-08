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

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class DeleteQueueParameters {
    private boolean ifEmpty;
    private boolean ifUnused;

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

    public MultiValueMap<String, String> getAsQueryParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        if (ifEmpty) {
            params.add("if-empty", "true");
        }
        if (ifUnused) {
            params.add("if-unused", "true");
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
