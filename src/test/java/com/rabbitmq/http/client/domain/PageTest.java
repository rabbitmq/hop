/*
 * Copyright 2025 the original author or authors.
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
package com.rabbitmq.http.client.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PageTest {

  @Test
  void itemsAsList() {
    String[] items = new String[] {"a", "b", "c"};
    Page<String> page = new Page<>(items);
    List<String> itemsAsList = page.getItemsAsList();
    assertNotNull(itemsAsList);
    assertThat(itemsAsList).hasSize(3);
    assertTrue(Arrays.equals(items, itemsAsList.toArray()));
  }

  @Test
  void itemsAsListWithNullArrayReturnsEmptyList() {
    Page<String> page = new Page<>(null);
    List<String> itemsAsList = assertDoesNotThrow(() -> page.getItemsAsList());
    assertNotNull(itemsAsList);
    assertThat(itemsAsList).isEmpty();
  }

  @Test
  void itemsAsListReturnsUnmodifiableList() {
    String[] items = new String[] {"a", "b", "c"};
    Page<String> page = new Page<>(items);
    List<String> itemsAsList = page.getItemsAsList();
    assertThrows(UnsupportedOperationException.class, () -> itemsAsList.add("d"));

    page = new Page<>(null);
    List<String> nullItemsAsList = page.getItemsAsList();
    assertThrows(UnsupportedOperationException.class, () -> nullItemsAsList.add("a"));
  }
}
