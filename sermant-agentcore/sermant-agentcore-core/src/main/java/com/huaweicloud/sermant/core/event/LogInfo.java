package com.huaweicloud.sermant.core.event;

import java.util.logging.LogRecord;

/**
 * 日志信息
 *
 * @author luanwenfei
 * @since 2023-03-08
 */
public class LogInfo {
    private String logLevel;

    private String logMessage;

    private String logClass;

    private String logMethod;

    private int logThreadId;

    private Throwable throwable;

    public LogInfo(LogRecord logRecord) {
        this.logLevel = logRecord.getLevel().getName();
        this.logMessage = logRecord.getMessage();
        this.logClass = logRecord.getSourceClassName();
        this.logMethod = logRecord.getSourceMethodName();
        this.logThreadId = logRecord.getThreadID();
        this.throwable = logRecord.getThrown();
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getLogMessage() {
        return logMessage;
    }

    public void setLogMessage(String logMessage) {
        this.logMessage = logMessage;
    }

    public String getLogClass() {
        return logClass;
    }

    public void setLogClass(String logClass) {
        this.logClass = logClass;
    }

    public String getLogMethod() {
        return logMethod;
    }

    public void setLogMethod(String logMethod) {
        this.logMethod = logMethod;
    }

    public int getLogThreadId() {
        return logThreadId;
    }

    public void setLogThreadId(int logThreadId) {
        this.logThreadId = logThreadId;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    @Override
    public String toString() {
        return "LogInfo{" + "logLevel='" + logLevel + '\'' + ", logMessage='" + logMessage + '\'' + ", logClass='"
            + logClass + '\'' + ", logMethod='" + logMethod + '\'' + ", logThreadId='" + logThreadId + '\''
            + ", throwable=" + throwable + '}';
    }
}
