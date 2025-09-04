package com.jbh.account_app.common.logging;

import org.slf4j.Logger;

public final class LoggerFactory {

    private LoggerFactory() {
        // Utility class
    }

    public static Logger getLogger(Class<?> clazz) {
        return org.slf4j.LoggerFactory.getLogger(clazz);
    }

    public static Logger getLogger(String name) {
        return org.slf4j.LoggerFactory.getLogger(name);
    }

    public static StructuredLogger getStructuredLogger(Class<?> clazz) {
        return new StructuredLogger(getLogger(clazz));
    }

    public static StructuredLogger getStructuredLogger(String name) {
        return new StructuredLogger(getLogger(name));
    }

    public static class StructuredLogger {
        private final Logger logger;

        public StructuredLogger(Logger logger) {
            this.logger = logger;
        }

        public void info(String message, Object... args) {
            logger.info(message, args);
        }

        public void debug(String message, Object... args) {
            logger.debug(message, args);
        }

        public void warn(String message, Object... args) {
            logger.warn(message, args);
        }

        public void error(String message, Object... args) {
            logger.error(message, args);
        }

        public void error(String message, Throwable throwable, Object... args) {
            logger.error(message, throwable);
        }

        public Logger getLogger() {
            return logger;
        }

        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        public boolean isInfoEnabled() {
            return logger.isInfoEnabled();
        }

        public boolean isWarnEnabled() {
            return logger.isWarnEnabled();
        }

        public boolean isErrorEnabled() {
            return logger.isErrorEnabled();
        }
    }
}