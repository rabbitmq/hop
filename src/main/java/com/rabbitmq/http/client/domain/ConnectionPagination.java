package com.rabbitmq.http.client.domain;

import java.util.Arrays;
import java.util.List;

public class ConnectionPagination extends AbstractPagination {
    private ConnectionInfo[] items;

    public ConnectionPagination() {
        super();
    }

    public ConnectionPagination(ConnectionInfo[] items) {
        super(items != null ? items.length : 0);
        this.items = items;
    }

    public ConnectionInfo[] getItems() {
        return items;
    }

    public List<ConnectionInfo> getItemsAsList() {
        return Arrays.asList(items);
    }

    public void setItems(ConnectionInfo[] items) {
        this.items = items;
    }
}
