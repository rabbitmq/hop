/*
 * Copyright 2025 the original author or authors.
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
package com.rabbitmq.http.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.http.client.domain.ConsumerDetails;
import com.rabbitmq.http.client.domain.QueueInfo;
import com.rabbitmq.http.client.domain.ShovelDetails;
import com.rabbitmq.http.client.domain.ShovelInfo;
import com.rabbitmq.http.client.domain.UserInfo;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class JsonMappingTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = JsonUtils.createDefaultObjectMapper();
  }

  @Test
  void jsonDocumentForQueueWithNaNMessageCountShouldReturnNegativeOneForMessageCount()
      throws IOException {
    // when: "JSON document for queue has no ready messages count field"
    QueueInfo queue = mapper.readValue(JSON_QUEUE_NO_READY_MESSAGES, QueueInfo.class);

    // then: "the field value should be -1 in the Java object"
    assertThat(queue.getMessagesReady()).isEqualTo(-1);
  }

  @Test
  void jsonDocumentForQueueWithDefinedMessageCountShouldReturnAppropriateValue()
      throws IOException {
    // when: "JSON document for queue has a ready messages count field with a value"
    QueueInfo queue = mapper.readValue(JSON_QUEUE_SOME_READY_MESSAGES, QueueInfo.class);

    // then: "the field value of the Java object should be the same as in the JSON document"
    assertThat(queue.getMessagesReady()).isEqualTo(1000);
  }

  @Test
  void fieldsForClassicHaQueueShouldBeMappedCorrectly() throws IOException {
    // when: "JSON document for classic HA queue has details on nodes"
    QueueInfo queue = mapper.readValue(JSON_CLASSIC_HA_QUEUE, QueueInfo.class);

    // then: "the Java object should be filled accordingly"
    assertThat(queue.getType()).isEqualTo("classic");
    assertThat(queue.getRecoverableMirrors()).containsExactly("rabbit-3@host3", "rabbit-2@host2");
    assertThat(queue.getMirrorNodes()).containsExactly("rabbit-3@host3", "rabbit-2@host2");
    assertThat(queue.getSynchronisedMirrorNodes())
        .containsExactly("rabbit-3@host3", "rabbit-2@host2");
  }

  @Test
  void fieldsForQuorumQueueShouldBeMappedCorrectly() throws IOException {
    // when: "JSON document for quorum queue has details on nodes"
    QueueInfo queue = mapper.readValue(JSON_QUORUM_QUEUE, QueueInfo.class);

    // then: "the Java object should be filled accordingly"
    assertThat(queue.getType()).isEqualTo("quorum");
    assertThat(queue.getLeaderNode()).isEqualTo("rabbit-1@host1");
    assertThat(queue.getMemberNodes())
        .containsExactly("rabbit-3@host3", "rabbit-2@host2", "rabbit-1@host1");
  }

  @ParameterizedTest
  @ValueSource(strings = {JSON_USER_WITH_USER_TAGS_AS_ARRAY, JSON_USER_WITH_USER_TAGS_AS_STRING})
  void userTagsShouldBeDeserializedFromArrayOrString(String json) throws IOException {
    // when: "JSON document for user with user tags as array or as string"
    UserInfo user = mapper.readValue(json, UserInfo.class);

    // then: "the Java object should be filled accordingly"
    assertThat(user.getTags()).containsExactly("monitoring", "management");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {JSON_USER_WITH_USER_TAGS_AS_EMPTY_ARRAY, JSON_USER_WITH_USER_TAGS_AS_EMPTY_STRING})
  void userTagsShouldBeDeserializedFromEmptyArrayOrEmptyString(String json) throws IOException {
    // when: "JSON document for user with user tags as empty array or as empty string"
    UserInfo user = mapper.readValue(json, UserInfo.class);

    // then: "the user tags list of Java object is empty"
    assertThat(user.getTags()).isEmpty();
  }

  @Test
  void consumerDetailsShouldDeserializeWithoutErrors() throws IOException {
    // when: "JSON document for consumer"
    ConsumerDetails consumer = mapper.readValue(JSON_CONSUMER_DETAILS, ConsumerDetails.class);

    // then: "consumer details properties should be set correctly"
    assertThat(consumer.getConsumerTag()).isEqualTo("amq.ctag-Od3nR5Kglfbkm6dB3Jw-dg");
    assertThat(consumer.getPrefetchCount()).isZero();
    assertThat(consumer.getChannelDetails().getConnectionName())
        .isEqualTo("127.0.0.1:40548 -> 127.0.0.1:5672");
    assertThat(consumer.getChannelDetails().getName())
        .isEqualTo("127.0.0.1:40548 -> 127.0.0.1:5672 (1)");
    assertThat(consumer.getChannelDetails().getNode()).isEqualTo("rabbit@my-host");
    assertThat(consumer.getChannelDetails().getNumber()).isEqualTo(1);
    assertThat(consumer.getChannelDetails().getPeerHost()).isEqualTo("127.0.0.1");
    assertThat(consumer.getChannelDetails().getPeerPort()).isEqualTo(40548);
    assertThat(consumer.getChannelDetails().getUser()).isEqualTo("guest");
    assertThat(consumer.isExclusive()).isFalse();
    assertThat(consumer.getArguments()).isEmpty();
    assertThat(consumer.getQueueDetails().getName()).isEqualTo("amq.gen-ofOF2lL5hhOzhchiFCPhjg");
    assertThat(consumer.getQueueDetails().getVhost()).isEqualTo("/");
    assertThat(consumer.isActive()).isTrue();
    assertThat(consumer.getActivityStatus()).isEqualTo("up");
    assertThat(consumer.isAckRequired()).isTrue();
    assertThat(consumer.getConsumerTimeout()).isEqualTo(1800000);
  }

  @Test
  void channelDetailsWithUndefinedPeerPortShouldDeserializeWithoutErrors() throws IOException {
    // when: "JSON document for consumer with undefined channel peer port"
    ConsumerDetails consumer =
        mapper.readValue(
            JSON_CONSUMER_DETAILS_WITH_UNDEFINED_CHANNEL_PEER_PORT, ConsumerDetails.class);

    // then: "the channel peer port should be 0"
    assertThat(consumer.getChannelDetails().getPeerPort()).isZero();
  }

  @Test
  void channelDetailsWithPeerPortShouldDeserializeWithoutErrors() throws IOException {
    // when: "JSON document for consumer with correct channel peer port"
    ConsumerDetails consumer = mapper.readValue(JSON_CONSUMER_DETAILS, ConsumerDetails.class);

    // then: "channel details properties should be set correctly"
    assertThat(consumer.getChannelDetails().getConnectionName())
        .isEqualTo("127.0.0.1:40548 -> 127.0.0.1:5672");
    assertThat(consumer.getChannelDetails().getName())
        .isEqualTo("127.0.0.1:40548 -> 127.0.0.1:5672 (1)");
    assertThat(consumer.getChannelDetails().getNumber()).isEqualTo(1);
    assertThat(consumer.getChannelDetails().getPeerHost()).isEqualTo("127.0.0.1");
    assertThat(consumer.getChannelDetails().getPeerPort()).isEqualTo(40548);
  }

  @Test
  void shovelDetailsSourcePrefetchCountNotPresentIfNotSet() throws IOException {
    // when: "basic topology with null sourcePrefetchCount"
    ShovelDetails details = new ShovelDetails("amqp://", "amqp://", 30, true, null);
    details.setSourceQueue("queue1");
    details.setDestinationExchange("exchange1");
    String json = mapper.writeValueAsString(new ShovelInfo("shovel1", details));
    @SuppressWarnings("unchecked")
    Map<String, Object> body = mapper.readValue(json, Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> value = (Map<String, Object>) body.get("value");

    // then: "the json will not include src-prefetch-count"
    assertThat(value).doesNotContainKey("src-prefetch-count");
  }

  @Test
  void shovelDetailsDestinationAddTimestampHeaderNotSentIfNotSet() throws IOException {
    // when: "basic topology with null destinationAddTimestampHeader"
    ShovelDetails details = new ShovelDetails("amqp://", "amqp://", 30, true, null);
    details.setSourceQueue("queue1");
    details.setDestinationExchange("exchange1");
    String json = mapper.writeValueAsString(new ShovelInfo("shovel1", details));
    @SuppressWarnings("unchecked")
    Map<String, Object> body = mapper.readValue(json, Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> value = (Map<String, Object>) body.get("value");

    // then: "the json will not include dest-add-timestamp-header"
    assertThat(value).doesNotContainKey("dest-add-timestamp-header");
  }

  // RabbitMQ 3.9+
  private static final String JSON_USER_WITH_USER_TAGS_AS_ARRAY =
      "{\n"
          + "      \"name\": \"user-management\",\n"
          + "      \"password_hash\": \"z9+SFmbi/MyQuz11Xwr5dgb5s6/tw00GIQR4NNYZWMDrQu3E\",\n"
          + "      \"hashing_algorithm\": \"rabbit_password_hashing_sha256\",\n"
          + "      \"tags\": [\n"
          + "        \"monitoring\",\n"
          + "        \"management\"\n"
          + "      ],\n"
          + "      \"limits\": {}\n"
          + "    }";

  // RabbitMQ 3.8-
  private static final String JSON_USER_WITH_USER_TAGS_AS_STRING =
      "{\n"
          + "      \"name\": \"user-management\",\n"
          + "      \"password_hash\": \"AEejMbwFJBmSqG+OUd1hT1wvuLmOoJQ02xkzYzLsDe3iY1HQ\",\n"
          + "      \"hashing_algorithm\": \"rabbit_password_hashing_sha256\",\n"
          + "      \"tags\": \"monitoring,management\"\n"
          + "    }";

  // RabbitMQ 3.9+
  private static final String JSON_USER_WITH_USER_TAGS_AS_EMPTY_ARRAY =
      "{\n"
          + "      \"name\": \"user-management\",\n"
          + "      \"password_hash\": \"z9+SFmbi/MyQuz11Xwr5dgb5s6/tw00GIQR4NNYZWMDrQu3E\",\n"
          + "      \"hashing_algorithm\": \"rabbit_password_hashing_sha256\",\n"
          + "      \"tags\": [],\n"
          + "      \"limits\": {}\n"
          + "    }";

  // RabbitMQ 3.8-
  private static final String JSON_USER_WITH_USER_TAGS_AS_EMPTY_STRING =
      "{\n"
          + "      \"name\": \"user-management\",\n"
          + "      \"password_hash\": \"AEejMbwFJBmSqG+OUd1hT1wvuLmOoJQ02xkzYzLsDe3iY1HQ\",\n"
          + "      \"hashing_algorithm\": \"rabbit_password_hashing_sha256\",\n"
          + "      \"tags\": \"\"\n"
          + "    }";

  private static final String JSON_QUEUE_NO_READY_MESSAGES =
      "   {\n"
          + "      \"arguments\":{\n"
          + "         \n"
          + "      },\n"
          + "      \"auto_delete\":false,\n"
          + "      \"backing_queue_status\":{\n"
          + "         \"avg_ack_egress_rate\":0.0,\n"
          + "         \"avg_ack_ingress_rate\":0.0,\n"
          + "         \"avg_egress_rate\":0.0,\n"
          + "         \"avg_ingress_rate\":0.0,\n"
          + "         \"delta\":[\n"
          + "            \"delta\",\n"
          + "            \"undefined\",\n"
          + "            0,\n"
          + "            0,\n"
          + "            \"undefined\"\n"
          + "         ],\n"
          + "         \"len\":0,\n"
          + "         \"mode\":\"default\",\n"
          + "         \"next_seq_id\":0,\n"
          + "         \"q1\":0,\n"
          + "         \"q2\":0,\n"
          + "         \"q3\":0,\n"
          + "         \"q4\":0,\n"
          + "         \"target_ram_count\":\"infinity\"\n"
          + "      },\n"
          + "      \"consumer_utilisation\":null,\n"
          + "      \"consumers\":0,\n"
          + "      \"durable\":true,\n"
          + "      \"effective_policy_definition\":{\n"
          + "         \n"
          + "      },\n"
          + "      \"exclusive\":false,\n"
          + "      \"exclusive_consumer_tag\":null,\n"
          + "      \"garbage_collection\":{\n"
          + "         \"fullsweep_after\":65535,\n"
          + "         \"max_heap_size\":0,\n"
          + "         \"min_bin_vheap_size\":46422,\n"
          + "         \"min_heap_size\":233,\n"
          + "         \"minor_gcs\":2\n"
          + "      },\n"
          + "      \"head_message_timestamp\":null,\n"
          + "      \"idle_since\":\"2020-10-08 7:35:55\",\n"
          + "      \"memory\":18260,\n"
          + "      \"message_bytes\":0,\n"
          + "      \"message_bytes_paged_out\":0,\n"
          + "      \"message_bytes_persistent\":0,\n"
          + "      \"message_bytes_ram\":0,\n"
          + "      \"message_bytes_ready\":0,\n"
          + "      \"message_bytes_unacknowledged\":0,\n"
          + "      \"messages\":0,\n"
          + "      \"messages_details\":{\n"
          + "         \"rate\":0.0\n"
          + "      },\n"
          + "      \"messages_paged_out\":0,\n"
          + "      \"messages_persistent\":0,\n"
          + "      \"messages_ram\":0,\n"
          + "      \"messages_ready_details\":{\n"
          + "         \"rate\":0.0\n"
          + "      },\n"
          + "      \"messages_ready_ram\":0,\n"
          + "      \"messages_unacknowledged\":0,\n"
          + "      \"messages_unacknowledged_details\":{\n"
          + "         \"rate\":0.0\n"
          + "      },\n"
          + "      \"messages_unacknowledged_ram\":0,\n"
          + "      \"name\":\"queue1\",\n"
          + "      \"operator_policy\":null,\n"
          + "      \"policy\":null,\n"
          + "      \"recoverable_slaves\":null,\n"
          + "      \"reductions\":4474,\n"
          + "      \"reductions_details\":{\n"
          + "         \"rate\":0.0\n"
          + "      },\n"
          + "      \"single_active_consumer_tag\":null,\n"
          + "      \"state\":\"running\",\n"
          + "      \"type\":\"classic\",\n"
          + "      \"vhost\":\"vh1\"\n"
          + "   }\n";

  private static final String JSON_QUEUE_SOME_READY_MESSAGES =
      "   {\n"
          + "      \"arguments\":{\n"
          + "         \n"
          + "      },\n"
          + "      \"auto_delete\":false,\n"
          + "      \"backing_queue_status\":{\n"
          + "         \"avg_ack_egress_rate\":0.0,\n"
          + "         \"avg_ack_ingress_rate\":0.0,\n"
          + "         \"avg_egress_rate\":0.0,\n"
          + "         \"avg_ingress_rate\":0.0,\n"
          + "         \"delta\":[\n"
          + "            \"delta\",\n"
          + "            \"undefined\",\n"
          + "            0,\n"
          + "            0,\n"
          + "            \"undefined\"\n"
          + "         ],\n"
          + "         \"len\":0,\n"
          + "         \"mode\":\"default\",\n"
          + "         \"next_seq_id\":0,\n"
          + "         \"q1\":0,\n"
          + "         \"q2\":0,\n"
          + "         \"q3\":0,\n"
          + "         \"q4\":0,\n"
          + "         \"target_ram_count\":\"infinity\"\n"
          + "      },\n"
          + "      \"consumer_utilisation\":null,\n"
          + "      \"consumers\":0,\n"
          + "      \"durable\":true,\n"
          + "      \"effective_policy_definition\":{\n"
          + "         \n"
          + "      },\n"
          + "      \"exclusive\":false,\n"
          + "      \"exclusive_consumer_tag\":null,\n"
          + "      \"garbage_collection\":{\n"
          + "         \"fullsweep_after\":65535,\n"
          + "         \"max_heap_size\":0,\n"
          + "         \"min_bin_vheap_size\":46422,\n"
          + "         \"min_heap_size\":233,\n"
          + "         \"minor_gcs\":2\n"
          + "      },\n"
          + "      \"head_message_timestamp\":null,\n"
          + "      \"idle_since\":\"2020-10-08 7:35:55\",\n"
          + "      \"memory\":18260,\n"
          + "      \"message_bytes\":0,\n"
          + "      \"message_bytes_paged_out\":0,\n"
          + "      \"message_bytes_persistent\":0,\n"
          + "      \"message_bytes_ram\":0,\n"
          + "      \"message_bytes_ready\":0,\n"
          + "      \"message_bytes_unacknowledged\":0,\n"
          + "      \"messages\":0,\n"
          + "      \"messages_details\":{\n"
          + "         \"rate\":0.0\n"
          + "      },\n"
          + "      \"messages_paged_out\":0,\n"
          + "      \"messages_persistent\":0,\n"
          + "      \"messages_ram\":0,\n"
          + "      \"messages_ready\":1000,\n"
          + "      \"messages_ready_details\":{\n"
          + "         \"rate\":0.0\n"
          + "      },\n"
          + "      \"messages_ready_ram\":0,\n"
          + "      \"messages_unacknowledged\":0,\n"
          + "      \"messages_unacknowledged_details\":{\n"
          + "         \"rate\":0.0\n"
          + "      },\n"
          + "      \"messages_unacknowledged_ram\":0,\n"
          + "      \"name\":\"queue1\",\n"
          + "      \"operator_policy\":null,\n"
          + "      \"policy\":null,\n"
          + "      \"recoverable_slaves\":null,\n"
          + "      \"reductions\":4474,\n"
          + "      \"reductions_details\":{\n"
          + "         \"rate\":0.0\n"
          + "      },\n"
          + "      \"single_active_consumer_tag\":null,\n"
          + "      \"state\":\"running\",\n"
          + "      \"type\":\"classic\",\n"
          + "      \"vhost\":\"vh1\"\n"
          + "   }\n";

  private static final String JSON_CLASSIC_HA_QUEUE =
      "{\n"
          + "  \"name\": \"ha-classic\",\n"
          + "  \"node\": \"rabbit-1@host1\",\n"
          + "  \"policy\": \"ha\",\n"
          + "  \"recoverable_slaves\": [\n"
          + "    \"rabbit-3@host3\",\n"
          + "    \"rabbit-2@host2\"\n"
          + "  ],\n"
          + "  \"slave_nodes\": [\n"
          + "    \"rabbit-3@host3\",\n"
          + "    \"rabbit-2@host2\"\n"
          + "  ],\n"
          + "  \"state\": \"running\",\n"
          + "  \"synchronised_slave_nodes\": [\n"
          + "    \"rabbit-3@host3\",\n"
          + "    \"rabbit-2@host2\"\n"
          + "  ],\n"
          + "  \"type\": \"classic\",\n"
          + "  \"vhost\": \"/\"\n"
          + "}";

  private static final String JSON_QUORUM_QUEUE =
      "{\n"
          + "  \"leader\": \"rabbit-1@host1\",\n"
          + "  \"members\": [\n"
          + "    \"rabbit-3@host3\",\n"
          + "    \"rabbit-2@host2\",\n"
          + "    \"rabbit-1@host1\"\n"
          + "  ],\n"
          + "  \"name\": \"quorum-queue\",\n"
          + "  \"node\": \"rabbit-1@host1\",\n"
          + "  \"online\": [\n"
          + "    \"rabbit-3@host3\",\n"
          + "    \"rabbit-2@host2\",\n"
          + "    \"rabbit-1@host1\"\n"
          + "  ],\n"
          + "  \"open_files\": {\n"
          + "    \"rabbit-1@host1\": 0,\n"
          + "    \"rabbit-2@host2\": 0,\n"
          + "    \"rabbit-3@host3\": 0\n"
          + "  },\n"
          + "  \"type\": \"quorum\",\n"
          + "  \"vhost\": \"/\"\n"
          + "}";

  private static final String JSON_CONSUMER_DETAILS_WITH_UNDEFINED_CHANNEL_PEER_PORT =
      "{\n"
          + "   \"arguments\":{\n"
          + "      \n"
          + "   },\n"
          + "   \"ack_required\":true,\n"
          + "   \"active\":true,\n"
          + "   \"activity_status\":\"up\",\n"
          + "   \"channel_details\":{\n"
          + "      \"connection_name\":\"rabbit@10-244-108-91.1628951087.17337.1\",\n"
          + "      \"name\":\"rabbit@10-244-108-91.1628951087.17337.1 (1)\",\n"
          + "      \"node\":\"rabbit@10-244-108-91\",\n"
          + "      \"number\":1,\n"
          + "      \"peer_host\":\"undefined\",\n"
          + "      \"peer_port\":\"undefined\",\n"
          + "      \"user\":\"shovel\"\n"
          + "   },\n"
          + "   \"consumer_tag\":\"amq.ctag-uLSmF70x9pQEDL34OCbkvg\",\n"
          + "   \"exclusive\":false,\n"
          + "   \"prefetch_count\":1000,\n"
          + "   \"queue\":{\n"
          + "      \"name\":\"amq.gen-vIuezFJTmuEAOB2SouuafQ\",\n"
          + "      \"vhost\":\"PROD\"\n"
          + "   }\n"
          + "}";

  private static final String JSON_CONSUMER_DETAILS =
      "{\n"
          + "   \"arguments\":{\n"
          + "      \n"
          + "   },\n"
          + "   \"ack_required\":true,\n"
          + "   \"active\":true,\n"
          + "   \"activity_status\":\"up\",\n"
          + "   \"channel_details\":{\n"
          + "      \"connection_name\":\"127.0.0.1:40548 -> 127.0.0.1:5672\",\n"
          + "      \"name\":\"127.0.0.1:40548 -> 127.0.0.1:5672 (1)\",\n"
          + "      \"node\":\"rabbit@my-host\",\n"
          + "      \"number\":1,\n"
          + "      \"peer_host\":\"127.0.0.1\",\n"
          + "      \"peer_port\":40548,\n"
          + "      \"user\":\"guest\"\n"
          + "   },\n"
          + "   \"consumer_tag\":\"amq.ctag-Od3nR5Kglfbkm6dB3Jw-dg\",\n"
          + "   \"consumer_timeout\":1800000,\n"
          + "   \"exclusive\":false,\n"
          + "   \"prefetch_count\":0,\n"
          + "   \"queue\":{\n"
          + "      \"name\":\"amq.gen-ofOF2lL5hhOzhchiFCPhjg\",\n"
          + "      \"vhost\":\"/\"\n"
          + "   }\n"
          + "}";
}
