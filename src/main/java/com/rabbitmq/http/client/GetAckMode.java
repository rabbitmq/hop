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
package com.rabbitmq.http.client;

/**
 * Acknowledgment mode when getting messages with the HTTP API.
 * <p>
 * This determines whether the messages will be removed from the queue or not.
 *
 * @see Client#get(String, String, int, GetAckMode, GetEncoding, int)
 * @see ReactorNettyClient#get(String, String, int, GetAckMode, GetEncoding, int)
 * @since 3.4.0
 */
public enum GetAckMode {

    /**
     * <code>basic.get</code> with acknowledgment enabled,
     * but never acknowledges, so the message(s) will
     * be requeued when the internal AMQP connection is closed.
     */
    NACK_REQUEUE_TRUE("ack_requeue_true"),

    /**
     * Reject the messages, request requeuing.
     */
    REJECT_REQUEUE_TRUE("reject_requeue_true"),

    /**
     * Acknowledge the message, removing it from the queue.
     */
    ACK_REQUEUE_FALSE("ack_requeue_false"),

    /**
     * Reject the message without requeuing.
     */
    REJECT_REQUEUE_FALSE("reject_requeue_false");

    final String ackMode;

    GetAckMode(String ackMode) {
        this.ackMode = ackMode;
    }

}
