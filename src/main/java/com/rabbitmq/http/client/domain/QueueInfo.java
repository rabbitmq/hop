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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
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
//        introduced in RabbitMQ 3.6.0; older versions have to check owner_pid_details.
//        "exclusive": false,
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

@SuppressWarnings("unused")
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
  private Long diskReads = null;
  @JsonProperty("disk_writes")
  private Long diskWrites = null;
  @JsonProperty("memory")
  private Long memoryUsed = null;
  @JsonProperty("message_bytes")
  private Long messageBytes = null;
  @JsonProperty("message_bytes_persistent")
  private Long messageBytesPersistent = null;
  @JsonProperty("message_bytes_ram")
  private Long messageBytesRAM = null;
  @JsonProperty("message_bytes_ready")
  private Long messageBytesReady = null;
  @JsonProperty("message_bytes_unacknowledged")
  private Long messageBytesUnacknowledged = null;
  @JsonProperty("messages")
  private Long totalMessages = null;
  @JsonProperty("message_stats")
  private MessageStats messageStats;
  @JsonProperty("messages_persistent")
  private Long totalPersistentMessages = null;
  @JsonProperty("messages_ram")
  private Long totalTransientMessages = null;
  @JsonProperty("messages_ready")
  private Long messagesReady = null;
  @JsonProperty("messages_ready_details")
  private RateDetails messagesReadyDetails;
  @JsonProperty("messages_unacknowledged")
  private Long messagesUnacknowledged = null;
  @JsonProperty("messages_unacknowledged_details")
  private RateDetails messagesUnacknowledgedDetails;
  @JsonProperty("owner_pid_details")
  private OwnerPidDetails ownerPidDetails;
  // TODO: messages_ready_ram
  // TODO: recoverable_slaves

  @JsonProperty("consumers")
  private Long consumerCount = null;
  // ignored due to rabbitmq/rabbitmq-management#26
  @JsonIgnore
  @JsonProperty("consumer_utilisation")
  private Long consumerUtilisation = null;

  // TODO: should we expose backing_queue_status,
  //       which is an implementation detail?

  @JsonProperty("consumer_details")
  private List<ConsumerDetails> consumerDetails;
  @JsonProperty("single_active_consumer_tag")
  private String singleActiveConsumerTag;

  public QueueInfo() {
  }

  public QueueInfo(boolean durable, boolean exclusive, boolean autoDelete) {
    this(durable, exclusive, autoDelete, new HashMap<String, Object>());
  }

  public QueueInfo(boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments) {
    this.durable = durable;
    this.exclusive = exclusive;
    this.autoDelete = autoDelete;
    this.arguments = arguments;
  }

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
    // versions prior to 3.6.0 did not have an exclusive field in the payload. Fallback to ownerPidDetails for
    // compatibility.
    return exclusive || (ownerPidDetails != null);
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

  public Long getDiskReads() {
    return diskReads;
  }

  public void setDiskReads(Long diskReads) {
    this.diskReads = diskReads;
  }

  public Long getDiskWrites() {
    return diskWrites;
  }

  public void setDiskWrites(Long diskWrites) {
    this.diskWrites = diskWrites;
  }

  public Long getMemoryUsed() {
    return memoryUsed;
  }

  public void setMemoryUsed(Long memoryUsed) {
    this.memoryUsed = memoryUsed;
  }

  public Long getMessageBytes() {
    return messageBytes;
  }

  public void setMessageBytes(Long messageBytes) {
    this.messageBytes = messageBytes;
  }

  public Long getMessageBytesPersistent() {
    return messageBytesPersistent;
  }

  public void setMessageBytesPersistent(Long messageBytesPersistent) {
    this.messageBytesPersistent = messageBytesPersistent;
  }

  public Long getMessageBytesRAM() {
    return messageBytesRAM;
  }

  public void setMessageBytesRAM(Long messageBytesRAM) {
    this.messageBytesRAM = messageBytesRAM;
  }

  public Long getMessageBytesReady() {
    return messageBytesReady;
  }

  public void setMessageBytesReady(Long messageBytesReady) {
    this.messageBytesReady = messageBytesReady;
  }

  public Long getMessageBytesUnacknowledged() {
    return messageBytesUnacknowledged;
  }

  public void setMessageBytesUnacknowledged(Long messageBytesUnacknowledged) {
    this.messageBytesUnacknowledged = messageBytesUnacknowledged;
  }

  public Long getTotalMessages() {
    return totalMessages;
  }

  public void setTotalMessages(Long totalMessages) {
    this.totalMessages = totalMessages;
  }

  public MessageStats getMessageStats() {
    return messageStats;
  }

  public void setMessageStats(MessageStats messageStats) {
    this.messageStats = messageStats;
  }

  public Long getTotalPersistentMessages() {
    return totalPersistentMessages;
  }

  public void setTotalPersistentMessages(Long totalPersistentMessages) {
    this.totalPersistentMessages = totalPersistentMessages;
  }

  public Long getTotalTransientMessages() {
    return totalTransientMessages;
  }

  public void setTotalTransientMessages(Long totalTransientMessages) {
    this.totalTransientMessages = totalTransientMessages;
  }

  public Long getMessagesReady() {
    return messagesReady;
  }

  public void setMessagesReady(Long messagesReady) {
    this.messagesReady = messagesReady;
  }

  public RateDetails getMessagesReadyDetails() {
    return messagesReadyDetails;
  }

  public void setMessagesReadyDetails(RateDetails messagesReadyDetails) {
    this.messagesReadyDetails = messagesReadyDetails;
  }

  public Long getMessagesUnacknowledged() {
    return messagesUnacknowledged;
  }

  public void setMessagesUnacknowledged(Long messagesUnacknowledged) {
    this.messagesUnacknowledged = messagesUnacknowledged;
  }

  public RateDetails getMessagesUnacknowledgedDetails() {
    return messagesUnacknowledgedDetails;
  }

  public void setMessagesUnacknowledgedDetails(RateDetails messagesUnacknowledgedDetails) {
    this.messagesUnacknowledgedDetails = messagesUnacknowledgedDetails;
  }

  public Long getConsumerCount() {
    return consumerCount;
  }

  public void setConsumerCount(Long consumerCount) {
    this.consumerCount = consumerCount;
  }

  public Long getConsumerUtilisation() {
    return consumerUtilisation;
  }

  public void setConsumerUtilisation(Long consumerUtilisation) {
    this.consumerUtilisation = consumerUtilisation;
  }

  public void setOwnerPidDetails(OwnerPidDetails ownerPidDetails) {
    this.ownerPidDetails = ownerPidDetails;
  }

  public List<ConsumerDetails> getConsumerDetails() {
    return consumerDetails;
  }

  public void setConsumerDetails(List<ConsumerDetails> consumerDetails) {
    this.consumerDetails = consumerDetails;
  }

  public String getSingleActiveConsumerTag() {
    return singleActiveConsumerTag;
  }

  public void setSingleActiveConsumerTag(String singleActiveConsumerTag) {
    this.singleActiveConsumerTag = singleActiveConsumerTag;
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
        ", singleActiveConsumerTag=" + singleActiveConsumerTag +
        '}';
  }
}
