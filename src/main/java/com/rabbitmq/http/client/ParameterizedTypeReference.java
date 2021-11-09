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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * To capture and pass a generic {@link Type}.
 *
 * <p>From the <a
 * href="https://github.com/spring-projects/spring-framework/blob/main/spring-core/src/main/java/org/springframework/core/ParameterizedTypeReference.java">Spring
 * Framework.</a>
 *
 * @param <T>
 */
abstract class ParameterizedTypeReference<T> {

  private final Type type;

  protected ParameterizedTypeReference() {
    Class<?> parameterizedTypeReferenceSubclass =
        findParameterizedTypeReferenceSubclass(getClass());
    Type type = parameterizedTypeReferenceSubclass.getGenericSuperclass();
    if (!(type instanceof ParameterizedType)) {
      throw new IllegalArgumentException("Type must be a parameterized type");
    }
    ParameterizedType parameterizedType = (ParameterizedType) type;
    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
    if (actualTypeArguments.length != 1) {
      throw new IllegalArgumentException("Number of type arguments must be 1");
    }
    this.type = actualTypeArguments[0];
  }

  private ParameterizedTypeReference(Type type) {
    this.type = type;
  }

  /**
   * Build a {@code ParameterizedTypeReference} wrapping the given type.
   *
   * @param type a generic type (possibly obtained via reflection, e.g. from {@link
   *     java.lang.reflect.Method#getGenericReturnType()})
   * @return a corresponding reference which may be passed into {@code
   *     ParameterizedTypeReference}-accepting methods
   * @since 4.3.12
   */
  public static <T> ParameterizedTypeReference<T> forType(Type type) {
    return new ParameterizedTypeReference<T>(type) {};
  }

  private static Class<?> findParameterizedTypeReferenceSubclass(Class<?> child) {
    Class<?> parent = child.getSuperclass();
    if (Object.class == parent) {
      throw new IllegalStateException("Expected ParameterizedTypeReference superclass");
    } else if (ParameterizedTypeReference.class == parent) {
      return child;
    } else {
      return findParameterizedTypeReferenceSubclass(parent);
    }
  }

  public Type getType() {
    return this.type;
  }

  @Override
  public boolean equals(Object other) {
    return (this == other
        || (other instanceof org.springframework.core.ParameterizedTypeReference
            && this.type.equals(((ParameterizedTypeReference<?>) other).type)));
  }

  @Override
  public int hashCode() {
    return this.type.hashCode();
  }

  @Override
  public String toString() {
    return "ParameterizedTypeReference<" + this.type + ">";
  }
}
