package be.agence_interim.chat;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import be.agence_interim.security.CurrentUser;

/**
 * Authentifie la poignée de main WebSocket. Le navigateur ne permettant pas d'ajouter
 * l'en-tête Authorization sur une WebSocket, le JWT est passé en paramètre de requête
 * ({@code ?token=...}) puis vérifié ici ; l'identifiant utilisateur est placé dans les
 * attributs de session.
 */
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
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (token == null || token.isBlank()) {
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
            Exception exception) {
        // Rien à faire après la poignée de main.
    }
}
