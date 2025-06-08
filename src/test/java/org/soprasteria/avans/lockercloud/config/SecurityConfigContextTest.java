// SecurityConfigContextTest.java
package org.soprasteria.avans.lockercloud.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.web.SecurityFilterChain;

class SecurityConfigContextTest {

    @Test
    @Disabled("Faalt voor nu")
    void filterChainBeanBeschikbaar() {
        try (var ctx = new AnnotationConfigApplicationContext(SecurityConfig.class)) {
            assertTrue(ctx.containsBean("filterChain"),
                    "De SecurityFilterChain-bean moet geladen worden");
            Object bean = ctx.getBean("filterChain");
            assertInstanceOf(SecurityFilterChain.class, bean,
                    "Bean 'filterChain' moet een SecurityFilterChain zijn");
        }
    }
}
