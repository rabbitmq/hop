/*
 * Copyright 2015-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Definitions {

    @JsonProperty("rabbit_version")
    private String serverVersion;

    @JsonProperty("rabbitmq_version")
    private String rabbitmqVersion;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("product_version")
    private String productVersion;

    private String explanation;

    @JsonProperty("rabbitmq_definition_format")
    private String rabbitmqDefinitionFormat;

    @JsonProperty("original_cluster_name")
    private String originalClusterName;

    private List<VhostInfo> vhosts = new ArrayList<VhostInfo>();

    private List<UserInfo> users = new ArrayList<UserInfo>();

    private List<UserPermissions> permissions = new ArrayList<UserPermissions>();

    @JsonProperty("topic_permissions")
    private List<TopicPermissions> topicPermissions = new ArrayList<>();

    private List<RuntimeParameter<Object>> parameters = new ArrayList<>();

    @JsonProperty("global_parameters")
    private List<RuntimeParameter<Object>> globalParameters = new ArrayList<>();

    private List<PolicyInfo> policies = new ArrayList<>();

    private List<QueueInfo> queues = new ArrayList<QueueInfo>();

    private List<ExchangeInfo> exchanges = new ArrayList<ExchangeInfo>();

    private List<BindingInfo> bindings = new ArrayList<BindingInfo>();

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public String getRabbitmqVersion() {
        return rabbitmqVersion;
    }

    public void setRabbitmqVersion(String rabbitmqVersion) {
        this.rabbitmqVersion = rabbitmqVersion;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(String productVersion) {
        this.productVersion = productVersion;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getRabbitmqDefinitionFormat() {
        return rabbitmqDefinitionFormat;
    }

    public void setRabbitmqDefinitionFormat(String rabbitmqDefinitionFormat) {
        this.rabbitmqDefinitionFormat = rabbitmqDefinitionFormat;
    }

    public String getOriginalClusterName() {
        return originalClusterName;
    }

    public void setOriginalClusterName(String originalClusterName) {
        this.originalClusterName = originalClusterName;
    }

    public List<VhostInfo> getVhosts() {
        return vhosts;
    }

    public void setVhosts(List<VhostInfo> vhosts) {
        this.vhosts = vhosts;
    }

    public List<UserInfo> getUsers() {
        return users;
    }

    public void setUsers(List<UserInfo> users) {
        this.users = users;
    }

    public List<UserPermissions> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<UserPermissions> permissions) {
        this.permissions = permissions;
    }

    public List<QueueInfo> getQueues() {
        return queues;
    }

    public void setQueues(List<QueueInfo> queues) {
        this.queues = queues;
    }

    public List<ExchangeInfo> getExchanges() {
        return exchanges;
    }

    public void setExchanges(List<ExchangeInfo> exchanges) {
        this.exchanges = exchanges;
    }

    public List<BindingInfo> getBindings() {
        return bindings;
    }

    public void setBindings(List<BindingInfo> bindings) {
        this.bindings = bindings;
    }

    public void setTopicPermissions(List<TopicPermissions> topicPermissions) {
        this.topicPermissions = topicPermissions;
    }

    public List<TopicPermissions> getTopicPermissions() {
        return topicPermissions;
    }

    public void setParameters(List<RuntimeParameter<Object>> parameters) {
        this.parameters = parameters;
    }

    public List<RuntimeParameter<Object>> getParameters() {
        return parameters;
    }

    public void setGlobalParameters(List<RuntimeParameter<Object>> globalParameters) {
        this.globalParameters = globalParameters;
    }

    public List<RuntimeParameter<Object>> getGlobalParameters() {
        return globalParameters;
    }

    public List<PolicyInfo> getPolicies() {
        return policies;
    }

    public void setPolicies(List<PolicyInfo> policies) {
        this.policies = policies;
    }

    @Override
    public String toString() {
        return "Definitions{" +
                "serverVersion='" + serverVersion + '\'' +
                ", rabbitmqVersion='" + rabbitmqVersion + '\'' +
                ", productName='" + productName + '\'' +
                ", productVersion='" + productVersion + '\'' +
                ", explanation='" + explanation + '\'' +
                ", rabbitmqDefinitionFormat='" + rabbitmqDefinitionFormat + '\'' +
                ", originalClusterName='" + originalClusterName + '\'' +
                ", vhosts=" + vhosts +
                ", users=" + users +
                ", permissions=" + permissions +
                ", topicPermissions=" + topicPermissions +
                ", policies=" + policies +
                ", queues=" + queues +
                ", exchanges=" + exchanges +
                ", bindings=" + bindings +
                '}';
    }
}
