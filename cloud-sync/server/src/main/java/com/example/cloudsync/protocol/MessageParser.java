package com.example.cloudsync.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MessageParser {
    private static final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static Message parse(InputStream in) throws IOException {
        byte[] data = in.readAllBytes();
        Map<String, Object> map = mapper.readValue(data, Map.class);
        String startLine = (String) map.get("startLine");
        Map<String, String> headers = (Map<String, String>) map.getOrDefault("headers", Collections.emptyMap());
        String bodyBase64 = (String) map.getOrDefault("bodyBase64", "");
        byte[] body = bodyBase64.isEmpty() ? new byte[0] : Base64.getDecoder().decode(bodyBase64);
        return new Message(startLine, headers, body);
    }
}
