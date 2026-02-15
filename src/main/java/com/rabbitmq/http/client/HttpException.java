/*
 * Copyright 2018 the original author or authors.
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

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

/**
 * Base exception.
 */
public class HttpException extends RuntimeException {

    static final long serialVersionUID = 1;

    public HttpException(Throwable cause) {
        super(cause);
    }

    public HttpException(String message) {
        super(message);
    }

    public boolean isConnectionError() {
        Throwable cause = getCause();
        if (cause instanceof ConnectException) {
            return !isTlsRelated(cause);
        }
        return false;
    }

    public boolean isTimeout() {
        Throwable cause = getCause();
        if (cause instanceof HttpTimeoutException) {
            return true;
        }
        if (cause instanceof SocketTimeoutException) {
            return true;
        }
        if (cause instanceof InterruptedIOException) {
            String message = cause.getMessage();
            return message != null && message.contains("timed out");
        }
        return false;
    }

    public boolean isTlsHandshakeError() {
        Throwable current = getCause();
        while (current != null) {
            if (current instanceof SSLHandshakeException) {
                return true;
            }
            if (current instanceof SSLException && isTlsRelated(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isTlsRelated(Throwable t) {
        String message = t.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("ssl") || lower.contains("tls")
            || lower.contains("certificate") || lower.contains("handshake");
    }
}
