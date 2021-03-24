package com.rabbitmq.http.client.domain;

import java.util.Arrays;
import java.util.List;

public class ChannelPagination extends AbstractPagination {
    private ChannelInfo[] items;

    public ChannelPagination() {
        super();
    }

    public ChannelPagination(ChannelInfo[] items) {
        super(items != null ? items.length : 0);
        this.items = items;
    }

    public ChannelInfo[] getItems() {
        return items;
    }

    public List<ChannelInfo> getItemsAsList() {
        return Arrays.asList(items);
    }

    public void setItems(ChannelInfo[] items) {
        this.items = items;
    }
}
