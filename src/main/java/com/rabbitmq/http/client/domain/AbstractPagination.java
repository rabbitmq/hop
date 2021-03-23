package com.rabbitmq.http.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 *  {
 *   "filtered_count": 3,
 *   "item_count": 3,
 *   "items": [
 *      ...
 *   ],
 *   "page": 1,
 *   "page_count": 1,
 *   "page_size": 50,
 *   "total_count": 3
 *   }
 */
public abstract class AbstractPagination {
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

    public AbstractPagination() {
    }

    public AbstractPagination(int totalItems) {
        itemCount = totalItems;
        totalCount = totalItems;
        pageCount = 1;
        pageSize = totalItems;
        filteredCount = totalItems;
    }

    public int getFilteredCount() {
        return filteredCount;
    }

    public void setFilteredCount(int filteredCount) {
        this.filteredCount = filteredCount;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

}
