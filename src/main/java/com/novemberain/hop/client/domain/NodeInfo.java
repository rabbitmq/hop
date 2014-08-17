package com.novemberain.hop.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// TODO: deserialize partitions
@JsonIgnoreProperties({"partitions"})
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

  @JsonProperty("sockets_used")
  private int socketsUsed;
  @JsonProperty("sockets_total")
  private int socketsTotal;

  @JsonProperty("mem_used")
  private long memoryUsed;
  @JsonProperty("mem_limit")
  private long memoryLimit;
  @JsonProperty("mem_alarm")
  private boolean memoryAlarmActive;

  @JsonProperty("disk_free_limit")
  private long diskFreeLimit;
  @JsonProperty("disk_free")
  private long diskFree;
  @JsonProperty("disk_free_alarm")
  private boolean diskAlarmActive;

  @JsonProperty("proc_used")
  private int erlangProcessesUsed;
  @JsonProperty("proc_total")
  private int erlangProcessesTotal;

  @JsonProperty("statistics_level")
  private String statisticsLevel;

  private int uptime;
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

  public int getErlangProcessesUsed() {
    return erlangProcessesUsed;
  }

  public void setErlangProcessesUsed(int erlangProcessesUsed) {
    this.erlangProcessesUsed = erlangProcessesUsed;
  }

  public int getErlangProcessesTotal() {
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

  public int getUptime() {
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

  @Override
  public String toString() {
    return "NodeInfo{" +
        "name='" + name + '\'' +
        ", type='" + type + '\'' +
        ", running=" + running +
        ", fileDescriptorsUsed=" + fileDescriptorsUsed +
        ", fileDescriptorsTotal=" + fileDescriptorsTotal +
        ", socketsUsed=" + socketsUsed +
        ", socketsTotal=" + socketsTotal +
        ", memoryUsed=" + memoryUsed +
        ", memoryLimit=" + memoryLimit +
        ", memoryAlarmActive=" + memoryAlarmActive +
        ", diskFreeLimit=" + diskFreeLimit +
        ", diskFree=" + diskFree +
        ", diskAlarmActive=" + diskAlarmActive +
        ", erlangProcessesUsed=" + erlangProcessesUsed +
        ", erlangProcessesTotal=" + erlangProcessesTotal +
        ", statisticsLevel='" + statisticsLevel + '\'' +
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
}
