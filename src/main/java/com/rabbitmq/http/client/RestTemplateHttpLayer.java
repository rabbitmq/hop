/*
 * Copyright 2021 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.http.client.domain.ChannelDetails;
import com.rabbitmq.http.client.domain.CurrentUserDetails;
import com.rabbitmq.http.client.domain.UserInfo;
import com.rabbitmq.http.client.domain.VhostLimits;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

class RestTemplateHttpLayer implements HttpLayer {

  private final RestTemplate rt;

  RestTemplateHttpLayer(RestTemplate rt) {
    this.rt = rt;
  }

  static ObjectMapper createDefaultObjectMapper() {
    return Jackson2ObjectMapperBuilder.json()
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        .featuresToEnable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
        .deserializerByType(VhostLimits.class, JsonUtils.VHOST_LIMITS_DESERIALIZER_INSTANCE)
        .deserializerByType(UserInfo.class, JsonUtils.USER_INFO_DESERIALIZER_INSTANCE)
        .deserializerByType(
            CurrentUserDetails.class, JsonUtils.CURRENT_USER_DETAILS_DESERIALIZER_INSTANCE)
        .deserializerByType(ChannelDetails.class, JsonUtils.CHANNEL_DETAILS_DESERIALIZER_INSTANCE)
        .build();
  }

  private static List<HttpMessageConverter<?>> getMessageConverters() {
    List<HttpMessageConverter<?>> xs = new ArrayList<HttpMessageConverter<?>>();
    xs.add(new MappingJackson2HttpMessageConverter(createDefaultObjectMapper()));
    return xs;
  }

  @Override
  public <T> T get(URI uri, Class<T> responseClass) {
    try {
      return this.rt.getForObject(uri, responseClass);
    } catch (final HttpClientErrorException ce) {
      if (ce.getStatusCode() == HttpStatus.NOT_FOUND) {
        return null;
      } else {
        throw ce;
      }
    }
  }

  @Override
  public <T> T get(URI uri, ParameterizedTypeReference<T> typeReference) {
    RequestCallback requestCallback = this.rt.httpEntityCallback(null, typeReference.getType());
    ResponseExtractor<ResponseEntity<T>> responseExtractor =
        this.rt.responseEntityExtractor(typeReference.getType());
    ResponseEntity<T> response =
        this.rt.execute(uri, HttpMethod.GET, requestCallback, responseExtractor);
    if (response == null) {
      throw new IllegalStateException("Response cannot be null");
    } else {
      return response.getBody();
    }
  }

  @Override
  public <T> T post(URI uri, Object requestBody, Class<T> responseClass) {
    if (responseClass == null) {
      this.rt.postForLocation(uri, requestBody);
      return null;
    } else {
      return this.rt.postForObject(uri, requestBody, responseClass);
    }
  }

  @Override
  public void put(URI uri, Object requestBody) {
    this.rt.put(uri, requestBody);
  }

  @Override
  public void delete(URI uri, Map<String, String> headers) {
    if (headers == null) {
      try {
        this.rt.delete(uri);
      } catch (final HttpClientErrorException ce) {
        if (!(ce.getStatusCode() == HttpStatus.NOT_FOUND)) {
          throw ce;
        }
      }
    } else {
      try {
        MultiValueMap<String, String> h = new LinkedMultiValueMap<>();
        headers.entrySet().forEach(entry -> h.add(entry.getKey(), entry.getValue()));
        HttpEntity<Object> entity = new HttpEntity<>(null, h);
        this.rt.exchange(uri, HttpMethod.DELETE, entity, Object.class);
      } catch (final HttpClientErrorException ce) {
        if (!(ce.getStatusCode() == HttpStatus.NOT_FOUND)) {
          throw ce;
        }
      }
    }
  }

  static class RestTemplateHttpLayerFactory implements HttpLayerFactory {

    @Override
    public HttpLayer create(ClientCreationContext context) {
      ClientParameters parameters = context.getClientParameters();
      RestTemplate restTemplate = new RestTemplate();
      context.restTemplate(restTemplate);
      restTemplate.setMessageConverters(getMessageConverters());
      RestTemplateConfigurator restTemplateConfigurator =
          parameters.getRestTemplateConfigurator() == null
              ? new SimpleRestTemplateConfigurator()
              : parameters.getRestTemplateConfigurator();
      restTemplate = restTemplateConfigurator.configure(context);
      return new RestTemplateHttpLayer(restTemplate);
    }
  }
}
