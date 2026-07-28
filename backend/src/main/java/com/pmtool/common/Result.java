package com.pmtool.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    /**
     * 成功响应
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> fail(String msg) {
        return new Result<>(500, msg, null);
    }

    /**
     * 失败响应（自定义状态码）
     */
    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}
