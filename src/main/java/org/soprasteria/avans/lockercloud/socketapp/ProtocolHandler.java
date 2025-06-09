package org.soprasteria.avans.lockercloud.socketapp;

public class ProtocolHandler {
    enum CommandType { AUTH, UPLOAD, DOWNLOAD, LIST, DELETE, UNKNOWN }

    public static CommandType parseCommand(String line) {
        if (line == null || line.isBlank()) return CommandType.UNKNOWN;
        String cmd = line.split(" ")[0].toUpperCase();
        return switch (cmd) {
            case "AUTH" -> CommandType.AUTH;
            case "UPLOAD" -> CommandType.UPLOAD;
            case "DOWNLOAD" -> CommandType.DOWNLOAD;
            case "LIST" -> CommandType.LIST;
            case "DELETE" -> CommandType.DELETE;
            default -> CommandType.UNKNOWN;
        };
    }
}
