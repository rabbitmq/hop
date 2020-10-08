/*
 * Copyright 2019 the original author or authors.
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

package com.rabbitmq.http.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.rabbitmq.http.client.domain.QueueInfo
import spock.lang.Specification
import spock.lang.Unroll


class JsonMappingSpec extends Specification {

  static ObjectMapper[] mappers() {
    [Client.createDefaultObjectMapper(), ReactorNettyClient.createDefaultObjectMapper()]
  }

  @Unroll
  def "JSON document for queue with NaN message count should return -1 for message count"() {
    when: "JSON document for queue has no ready messages count field"
    def q = mapper.readValue(JSON_QUEUE_NO_READY_MESSAGES, QueueInfo.class)

    then: "the field value should be -1 in the Java object"
    q.messagesReady == -1

    where:
    mapper << mappers()
  }

  @Unroll
  def "JSON document for queue with defined message count should return appropriate value for message count"() {
    when: "JSON document for queue has a ready messages count field with a value"
    def q = Client.createDefaultObjectMapper().readValue(JSON_QUEUE_SOME_READY_MESSAGES, QueueInfo.class)

    then: "the field value of the Java object should be the same as in the JSON document"
    q.messagesReady == 1000

    where:
    mapper << mappers()
  }

  static final String JSON_QUEUE_NO_READY_MESSAGES =
          "   {\n" +
          "      \"arguments\":{\n" +
          "         \n" +
          "      },\n" +
          "      \"auto_delete\":false,\n" +
          "      \"backing_queue_status\":{\n" +
          "         \"avg_ack_egress_rate\":0.0,\n" +
          "         \"avg_ack_ingress_rate\":0.0,\n" +
          "         \"avg_egress_rate\":0.0,\n" +
          "         \"avg_ingress_rate\":0.0,\n" +
          "         \"delta\":[\n" +
          "            \"delta\",\n" +
          "            \"undefined\",\n" +
          "            0,\n" +
          "            0,\n" +
          "            \"undefined\"\n" +
          "         ],\n" +
          "         \"len\":0,\n" +
          "         \"mode\":\"default\",\n" +
          "         \"next_seq_id\":0,\n" +
          "         \"q1\":0,\n" +
          "         \"q2\":0,\n" +
          "         \"q3\":0,\n" +
          "         \"q4\":0,\n" +
          "         \"target_ram_count\":\"infinity\"\n" +
          "      },\n" +
          "      \"consumer_utilisation\":null,\n" +
          "      \"consumers\":0,\n" +
          "      \"durable\":true,\n" +
          "      \"effective_policy_definition\":{\n" +
          "         \n" +
          "      },\n" +
          "      \"exclusive\":false,\n" +
          "      \"exclusive_consumer_tag\":null,\n" +
          "      \"garbage_collection\":{\n" +
          "         \"fullsweep_after\":65535,\n" +
          "         \"max_heap_size\":0,\n" +
          "         \"min_bin_vheap_size\":46422,\n" +
          "         \"min_heap_size\":233,\n" +
          "         \"minor_gcs\":2\n" +
          "      },\n" +
          "      \"head_message_timestamp\":null,\n" +
          "      \"idle_since\":\"2020-10-08 7:35:55\",\n" +
          "      \"memory\":18260,\n" +
          "      \"message_bytes\":0,\n" +
          "      \"message_bytes_paged_out\":0,\n" +
          "      \"message_bytes_persistent\":0,\n" +
          "      \"message_bytes_ram\":0,\n" +
          "      \"message_bytes_ready\":0,\n" +
          "      \"message_bytes_unacknowledged\":0,\n" +
          "      \"messages\":0,\n" +
          "      \"messages_details\":{\n" +
          "         \"rate\":0.0\n" +
          "      },\n" +
          "      \"messages_paged_out\":0,\n" +
          "      \"messages_persistent\":0,\n" +
          "      \"messages_ram\":0,\n" +
          "      \"messages_ready_details\":{\n" +
          "         \"rate\":0.0\n" +
          "      },\n" +
          "      \"messages_ready_ram\":0,\n" +
          "      \"messages_unacknowledged\":0,\n" +
          "      \"messages_unacknowledged_details\":{\n" +
          "         \"rate\":0.0\n" +
          "      },\n" +
          "      \"messages_unacknowledged_ram\":0,\n" +
          "      \"name\":\"queue1\",\n" +
          "      \"operator_policy\":null,\n" +
          "      \"policy\":null,\n" +
          "      \"recoverable_slaves\":null,\n" +
          "      \"reductions\":4474,\n" +
          "      \"reductions_details\":{\n" +
          "         \"rate\":0.0\n" +
          "      },\n" +
          "      \"single_active_consumer_tag\":null,\n" +
          "      \"state\":\"running\",\n" +
          "      \"type\":\"classic\",\n" +
          "      \"vhost\":\"vh1\"\n" +
          "   }\n"

  static final String JSON_QUEUE_SOME_READY_MESSAGES =
          "   {\n" +
                  "      \"arguments\":{\n" +
                  "         \n" +
                  "      },\n" +
                  "      \"auto_delete\":false,\n" +
                  "      \"backing_queue_status\":{\n" +
                  "         \"avg_ack_egress_rate\":0.0,\n" +
                  "         \"avg_ack_ingress_rate\":0.0,\n" +
                  "         \"avg_egress_rate\":0.0,\n" +
                  "         \"avg_ingress_rate\":0.0,\n" +
                  "         \"delta\":[\n" +
                  "            \"delta\",\n" +
                  "            \"undefined\",\n" +
                  "            0,\n" +
                  "            0,\n" +
                  "            \"undefined\"\n" +
                  "         ],\n" +
                  "         \"len\":0,\n" +
                  "         \"mode\":\"default\",\n" +
                  "         \"next_seq_id\":0,\n" +
                  "         \"q1\":0,\n" +
                  "         \"q2\":0,\n" +
                  "         \"q3\":0,\n" +
                  "         \"q4\":0,\n" +
                  "         \"target_ram_count\":\"infinity\"\n" +
                  "      },\n" +
                  "      \"consumer_utilisation\":null,\n" +
                  "      \"consumers\":0,\n" +
                  "      \"durable\":true,\n" +
                  "      \"effective_policy_definition\":{\n" +
                  "         \n" +
                  "      },\n" +
                  "      \"exclusive\":false,\n" +
                  "      \"exclusive_consumer_tag\":null,\n" +
                  "      \"garbage_collection\":{\n" +
                  "         \"fullsweep_after\":65535,\n" +
                  "         \"max_heap_size\":0,\n" +
                  "         \"min_bin_vheap_size\":46422,\n" +
                  "         \"min_heap_size\":233,\n" +
                  "         \"minor_gcs\":2\n" +
                  "      },\n" +
                  "      \"head_message_timestamp\":null,\n" +
                  "      \"idle_since\":\"2020-10-08 7:35:55\",\n" +
                  "      \"memory\":18260,\n" +
                  "      \"message_bytes\":0,\n" +
                  "      \"message_bytes_paged_out\":0,\n" +
                  "      \"message_bytes_persistent\":0,\n" +
                  "      \"message_bytes_ram\":0,\n" +
                  "      \"message_bytes_ready\":0,\n" +
                  "      \"message_bytes_unacknowledged\":0,\n" +
                  "      \"messages\":0,\n" +
                  "      \"messages_details\":{\n" +
                  "         \"rate\":0.0\n" +
                  "      },\n" +
                  "      \"messages_paged_out\":0,\n" +
                  "      \"messages_persistent\":0,\n" +
                  "      \"messages_ram\":0,\n" +
                  "      \"messages_ready\":1000,\n" +
                  "      \"messages_ready_details\":{\n" +
                  "         \"rate\":0.0\n" +
                  "      },\n" +
                  "      \"messages_ready_ram\":0,\n" +
                  "      \"messages_unacknowledged\":0,\n" +
                  "      \"messages_unacknowledged_details\":{\n" +
                  "         \"rate\":0.0\n" +
                  "      },\n" +
                  "      \"messages_unacknowledged_ram\":0,\n" +
                  "      \"name\":\"queue1\",\n" +
                  "      \"operator_policy\":null,\n" +
                  "      \"policy\":null,\n" +
                  "      \"recoverable_slaves\":null,\n" +
                  "      \"reductions\":4474,\n" +
                  "      \"reductions_details\":{\n" +
                  "         \"rate\":0.0\n" +
                  "      },\n" +
                  "      \"single_active_consumer_tag\":null,\n" +
                  "      \"state\":\"running\",\n" +
                  "      \"type\":\"classic\",\n" +
                  "      \"vhost\":\"vh1\"\n" +
                  "   }\n"

}
