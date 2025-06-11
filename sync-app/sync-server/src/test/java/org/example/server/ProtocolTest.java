package org.example.server;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ProtocolTest {
    @Test
    void parseSimple() throws Exception {
        String data = "GET /download?file=test.txt\nContent-Length: 0\n\n";
        Protocol.Request r = Protocol.parseRequest(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
        assertEquals("GET", r.method);
        assertEquals("/download?file=test.txt", r.path);
        assertEquals("0", r.headers.get("Content-Length"));
    }
}
