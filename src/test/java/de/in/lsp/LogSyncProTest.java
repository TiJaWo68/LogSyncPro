package de.in.lsp;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class LogSyncProTest {

    @Test
    void testDetectApplicationName() throws Exception {
        LogSyncPro app = new LogSyncPro(new String[] {});
        Method method = LogSyncPro.class.getDeclaredMethod("detectApplicationName", String.class);
        method.setAccessible(true);

        // Basic cases
        assertEquals("access", method.invoke(app, "access.log"));
        assertEquals("error", method.invoke(app, "error.log"));

        // Rotated logs
        assertEquals("access", method.invoke(app, "access.log.1"));
        assertEquals("access", method.invoke(app, "access.log.2.gz"));

        // Date suffixed
        assertEquals("app", method.invoke(app, "app-2023-10-27.log"));
        assertEquals("server", method.invoke(app, "server_2023-10-27.log"));

        // Versioned
        assertEquals("my-app", method.invoke(app, "my-app-1.0.0.log"));
        assertEquals("service", method.invoke(app, "service-v2.log"));

        // Complex
        assertEquals("audit", method.invoke(app, "audit.log.2023-10-27.txt"));
    }
}
