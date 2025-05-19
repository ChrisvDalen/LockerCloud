package org.soprasteria.avans.lockercloud;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(classes = LockerCloudApplication.class)
class LockerCloudApplicationTest {

    @Test
    void contextLoads() {
        // Application context should start without any exceptions
    }

    @Test
    void mainMethod_ShouldNotThrowException() {
        assertDoesNotThrow(() -> LockerCloudApplication.main(new String[]{}),
                "Running the main method should not throw any exception");
    }
}
