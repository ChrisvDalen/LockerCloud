package org.soprasteria.avans.lockercloud.socketapp;

public class ProtocolHandler {
    enum RequestType {
        GET_DOWNLOAD,
        POST_UPLOAD,
        POST_LIST,
        DELETE_FILE,
        UNKNOWN
    }

    public static RequestType parseStartLine(String line) {
        if (line == null) return RequestType.UNKNOWN;
        String[] parts = line.split(" ");
        if (parts.length < 2) return RequestType.UNKNOWN;
        String method = parts[0].toUpperCase();
        String path = parts[1];
        if ("GET".equals(method) && path.startsWith("/download")) {
            return RequestType.GET_DOWNLOAD;
        }
        if ("POST".equals(method) && path.equals("/upload")) {
            return RequestType.POST_UPLOAD;
        }
        if ("POST".equals(method) && path.equals("/listFiles")) {
            return RequestType.POST_LIST;
        }
        if ("DELETE".equals(method) && path.startsWith("/delete")) {
            return RequestType.DELETE_FILE;
        }
        return RequestType.UNKNOWN;
    }

    public static String extractQueryParam(String path, String name) {
        int idx = path.indexOf('?');
        if (idx == -1) return null;
        String[] params = path.substring(idx + 1).split("&");
        for (String p : params) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                return kv[1];
            }
        }
        return null;
    }
}
