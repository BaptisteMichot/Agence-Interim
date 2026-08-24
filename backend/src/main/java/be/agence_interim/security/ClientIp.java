package be.agence_interim.security;

import jakarta.servlet.http.HttpServletRequest;

/** Adresse de l'appelant, telle que vue par l'application. */
public final class ClientIp {

    private ClientIp() {
    }

    /**
     * Adresse d'origine de la requête.
     *
     * <p>Volontairement {@code getRemoteAddr()} et non une lecture directe de
     * {@code X-Forwarded-For} : cet en-tête est fourni par le client et se falsifie en
     * une ligne, ce qui suffirait à contourner tout quota par IP. Derrière un reverse
     * proxy, c'est à Spring de le prendre en compte — via
     * {@code server.forward-headers-strategy=framework} — parce que lui seul sait quels
     * mandataires sont de confiance.
     */
    public static String of(HttpServletRequest request) {
        String address = request.getRemoteAddr();
        return address == null ? "inconnue" : address;
    }
}
