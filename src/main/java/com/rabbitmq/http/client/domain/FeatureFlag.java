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
 * Represents a RabbitMQ feature flag.
 *
 * @since 5.5.0
 * @see <a href="https://www.rabbitmq.com/docs/feature-flags">Feature Flags</a>
 */
public class FeatureFlag {

    private String name;

    @JsonProperty("desc")
    private String description;

    @JsonProperty("doc_url")
    private String docUrl;

    @JsonProperty("provided_by")
    private String providedBy;

    private FeatureFlagState state;

    private FeatureFlagStability stability;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDocUrl() {
        return docUrl;
    }

    public void setDocUrl(String docUrl) {
        this.docUrl = docUrl;
    }

    public String getProvidedBy() {
        return providedBy;
    }

    public void setProvidedBy(String providedBy) {
        this.providedBy = providedBy;
    }

    public FeatureFlagState getState() {
        return state;
    }

    public void setState(FeatureFlagState state) {
        this.state = state;
    }

    public FeatureFlagStability getStability() {
        return stability;
    }

    public void setStability(FeatureFlagStability stability) {
        this.stability = stability;
    }

    @Override
    public String toString() {
        return "FeatureFlag{" +
                "name='" + name + '\'' +
                ", state=" + state +
                ", stability=" + stability +
                '}';
    }
}
