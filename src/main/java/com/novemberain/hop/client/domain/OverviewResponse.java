package com.novemberain.hop.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties({"message_stats", "queue_totals", "object_totals",
                       "listeners", "contexts"})
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
  private String rabbitMQVersion;
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

  //
  // API
  //


  public String getManagementPluginVersion() {
    return managementPluginVersion;
  }

  public void setManagementPluginVersion(String managementPluginVersion) {
    this.managementPluginVersion = managementPluginVersion;
  }

  public String getRabbitMQVersion() {
    return rabbitMQVersion;
  }

  public void setRabbitMQVersion(String rabbitMQVersion) {
    this.rabbitMQVersion = rabbitMQVersion;
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

  @Override
  public String toString() {
    return "OverviewResponse{" +
        "node='" + node + '\'' +
        ", clusterName='" + clusterName + '\'' +
        ", managementPluginVersion='" + managementPluginVersion + '\'' +
        ", rabbitMQVersion='" + rabbitMQVersion + '\'' +
        ", erlangVersion='" + erlangVersion + '\'' +
        ", fullErlangVersion='" + fullErlangVersion + '\'' +
        ", statisticsLevel='" + statisticsLevel + '\'' +
        ", statisticsDbNode='" + statisticsDbNode + '\'' +
        '}';
  }
}
