package be.agence_interim.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import be.agence_interim.chat.ChatHandshakeInterceptor;
import be.agence_interim.chat.ChatWebSocketHandler;

/** Expose le point d'entrée temps réel du chat sur {@code /ws/chat}. */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(
            ChatWebSocketHandler chatWebSocketHandler, ChatHandshakeInterceptor handshakeInterceptor) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(handshakeInterceptor)
                // En dev le frontend est servi par Vite (port 5173) : l'origine diffère du backend.
                .setAllowedOriginPatterns("*");
    }
}
