package com.example.cloudsync;

import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import com.example.cloudsync.protocol.Message;
import com.example.cloudsync.protocol.MessageParser;
import com.example.cloudsync.protocol.MessageSerializer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;

@ServerEndpoint("/sync")
public class SyncEndpoint {
    @OnOpen
    public void onOpen(Session session) {
        try {
            session.getBasicRemote().sendText("Connection established");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onMessage(Session session, String text) {
        try {
            Message req = MessageParser.parse(new ByteArrayInputStream(text.getBytes()));
            String status;
            if (req.startLine.startsWith("GET")) {
                status = "HTTP/1.1 200 OK";
            } else if (req.startLine.startsWith("POST")) {
                status = "HTTP/1.1 201 Created";
            } else {
                status = "HTTP/1.1 400 Bad Request";
            }
            Message resp = new Message(status, new HashMap<>(), new byte[0]);
            byte[] data = MessageSerializer.serialize(resp);
            session.getBasicRemote().sendText(new String(data));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
