package org.soprasteria.avans.lockercloud.helper;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream wrapper that throws an IOException after a configured
 * number of bytes have been read.  Used to simulate network drops.
 */
public class FaultyInputStream extends FilterInputStream {
    private long remaining;
    private final boolean closeAbruptly;

    public FaultyInputStream(InputStream in, long failAfterBytes, boolean closeAbruptly) {
        super(in);
        this.remaining = failAfterBytes;
        this.closeAbruptly = closeAbruptly;
    }

    private void checkFail() throws IOException {
        if (remaining <= 0) {
            if (closeAbruptly) {
                try { super.close(); } catch (IOException ignore) {}
            }
            throw new IOException("Simulated connection drop");
        }
    }

    @Override
    public int read() throws IOException {
        checkFail();
        int r = super.read();
        if (r != -1) {
            remaining--;
        }
        return r;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkFail();
        int toRead = (int)Math.min(len, remaining);
        int r = super.read(b, off, toRead);
        if (r != -1) {
            remaining -= r;
        }
        return r;
    }
}
