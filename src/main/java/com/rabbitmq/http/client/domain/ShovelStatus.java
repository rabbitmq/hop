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

import com.fasterxml.jackson.annotation.JsonProperty;

// {
//    "node": "",
//    "timestamp": "",
//    "name": "",
//    "vhost": "",
//    "type": "",
//    "state": "",
//    "reason": ""
//},
//{
//    "node": "",
//    "timestamp": "",
//    "name": "",
//    "vhost": "",
//    "type": "",
//    "state": "",
//    "definition": {
//        "src-queue": "",
//        "dest-exchange": ""
//    },
//    "src_uri": "",
//    "dest_uri": ""
//}

public class ShovelStatus {

	private String node;
	private String name;
	@JsonProperty("vhost")
	private String virtualHost;
	private String type;
	private String state;
	private ShovelDefinition definition;
	@JsonProperty("src-uri")
	private String sourceURI;
	@JsonProperty("dest-uri")
	private String destinationURI;
	private String reason;

	public ShovelStatus() {
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVirtualHost() {
		return virtualHost;
	}

	public void setVirtualHost(String virtualHost) {
		this.virtualHost = virtualHost;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public ShovelDefinition getDefinition() {
		return definition;
	}

	public void setDefinition(ShovelDefinition definition) {
		this.definition = definition;
	}

	public String getSourceURI() {
		return sourceURI;
	}

	public void setSourceURI(String sourceURI) {
		this.sourceURI = sourceURI;
	}

	public String getDestinationURI() {
		return destinationURI;
	}

	public void setDestinationURI(String destinationURI) {
		this.destinationURI = destinationURI;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@Override
	public String toString() {
		return "ShovelStatus{" + "node='" + node + "\', " + "name='" + name + "\', " + "virtualHost='" + virtualHost + "\', " + "type='" + type + '\''
				+ ", state=" + state + '\'' + ", definition=" + definition + '\'' + ", sourceURI=" + sourceURI + '\'' + ", destinationURI=" + destinationURI
				+ '\'' + ", reason=" + reason + '}';
	}

}
