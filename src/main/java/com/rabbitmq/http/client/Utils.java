/*
 * Copyright 2018-2020 the original author or authors.
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

import com.rabbitmq.http.client.domain.OutboundMessage;
import com.rabbitmq.http.client.domain.QueryParameters;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
final class Utils {

    private Utils() { }

    static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    static Map<String, Object> bodyForPublish(String routingKey, OutboundMessage outboundMessage) {
        if (routingKey == null) {
            throw new IllegalArgumentException("routing key cannot be null");
        }
        if (outboundMessage == null) {
            throw new IllegalArgumentException("message cannot be null");
        }
        if (outboundMessage.getPayload() == null) {
            throw new IllegalArgumentException("message payload cannot be null");
        }
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("routing_key", routingKey);
        body.put("properties", outboundMessage.getProperties() == null ? Collections.EMPTY_MAP : outboundMessage.getProperties());
        body.put("payload", outboundMessage.getPayload());
        body.put("payload_encoding", outboundMessage.getPayloadEncoding());
        return body;
    }

    static Map<String, Object> bodyForGet(int count, GetAckMode ackMode, GetEncoding encoding, int truncate) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be greater than 0");
        }
        if (ackMode == null) {
            throw new IllegalArgumentException("acknowledgment mode cannot be null");
        }
        if (encoding == null) {
            throw new IllegalArgumentException("encoding cannot be null");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("count", count);
        body.put("ackmode", ackMode.ackMode);
        body.put("encoding", encoding.encoding);
        if (truncate >= 0) {
            body.put("truncate", truncate);
        }
        return body;
    }

    static String[] extractUsernamePassword(String url) {
        String userInfo = null;
        try {
            userInfo = new URI(url).toURL().getUserInfo();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalArgumentException("Malformed URL", e);
        }
        if (userInfo == null) {
            throw new IllegalArgumentException("Could not extract password from URL. " +
                    "URL should be like 'https://guest:guest@localhost:15672/api/'");
        }
        String[] usernamePassword = userInfo.split(":");
        if (usernamePassword.length != 2) {
            throw new IllegalArgumentException("Could not extract password from URL. " +
                    "URL should be like 'https://guest:guest@localhost:15672/api/'");
        }

        return new String[]{
                decode(usernamePassword[0]), decode(usernamePassword[1])
        };
    }

    static URI rootUri(URL url) throws URISyntaxException {
        if (url.toString().endsWith("/")) {
            return url.toURI();
        } else {
            return new URI(url + "/");
        }
    }

    static String decode(String potentiallyEncodedString) {
        if (potentiallyEncodedString != null && !potentiallyEncodedString.isEmpty()) {
            return URLDecoder.decode(potentiallyEncodedString, StandardCharsets.UTF_8);
        } else {
            return potentiallyEncodedString;
        }
    }

    static String urlWithoutCredentials(String url) {
        URL url1 = null;
        try {
            url1 = new URI(url).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalArgumentException("URL is malformed");
        }
        return url.replace(url1.getUserInfo() + "@", "");
    }

    static class URIBuilder {
        URI rootURI;
        StringBuilder sb = new StringBuilder();
        QueryParameters queryParameters;
        Map<String,String> mapOfParameters;

        public URIBuilder(URI rootURI) {
            this.rootURI = rootURI;
        }

        URIBuilder withEncodedPath(String path) {
            if (sb.length() > 0 && sb.charAt(sb.length()-1) != '/') sb.append("/");
            sb.append(path);
            return this;
        }
        URIBuilder withPath(String path) {
            if (sb.length() > 0 && sb.charAt(sb.length()-1) != '/') sb.append("/");
            sb.append(PercentEncoder.encodePathSegment(path));
            return this;
        }
        URIBuilder withQueryParameters(QueryParameters queryParameters) {
            this.queryParameters = queryParameters;
            return this;
        }
        URIBuilder withQueryParameters(Map<String,String> mapOfParameters) {
            this.mapOfParameters = mapOfParameters;
            return this;
        }
        URI get() {
            try {
                if ((queryParameters != null && !queryParameters.isEmpty()) || mapOfParameters != null
                        && !mapOfParameters.isEmpty()) sb.append("?");

                if (queryParameters != null && !queryParameters.isEmpty()) {
                    for (Map.Entry<String, String> param : queryParameters.parameters().entrySet()) {
                        sb.append(param.getKey()).append("=").append(PercentEncoder.encodeParameter(param.getValue())).append("&");
                    }
                    sb.deleteCharAt(sb.length() - 1); // eliminate last &
                }
                if (mapOfParameters != null && !mapOfParameters.isEmpty()) {
                    for (Map.Entry<String, String> param : mapOfParameters.entrySet()) {
                        sb.append(param.getKey()).append("=").append(PercentEncoder.encodeParameter(param.getValue())).append("&");
                    }
                    sb.deleteCharAt(sb.length() - 1); // eliminate last &
                }
                return rootURI.resolve(sb.toString());
            }finally {
                sb.setLength(0);
            }
        }

        public URIBuilder withPathSeparator() {
            sb.append("/");
            return this;
        }
    }

    static String base64(String in) {
        return Base64.getEncoder().encodeToString(in.getBytes(StandardCharsets.UTF_8));
    }

}
