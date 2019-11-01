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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static com.fasterxml.jackson.annotation.JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED;

//{"src-uri":  "amqp://",              "src-queue":  "my-queue",
//    "dest-uri": "amqp://remote-server", "dest-queue": "another-queue"}

public class ShovelDetails {

	@JsonProperty("src-uri")
	@JsonFormat(with = {ACCEPT_SINGLE_VALUE_AS_ARRAY, WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED})
    private List<String> sourceURIs;
	@JsonProperty("src-exchange")
	private String sourceExchange;
	@JsonProperty("src-exchange-key")
	private String sourceExchangeKey;
	@JsonProperty("src-queue")
	private String sourceQueue;
	@JsonProperty("src-prefetch-count")
	private Long sourcePrefetchCount;
	@JsonProperty("src-delete-after")
	private String sourceDeleteAfter;

	@JsonProperty("dest-uri")
	@JsonFormat(with = {ACCEPT_SINGLE_VALUE_AS_ARRAY, WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED})
    private List<String> destinationURIs;
	@JsonProperty("dest-exchange")
	private String destinationExchange;
	@JsonProperty("dest-exchange-key")
	private String destinationExchangeKey;
	@JsonProperty("dest-queue")
	private String destinationQueue;
	@JsonProperty("dest-add-timestamp-header")
	private Boolean destinationAddTimestampHeader;

	@JsonProperty("reconnect-delay")
	private long reconnectDelay;
	@JsonProperty("add-forward-headers")
	private boolean addForwardHeaders;

	@JsonProperty("ack-mode")
	private String ackMode;

	@JsonProperty("publish-properties")
	private Map<String, Object> publishProperties;

	public ShovelDetails() {
	}

	public ShovelDetails(String sourceURI, String destURI, long reconnectDelay, boolean addForwardHeaders, Map<String, Object> publishProperties) {
		this(Collections.singletonList(sourceURI), Collections.singletonList(destURI), reconnectDelay, addForwardHeaders, publishProperties);
	}

    public ShovelDetails(List<String> sourceURIs, List<String> destURIs, long reconnectDelay, boolean addForwardHeaders, Map<String, Object> publishProperties) {
        checkURIsArgument("sourceURIs", sourceURIs);
        checkURIsArgument("destURIs", destURIs);

        this.sourceURIs = Collections.unmodifiableList(sourceURIs);
        this.destinationURIs = Collections.unmodifiableList(destURIs);
		this.reconnectDelay = reconnectDelay;
		this.addForwardHeaders = addForwardHeaders;
		this.publishProperties = publishProperties;
	}

	/**
	 * @return the first source source URI
	 * @deprecated use {@link #getSourceURIs()} instead
	 */
	@Deprecated
	@JsonIgnore
	public String getSourceURI() {
		if(sourceURIs.isEmpty()) {
			throw new IllegalStateException("URIs may not be empty.");
		} else {
			return sourceURIs.get(0);
		}
	}

	/**
	 * @param sourceURI the source URI
	 * @deprecated use {@link #setSourceURIs(List)} instead
	 */
	@Deprecated
	public void setSourceURI(String sourceURI) {
		this.sourceURIs = Collections.singletonList(sourceURI);
	}

	public List<String> getSourceURIs() {
        return sourceURIs;
	}

	public void setSourceURIs(List<String> sourceURIs) {
        checkURIsArgument("sourceURIs", sourceURIs);
        this.sourceURIs = Collections.unmodifiableList(sourceURIs);
	}

	public String getSourceExchange() {
		return sourceExchange;
	}

	public void setSourceExchange(String sourceExchange) {
		this.sourceExchange = sourceExchange;
	}

	public String getSourceExchangeKey() {
		return sourceExchangeKey;
	}

	public void setSourceExchangeKey(String sourceExchangeKey) {
		this.sourceExchangeKey = sourceExchangeKey;
	}

	public String getSourceQueue() {
		return sourceQueue;
	}

	public void setSourceQueue(String sourceQueue) {
		this.sourceQueue = sourceQueue;
	}

	/**
	 * @return the first destination URI
	 * @deprecated use {@link #getDestinationURIs()} instead
	 */
	@Deprecated
	@JsonIgnore
	public String getDestinationURI() {
		return destinationURIs.get(0);
	}

	/**
	 *
	 * @param destURI the destination URI
	 * @deprecated use {@link #setDestinationURIs(List)} instead
	 */
	@Deprecated
	public void setDestinationURI(String destURI) {
        this.destinationURIs = Collections.singletonList(destURI);
	}

	public List<String> getDestinationURIs() {
        return destinationURIs;
	}

	public void setDestinationURIs(List<String> destURIs) {
        checkURIsArgument("destURIs", destURIs);
        this.destinationURIs = Collections.unmodifiableList(destURIs);
	}

	public String getDestinationExchange() {
		return destinationExchange;
	}

	public void setDestinationExchange(String destExchange) {
		this.destinationExchange = destExchange;
	}

	public String getDestinationExchangeKey() {
		return destinationExchangeKey;
	}

	public void setDestinationExchangeKey(String destExchangeKey) {
		this.destinationExchangeKey = destExchangeKey;
	}


	public String getDestinationQueue() {
		return destinationQueue;
	}

	public void setDestinationQueue(String destQueue) {
		this.destinationQueue = destQueue;
	}

	public long getReconnectDelay() {
		return reconnectDelay;
	}

	public void setReconnectDelay(long reconnectDelay) {
		this.reconnectDelay = reconnectDelay;
	}

	public boolean isAddForwardHeaders() {
		return addForwardHeaders;
	}

	public void setAddForwardHeaders(boolean addForwardHeaders) {
		this.addForwardHeaders = addForwardHeaders;
	}

	public String getAckMode() {
		return ackMode;
	}

	public void setAckMode(String ackMode) {
		this.ackMode = ackMode;
	}

	public Map<String, Object> getPublishProperties() {
		return publishProperties;
	}

	public void setPublishProperties(Map<String, Object> publishProperties) {
		this.publishProperties = publishProperties;
	}

	public Long getSourcePrefetchCount() {
		return sourcePrefetchCount;
	}

	public void setSourcePrefetchCount(Long sourcePrefetchCount) {
		this.sourcePrefetchCount = sourcePrefetchCount;
	}

	public String getSourceDeleteAfter() {
		return sourceDeleteAfter;
	}

	public void setSourceDeleteAfter(String sourceDeleteAfter) {
		this.sourceDeleteAfter = sourceDeleteAfter;
	}

	public Boolean isDestinationAddTimestampHeader() {
		return destinationAddTimestampHeader;
	}

	public void setDestinationAddTimestampHeader(Boolean destinationAddTimestampHeader) {
		this.destinationAddTimestampHeader = destinationAddTimestampHeader;
	}

	@Override
	public String toString() {
		return "ShovelDetails{" +
                "sourceURIs='" + URIsToString(sourceURIs) + '\'' +
				", sourceExchange='" + sourceExchange + '\'' +
				", sourceExchangeKey='" + sourceExchangeKey + '\'' +
				", sourceQueue='" + sourceQueue + '\'' +
				", sourcePrefetchCount='" + sourcePrefetchCount + '\'' +
				", sourceDeleteAfter='" + sourceDeleteAfter + '\'' +
                ", destinationURIs='" + URIsToString(destinationURIs) + '\'' +
				", destinationExchange='" + destinationExchange + '\'' +
				", destinationExchangeKey='" + destinationExchangeKey + '\'' +
				", destinationQueue='" + destinationQueue + '\'' +
				", destinationAddTimestampHeader=" + destinationAddTimestampHeader +
				", reconnectDelay=" + reconnectDelay +
				", addForwardHeaders=" + addForwardHeaders +
				", ackMode='" + ackMode + '\'' +
				", publishProperties=" + publishProperties +
				'}';
	}

	private static void checkURIsArgument(String argumentName, List<String> argument) {
		if (argument == null || argument.isEmpty()) {
			throw new IllegalArgumentException(argumentName + " argument must contains at least one URI");
		}
	}

	private static String URIsToString(List<String> uris) {
		if (uris.size() == 1) {
			// for back compatibility
			return uris.get(0);
		}
		return Arrays.toString(uris.toArray());
	}
}
