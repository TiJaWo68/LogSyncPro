package de.in.lsp.parser;

/**
 * Configuration object defining the structure and regex patterns for a specific
 * log format.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public record LogFormatConfig(
        String name,
        String firstLinePattern,
        String entryRegex,
        String timestampPattern, // For DateTimeFormatter
        int timestampGroup,
        int levelGroup,
        int threadGroup,
        int loggerGroup,
        int messageGroup) {
}
