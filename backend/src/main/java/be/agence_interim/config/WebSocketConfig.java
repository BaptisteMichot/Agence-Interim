package be.agence_interim.config;

import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import be.agence_interim.chat.ChatHandshakeInterceptor;
import be.agence_interim.chat.ChatWebSocketHandler;

/** Expose le point d'entrée temps réel du chat sur {@code /ws/chat}. */
@NullMarked
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatHandshakeInterceptor handshakeInterceptor;
    private final String frontendUrl;

    public WebSocketConfig(
            ChatWebSocketHandler chatWebSocketHandler,
            ChatHandshakeInterceptor handshakeInterceptor,
            @Value("${app.frontend.url}") String frontendUrl) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(handshakeInterceptor)
                // Le cookie de session accompagne la poignée de main : sans restriction
                // d'origine, n'importe quel site pourrait ouvrir une WebSocket au nom de
                // l'utilisateur connecté. En dev, le frontend est servi par Vite sur un
                // autre port, d'où la lecture de l'origine attendue en configuration.
                .setAllowedOrigins(frontendUrl);
    }
}
