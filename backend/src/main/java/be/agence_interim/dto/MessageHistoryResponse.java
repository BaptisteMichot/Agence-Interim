package be.agence_interim.dto;

import java.util.List;

/**
 * Un morceau d'historique d'une conversation, du plus ancien au plus récent.
 *
 * <p>Un fil de discussion ne se lit pas comme une liste paginée : on ouvre sur les
 * derniers messages et on remonte le temps. D'où un simple drapeau plutôt qu'un
 * numéro de page.
 *
 * @param messages les messages du lot, dans l'ordre chronologique
 * @param hasMore  vrai s'il reste des messages plus anciens à charger
 */
public record MessageHistoryResponse(List<ChatMessageResponse> messages, boolean hasMore) {
}
