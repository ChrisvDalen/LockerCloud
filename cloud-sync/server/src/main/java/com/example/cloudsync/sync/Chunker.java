package com.example.cloudsync.sync;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Chunker {
    public static List<Chunk> split(File f, int chunkSize) throws IOException {
        byte[] all = Files.readAllBytes(f.toPath());
        List<Chunk> chunks = new ArrayList<>();
        int index = 0;
        for (int pos = 0; pos < all.length; pos += chunkSize) {
            int end = Math.min(all.length, pos + chunkSize);
            byte[] part = new byte[end - pos];
            System.arraycopy(all, pos, part, 0, part.length);
            chunks.add(new Chunk(index++, part));
        }
        return chunks;
    }

    public static File reassemble(List<Chunk> chunks, File out) throws IOException {
        try (var os = Files.newOutputStream(out.toPath())) {
            for (Chunk c : chunks) {
                os.write(c.data);
            }
        }
        return out;
    }
}
