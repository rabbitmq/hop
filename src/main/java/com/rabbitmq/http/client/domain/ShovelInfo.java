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
//     "value": {
//         "src-uri": "",
//         "src-exchange": "",
//         "src-queue": "",
//         "dest-uri": "",
//         "dest-exchange": "",
//         "dest-queue": "",
//         "reconnect-delay": 0,
//         "add-forward-headers": true,
//         "publish-properties": {
//         }
//     },
//     "vhost": "",
//     "component": "shovel",
//     "name": ""
// }

public class ShovelInfo {

	private String name;
	@JsonProperty("vhost")
	private String virtualHost;
	private String component;
	@JsonProperty("value")
	private ShovelDetails details;

	public ShovelInfo() {
	}

	public ShovelInfo(String name, ShovelDetails details) {
		this.name = name;
		this.details = details;
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

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
	}

	public ShovelDetails getDetails() {
		return details;
	}

	public void setDetails(ShovelDetails details) {
		this.details = details;
	}

	@Override
	public String toString() {
		return "ShovelInfo{" + "name='" + name + "\', " + "virtualHost='" + virtualHost + "\', " + "component='" + component + '\'' + ", details=" + details
				+ '}';
	}

}
