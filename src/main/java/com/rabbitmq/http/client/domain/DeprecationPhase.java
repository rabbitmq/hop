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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Phase of a deprecated feature.
 *
 * @since 5.5.0
 * @see DeprecatedFeature
 * @see <a href="https://www.rabbitmq.com/docs/deprecated">Deprecated Features</a>
 */
public enum DeprecationPhase {

    @JsonProperty("permitted_by_default")
    PERMITTED_BY_DEFAULT,

    @JsonProperty("denied_by_default")
    DENIED_BY_DEFAULT,

    @JsonProperty("disconnected")
    DISCONNECTED,

    @JsonProperty("removed")
    REMOVED
}
