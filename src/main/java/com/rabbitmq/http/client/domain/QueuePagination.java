package com.rabbitmq.http.client.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class QueuePagination extends AbstractPagination {
    private QueueInfo[] items;

    public QueuePagination() {
        super();
    }

    public QueuePagination(QueueInfo[] items) {
        super(items != null ? items.length : 0);
        this.items = items;
    }

    public QueueInfo[] getItems() {
        return items;
    }

    public List<QueueInfo> getItemsAsList() {
        return Arrays.asList(items);
    }

    public void setItems(QueueInfo[] items) {
        this.items = items;
    }
}
