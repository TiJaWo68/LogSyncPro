package de.in.lsp.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for MultiSelectFilter.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class MultiSelectFilterTest {

    @Test
    public void testSetOptionsMaintainsSelectAll() {
        AtomicInteger callbackCount = new AtomicInteger(0);
        MultiSelectFilter filter = new MultiSelectFilter("Test", opts -> callbackCount.incrementAndGet());

        Set<String> options1 = Set.of("A", "B");
        filter.setOptions(options1);

        // Initial setOptions should default to everything selected and trigger callback
        assertEquals(1, callbackCount.get());
        assertTrue(filter.getSelectedOptions().containsAll(options1));
        assertFalse(filter.isActive()); // isActive should be false if all selected

        Set<String> options2 = Set.of("A", "B", "C");
        filter.setOptions(options2);

        // Should automatically select "C" because all were selected before
        assertEquals(2, callbackCount.get());
        assertTrue(filter.getSelectedOptions().containsAll(options2));
        assertFalse(filter.isActive());
    }

    @Test
    public void testSetOptionsMaintainsPartialSelection() {
        AtomicInteger callbackCount = new AtomicInteger(0);
        MultiSelectFilter filter = new MultiSelectFilter("Test", opts -> callbackCount.incrementAndGet());

        Set<String> options1 = Set.of("A", "B", "C");
        filter.setOptions(options1); // Callback 1

        filter.setSelectedOptions(Set.of("A", "B")); // Callback 2
        assertTrue(filter.isActive());
        assertEquals(2, callbackCount.get());

        Set<String> options2 = Set.of("A", "B", "D");
        filter.setOptions(options2);

        // Should NOT automatically select "D" because NOT all were selected before
        // AND should NOT trigger callback because the effective selection {"A", "B"}
        // remains the same.
        assertEquals(2, callbackCount.get());
        assertEquals(Set.of("A", "B"), filter.getSelectedOptions());
        assertTrue(filter.isActive());
    }
}
