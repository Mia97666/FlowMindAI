package com.flowmind.common.dto;

import java.util.List;

/**
 * 统一分页响应体。
 *
 * @param <T> 列表元素类型
 */
public class PageResponse<T> {

    private List<T> list;
    private int page;
    private int size;
    private long total;
    private int totalPages;

    public PageResponse() {}

    public PageResponse(List<T> list, int page, int size, long total, int totalPages) {
        this.list = list;
        this.page = page;
        this.size = size;
        this.total = total;
        this.totalPages = totalPages;
    }

    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}