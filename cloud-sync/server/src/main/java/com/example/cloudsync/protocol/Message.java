package com.example.cloudsync.protocol;

import java.util.Map;

public class Message {
    public String startLine;
    public Map<String, String> headers;
    public byte[] body;

    public Message(String startLine, Map<String, String> headers, byte[] body) {
        this.startLine = startLine;
        this.headers = headers;
        this.body = body;
    }
}
