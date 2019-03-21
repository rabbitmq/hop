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

@SuppressWarnings("unused")
public class ObjectTotals {
  private long connections;
  private long channels;
  private long exchanges;
  private long queues;
  private long consumers;

  public long getConnections() {
    return connections;
  }

  public void setConnections(long connections) {
    this.connections = connections;
  }

  public long getChannels() {
    return channels;
  }

  public void setChannels(long channels) {
    this.channels = channels;
  }

  public long getExchanges() {
    return exchanges;
  }

  public void setExchanges(long exchanges) {
    this.exchanges = exchanges;
  }

  public long getQueues() {
    return queues;
  }

  public void setQueues(long queues) {
    this.queues = queues;
  }

  public long getConsumers() {
    return consumers;
  }

  public void setConsumers(long consumers) {
    this.consumers = consumers;
  }

  @Override
  public String toString() {
    return "ObjectTotals{" +
        "connections=" + connections +
        ", channels=" + channels +
        ", exchanges=" + exchanges +
        ", queues=" + queues +
        ", consumers=" + consumers +
        '}';
  }
}
