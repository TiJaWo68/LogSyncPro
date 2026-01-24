package de.in.lsp.ui;

import java.util.Arrays;

/**
 * Enumeration defining the columns in the LogView table. Replaces magic numbers for column indices.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public enum LogColumn {
	TIMESTAMP(0, "Timestamp"),
	LEVEL(1, "Level"),
	THREAD(2, "Thread"),
	LOGGER(3, "Logger"),
	IP(4, "IP"),
	MESSAGE(5, "Message"),
	SOURCE(6, "Source");

	private final int index;
	private final String header;

	LogColumn(int index, String header) {
		this.index = index;
		this.header = header;
	}

	public int getIndex() {
		return index;
	}

	public String getHeader() {
		return header;
	}

	public static LogColumn fromIndex(int index) {
		return Arrays.stream(values()).filter(c -> c.index == index).findFirst().orElse(null);
	}
}
