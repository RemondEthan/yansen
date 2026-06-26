package com.glodon.mordor.yansen.tool.builtin;

import com.glodon.mordor.yansen.tool.ToolProvider;
import io.agentscope.core.tool.Tool;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Built-in date/time tool. Discovered via SPI under id "datetime"; the bean's
 * {@code @Tool}-annotated methods are the individual invocable tools.
 * Each public method annotated with {@code @Tool} becomes an individually invokable tool.
 */
public class DateTimeTool implements ToolProvider {

    private static final String ID = "datetime";

    @Override
    public String toolId() {
        return ID;
    }

    @Tool(name = "current_datetime", description = "Returns the current local date-time in ISO-8601 format.")
    public String now() {
        return LocalDateTime.now().toString();
    }

    @Tool(name = "current_date", description = "Returns today's local date in ISO-8601 format (yyyy-MM-dd).")
    public String today() {
        return LocalDate.now().toString();
    }
}
