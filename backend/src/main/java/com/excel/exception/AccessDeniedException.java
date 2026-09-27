package com.excel.exception;

/**
 * 数据越权访问异常
 * 当科室人员尝试访问其他科室上传的批次数据时抛出
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
