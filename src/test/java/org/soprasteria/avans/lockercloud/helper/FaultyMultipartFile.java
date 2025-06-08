package org.soprasteria.avans.lockercloud.helper;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * MultipartFile wrapper that returns a FaultyInputStream for the first
 * configured attempts to simulate upload interruptions.
 */
public class FaultyMultipartFile implements MultipartFile {
    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final Supplier<InputStream> streamSupplier;
    private final long size;
    private final int failAttempts;
    private final long failAfter;
    private int attempts = 0;

    public FaultyMultipartFile(String name,
                               String originalFilename,
                               String contentType,
                               Supplier<InputStream> streamSupplier,
                               long size,
                               int failAttempts,
                               long failAfterBytes) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.streamSupplier = streamSupplier;
        this.size = size;
        this.failAttempts = failAttempts;
        this.failAfter = failAfterBytes;
    }

    public int getAttemptCount() {
        return attempts;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public byte[] getBytes() throws IOException {
        try (InputStream in = getInputStream()) {
            return in.readAllBytes();
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        attempts++;
        InputStream base = streamSupplier.get();
        if (attempts <= failAttempts) {
            return new FaultyInputStream(base, failAfter, true);
        }
        return base;
    }

    @Override
    public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
        try (InputStream in = getInputStream();
             java.io.OutputStream out = new java.io.FileOutputStream(dest)) {
            in.transferTo(out);
        }
    }
}
