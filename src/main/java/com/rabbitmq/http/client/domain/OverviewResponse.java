/*
 * Copyright 2015 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.rabbitmq.http.client.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
public class OverviewResponse {

  //
  // Fields
  //

  private String node;
  @JsonProperty("cluster_name")
  private String clusterName;
  @JsonProperty("management_version")
  private String managementPluginVersion;
  @JsonProperty("rabbitmq_version")
  private String serverVersion;
  @JsonProperty("erlang_version")
  private String erlangVersion;
  @JsonProperty("erlang_full_version")
  private String fullErlangVersion;
  @JsonProperty("statistics_level")
  private String statisticsLevel;
  @JsonProperty("statistics_db_node")
  private String statisticsDbNode;
  @JsonProperty("exchange_types")
  private List<ExchangeType> exchangeTypes;
  @JsonProperty("message_stats")
  private MessageStats messageStats;
  @JsonProperty("queue_totals")
  private QueueTotals queueTotals;
  @JsonProperty("object_totals")
  private ObjectTotals objectTotals;
  @JsonProperty("rates_mode")
  private String ratesMode;
  @JsonProperty("listeners")
  private List<NetworkListener> listeners;
  @JsonProperty("contexts")
  private List<PluginContext> contexts;
  @JsonProperty("statistics_db_event_queue")
  private long statisticsDBEventQueueLength;

  //
  // API
  //


  public String getManagementPluginVersion() {
    return managementPluginVersion;
  }

  public void setManagementPluginVersion(String managementPluginVersion) {
    this.managementPluginVersion = managementPluginVersion;
  }

  /**
   *
   * @return the server version
   * @deprecated use {@link #getServerVersion()} instead.
   */
  @Deprecated
  public String getRabbitMQVersion() {
    return serverVersion;
  }

  /**
   *
   * @param rabbitMQVersion the server version
   * @deprecated use {@link #setServerVersion(String)} instead.
   */
  @Deprecated
  public void setRabbitMQVersion(String rabbitMQVersion) {
    this.serverVersion = rabbitMQVersion;
  }

  public String getServerVersion() {
    return serverVersion;
  }

  public void setServerVersion(String serverVersion) {
    this.serverVersion = serverVersion;
  }

  public String getErlangVersion() {
    return erlangVersion;
  }

  public void setErlangVersion(String erlangVersion) {
    this.erlangVersion = erlangVersion;
  }

  public String getFullErlangVersion() {
    return fullErlangVersion;
  }

  public void setFullErlangVersion(String fullErlangVersion) {
    this.fullErlangVersion = fullErlangVersion;
  }

  public String getNode() {
    return node;
  }

  public void setNode(String node) {
    this.node = node;
  }

  public String getStatisticsLevel() {
    return statisticsLevel;
  }

  public void setStatisticsLevel(String statisticsLevel) {
    this.statisticsLevel = statisticsLevel;
  }

  public String getStatisticsDbNode() {
    return statisticsDbNode;
  }

  public void setStatisticsDbNode(String statisticsDbNode) {
    this.statisticsDbNode = statisticsDbNode;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public List<ExchangeType> getExchangeTypes() {
    return exchangeTypes;
  }

  public void setExchangeTypes(List<ExchangeType> exchangeTypes) {
    this.exchangeTypes = exchangeTypes;
  }

  public QueueTotals getQueueTotals() {
    return queueTotals;
  }

  public void setQueueTotals(QueueTotals queueTotals) {
    this.queueTotals = queueTotals;
  }

  public ObjectTotals getObjectTotals() {
    return objectTotals;
  }

  public void setObjectTotals(ObjectTotals objectTotals) {
    this.objectTotals = objectTotals;
  }

  public List<NetworkListener> getListeners() {
    return listeners;
  }

  public void setListeners(List<NetworkListener> listeners) {
    this.listeners = listeners;
  }

  public MessageStats getMessageStats() {
    return messageStats;
  }

  public void setMessageStats(MessageStats messageStats) {
    this.messageStats = messageStats;
  }

  public List<PluginContext> getContexts() {
    return contexts;
  }

  public void setContexts(List<PluginContext> contexts) {
    this.contexts = contexts;
  }

  public String getRatesMode() {
    return ratesMode;
  }

  public void setRatesMode(String ratesMode) {
    this.ratesMode = ratesMode;
  }

  @Override
  public String toString() {
    return "OverviewResponse{" +
        "node='" + node + '\'' +
        ", clusterName='" + clusterName + '\'' +
        ", managementPluginVersion='" + managementPluginVersion + '\'' +
        ", serverVersion='" + serverVersion + '\'' +
        ", erlangVersion='" + erlangVersion + '\'' +
        ", fullErlangVersion='" + fullErlangVersion + '\'' +
        ", statisticsLevel='" + statisticsLevel + '\'' +
        ", statisticsDbNode='" + statisticsDbNode + '\'' +
        ", exchangeTypes=" + exchangeTypes +
        ", messageStats=" + messageStats +
        ", queueTotals=" + queueTotals +
        ", objectTotals=" + objectTotals +
        ", ratesMode='" + ratesMode + '\'' +
        ", listeners=" + listeners +
        ", contexts=" + contexts +
        ", statisticsDBEventQueueLength=" + statisticsDBEventQueueLength +
        '}';
  }

  public long getStatisticsDBEventQueueLength() {
    return statisticsDBEventQueueLength;
  }

  public void setStatisticsDBEventQueueLength(long statisticsDBEventQueueLength) {
    this.statisticsDBEventQueueLength = statisticsDBEventQueueLength;
  }

}
