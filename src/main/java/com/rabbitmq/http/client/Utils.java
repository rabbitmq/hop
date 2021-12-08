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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
final class Utils {

    private Utils() { }

    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

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
            userInfo = new URL(url).getUserInfo();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Malformed URL", e);
        }
        if (userInfo == null) {
            throw new IllegalArgumentException("Could not extract password from URL. " +
                    "URL should be like 'https://guest:guest@localhost:15672/api/'");
        }
        String[] usernamePassword = userInfo.split(":");
        if (usernamePassword == null || usernamePassword.length != 2) {
            throw new IllegalArgumentException("Could not extract password from URL. " +
                    "URL should be like 'https://guest:guest@localhost:15672/api/'");
        }

        return new String[]{
                decode(usernamePassword[0]), decode(usernamePassword[1])
        };
    }

    static URI rootUri(URL url) throws URISyntaxException, MalformedURLException {
        if (url.toString().endsWith("/")) {
            return url.toURI();
        } else {
            return new URL(url + "/").toURI();
        }
    }

    static String decode(String potentiallyEncodedString) {
        if (potentiallyEncodedString != null && !potentiallyEncodedString.isEmpty()) {
            try {
                return URLDecoder.decode(potentiallyEncodedString, CHARSET_UTF8.name());
            } catch (UnsupportedEncodingException e) {
                // should not happen, the character encoding is fixed and valid
                throw new IllegalStateException("Error while decoding string", e);
            }
        } else {
            return potentiallyEncodedString;
        }
    }

    static String urlWithoutCredentials(String url) {
        URL url1 = null;
        try {
            url1 = new URL(url);
        } catch (MalformedURLException e) {
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
            appendEncodePath(sb, path, CHARSET_UTF8);
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
                        sb.append(param.getKey()).append("=").append(Utils.encodeHttpParameter(param.getValue())).append("&");
                    }
                    sb.deleteCharAt(sb.length() - 1); // eliminate last &
                }
                if (mapOfParameters != null && !mapOfParameters.isEmpty()) {
                    for (Map.Entry<String, String> param : mapOfParameters.entrySet()) {
                        sb.append(param.getKey()).append("=").append(Utils.encodeHttpParameter(param.getValue())).append("&");
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

    /* from https://github.com/apache/httpcomponents-client/commit/b58e7d46d75e1d3c42f5fd6db9bd45f32a49c639#diff-a74b24f025e68ec11e4550b42e9f807d */

    static String encodePath(String content, Charset charset) {
        return appendEncodePath(new StringBuilder(), content, charset).toString();
    }
    static StringBuilder appendEncodePath(StringBuilder buf, String content, Charset charset) {
        final ByteBuffer bb = charset.encode(content);
        while (bb.hasRemaining()) {
            final int b = bb.get() & 0xff;
            if (PATHSAFE.get(b)) {
                buf.append((char) b);
            } else {
                buf.append("%");
                final char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, RADIX));
                final char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, RADIX));
                buf.append(hex1);
                buf.append(hex2);
            }
        }
        return buf;
    }
    static String encodeHttpParameter(String value) {
        return URLEncoder.encode(value, CHARSET_UTF8);
    }

    static String encode(String pathSegment) {
        return encodePath(pathSegment, CHARSET_UTF8);
    }

    private static final int RADIX = 16;

    /**
     * Unreserved characters, i.e. alphanumeric, plus: {@code _ - ! . ~ ' ( ) *}
     * <p>
     * This list is the same as the {@code unreserved} list in
     * <a href="https://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>
     */
    private static final BitSet UNRESERVED = new BitSet(256);
    /**
     * Punctuation characters: , ; : $ & + =
     * <p>
     * These are the additional characters allowed by userinfo.
     */
    private static final BitSet PUNCT = new BitSet(256);
    /**
     * Characters which are safe to use in userinfo,
     * i.e. {@link #UNRESERVED} plus {@link #PUNCT}uation
     */
    private static final BitSet USERINFO = new BitSet(256);
    /**
     * Characters which are safe to use in a path,
     * i.e. {@link #UNRESERVED} plus {@link #PUNCT}uation plus / @
     */
    private static final BitSet PATHSAFE = new BitSet(256);
    /**
     * Characters which are safe to use in a query or a fragment,
     * i.e. {@link #RESERVED} plus {@link #UNRESERVED}
     */
    private static final BitSet URIC = new BitSet(256);

    /**
     * Reserved characters, i.e. {@code ;/?:@&=+$,[]}
     * <p>
     * This list is the same as the {@code reserved} list in
     * <a href="https://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>
     * as augmented by
     * <a href="https://www.ietf.org/rfc/rfc2732.txt">RFC 2732</a>
     */
    private static final BitSet RESERVED = new BitSet(256);


    /**
     * Safe characters for x-www-form-urlencoded data, as per java.net.URLEncoder and browser behaviour,
     * i.e. alphanumeric plus {@code "-", "_", ".", "*"}
     */
    private static final BitSet URLENCODER = new BitSet(256);

    static {
        // unreserved chars
        // alpha characters
        for (int i = 'a'; i <= 'z'; i++) {
            UNRESERVED.set(i);
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            UNRESERVED.set(i);
        }
        // numeric characters
        for (int i = '0'; i <= '9'; i++) {
            UNRESERVED.set(i);
        }
        UNRESERVED.set('_'); // these are the charactes of the "mark" list
        UNRESERVED.set('-');
        UNRESERVED.set('.');
        UNRESERVED.set('*');
        URLENCODER.or(UNRESERVED); // skip remaining unreserved characters
        UNRESERVED.set('!');
        UNRESERVED.set('~');
        UNRESERVED.set('\'');
        UNRESERVED.set('(');
        UNRESERVED.set(')');
        // punct chars
        PUNCT.set(',');
        PUNCT.set(';');
        PUNCT.set(':');
        PUNCT.set('$');
        PUNCT.set('&');
        PUNCT.set('+');
        PUNCT.set('=');
        // Safe for userinfo
        USERINFO.or(UNRESERVED);
        USERINFO.or(PUNCT);

        // URL path safe
        PATHSAFE.or(UNRESERVED);
        // here we want to encode the segment separator, because we encode segment by segment
        // PATHSAFE.set('/'); // segment separator
        PATHSAFE.set(';'); // param separator
        PATHSAFE.set(':'); // rest as per list in 2396, i.e. : @ & = + $ ,
        PATHSAFE.set('@');
        PATHSAFE.set('&');
        PATHSAFE.set('=');
        PATHSAFE.set('+');
        PATHSAFE.set('$');
        PATHSAFE.set(',');

        RESERVED.set(';');
        RESERVED.set('/');
        RESERVED.set('?');
        RESERVED.set(':');
        RESERVED.set('@');
        RESERVED.set('&');
        RESERVED.set('=');
        RESERVED.set('+');
        RESERVED.set('$');
        RESERVED.set(',');
        RESERVED.set('['); // added by RFC 2732
        RESERVED.set(']'); // added by RFC 2732

        URIC.or(RESERVED);
        URIC.or(UNRESERVED);
    }

    /* end of from https://github.com/apache/httpcomponents-client/commit/b58e7d46d75e1d3c42f5fd6db9bd45f32a49c639#diff-a74b24f025e68ec11e4550b42e9f807d */
}
