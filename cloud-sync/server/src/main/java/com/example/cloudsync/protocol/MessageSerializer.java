package com.example.cloudsync.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class MessageSerializer {
    public static byte[] serialize(Message msg) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write((msg.startLine + "\n").getBytes());
        for (Map.Entry<String, String> e : msg.headers.entrySet()) {
            out.write((e.getKey() + ": " + e.getValue() + "\n").getBytes());
        }
        out.write("\n".getBytes());
        if (msg.body != null) out.write(msg.body);
        return out.toByteArray();
    }
}
