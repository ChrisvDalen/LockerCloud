package org.soprasteria.avans.lockercloud.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebSocketConfigTest {

    private final WebSocketConfig config = new WebSocketConfig();

    @Test
    void configureMessageBroker_steltPrefixesIn() {
        MessageBrokerRegistry broker = mock(MessageBrokerRegistry.class);

        config.configureMessageBroker(broker);

        verify(broker).enableSimpleBroker("/topic");
        verify(broker).setApplicationDestinationPrefixes("/app");
    }

    @Test
    void registerStompEndpoints_registreertWsEndpointEnSockJs() {
        // Mocks voor registry en registrations
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration stompRegistration = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration sockJsRegistration = mock(SockJsServiceRegistration.class);

        // Stub addEndpoint -> stompRegistration
        when(registry.addEndpoint("/ws")).thenReturn(stompRegistration);

        // Stub chaining-methodes:
        // setAllowedOrigins(...) moet stomRegistration teruggeven
        doReturn(stompRegistration)
                .when(stompRegistration)
                .setAllowedOrigins(any(String[].class));
        // withSockJS() moet sockJsRegistration teruggeven
        doReturn(sockJsRegistration)
                .when(stompRegistration)
                .withSockJS();

        // Act
        config.registerStompEndpoints(registry);

        // Verify dat we de juiste calls deden
        verify(registry).addEndpoint("/ws");
        verify(stompRegistration).setAllowedOrigins("*");
        verify(stompRegistration).withSockJS();
    }
}