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

import com.fasterxml.jackson.annotation.JsonProperty;

//{"src-uri":  "amqp://",              "src-queue":  "my-queue",
//    "dest-uri": "amqp://remote-server", "dest-queue": "another-queue"}

public class ShovelDefinition {

	@JsonProperty("src-exchange")
	private String sourceExchange;
	@JsonProperty("src-queue")
	private String sourceQueue;

	@JsonProperty("dest-exchange")
	private String destinationExchange;
	@JsonProperty("dest-queue")
	private String destinationQueue;

	public ShovelDefinition() {
	}

	public String getSourceExchange() {
		return sourceExchange;
	}

	public void setSourceExchange(String sourceExchange) {
		this.sourceExchange = sourceExchange;
	}

	public String getSourceQueue() {
		return sourceQueue;
	}

	public void setSourceQueue(String sourceQueue) {
		this.sourceQueue = sourceQueue;
	}

	public String getDestinationExchange() {
		return destinationExchange;
	}

	public void setDestinationExchange(String destExchange) {
		this.destinationExchange = destExchange;
	}

	public String getDestinationQueue() {
		return destinationQueue;
	}

	public void setDestinationQueue(String destQueue) {
		this.destinationQueue = destQueue;
	}

	@Override
	public String toString() {
		return "ShovelDetails{" + "sourceExchange=" + sourceExchange + ", sourceQueue=" + sourceQueue + ", destinationExchange='" + destinationExchange + '\''
				+ ", destinationQueue='" + destinationQueue + '}';
	}
}
