package de.in.lsp.parser;

import de.in.lsp.model.LogEntry;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MirthLogParserTest {

        @Test
        void testMirthFormatWithMultiLine() throws Exception {
                LogFormatConfig config = new LogFormatConfig(
                                "Mirth-Connect",
                                "^[A-Z]+\\s+\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s+\\[.*$",
                                "^([A-Z]+)\\s+(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+\\[(.*?)\\]\\s+(.*?):\\s+(.*)$",
                                "yyyy-MM-dd HH:mm:ss.SSS",
                                2, 1, 3, 4, 5);
                ConfigurableLogParser parser = new ConfigurableLogParser(config);

                String logContent = "INFO  2023-01-19 09:38:21.291 [Shutdown Hook Thread] com.mirth.connect.server.Mirth: shutting down mirth\n"
                                +
                                "ERROR 2023-01-19 09:39:29.590 [DeployTask] com.mirth...util.javascript.JavaScriptUtil: Error executing script\n"
                                +
                                "com.mirth.connect.server.MirthJavascriptTransformerException: \n" +
                                "CHANNEL:\tSynchronizeWithOrbis\n" +
                                "SOURCE CODE:\t\n" +
                                " 97: }";

                List<LogEntry> entries = parser.parse(
                                new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8)),
                                "mirth.log");

                assertEquals(2, entries.size());

                LogEntry info = entries.get(0);
                assertEquals("INFO", info.level());
                assertEquals(LocalDateTime.of(2023, 1, 19, 9, 38, 21, 291000000), info.timestamp());
                assertEquals("Shutdown Hook Thread", info.thread());
                assertEquals("com.mirth.connect.server.Mirth", info.loggerName());
                assertEquals("shutting down mirth", info.message());

                LogEntry error = entries.get(1);
                assertEquals("ERROR", error.level());
                assertEquals("DeployTask", error.thread());
                assertEquals("com.mirth...util.javascript.JavaScriptUtil", error.loggerName());
                assertTrue(error.message().contains("Error executing script"));
                assertTrue(error.message().contains("com.mirth.connect.server.MirthJavascriptTransformerException:"));
                assertTrue(error.message().contains("CHANNEL:\tSynchronizeWithOrbis"));
                assertTrue(error.message().contains("97: }"));
        }
}
