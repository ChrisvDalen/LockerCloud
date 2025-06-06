package org.soprasteria.avans.lockercloud.helper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Generates a stream of a given size without allocating the entire byte array.
 * Useful for simulating very large files in tests.
 */
public class LargeInputStream extends InputStream {
    private final long size;
    private long position;
    private final byte pattern;

    public LargeInputStream(long size, byte pattern) {
        this.size = size;
        this.pattern = pattern;
    }

    @Override
    public int read() throws IOException {
        if (position >= size) {
            return -1;
        }
        position++;
        return pattern & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (position >= size) {
            return -1;
        }
        int toRead = (int)Math.min(len, size - position);
        Arrays.fill(b, off, off + toRead, pattern);
        position += toRead;
        return toRead;
    }
}
