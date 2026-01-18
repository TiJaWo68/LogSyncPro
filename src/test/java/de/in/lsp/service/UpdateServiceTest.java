package de.in.lsp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.in.updraft.UpdateChannel;

public class UpdateServiceTest {

    private String originalProperty;

    @BeforeEach
    public void setup() {
        originalProperty = System.getProperty("lsp.test.version");
    }

    @AfterEach
    public void tearDown() {
        if (originalProperty != null) {
            System.setProperty("lsp.test.version", originalProperty);
        } else {
            System.clearProperty("lsp.test.version");
        }
    }

    @Test
    public void testVersionInjection() throws Exception {
        System.setProperty("lsp.test.version", "0.0.0");

        UpdateService service = new UpdateService(null, "1.0.0", UpdateChannel.STABLE);

        Field versionField = UpdateService.class.getDeclaredField("currentVersion");
        versionField.setAccessible(true);
        String currentVersion = (String) versionField.get(service);

        assertEquals("0.0.0", currentVersion, "Current version should be overridden by system property");
    }

    @Test
    public void testNoInjection() throws Exception {
        System.clearProperty("lsp.test.version");

        UpdateService service = new UpdateService(null, "1.0.0", UpdateChannel.STABLE);

        Field versionField = UpdateService.class.getDeclaredField("currentVersion");
        versionField.setAccessible(true);
        String currentVersion = (String) versionField.get(service);

        assertEquals("1.0.0", currentVersion, "Current version should NOT be overridden when property is missing");
    }
}
