/*
 * Copyright 2020 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.rabbitmq.http.client.domain.ChannelDetails;
import com.rabbitmq.http.client.domain.CurrentUserDetails;
import com.rabbitmq.http.client.domain.UserInfo;
import com.rabbitmq.http.client.domain.UserLimits;
import com.rabbitmq.http.client.domain.VhostLimits;

final class JsonUtils {

  static final JsonDeserializer<VhostLimits> VHOST_LIMITS_DESERIALIZER_INSTANCE =
      new VhostLimitsDeserializer();
  static final JsonDeserializer<UserLimits> USER_LIMITS_DESERIALIZER_INSTANCE =
      new UserLimitsDeserializer();
  static final JsonDeserializer<CurrentUserDetails> CURRENT_USER_DETAILS_DESERIALIZER_INSTANCE =
      new CurrentUserDetailsDeserializer();
  static final JsonDeserializer<UserInfo> USER_INFO_DESERIALIZER_INSTANCE =
      new UserInfoDeserializer();
  static final JsonDeserializer<ChannelDetails> CHANNEL_DETAILS_DESERIALIZER_INSTANCE =
      new ChannelDetailsDeserializer();

  private JsonUtils() {}

  private static String get(JsonNode jp, String name) {
    JsonNode node = jp.get(name);
    return node == null ? null : node.asText();
  }

  static ObjectMapper createDefaultObjectMapper() {
    JsonMapper objectMapper =
        JsonMapper.builder().disable(MapperFeature.DEFAULT_VIEW_INCLUSION).build();
    objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    SimpleModule module = new SimpleModule();
    module.addDeserializer(VhostLimits.class, JsonUtils.VHOST_LIMITS_DESERIALIZER_INSTANCE);
    module.addDeserializer(UserLimits.class, JsonUtils.USER_LIMITS_DESERIALIZER_INSTANCE);
    module.addDeserializer(UserInfo.class, JsonUtils.USER_INFO_DESERIALIZER_INSTANCE);
    module.addDeserializer(
        CurrentUserDetails.class, JsonUtils.CURRENT_USER_DETAILS_DESERIALIZER_INSTANCE);
    module.addDeserializer(ChannelDetails.class, JsonUtils.CHANNEL_DETAILS_DESERIALIZER_INSTANCE);
    objectMapper.registerModule(module);
    return objectMapper;
  }

  private static final class VhostLimitsDeserializer extends StdDeserializer<VhostLimits> {
    private static final String VHOST_FIELD = "vhost";
    private static final String VALUE_FIELD = "value";
    private static final String MAX_QUEUES_FIELD = "max-queues";
    private static final String MAX_CONNECTIONS_FIELD = "max-connections";
    private static final long serialVersionUID = -1881403692606830843L;

    private VhostLimitsDeserializer() {
      super(VhostLimits.class);
    }

    @Override
    public VhostLimits deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      JsonNode node = jp.getCodec().readTree(jp);
      if (node.isArray()) {
        if (node.isEmpty()) {
          return new VhostLimits(null, -1, -1);
        }
        node = node.get(0);
      }
      JsonNode value = node.get(VALUE_FIELD);
      return new VhostLimits(
          get(node, VHOST_FIELD),
          getLimit(value, MAX_QUEUES_FIELD),
          getLimit(value, MAX_CONNECTIONS_FIELD));
    }

    private int getLimit(JsonNode value, String name) {
      JsonNode limit = value.get(name);
      if (limit == null) {
        return -1;
      } else {
        return limit.asInt(-1);
      }
    }
  }

  private static final class UserLimitsDeserializer extends StdDeserializer<UserLimits> {
    private static final String USER_FIELD = "user";
    private static final String VALUE_FIELD = "value";
    private static final String MAX_CONNECTIONS_FIELD = "max-connections";
    private static final String MAX_CHANNELS_FIELD = "max-channels";
    private static final long serialVersionUID = -1881403692606830844L;

    private UserLimitsDeserializer() {
      super(UserLimits.class);
    }

    @Override
    public UserLimits deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      JsonNode node = jp.getCodec().readTree(jp);
      if (node.isArray()) {
        if (node.isEmpty()) {
          return new UserLimits(null, -1, -1);
        }
        node = node.get(0);
      }
      JsonNode value = node.get(VALUE_FIELD);
      return new UserLimits(
          get(node, USER_FIELD),
          getLimit(value, MAX_CONNECTIONS_FIELD),
          getLimit(value, MAX_CHANNELS_FIELD));
    }

    private int getLimit(JsonNode value, String name) {
      JsonNode limit = value.get(name);
      if (limit == null) {
        return -1;
      } else {
        return limit.asInt(-1);
      }
    }
  }

  private abstract static class UserDeserializer<T> extends StdDeserializer<T> {

    protected static final String USERNAME_FIELD = "name";
    protected static final String TAGS_FIELD = "tags";
    private static final long serialVersionUID = 3473030985825959020L;

    protected UserDeserializer(Class<?> vc) {
      super(vc);
    }

    protected String getUsername(JsonNode node) {
      return node.get(USERNAME_FIELD).asText();
    }

    protected List<String> getTags(JsonNode parent) {
      JsonNode node = parent.get(TAGS_FIELD);

      List<String> tags;
      if (node.isArray()) {
        if (node.isEmpty()) {
          tags = Collections.emptyList();
        } else {
          tags = new ArrayList<>(node.size());
          for (int i = 0; i < node.size(); i++) {
            tags.add(node.get(i).asText());
          }
        }
      } else {
        if (node.asText() == null || node.asText().isEmpty()) {
          tags = Collections.emptyList();
        } else {
          tags = Arrays.asList(node.asText().split(","));
        }
      }
      return tags;
    }
  }

  private static final class UserInfoDeserializer extends UserDeserializer<UserInfo> {
    private static final long serialVersionUID = -1871403623406830843L;

    private static final String PASSWORD_HASH_FIELD = "password_hash";
    private static final String HASHING_ALGORITHM_FIELD = "hashing_algorithm";

    private UserInfoDeserializer() {
      super(UserInfo.class);
    }

    @Override
    public UserInfo deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      JsonNode node = jp.getCodec().readTree(jp);

      return new UserInfo(
          getUsername(node),
          get(node, PASSWORD_HASH_FIELD),
          get(node, HASHING_ALGORITHM_FIELD),
          getTags(node));
    }
  }

  private static final class CurrentUserDetailsDeserializer
      extends UserDeserializer<CurrentUserDetails> {
    private static final long serialVersionUID = -1871403623406830843L;

    private CurrentUserDetailsDeserializer() {
      super(UserInfo.class);
    }

    @Override
    public CurrentUserDetails deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
      JsonNode node = jp.getCodec().readTree(jp);

      return new CurrentUserDetails(get(node, USERNAME_FIELD), getTags(node));
    }
  }

  private static final class ChannelDetailsDeserializer extends StdDeserializer<ChannelDetails> {

    private static final String CONNECTION_NAME_FIELD = "connection_name";
    private static final String NAME_FIELD = "name";
    private static final String NODE_FIELD = "node";
    private static final String NUMBER_FIELD = "number";
    private static final String PEER_HOST_FIELD = "peer_host";
    private static final String PEER_PORT_FIELD = "peer_port";
    private static final String USER_FIELD = "user";
    private static final long serialVersionUID = -1831999885508961350L;

    private ChannelDetailsDeserializer() {
      super(ChannelDetails.class);
    }

    private static int getPeerPort(JsonNode node) {
      return node == null ? 0 : node.asInt(0);
    }

    @Override
    public ChannelDetails deserialize(JsonParser jp, DeserializationContext deserializationContext)
        throws IOException {
      JsonNode node = jp.getCodec().readTree(jp);
      ChannelDetails channelDetails = new ChannelDetails();
      channelDetails.setConnectionName(get(node, CONNECTION_NAME_FIELD));
      channelDetails.setName(get(node, NAME_FIELD));
      channelDetails.setNode(get(node, NODE_FIELD));
      JsonNode numberNode = node.get(NUMBER_FIELD);
      channelDetails.setNumber(numberNode == null ? -1 : numberNode.asInt());
      channelDetails.setPeerHost(get(node, PEER_HOST_FIELD));
      channelDetails.setPeerPort(getPeerPort(node.get(PEER_PORT_FIELD)));
      channelDetails.setUser(get(node, USER_FIELD));
      return channelDetails;
    }
  }
}
