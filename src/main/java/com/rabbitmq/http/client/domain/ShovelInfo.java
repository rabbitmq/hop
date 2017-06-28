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

//{"value":{"src-uri":  "amqp://",              "src-queue":  "my-queue",
//    "dest-uri": "amqp://remote-server", "dest-queue": "another-queue"}}

public class ShovelInfo {

	private String name;

	private ShovelDetails value;

	public ShovelInfo() {
	}

	public ShovelInfo(String name, ShovelDetails value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ShovelDetails getValue() {
		return value;
	}

	public void setValue(ShovelDetails value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "ShovelInfo{" + "name='" + name + '\'' + ", value=" + value + '}';
	}
}
