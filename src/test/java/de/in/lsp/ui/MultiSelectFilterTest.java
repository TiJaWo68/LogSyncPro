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
    public void testFacetedSearchIndicatorPersistence() {
        AtomicInteger callbackCount = new AtomicInteger(0);
        MultiSelectFilter filter = new MultiSelectFilter("Test", opts -> callbackCount.incrementAndGet());

        Set<String> domain = Set.of("Namespace-A", "Namespace-B", "Namespace-C");
        filter.setOptions(domain); // Initial domain setup. All selected.
        assertFalse(filter.isActive(), "Should not be active when all are selected");

        // User manually selects ONLY Namespace-A
        filter.setSelectedOptions(Set.of("Namespace-A"));
        assertTrue(filter.isActive(), "Should be active when only a subset is selected");
        assertEquals(Set.of("Namespace-A"), filter.getSelectedOptions());

        // Faceted search: another column filters the data so only Namespace-A remains
        // VISIBLE
        filter.setOptions(Set.of("Namespace-A"));

        // BUG FIX CHECK:
        assertTrue(filter.isActive(),
                "Should REMAIN active even if only Namespace-A is visible, because B and C are still deselected in the domain");
        assertEquals(Set.of("Namespace-A"), filter.getSelectedOptions());
    }

    @Test
    public void testSelectAllAsReset() {
        MultiSelectFilter filter = new MultiSelectFilter("Test", opts -> {
        });

        Set<String> domain = Set.of("A", "B", "C");
        filter.setOptions(domain);

        // Filter to A
        filter.setSelectedOptions(Set.of("A"));
        assertTrue(filter.isActive());

        // Even if only A is visible
        filter.setOptions(Set.of("A"));

        // "Resetting" by selecting all
        filter.setSelectedOptions(filter.getAllOptions());
        assertFalse(filter.isActive(), "Filter should be inactive after selecting all domain options");
        assertEquals(3, filter.getSelectedOptions().size());
    }
}
