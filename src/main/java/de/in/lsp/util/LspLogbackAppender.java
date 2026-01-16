package de.in.lsp.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * Logback appender that redirects logs to LspLogger.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LspLogbackAppender extends AppenderBase<ILoggingEvent> {

    @Override
    protected void append(ILoggingEvent event) {
        LspLogger.LogLevel level;
        switch (event.getLevel().toInt()) {
            case ch.qos.logback.classic.Level.ERROR_INT:
                level = LspLogger.LogLevel.ERROR;
                break;
            case ch.qos.logback.classic.Level.WARN_INT:
                level = LspLogger.LogLevel.WARN;
                break;
            case ch.qos.logback.classic.Level.INFO_INT:
                level = LspLogger.LogLevel.INFO;
                break;
            case ch.qos.logback.classic.Level.DEBUG_INT:
            case ch.qos.logback.classic.Level.TRACE_INT:
            default:
                level = LspLogger.LogLevel.DEBUG;
                break;
        }
        LspLogger.log(level, event.getFormattedMessage());
    }
}
