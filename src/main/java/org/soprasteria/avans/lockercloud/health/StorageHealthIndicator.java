package org.soprasteria.avans.lockercloud.health;

import org.springframework.boot.actuate.health.*;
import org.springframework.stereotype.Component;
import java.nio.file.*;

@Component
public class StorageHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        Path storage = Paths.get("filestorage");
        if (Files.isDirectory(storage) && Files.isWritable(storage)) {
            return Health.up().withDetail("storage", "writable").build();
        }
        return Health.down().withDetail("storage", "not-writable").build();
    }
}
