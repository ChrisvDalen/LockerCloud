package com.example.cloudsync.sync;

public class Chunk {
    public final int index;
    public final byte[] data;

    public Chunk(int index, byte[] data) {
        this.index = index;
        this.data = data;
    }
}
