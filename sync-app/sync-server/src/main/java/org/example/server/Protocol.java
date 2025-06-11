package org.example.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Protocol {
    public static class Request {
        public String method;
        public String path;
        public Map<String, String> headers = new HashMap<>();
        public byte[] body;
    }

    public static Request parseRequest(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String start = reader.readLine();
        if (start == null) {
            throw new IOException("Empty request");
        }
        String[] parts = start.split(" ");
        Request r = new Request();
        r.method = parts[0];
        r.path = parts[1];
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int idx = line.indexOf(":");
            if (idx > 0) {
                r.headers.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
            }
        }
        int len = Integer.parseInt(r.headers.getOrDefault("Content-Length", "0"));
        if (len > 0) {
            byte[] body = new byte[len];
            int read = 0;
            while (read < len) {
                int rlen = in.read(body, read, len - read);
                if (rlen == -1) break;
                read += rlen;
            }
            r.body = body;
        } else {
            r.body = new byte[0];
        }
        return r;
    }
}
