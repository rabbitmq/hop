/*
 * Copyright 2015 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.rabbitmq.http.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// TODO
@JsonIgnoreProperties({"partitions", "cluster_links"})
@SuppressWarnings("unused")
public class NodeInfo {
  private static final String DISK_TYPE = "disc";

  private String name;
  private String type;
  private boolean running;

  @JsonProperty("fd_used")
  private int fileDescriptorsUsed;
  @JsonProperty("fd_total")
  private int fileDescriptorsTotal;
  @JsonProperty("fd_used_details")
  private RateDetails fdUsedDetails;

  @JsonProperty("sockets_used")
  private int socketsUsed;
  @JsonProperty("sockets_total")
  private int socketsTotal;
  @JsonProperty("sockets_used_details")
  private RateDetails socketsUsedDetails;

  @JsonProperty("mem_used")
  private long memoryUsed;
  @JsonProperty("mem_limit")
  private long memoryLimit;
  @JsonProperty("mem_alarm")
  private boolean memoryAlarmActive;
  @JsonProperty("mem_used_details")
  private RateDetails memoryUsedDetails;

  @JsonProperty("disk_free_limit")
  private long diskFreeLimit;
  @JsonProperty("disk_free")
  private long diskFree;
  @JsonProperty("disk_free_alarm")
  private boolean diskAlarmActive;
  @JsonProperty("disk_free_details")
  private RateDetails diskFreeDetails;

  @JsonProperty("io_read_avg_time")
  private long ioReadAvgTime;
  @JsonProperty("io_read_avg_time_details")
  private RateDetails ioReadAvgTimeDetails;

  @JsonProperty("io_write_avg_time")
  private long ioWriteAvgTime;
  @JsonProperty("io_write_avg_time_details")
  private RateDetails ioWriteAvgTimeDetails;

  @JsonProperty("io_sync_avg_time")
  private long ioSyncAvgTime;
  @JsonProperty("io_sync_avg_time_details")
  private RateDetails ioSyncAvgTimeDetails;

  @JsonProperty("mnesia_disk_tx_count")
  private long mnesiaDiskTransactionsCount;
  @JsonProperty("mnesia_disk_tx_count_details")
  private RateDetails mnesiaDiskTransactionCountDetails;

  @JsonProperty("mnesia_ram_tx_count")
  private long mnesiaRamTransactionsCount;
  @JsonProperty("mnesia_ram_tx_count_details")
  private RateDetails mnesiaRamTransactionCountDetails;

  @JsonProperty("proc_used")
  private long erlangProcessesUsed;
  @JsonProperty("proc_total")
  private long erlangProcessesTotal;
  @JsonProperty("proc_used_details")
  private RateDetails erlangProcessesUsedDetails;

  @JsonProperty("statistics_level")
  private String statisticsLevel;
  @JsonProperty("rates_mode")
  private String ratesMode;

  @JsonProperty("log_file")
  private String logFilePath;
  @JsonProperty("sasl_log_file")
  private String saslLogFilePath;

  @JsonProperty("db_dir")
  private String dbDirectoryPath;
  @JsonProperty("config_files")
  private List<String> configFilePaths;

  @JsonProperty("net_ticktime")
  private long netTicktime;

  @JsonProperty("enabled_plugins")
  private List<String> enabledPlugins;

  private long uptime;
  @JsonProperty("run_queue")
  private int erlangRunQueueLength;
  @JsonProperty("processors")
  private int numberOfProcessors;

  @JsonProperty("os_pid")
  private String osPid;

  @JsonProperty("exchange_types")
  List<ExchangeType> exchangeTypes;
  @JsonProperty("auth_mechanisms")
  List<AuthMechanism> authMechanisms;
  @JsonProperty("applications")
  List<ErlangApp> erlangApps;
  @JsonProperty("contexts")
  List<PluginContext> pluginContexts;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isRunning() {
    return running;
  }

  public void setRunning(boolean running) {
    this.running = running;
  }

  public int getFileDescriptorsUsed() {
    return fileDescriptorsUsed;
  }

  public void setFileDescriptorsUsed(int fileDescriptorsUsed) {
    this.fileDescriptorsUsed = fileDescriptorsUsed;
  }

  public int getFileDescriptorsTotal() {
    return fileDescriptorsTotal;
  }

  public void setFileDescriptorsTotal(int fileDescriptorsTotal) {
    this.fileDescriptorsTotal = fileDescriptorsTotal;
  }

  public int getSocketsUsed() {
    return socketsUsed;
  }

  public void setSocketsUsed(int socketsUsed) {
    this.socketsUsed = socketsUsed;
  }

  public int getSocketsTotal() {
    return socketsTotal;
  }

  public void setSocketsTotal(int socketsTotal) {
    this.socketsTotal = socketsTotal;
  }

  public long getMemoryUsed() {
    return memoryUsed;
  }

  public void setMemoryUsed(long memoryUsed) {
    this.memoryUsed = memoryUsed;
  }

  public long getMemoryLimit() {
    return memoryLimit;
  }

  public void setMemoryLimit(long memoryLimit) {
    this.memoryLimit = memoryLimit;
  }

  public boolean isMemoryAlarmActive() {
    return memoryAlarmActive;
  }

  public void setMemoryAlarmActive(boolean memoryAlarmActive) {
    this.memoryAlarmActive = memoryAlarmActive;
  }

  public long getDiskFreeLimit() {
    return diskFreeLimit;
  }

  public void setDiskFreeLimit(long diskFreeLimit) {
    this.diskFreeLimit = diskFreeLimit;
  }

  public long getDiskFree() {
    return diskFree;
  }

  public void setDiskFree(long diskFree) {
    this.diskFree = diskFree;
  }

  public boolean isDiskAlarmActive() {
    return diskAlarmActive;
  }

  public void setDiskAlarmActive(boolean diskAlarmActive) {
    this.diskAlarmActive = diskAlarmActive;
  }

  public long getErlangProcessesUsed() {
    return erlangProcessesUsed;
  }

  public void setErlangProcessesUsed(int erlangProcessesUsed) {
    this.erlangProcessesUsed = erlangProcessesUsed;
  }

  public long getErlangProcessesTotal() {
    return erlangProcessesTotal;
  }

  public void setErlangProcessesTotal(int erlangProcessesTotal) {
    this.erlangProcessesTotal = erlangProcessesTotal;
  }

  public String getStatisticsLevel() {
    return statisticsLevel;
  }

  public void setStatisticsLevel(String statisticsLevel) {
    this.statisticsLevel = statisticsLevel;
  }

  public long getUptime() {
    return uptime;
  }

  public void setUptime(int uptime) {
    this.uptime = uptime;
  }

  public int getErlangRunQueueLength() {
    return erlangRunQueueLength;
  }

  public void setErlangRunQueueLength(int erlangRunQueueLength) {
    this.erlangRunQueueLength = erlangRunQueueLength;
  }

  public int getNumberOfProcessors() {
    return numberOfProcessors;
  }

  public void setNumberOfProcessors(int numberOfProcessors) {
    this.numberOfProcessors = numberOfProcessors;
  }

  public String getOsPid() {
    return osPid;
  }

  public void setOsPid(String osPid) {
    this.osPid = osPid;
  }

  public List<ExchangeType> getExchangeTypes() {
    return exchangeTypes;
  }

  public void setExchangeTypes(List<ExchangeType> exchangeTypes) {
    this.exchangeTypes = exchangeTypes;
  }

  public List<AuthMechanism> getAuthMechanisms() {
    return authMechanisms;
  }

  public void setAuthMechanisms(List<AuthMechanism> authMechanisms) {
    this.authMechanisms = authMechanisms;
  }

  public List<ErlangApp> getErlangApps() {
    return erlangApps;
  }

  public void setErlangApps(List<ErlangApp> erlangApps) {
    this.erlangApps = erlangApps;
  }

  public List<PluginContext> getPluginContexts() {
    return pluginContexts;
  }

  public void setPluginContexts(List<PluginContext> pluginContexts) {
    this.pluginContexts = pluginContexts;
  }

  public boolean isDiskNode() {
    return this.type.equals(DISK_TYPE);
  }

  public RateDetails getDiskFreeDetails() {
    return diskFreeDetails;
  }

  public void setDiskFreeDetails(RateDetails diskFreeDetails) {
    this.diskFreeDetails = diskFreeDetails;
  }

  public RateDetails getFdUsedDetails() {
    return fdUsedDetails;
  }

  public void setFdUsedDetails(RateDetails fdUsedDetails) {
    this.fdUsedDetails = fdUsedDetails;
  }

  public long getIoReadAvgTime() {
    return ioReadAvgTime;
  }

  public void setIoReadAvgTime(long ioReadAvgTime) {
    this.ioReadAvgTime = ioReadAvgTime;
  }

  public RateDetails getIoReadAvgTimeDetails() {
    return ioReadAvgTimeDetails;
  }

  public void setIoReadAvgTimeDetails(RateDetails ioReadAvgTimeDetails) {
    this.ioReadAvgTimeDetails = ioReadAvgTimeDetails;
  }

  public long getIoWriteAvgTime() {
    return ioWriteAvgTime;
  }

  public void setIoWriteAvgTime(long ioWriteAvgTime) {
    this.ioWriteAvgTime = ioWriteAvgTime;
  }

  public RateDetails getIoWriteAvgTimeDetails() {
    return ioWriteAvgTimeDetails;
  }

  public void setIoWriteAvgTimeDetails(RateDetails ioWriteAvgTimeDetails) {
    this.ioWriteAvgTimeDetails = ioWriteAvgTimeDetails;
  }

  public long getIoSyncAvgTime() {
    return ioSyncAvgTime;
  }

  public void setIoSyncAvgTime(long ioSyncAvgTime) {
    this.ioSyncAvgTime = ioSyncAvgTime;
  }

  public RateDetails getIoSyncAvgTimeDetails() {
    return ioSyncAvgTimeDetails;
  }

  public void setIoSyncAvgTimeDetails(RateDetails ioSyncAvgTimeDetails) {
    this.ioSyncAvgTimeDetails = ioSyncAvgTimeDetails;
  }

  public RateDetails getMemoryUsedDetails() {
    return memoryUsedDetails;
  }

  public void setMemoryUsedDetails(RateDetails memoryUsedDetails) {
    this.memoryUsedDetails = memoryUsedDetails;
  }

  public long getMnesiaDiskTransactionsCount() {
    return mnesiaDiskTransactionsCount;
  }

  public void setMnesiaDiskTransactionsCount(long mnesiaDiskTransactionsCount) {
    this.mnesiaDiskTransactionsCount = mnesiaDiskTransactionsCount;
  }

  public RateDetails getMnesiaDiskTransactionCountDetails() {
    return mnesiaDiskTransactionCountDetails;
  }

  public void setMnesiaDiskTransactionCountDetails(RateDetails mnesiaDiskTransactionCountDetails) {
    this.mnesiaDiskTransactionCountDetails = mnesiaDiskTransactionCountDetails;
  }

  public long getMnesiaRamTransactionsCount() {
    return mnesiaRamTransactionsCount;
  }

  public void setMnesiaRamTransactionsCount(long mnesiaRamTransactionsCount) {
    this.mnesiaRamTransactionsCount = mnesiaRamTransactionsCount;
  }

  public RateDetails getMnesiaRamTransactionCountDetails() {
    return mnesiaRamTransactionCountDetails;
  }

  public void setMnesiaRamTransactionCountDetails(RateDetails mnesiaRamTransactionCountDetails) {
    this.mnesiaRamTransactionCountDetails = mnesiaRamTransactionCountDetails;
  }

  public RateDetails getErlangProcessesUsedDetails() {
    return erlangProcessesUsedDetails;
  }

  public void setErlangProcessesUsedDetails(RateDetails erlangProcessesUsedDetails) {
    this.erlangProcessesUsedDetails = erlangProcessesUsedDetails;
  }

  public RateDetails getSocketsUsedDetails() {
    return socketsUsedDetails;
  }

  public void setSocketsUsedDetails(RateDetails socketsUsedDetails) {
    this.socketsUsedDetails = socketsUsedDetails;
  }

  public String getRatesMode() {
    return ratesMode;
  }

  public void setRatesMode(String ratesMode) {
    this.ratesMode = ratesMode;
  }

  public String getLogFilePath() {
    return logFilePath;
  }

  public void setLogFilePath(String logFilePath) {
    this.logFilePath = logFilePath;
  }

  public String getSaslLogFilePath() {
    return saslLogFilePath;
  }

  public void setSaslLogFilePath(String saslLogFilePath) {
    this.saslLogFilePath = saslLogFilePath;
  }

  @Override
  public String toString() {
    return "NodeInfo{" +
        "name='" + name + '\'' +
        ", type='" + type + '\'' +
        ", running=" + running +
        ", fileDescriptorsUsed=" + fileDescriptorsUsed +
        ", fileDescriptorsTotal=" + fileDescriptorsTotal +
        ", fdUsedDetails=" + fdUsedDetails +
        ", socketsUsed=" + socketsUsed +
        ", socketsTotal=" + socketsTotal +
        ", socketsUsedDetails=" + socketsUsedDetails +
        ", memoryUsed=" + memoryUsed +
        ", memoryLimit=" + memoryLimit +
        ", memoryAlarmActive=" + memoryAlarmActive +
        ", memoryUsedDetails=" + memoryUsedDetails +
        ", diskFreeLimit=" + diskFreeLimit +
        ", diskFree=" + diskFree +
        ", diskAlarmActive=" + diskAlarmActive +
        ", diskFreeDetails=" + diskFreeDetails +
        ", ioReadAvgTime=" + ioReadAvgTime +
        ", ioReadAvgTimeDetails=" + ioReadAvgTimeDetails +
        ", ioWriteAvgTime=" + ioWriteAvgTime +
        ", ioWriteAvgTimeDetails=" + ioWriteAvgTimeDetails +
        ", ioSyncAvgTime=" + ioSyncAvgTime +
        ", ioSyncAvgTimeDetails=" + ioSyncAvgTimeDetails +
        ", mnesiaDiskTransactionsCount=" + mnesiaDiskTransactionsCount +
        ", mnesiaDiskTransactionCountDetails=" + mnesiaDiskTransactionCountDetails +
        ", mnesiaRamTransactionsCount=" + mnesiaRamTransactionsCount +
        ", mnesiaRamTransactionCountDetails=" + mnesiaRamTransactionCountDetails +
        ", erlangProcessesUsed=" + erlangProcessesUsed +
        ", erlangProcessesTotal=" + erlangProcessesTotal +
        ", erlangProcessesUsedDetails=" + erlangProcessesUsedDetails +
        ", statisticsLevel='" + statisticsLevel + '\'' +
        ", ratesMode='" + ratesMode + '\'' +
        ", logFilePath='" + logFilePath + '\'' +
        ", saslLogFilePath='" + saslLogFilePath + '\'' +
        ", dbDirectoryPath='" + dbDirectoryPath + '\'' +
        ", configFilePaths=" + configFilePaths +
        ", netTicktime=" + netTicktime +
        ", enabledPlugins=" + enabledPlugins +
        ", uptime=" + uptime +
        ", erlangRunQueueLength=" + erlangRunQueueLength +
        ", numberOfProcessors=" + numberOfProcessors +
        ", osPid='" + osPid + '\'' +
        ", exchangeTypes=" + exchangeTypes +
        ", authMechanisms=" + authMechanisms +
        ", erlangApps=" + erlangApps +
        ", pluginContexts=" + pluginContexts +
        '}';
  }

  public String getDbDirectoryPath() {
    return dbDirectoryPath;
  }

  public void setDbDirectoryPath(String dbDirectoryPath) {
    this.dbDirectoryPath = dbDirectoryPath;
  }

  public List<String> getConfigFilePaths() {
    return configFilePaths;
  }

  public void setConfigFilePaths(List<String> configFilePaths) {
    this.configFilePaths = configFilePaths;
  }

  public long getNetTicktime() {
    return netTicktime;
  }

  public void setNetTicktime(long netTicktime) {
    this.netTicktime = netTicktime;
  }

  public List<String> getEnabledPlugins() {
    return enabledPlugins;
  }

  public void setEnabledPlugins(List<String> enabledPlugins) {
    this.enabledPlugins = enabledPlugins;
  }

}
