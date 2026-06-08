package com.doccase.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private List<T> records;
    private long total;
    private int pageNum;
    private int pageSize;
    private int totalPages;

    public static <T> PageResult<T> of(List<T> records, long total, int pageNum, int pageSize) {
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return new PageResult<>(records, total, pageNum, pageSize, totalPages);
    }
}
