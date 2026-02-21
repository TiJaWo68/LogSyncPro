package de.in.lsp.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.in.lsp.model.LogEntry;

/**
 * JUnit test class for verifying the parsing of istio-proxy logs.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
class IstioLogParserTest {

	@Test
	void testIstioProxyFormat() throws Exception {
		LogFormatConfig config = new LogFormatConfig("Istio Proxy", "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z\\t.*",
				"^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z)\\t(\\w+)\\t(.*?)(?:\\t([a-zA-Z0-9_.-]+))?$",
				"yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", 1, 2, -1, 4, -1, 3);
		ConfigurableLogParser parser = new ConfigurableLogParser(config);

		String logContent = "2025-12-25T08:59:40.206963Z\tinfo\tFLAG: --concurrency=\"0\"\tact4telerad-0_istio-proxy.log\n"
				+ "2025-12-25T08:59:40.206980Z\tinfo\tFLAG: --domain=\"apps.svc.cluster.local\"\tact4telerad-0_istio-proxy.log";

		List<LogEntry> entries = parser.parse(new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8)),
				"act4telerad-0_istio-proxy.log");

		assertEquals(2, entries.size());

		LogEntry first = entries.get(0);
		assertNotNull(first.timestamp());
		assertEquals(LocalDateTime.of(2025, 12, 25, 8, 59, 40, 206963000), first.timestamp());
		assertEquals("info", first.level());
		assertEquals("FLAG: --concurrency=\"0\"", first.message());
		assertEquals("act4telerad-0_istio-proxy.log", first.loggerName());
	}
}
