package org.soprasteria.avans.lockercloud.util;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Utility methods for handling file system paths across platforms.
 */
public final class PathUtils {
    private PathUtils() {
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase(Locale.ROOT).startsWith("windows");
    }

    /**
     * Normalize the given raw path string for the current platform.
     * Supports Windows drive letters, UNC paths and long path prefixes.
     *
     * @param rawPath raw input path
     * @return normalized path string
     */
    public static String normalize(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("Path must not be null or empty");
        }
        if (isWindows()) {
            return normalizeWindows(rawPath);
        }
        return normalizePosix(rawPath);
    }

    /**
     * Convert a raw path into a Path object for the current platform.
     *
     * @param rawPath raw input path
     * @return platform Path
     */
    public static Path toPlatformPath(String rawPath) {
        String normalized = normalize(rawPath);
        try {
            return Paths.get(normalized);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Invalid path: " + rawPath, e);
        }
    }

    /**
     * Validate a file name for the current platform.
     *
     * @param name file name
     * @return true if valid
     */
    public static boolean isValidFileName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        if (isWindows()) {
            String invalid = "<>:\"/\\|?*";
            for (char c : invalid.toCharArray()) {
                if (name.indexOf(c) >= 0) {
                    return false;
                }
            }
            if (name.endsWith(" ") || name.endsWith(".")) {
                return false;
            }
            String upper = name.toUpperCase(Locale.ROOT);
            Set<String> reserved = Set.of(
                    "CON", "PRN", "AUX", "NUL",
                    "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
                    "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");
            return !reserved.contains(upper);
        }
        return !name.contains("/");
    }

    private static String normalizePosix(String rawPath) {
        try {
            Path path = Paths.get(rawPath).normalize();
            return path.toString();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Invalid path: " + rawPath, e);
        }
    }

    private static String normalizeWindows(String rawPath) {
        String path = rawPath.replace('/', '\\');
        boolean addLongPrefix = false;
        if (path.startsWith("\\\\?\\")) {
            path = path.substring(4);
            addLongPrefix = true;
        }
        String prefix = "";
        String remaining = path;
        if (path.startsWith("\\\\")) {
            int first = path.indexOf('\\', 2);
            if (first > 0) {
                int second = path.indexOf('\\', first + 1);
                if (second > 0) {
                    prefix = path.substring(0, second);
                    remaining = path.substring(second + 1);
                } else {
                    prefix = path;
                    remaining = "";
                }
            }
        } else if (path.length() >= 2 && path.charAt(1) == ':') {
            prefix = path.substring(0, 2);
            if (path.length() > 2 && (path.charAt(2) == '\\' || path.charAt(2) == '/')) {
                remaining = path.substring(3);
            } else {
                remaining = path.substring(2);
            }
        }

        Deque<String> stack = new ArrayDeque<>();
        for (String part : remaining.split("\\\\+")) {
            if (part.isEmpty() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!stack.isEmpty() && !"..".equals(stack.peekLast())) {
                    stack.removeLast();
                } else {
                    stack.addLast(part);
                }
            } else {
                stack.addLast(part);
            }
        }
        StringBuilder sb = new StringBuilder();
        if (!prefix.isEmpty()) {
            sb.append(prefix);
            if (!prefix.endsWith("\\")) {
                sb.append('\\');
            }
        }
        Iterator<String> it = stack.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append('\\');
            }
        }
        String normalized = sb.toString();
        if (addLongPrefix || normalized.length() > 260) {
            if (normalized.startsWith("\\\\")) {
                normalized = "\\\\?\\UNC" + normalized.substring(1);
            } else {
                normalized = "\\\\?\\" + normalized;
            }
        }
        return normalized;
    }
}
