package com.novemberain.hop.client.domain;

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
}
