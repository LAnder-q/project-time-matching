package com.pmtool.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果封装
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> list;
    private Long total;
    private Long pageNum;
    private Long pageSize;

    /**
     * 快速构建分页结果
     */
    public static <T> PageResult<T> of(List<T> list, Long total, Long pageNum, Long pageSize) {
        return new PageResult<>(list, total, pageNum, pageSize);
    }
}
