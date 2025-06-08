package org.soprasteria.avans.lockercloud.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PathUtilsTest {

    private String originalOs = System.getProperty("os.name");

    @AfterEach
    void restoreOs() {
        System.setProperty("os.name", originalOs);
    }

    @Test
    void normalize_posixPath_shouldHandleDotsAndUnicode() {
        System.setProperty("os.name", "Linux");
        String normalized = PathUtils.normalize("folder/../\u30c7\u30a3\u30ec\u30af\u30c8\u30ea/./\u30d5\u30a1\u30a4\u30eb.txt");
        assertEquals("\u30c7\u30a3\u30ec\u30af\u30c8\u30ea/\u30d5\u30a1\u30a4\u30eb.txt", normalized);
    }

    @Test
    void normalize_windowsDrivePath_shouldAddLongPrefix() {
        System.setProperty("os.name", "Windows 10");
        StringBuilder longPart = new StringBuilder();
        for (int i = 0; i < 270; i++) {
            longPart.append('a');
        }
        String raw = "C:\\temp\\" + longPart + "\\file.txt";
        String normalized = PathUtils.normalize(raw);
        assertTrue(normalized.startsWith("\\\\?\\"));
        assertTrue(normalized.contains("file.txt"));
    }

    @Test
    void normalize_windowsUNCPath_shouldHandleParentSegments() {
        System.setProperty("os.name", "Windows 10");
        String raw = "\\\\server\\share\\dir\\..\\file.txt";
        String normalized = PathUtils.normalize(raw);
        assertEquals("\\\\server\\share\\file.txt", normalized);
    }

    @Test
    void isValidFileName_windowsChecksReservedAndInvalidChars() {
        System.setProperty("os.name", "Windows 10");
        assertFalse(PathUtils.isValidFileName("CON"));
        assertFalse(PathUtils.isValidFileName("a:b.txt"));
        assertTrue(PathUtils.isValidFileName("resume.pdf"));
        assertTrue(PathUtils.isValidFileName("\u30d5\u30a1\u30a4\u30eb.txt"));
    }

    @Test
    void isValidFileName_posixSimple() {
        System.setProperty("os.name", "Linux");
        assertFalse(PathUtils.isValidFileName("bad/name"));
        assertTrue(PathUtils.isValidFileName("r\u00e9sum\u00e9.pdf"));
    }
}
