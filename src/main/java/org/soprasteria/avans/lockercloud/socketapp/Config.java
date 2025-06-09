package org.soprasteria.avans.lockercloud.socketapp;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    private final int port;
    private final Path storagePath;
    private final String authToken;
    private final String aesKey;

    public Config(String file) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
        }
        this.port = Integer.parseInt(props.getProperty("port", "9000"));
        this.storagePath = Path.of(props.getProperty("storagePath", "filestorage"));
        this.authToken = props.getProperty("authToken", "");
        this.aesKey = props.getProperty("aesKey", "");
    }

    public int getPort() { return port; }
    public Path getStoragePath() { return storagePath; }
    public String getAuthToken() { return authToken; }
    public String getAesKey() { return aesKey; }
}
