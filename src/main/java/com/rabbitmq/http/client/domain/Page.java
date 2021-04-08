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

package com.rabbitmq.http.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.List;

public class Page<T> {

  private T[] items;

  @JsonProperty("filtered_count")
  private int filteredCount;

  @JsonProperty("item_count")
  private int itemCount;

  private int page;

  @JsonProperty("page_count")
  private int pageCount;

  @JsonProperty("page_size")
  private int pageSize;

  @JsonProperty("total_count")
  private int totalCount;

  public Page() {}

  public Page(T[] items) {
    int totalItems = items != null ? items.length : 0;
    this.items = items;
    itemCount = totalItems;
    totalCount = totalItems;
    pageCount = 1;
    page = 1;
    pageSize = totalItems;
    filteredCount = totalItems;
  }

  public T[] getItems() {
    return items;
  }

  public void setItems(T[] items) {
    this.items = items;
  }

  public List<T> getItemsAsList() {
    return Arrays.asList(items);
  }

  public int getFilteredCount() {
    return filteredCount;
  }

  public int getItemCount() {
    return itemCount;
  }

  public int getPage() {
    return page;
  }

  public int getPageCount() {
    return pageCount;
  }

  public int getPageSize() {
    return pageSize;
  }

  public int getTotalCount() {
    return totalCount;
  }

}
