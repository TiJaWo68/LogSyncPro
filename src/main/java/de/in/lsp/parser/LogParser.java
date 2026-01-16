package de.in.lsp.parser;

import java.io.InputStream;
import java.util.List;

import de.in.lsp.model.LogEntry;

/**
 * Strategy interface for parsing different log formats.
 * Implementations define how to detect and parse log content from an
 * InputStream.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public interface LogParser {

    /**
     * Checks if this parser can handle the given log file content.
     */
    boolean canParse(String firstLine);

    /**
     * Parses the input stream and returns a list of log entries.
     */
    List<LogEntry> parse(InputStream inputStream, String sourceName) throws Exception;

    /**
     * Returns a human-readable name of the format.
     */
    String getFormatName();
}
