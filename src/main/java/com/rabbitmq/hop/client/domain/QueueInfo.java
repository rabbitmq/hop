package com.rabbitmq.hop.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

//{
//        "arguments": {},
//        "auto_delete": true,
//        "backing_queue_status": {
//            "avg_ack_egress_rate": 0.0,
//            "avg_ack_ingress_rate": 0.0,
//            "avg_egress_rate": 0.0,
//            "avg_ingress_rate": 0.0,
//            "delta": [
//                "delta",
//                "undefined",
//                0,
//                "undefined"
//            ],
//            "len": 0,
//            "next_seq_id": 0,
//            "q1": 0,
//            "q2": 0,
//            "q3": 0,
//            "q4": 0,
//            "target_ram_count": "infinity"
//        },
//        "consumer_utilisation": "",
//        "consumers": 0,
//        "disk_reads": 0,
//        "disk_writes": 0,
//        "durable": false,
//        "exclusive_consumer_tag": "",
//        "idle_since": "2015-03-21 0:04:25",
//        "memory": 13960,
//        "message_bytes": 0,
//        "message_bytes_persistent": 0,
//        "message_bytes_ram": 0,
//        "message_bytes_ready": 0,
//        "message_bytes_unacknowledged": 0,
//        "messages": 0,
//        "messages_details": {
//            "rate": 0.0
//        },
//        "messages_persistent": 0,
//        "messages_ram": 0,
//        "messages_ready": 0,
//        "messages_ready_details": {
//            "rate": 0.0
//        },
//        "messages_ready_ram": 0,
//        "messages_unacknowledged": 0,
//        "messages_unacknowledged_details": {
//            "rate": 0.0
//        },
//        "messages_unacknowledged_ram": 0,
//        "name": "langohr.tests2.queues.non-auto-deleted1",
//        "node": "rabbit@mercurio",
//        "policy": "",
//        "recoverable_slaves": "",
//        "state": "running",
//        "vhost": "/"
//    }

@JsonIgnoreProperties("backing_queue_status")
public class QueueInfo {
  private String vhost;
  private String name;
  private boolean durable;
  private boolean exclusive;
  @JsonProperty("auto_delete")
  private boolean autoDelete;
  private Map<String, Object> arguments;


  private String node;
  @JsonProperty("exclusive_consumer_tag")
  private String exclusiveConsumerTag;
  private String state;
  private String policy;
  @JsonProperty("idle_since")
  private String idleSince;
  
  @JsonProperty("disk_reads")
  private long diskReads;
  @JsonProperty("disk_writes")
  private long diskWrites;
  @JsonProperty("memory")
  private long memoryUsed;
  @JsonProperty("message_bytes")
  private long messageBytes;
  @JsonProperty("message_bytes_persistent")
  private long messageBytesPersistent;
  @JsonProperty("message_bytes_ram")
  private long messageBytesRAM;
  @JsonProperty("message_bytes_ready")
  private long messageBytesReady;
  @JsonProperty("message_bytes_unacknowledged")
  private long messageBytesUnacknowledged;
  @JsonProperty("messages")
  private long totalMessages;
  @JsonProperty("message_stats")
  private RateDetails messageStats;
  @JsonProperty("messages_persistent")
  private long totalPersistentMessages;
  @JsonProperty("messages_ram")
  private long totalTransientMessages;
  @JsonProperty("messages_ready")
  private long messagesReady;
  @JsonProperty("messages_ready_details")
  private RateDetails messagesReadyDetails;
  @JsonProperty("messages_unacknowledged")
  private long messagesUnacknowledged;
  @JsonProperty("messages_unacknowledged_details")
  private RateDetails messagesUnacknowledgedDetails;  
  // TODO: messages_ready_ram
  // TODO: recoverable_slaves

  @JsonProperty("consumers")
  private long consumerCount;
  // ignored due to rabbitmq/rabbitmq-management#26
  @JsonIgnore
  @JsonProperty("consumer_utilisation")
  private long consumerUtilisation;

  // TODO: should we expose backing_queue_status,
  //       which is an implementation detail?


  public String getVhost() {
    return vhost;
  }

  public void setVhost(String vhost) {
    this.vhost = vhost;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isDurable() {
    return durable;
  }

  public void setDurable(boolean durable) {
    this.durable = durable;
  }

  public boolean isExclusive() {
    return exclusive;
  }

  public void setExclusive(boolean exclusive) {
    this.exclusive = exclusive;
  }

  public boolean isAutoDelete() {
    return autoDelete;
  }

  public void setAutoDelete(boolean autoDelete) {
    this.autoDelete = autoDelete;
  }

  public Map<String, Object> getArguments() {
    return arguments;
  }

  public void setArguments(Map<String, Object> arguments) {
    this.arguments = arguments;
  }

  public String getNode() {
    return node;
  }

  public void setNode(String node) {
    this.node = node;
  }

  public String getExclusiveConsumerTag() {
    return exclusiveConsumerTag;
  }

  public void setExclusiveConsumerTag(String exclusiveConsumerTag) {
    this.exclusiveConsumerTag = exclusiveConsumerTag;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getPolicy() {
    return policy;
  }

  public void setPolicy(String policy) {
    this.policy = policy;
  }

  public String getIdleSince() {
    return idleSince;
  }

  public void setIdleSince(String idleSince) {
    this.idleSince = idleSince;
  }

  public long getDiskReads() {
    return diskReads;
  }

  public void setDiskReads(long diskReads) {
    this.diskReads = diskReads;
  }

  public long getDiskWrites() {
    return diskWrites;
  }

  public void setDiskWrites(long diskWrites) {
    this.diskWrites = diskWrites;
  }

  public long getMemoryUsed() {
    return memoryUsed;
  }

  public void setMemoryUsed(long memoryUsed) {
    this.memoryUsed = memoryUsed;
  }

  public long getMessageBytes() {
    return messageBytes;
  }

  public void setMessageBytes(long messageBytes) {
    this.messageBytes = messageBytes;
  }

  public long getMessageBytesPersistent() {
    return messageBytesPersistent;
  }

  public void setMessageBytesPersistent(long messageBytesPersistent) {
    this.messageBytesPersistent = messageBytesPersistent;
  }

  public long getMessageBytesRAM() {
    return messageBytesRAM;
  }

  public void setMessageBytesRAM(long messageBytesRAM) {
    this.messageBytesRAM = messageBytesRAM;
  }

  public long getMessageBytesReady() {
    return messageBytesReady;
  }

  public void setMessageBytesReady(long messageBytesReady) {
    this.messageBytesReady = messageBytesReady;
  }

  public long getMessageBytesUnacknowledged() {
    return messageBytesUnacknowledged;
  }

  public void setMessageBytesUnacknowledged(long messageBytesUnacknowledged) {
    this.messageBytesUnacknowledged = messageBytesUnacknowledged;
  }

  public long getTotalMessages() {
    return totalMessages;
  }

  public void setTotalMessages(long totalMessages) {
    this.totalMessages = totalMessages;
  }

  public RateDetails getMessageStats() {
    return messageStats;
  }

  public void setMessageStats(RateDetails messageStats) {
    this.messageStats = messageStats;
  }

  public long getTotalPersistentMessages() {
    return totalPersistentMessages;
  }

  public void setTotalPersistentMessages(long totalPersistentMessages) {
    this.totalPersistentMessages = totalPersistentMessages;
  }

  public long getTotalTransientMessages() {
    return totalTransientMessages;
  }

  public void setTotalTransientMessages(long totalTransientMessages) {
    this.totalTransientMessages = totalTransientMessages;
  }

  public long getMessagesReady() {
    return messagesReady;
  }

  public void setMessagesReady(long messagesReady) {
    this.messagesReady = messagesReady;
  }

  public RateDetails getMessagesReadyDetails() {
    return messagesReadyDetails;
  }

  public void setMessagesReadyDetails(RateDetails messagesReadyDetails) {
    this.messagesReadyDetails = messagesReadyDetails;
  }

  public long getMessagesUnacknowledged() {
    return messagesUnacknowledged;
  }

  public void setMessagesUnacknowledged(long messagesUnacknowledged) {
    this.messagesUnacknowledged = messagesUnacknowledged;
  }

  public RateDetails getMessagesUnacknowledgedDetails() {
    return messagesUnacknowledgedDetails;
  }

  public void setMessagesUnacknowledgedDetails(RateDetails messagesUnacknowledgedDetails) {
    this.messagesUnacknowledgedDetails = messagesUnacknowledgedDetails;
  }

  public long getConsumerCount() {
    return consumerCount;
  }

  public void setConsumerCount(long consumerCount) {
    this.consumerCount = consumerCount;
  }

  public long getConsumerUtilisation() {
    return consumerUtilisation;
  }

  public void setConsumerUtilisation(long consumerUtilisation) {
    this.consumerUtilisation = consumerUtilisation;
  }

  @Override
  public String toString() {
    return "QueueInfo{" +
        "vhost='" + vhost + '\'' +
        ", name='" + name + '\'' +
        ", durable=" + durable +
        ", exclusive=" + exclusive +
        ", autoDelete=" + autoDelete +
        ", arguments=" + arguments +
        ", node='" + node + '\'' +
        ", exclusiveConsumerTag='" + exclusiveConsumerTag + '\'' +
        ", state='" + state + '\'' +
        ", policy='" + policy + '\'' +
        ", idleSince='" + idleSince + '\'' +
        ", diskReads=" + diskReads +
        ", diskWrites=" + diskWrites +
        ", memoryUsed=" + memoryUsed +
        ", messageBytes=" + messageBytes +
        ", messageBytesPersistent=" + messageBytesPersistent +
        ", messageBytesRAM=" + messageBytesRAM +
        ", messageBytesReady=" + messageBytesReady +
        ", messageBytesUnacknowledged=" + messageBytesUnacknowledged +
        ", totalMessages=" + totalMessages +
        ", messageStats=" + messageStats +
        ", totalPersistentMessages=" + totalPersistentMessages +
        ", totalTransientMessages=" + totalTransientMessages +
        ", messagesReady=" + messagesReady +
        ", messagesReadyDetails=" + messagesReadyDetails +
        ", messagesUnacknowledged=" + messagesUnacknowledged +
        ", messagesUnacknowledgedDetails=" + messagesUnacknowledgedDetails +
        ", consumerCount=" + consumerCount +
        '}';
  }
}
