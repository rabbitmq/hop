package com.rabbitmq.http.client.domain;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URIUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class QueryParameters {

    private Map<String,Object> parameters = new HashMap<>();
    private Pagination pagination = new Pagination();
    private Columns columns = new Columns();

    public Pagination pagination() {
        return pagination;
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


    public URIBuilder appendToURI(URIBuilder builder) {
        parameters.forEach((name, value) -> {
            @SuppressWarnings("unchecked")
            String valueAsString = value instanceof Collection ?
                    String.join(",", (Iterable<String>)value) : String.valueOf(value);
            builder.addParameter(name, valueAsString);

        });
        return builder;
    }

    public class Columns {
        public Columns add(String name) {
            @SuppressWarnings("unchecked")
            Set<String> columns = (Set<String>)parameters.get("columns");
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
            return parameters.containsKey("columns") || parameters.containsKey("sort") || parameters.containsKey("sort_reverse");
        }
    }

    public class Pagination {

        public Pagination setName(String name) {
            setName(name, false);
            return this;
        }
        public Pagination setName(String name, boolean usesRegex) {
            parameters.put("name", name);
            parameters.put("use_regex", usesRegex);
            setFirstPageIfNotSet();
            return this;
        }
        public Pagination setPageSize(int pageSize) {
            parameters.put("page_size", pageSize);
            setFirstPageIfNotSet();

            return this;
        }
        private void setFirstPageIfNotSet() {
            parameters.putIfAbsent("page", 1);
        }

        public Pagination nextPage(AbstractPagination pagination) {
            parameters.put("page", pagination.getPage()+1);
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
            return parameters.containsKey("name") || parameters.containsKey("page_size") || parameters.containsKey("page");
        }

    }
}
