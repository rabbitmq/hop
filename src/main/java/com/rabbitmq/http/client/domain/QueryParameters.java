/*
 * Copyright 2021-2022 the original author or authors.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;

public class QueryParameters {

  private final Map<String, Object> parameters = new HashMap<>();
  private final Pagination pagination = new Pagination();
  private final Columns columns = new Columns();

  public Pagination pagination() {
    return pagination;
  }

  public QueryParameters name(String name) {
    name(name, false);
    return this;
  }

  public QueryParameters name(String name, boolean usesRegex) {
    parameters.put("name", name);
    parameters.put("use_regex", usesRegex);
    this.pagination.setFirstPageIfNotSet();
    return this;
  }

  public Columns columns() {
    return columns;
  }

  public QueryParameters clear() {
    parameters.clear();
    return this;
  }

  public boolean isEmpty() {
    return parameters.isEmpty();
  }

  public Map<String, String> parameters() {
    return parameters.entrySet().stream().reduce(new LinkedHashMap<>(),
        (BiFunction<Map<String, String>, Entry<String, Object>, Map<String, String>>) (acc, entry) -> {
          String name = entry.getKey();
          Object value = entry.getValue();
          @SuppressWarnings("unchecked")
          String valueAsString =
              value instanceof Collection
                  ? String.join(",", (Iterable<String>) value)
                  : String.valueOf(value);
          acc.put(name, valueAsString);
          return acc;
        }, (map1, map2) -> {
          map1.putAll(map2);
          return map1;
        }
    );
  }

  QueryParameters parameter(String field, Object value) {
    this.parameters.put(field, value);
    return this;
  }

  public class Columns {
    public Columns add(String name) {
      @SuppressWarnings("unchecked")
      Set<String> columns = (Set<String>) parameters.get("columns");
      if (columns == null) {
        columns = new HashSet<>();
        parameters.put("columns", columns);
      }
      columns.add(name);
      return this;
    }

    public Columns sort(String name) {
      parameters.put("sort", name);
      return this;
    }

    public Columns sortReverse(boolean sortReverse) {
      parameters.put("sort_reverse", sortReverse);
      return this;
    }

    public Columns clear() {
      parameters.remove("columns");
      parameters.remove("sort");
      parameters.remove("sort_reverse");
      return this;
    }

    public QueryParameters query() {
      return QueryParameters.this;
    }

    public boolean hasAny() {
      return parameters.containsKey("columns")
          || parameters.containsKey("sort")
          || parameters.containsKey("sort_reverse");
    }

  }

  public class Pagination {

    public Pagination pageSize(int pageSize) {
      parameters.put("page_size", pageSize);
      setFirstPageIfNotSet();
      return this;
    }

    private void setFirstPageIfNotSet() {
      parameters.putIfAbsent("page", 1);
    }

    public Pagination nextPage(Page page) {
      parameters.put("page", page.getPage() + 1);
      return this;
    }

    public Pagination clear() {
      parameters.remove("name");
      parameters.remove("use_regex");
      parameters.remove("page_size");
      parameters.remove("page");
      return this;
    }

    public QueryParameters query() {
      return QueryParameters.this;
    }

    public boolean hasAny() {
      return parameters.containsKey("name")
          || parameters.containsKey("page_size")
          || parameters.containsKey("page");
    }
  }
}
