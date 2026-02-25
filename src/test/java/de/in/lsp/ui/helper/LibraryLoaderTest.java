package de.in.lsp.ui.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.in.lsp.ui.helper.LibraryLoader.LibraryInfo;

/**
 * Tests for {@link LibraryLoader}.
 */
public class LibraryLoaderTest {

    @Test
    public void loadLibraries_parsesArtifactIdCorrectly() {
        InputStream is = getClass().getResourceAsStream("/test-licenses.xml");
        LibraryLoader loader = new LibraryLoader();
        List<LibraryInfo> libs = loader.loadLibraries(is);

        assertFalse(libs.isEmpty(), "Libraries should be loaded");
        assertEquals(3, libs.size(), "Should load 3 dependencies");

        // First entry: flatlaf (known library)
        LibraryInfo flatlaf = libs.get(0);
        assertEquals("com.formdev", flatlaf.groupId());
        assertEquals("flatlaf", flatlaf.artifactId());
        assertEquals("3.7", flatlaf.version());
        assertEquals("The Apache License, Version 2.0", flatlaf.license());
        assertEquals("https://www.apache.org/licenses/LICENSE-2.0.txt", flatlaf.licenseUrl());
        assertEquals("https://github.com/JFormDesigner/FlatLaf", flatlaf.homepageUrl());
    }

    @Test
    public void loadLibraries_unknownLib_fallsBackToMvnRepository() {
        InputStream is = getClass().getResourceAsStream("/test-licenses.xml");
        LibraryLoader loader = new LibraryLoader();
        List<LibraryInfo> libs = loader.loadLibraries(is);

        LibraryInfo unknown = libs.get(1);
        assertEquals("org.example", unknown.groupId());
        assertEquals("unknown-lib", unknown.artifactId());
        assertEquals("1.0.0", unknown.version());
        assertEquals("MIT", unknown.license());
        assertEquals("https://opensource.org/licenses/MIT", unknown.licenseUrl());
        assertEquals("https://mvnrepository.com/artifact/org.example/unknown-lib", unknown.homepageUrl());
    }

    @Test
    public void loadLibraries_missingLicenseUrl_returnsEmptyString() {
        InputStream is = getClass().getResourceAsStream("/test-licenses.xml");
        LibraryLoader loader = new LibraryLoader();
        List<LibraryInfo> libs = loader.loadLibraries(is);

        LibraryInfo xz = libs.get(2);
        assertEquals("xz", xz.artifactId());
        assertEquals("Public Domain", xz.license());
        assertEquals("", xz.licenseUrl());
    }

    @Test
    public void loadLibraries_nullInput_returnsEmptyList() {
        LibraryLoader loader = new LibraryLoader();
        List<LibraryInfo> libs = loader.loadLibraries(null);
        assertTrue(libs.isEmpty());
    }

    @Test
    public void resolveHomepageUrl_knownLibrary() {
        assertEquals("https://github.com/JFormDesigner/FlatLaf",
                LibraryLoader.resolveHomepageUrl("com.formdev", "flatlaf"));
    }

    @Test
    public void resolveHomepageUrl_unknownLibrary() {
        assertEquals("https://mvnrepository.com/artifact/com.unknown/some-lib",
                LibraryLoader.resolveHomepageUrl("com.unknown", "some-lib"));
    }

    @Test
    public void loadLibraries_nameIsNotLicenseName() {
        InputStream is = getClass().getResourceAsStream("/test-licenses.xml");
        LibraryLoader loader = new LibraryLoader();
        List<LibraryInfo> libs = loader.loadLibraries(is);

        // Verify that the artifactId is NOT the license name
        for (LibraryInfo lib : libs) {
            assertFalse(lib.artifactId().equals(lib.license()),
                    "artifactId '" + lib.artifactId() + "' should differ from license name '" + lib.license() + "'");
        }
    }
}
