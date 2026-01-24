package de.in.lsp.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.in.lsp.model.LogEntry;

/**
 * Test for multi-pattern based log parsing, specifically for Postgres logs.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class PostgresLogParserTest {

	@Test
	public void testMixedPostgresLogs() throws Exception {
		List<String> patterns = Arrays.asList("%d{yyyy-MM-dd HH:mm:ss,SSS} %level: %msg%n",
				"%d{yyyy-MM-dd HH:mm:ss.SSS} UTC [%t] %level %msg%n");
		MultiPatternLogParser parser = new MultiPatternLogParser("Postgres (mixed)", patterns);

		String logContent = "2026-01-09 03:49:31,808 INFO: no action. I am (act4telerad-0), the leader with the lock\n"
				+ "2026-01-09 03:49:46.592 UTC [25] LOG {ticks: 0, maint: 0, retry: 0}\n" + "  continuation line for postgres native\n"
				+ "2026-01-09 03:50:01,806 INFO: no action. I am (act4telerad-0), the leader with the lock";

		ByteArrayInputStream is = new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8));
		List<LogEntry> entries = parser.parse(is, "postgres.log");

		assertEquals(3, entries.size(), "Should have 3 entries, check patterns and log content");

		// Entry 1 (Patroni style)
		assertEquals("INFO", entries.get(0).level(), "Entry 0 should be INFO");
		assertTrue(entries.get(0).message().startsWith("no action"), "Entry 0 message mismatch");

		// Entry 2 (Postgres native style)
		assertEquals("LOG", entries.get(1).level(), "Entry 1 should be LOG");
		assertEquals("25", entries.get(1).thread(), "Entry 1 thread should be 25");
		assertTrue(entries.get(1).message().contains("{ticks: 0, maint: 0, retry: 0}"), "Entry 1 message missing content");
		assertTrue(entries.get(1).message().contains("continuation line"), "Entry 1 message missing continuation");

		// Entry 3 (Patroni style again)
		assertEquals("INFO", entries.get(2).level(), "Entry 2 should be INFO");
		assertTrue(entries.get(2).message().startsWith("no action"), "Entry 2 message mismatch");
	}
}
