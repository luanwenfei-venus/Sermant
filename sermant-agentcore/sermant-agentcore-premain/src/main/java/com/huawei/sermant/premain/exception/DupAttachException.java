package com.huawei.sermant.premain.exception;

/**
 * 重复挂载异常
 *
 * @author luanwenfei
 * @since 2023-04-15
 */
public class DupAttachException extends RuntimeException {
    /**
     * 构造方法
     */
    public DupAttachException() {
        super("Unable to attach sermant agent duplicated, detach sermant agent firstly.");
    }
}
