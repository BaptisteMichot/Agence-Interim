package be.agence_interim.chat;

import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import be.agence_interim.security.AuthCookie;
import be.agence_interim.security.CurrentUser;

/**
 * Authentifie la poignée de main WebSocket à partir du cookie de session.
 *
 * <p>Le navigateur ne permet pas d'ajouter l'en-tête {@code Authorization} sur une
 * WebSocket, mais il joint ses cookies à la poignée de main comme à n'importe quelle
 * requête : le jeton n'a donc plus à transiter par l'URL, où il finissait dans les
 * journaux d'accès et l'historique. L'identifiant utilisateur est placé dans les
 * attributs de session.
 *
 * <p>La contrepartie du cookie — une poignée de main peut être déclenchée depuis un
 * autre site — est couverte par la liste d'origines autorisées de
 * {@link be.agence_interim.config.WebSocketConfig}.
 */
@NullMarked
@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    /** Clé de l'identifiant utilisateur dans les attributs de la session WebSocket. */
    public static final String USER_ID_ATTRIBUTE = "userId";

    private final JwtDecoder jwtDecoder;

    public ChatHandshakeInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler handler,
            Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return reject(response);
        }
        String token = AuthCookie.read(servletRequest.getServletRequest()).orElse(null);
        if (token == null) {
            return reject(response);
        }
        try {
            Jwt jwt = jwtDecoder.decode(token);
            attributes.put(USER_ID_ATTRIBUTE, CurrentUser.id(jwt));
            return true;
        } catch (JwtException | IllegalStateException e) {
            return reject(response);
        }
    }

    /** Refuse la poignée de main avec un 401 explicite (sans cela, le statut resterait 200). */
    private boolean reject(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler handler,
            @Nullable Exception exception) {
        // Rien à faire après la poignée de main.
    }
}
