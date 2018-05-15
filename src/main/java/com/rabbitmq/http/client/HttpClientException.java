/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rabbitmq.http.client;

/**
 *
 */
public class HttpClientException extends HttpException {

    static final long serialVersionUID = 1;

    public HttpClientException(reactor.ipc.netty.http.client.HttpClientException cause) {
        super(cause);
    }

    public int status() {
        return cause().status().code();
    }

    public String reason() {
        return cause().status().reasonPhrase();
    }

    private reactor.ipc.netty.http.client.HttpClientException cause() {
        return (reactor.ipc.netty.http.client.HttpClientException) getCause();
    }
}
