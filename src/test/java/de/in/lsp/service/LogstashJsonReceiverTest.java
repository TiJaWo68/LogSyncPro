package de.in.lsp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import de.in.lsp.model.LogEntry;

/**
 * Tests for the Logstash JSON receiver mapping.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogstashJsonReceiverTest {

	private static final String CLIENT_IP = "192.168.178.20";
	private static final int CLIENT_PORT = 4560;

	private static final String NORMAL_EVENT = "{\"@timestamp\":\"2026-11-08T15:18:59.2030841+01:00\",\"@version\":\"1\","
			+ "\"message\":\"Starting MoneyPolyApplication using Java 25.0.5 with PID 51824\","
			+ "\"logger_name\":\"de.in.mp.MoneyPolyApplication\",\"thread_name\":\"main\",\"level\":\"INFO\","
			+ "\"level_value\":20000,\"application\":\"moneypoly\"}";

	private static final String STACK_TRACE_EVENT = "{\"@timestamp\":\"2026-11-08T15:19:01.3344918+01:00\",\"@version\":\"1\","
			+ "\"message\":\"Exception sending context initialized event\","
			+ "\"logger_name\":\"org.apache.catalina.core.ContainerBase\",\"thread_name\":\"main\",\"level\":\"ERROR\","
			+ "\"level_value\":40000,\"stack_trace\":\"java.lang.RuntimeException: boom\\n\\tat de.in.mp.Foo.bar(Foo.java:42)\","
			+ "\"application\":\"moneypoly\"}";

	private final LogstashJsonReceiver receiver = new LogstashJsonReceiver(4560, (entry, address) -> {
		// No-op consumer for mapping tests
	});

	@Test
	public void testMapNormalEvent() {
		LogEntry entry = receiver.parse(NORMAL_EVENT, CLIENT_IP, CLIENT_PORT);

		LocalDateTime expected = OffsetDateTime.parse("2026-11-08T15:18:59.2030841+01:00").toInstant()
				.atZone(ZoneId.systemDefault()).toLocalDateTime();

		assertEquals(expected, entry.timestamp());
		assertEquals("INFO", entry.level());
		assertEquals("main", entry.thread());
		assertEquals("de.in.mp.MoneyPolyApplication", entry.loggerName());
		assertEquals("Starting MoneyPolyApplication using Java 25.0.5 with PID 51824", entry.message());
		assertEquals("moneypoly", entry.sourceFile());
		assertEquals(NORMAL_EVENT, entry.rawLine());
		assertEquals(CLIENT_IP, entry.ip());
		assertEquals(CLIENT_PORT, entry.port());
	}

	@Test
	public void testStackTraceIsAppendedToMessage() {
		LogEntry entry = receiver.parse(STACK_TRACE_EVENT, CLIENT_IP, CLIENT_PORT);

		String expectedMessage = "Exception sending context initialized event" + "\n"
				+ "java.lang.RuntimeException: boom\n\tat de.in.mp.Foo.bar(Foo.java:42)";

		assertEquals(expectedMessage, entry.message());
	}

	@Test
	public void testInvalidJsonLineReturnsNull() {
		assertNull(receiver.parse("{invalid json", CLIENT_IP, CLIENT_PORT));
	}

	@Test
	public void testEmptyLineReturnsNull() {
		assertNull(receiver.parse("", CLIENT_IP, CLIENT_PORT));
	}

	@Test
	public void testMissingApplicationFallsBackToMoneypoly() {
		String json = "{\"@timestamp\":\"2026-11-08T15:18:59.2030841+01:00\",\"message\":\"hello\",\"level\":\"INFO\"}";

		LogEntry entry = receiver.parse(json, CLIENT_IP, CLIENT_PORT);

		assertEquals("moneypoly", entry.sourceFile());
	}
}
