package org.soprasteria.avans.lockercloud;

import org.springframework.boot.SpringApplication;

public class TestLockerCloudApplication {

    public static void main(String[] args) {
        SpringApplication.from(LockerCloudApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
