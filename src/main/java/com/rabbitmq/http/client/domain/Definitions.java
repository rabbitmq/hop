/*
 * Copyright 2015 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Definitions {

    @JsonProperty("rabbit_version")
    private String serverVersion;

    private List<VhostInfo> vhosts = new ArrayList<VhostInfo>();

    private List<UserInfo> users = new ArrayList<UserInfo>();

    private List<UserPermissions> permissions = new ArrayList<UserPermissions>();

    @JsonProperty("topic_permissions")
    private List<TopicPermissions> topicPermissions = new ArrayList<>();

    private List<QueueInfo> queues = new ArrayList<QueueInfo>();

    private List<ExchangeInfo> exchanges = new ArrayList<ExchangeInfo>();

    private List<BindingInfo> bindings = new ArrayList<BindingInfo>();

    public String getRabbitMQVersion() {
        return serverVersion;
    }

    public void setRabbitMQVersion(String rabbitMQVersion) {
        this.serverVersion = rabbitMQVersion;
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

    @Override
    public String toString() {
        return "Definitions{" +
                "serverVersion='" + serverVersion + '\'' +
                ", vhosts=" + vhosts +
                ", users=" + users +
                ", permissions=" + permissions +
                ", topicPermissions=" + topicPermissions +
                ", queues=" + queues +
                ", exchanges=" + exchanges +
                ", bindings=" + bindings +
                '}';
    }
}
