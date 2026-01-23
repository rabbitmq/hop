/*
 * Copyright 2024-2025 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the OAuth 2.0 configuration for RabbitMQ authentication.
 *
 * @since 5.5.0
 * @see <a href="https://www.rabbitmq.com/docs/oauth2">OAuth 2 Guide</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuthConfiguration {

    @JsonProperty("oauth_enabled")
    private boolean oauthEnabled;

    @JsonProperty("oauth_client_id")
    private String oauthClientId;

    @JsonProperty("oauth_provider_url")
    private String oauthProviderUrl;

    public boolean isOauthEnabled() {
        return oauthEnabled;
    }

    public void setOauthEnabled(boolean oauthEnabled) {
        this.oauthEnabled = oauthEnabled;
    }

    public String getOauthClientId() {
        return oauthClientId;
    }

    public void setOauthClientId(String oauthClientId) {
        this.oauthClientId = oauthClientId;
    }

    public String getOauthProviderUrl() {
        return oauthProviderUrl;
    }

    public void setOauthProviderUrl(String oauthProviderUrl) {
        this.oauthProviderUrl = oauthProviderUrl;
    }

    @Override
    public String toString() {
        return "OAuthConfiguration{" +
                "oauthEnabled=" + oauthEnabled +
                ", oauthClientId='" + oauthClientId + '\'' +
                ", oauthProviderUrl='" + oauthProviderUrl + '\'' +
                '}';
    }
}
