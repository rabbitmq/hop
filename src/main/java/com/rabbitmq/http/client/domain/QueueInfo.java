/*
 * Copyright 2015-2022 the original author or authors.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
//        "effective_policy_definition": {
//            "overflow": "reject-publish"
//        }
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
  @JsonProperty("effective_policy_definition")
  private Map<String, Object> effectivePolicyDefinition;
  @JsonProperty("idle_since")
  private String idleSince;

  @JsonProperty("disk_reads")
  private long diskReads = -1;
  @JsonProperty("disk_writes")
  private long diskWrites = -1;
  @JsonProperty("memory")
  private long memoryUsed = -1;
  @JsonProperty("message_bytes")
  private long messageBytes = -1;
  @JsonProperty("message_bytes_persistent")
  private long messageBytesPersistent = -1;
  @JsonProperty("message_bytes_ram")
  private long messageBytesRAM = -1;
  @JsonProperty("message_bytes_ready")
  private long messageBytesReady = -1;
  @JsonProperty("message_bytes_unacknowledged")
  private long messageBytesUnacknowledged = -1;
  @JsonProperty("messages")
  private long totalMessages = -1;
  @JsonProperty("message_stats")
  private MessageStats messageStats;
  @JsonProperty("messages_persistent")
  private long totalPersistentMessages = -1;
  @JsonProperty("messages_ram")
  private long totalTransientMessages = -1;
  @JsonProperty("messages_details")
  private RateDetails messagesDetails;
  @JsonProperty("messages_ready")
  private long messagesReady = -1;
  @JsonProperty("messages_ready_details")
  private RateDetails messagesReadyDetails;
  @JsonProperty("messages_unacknowledged")
  private long messagesUnacknowledged = -1;
  @JsonProperty("messages_unacknowledged_details")
  private RateDetails messagesUnacknowledgedDetails;
  @JsonProperty("owner_pid_details")
  private OwnerPidDetails ownerPidDetails;
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

  @JsonProperty("consumer_details")
  private List<ConsumerDetails> consumerDetails;
  @JsonProperty("single_active_consumer_tag")
  private String singleActiveConsumerTag;

  @JsonProperty("type")
  private String type;

  // for classic HA queues
  @JsonProperty("recoverable_slaves")
  private List<String> recoverableMirrors;
  @JsonProperty("slave_nodes")
  private List<String> mirrorNodes;
  @JsonProperty("synchronised_slave_nodes")
  private List<String> synchronisedMirrorNodes;

  // for quorum queues
  @JsonProperty("leader")
  private String leaderNode;
  @JsonProperty("members")
  private List<String> memberNodes;

  public QueueInfo() {
  }

  public QueueInfo(boolean durable, boolean exclusive, boolean autoDelete) {
    this(durable, exclusive, autoDelete, new HashMap<>());
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

  public Map<String, Object> getEffectivePolicyDefinition() {
    return effectivePolicyDefinition;
  }

  public void setEffectivePolicyDefinition(Map<String, Object> effectivePolicyDefinition) {
    this.effectivePolicyDefinition = effectivePolicyDefinition;
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

  public MessageStats getMessageStats() {
    return messageStats;
  }

  public void setMessageStats(MessageStats messageStats) {
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

  public RateDetails getMessagesDetails() {
    return messagesDetails;
  }

  public void setMessagesDetails(RateDetails messagesDetails) {
    this.messagesDetails = messagesDetails;
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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public List<String> getRecoverableMirrors() {
    return recoverableMirrors;
  }

  public void setRecoverableMirrors(List<String> recoverableMirrors) {
    this.recoverableMirrors = recoverableMirrors;
  }

  public List<String> getMirrorNodes() {
    return mirrorNodes;
  }

  public void setMirrorNodes(List<String> mirrorNodes) {
    this.mirrorNodes = mirrorNodes;
  }

  public List<String> getSynchronisedMirrorNodes() {
    return synchronisedMirrorNodes;
  }

  public void setSynchronisedMirrorNodes(List<String> synchronisedMirrorNodes) {
    this.synchronisedMirrorNodes = synchronisedMirrorNodes;
  }

  public String getLeaderNode() {
    return leaderNode;
  }

  public void setLeaderNode(String leaderNode) {
    this.leaderNode = leaderNode;
  }

  public List<String> getMemberNodes() {
    return memberNodes;
  }

  public void setMemberNodes(List<String> memberNodes) {
    this.memberNodes = memberNodes;
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
        ", effective_policy_definition='" + effectivePolicyDefinition + '\'' +
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
        ", type=" + type +
        '}';
  }
}
