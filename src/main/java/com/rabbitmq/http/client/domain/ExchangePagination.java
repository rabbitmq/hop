package com.rabbitmq.http.client.domain;

import java.util.Arrays;
import java.util.List;

public class ExchangePagination extends AbstractPagination {
    private ExchangeInfo[] items;

    public ExchangePagination() {
        super();
    }

    public ExchangePagination(ExchangeInfo[] items) {
        super(items != null ? items.length : 0);
        this.items = items;
    }

    public ExchangeInfo[] getItems() {
        return items;
    }

    public List<ExchangeInfo> getItemsAsList() {
        return Arrays.asList(items);
    }

    public void setItems(ExchangeInfo[] items) {
        this.items = items;
    }
}
