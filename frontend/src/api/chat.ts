import { apiDelete, apiGet, apiPost } from './http';
import { apiGetPage, type Page } from './page';
import type { ChatMessage, Conversation, MessageHistory } from '../chat/types';

export function getConversations(page: number): Promise<Page<Conversation>> {
  return apiGetPage<Conversation>('/chat/conversations', page);
}

export function getConversation(id: number): Promise<Conversation> {
  return apiGet<Conversation>(`/chat/conversations/${id}`);
}

/**
 * Un lot d'historique du fil ; marque au passage les messages reçus comme lus.
 * Sans `before`, les derniers messages ; avec, ceux qui précèdent ce message.
 */
export function getMessages(conversationId: number, before?: number): Promise<MessageHistory> {
  const query = before === undefined ? '' : `?before=${before}`;
  return apiGet<MessageHistory>(`/chat/conversations/${conversationId}/messages${query}`);
}

/** Retire la conversation de sa propre liste ; l'autre partie la conserve. */
export function hideConversation(id: number): Promise<void> {
  return apiDelete(`/chat/conversations/${id}`);
}

export function getUnreadCount(): Promise<{ count: number }> {
  return apiGet<{ count: number }>('/chat/unread-count');
}

/** Envoi de secours quand la WebSocket n'est pas connectée. */
export function sendMessageHttp(conversationId: number, content: string): Promise<ChatMessage> {
  return apiPost<ChatMessage>(`/chat/conversations/${conversationId}/messages`, { content });
}

/** Démarre (ou retrouve) la conversation liée à une candidature — réservé à l'employeur. */
export function openConversationForApplication(applicationId: number): Promise<Conversation> {
  return apiPost<Conversation>(`/chat/conversations/application/${applicationId}`, {});
}
