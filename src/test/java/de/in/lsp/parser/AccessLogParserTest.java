package de.in.lsp.parser;

import de.in.lsp.model.LogEntry;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AccessLogParserTest {

    @Test
    void testAccessLogParsing() throws Exception {
        PatternBasedLogParser parser = new PatternBasedLogParser("%h - - [%d{dd/MMM/yyyy:HH:mm:ss Z}] %msg%n");

        String logContent = "127.0.0.6 - - [06/Jan/2026:11:02:42 +0000] \"GET /viewer/assets/conf/appConfig.yaml?t=1767697362619 HTTP/1.1\" 200 1373 \"https://mnc03-durad2.dedalus.lan/viewer/?locale=de-DE\" \"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0\"\n"
                +
                "127.0.0.6 - - [06/Jan/2026:11:02:42 +0000] \"GET /viewer/assets/default-conf/appConfigDefault.yaml?t=1767697362619 HTTP/1.1\" 200 799 \"https://mnc03-durad2.dedalus.lan/viewer/?locale=de-DE\" \"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0\"";

        List<LogEntry> entries = parser.parse(new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8)),
                "access.log");

        assertEquals(2, entries.size());

        LogEntry first = entries.get(0);
        assertEquals("127.0.0.6", first.ip());
        // Date: 06/Jan/2026:11:02:42 +0000
        // Our LocalDateTime doesn't store Zone, but we should check if it parsed
        // something reasonable
        assertNotNull(first.timestamp());
        assertEquals(2026, first.timestamp().getYear());
        assertEquals(1, first.timestamp().getMonthValue());
        assertEquals(6, first.timestamp().getDayOfMonth());
        assertEquals(11, first.timestamp().getHour());

        assertTrue(first.message().contains("GET /viewer/assets/conf/appConfig.yaml"));
    }
}
