import { apiDelete, apiGet, apiPost } from './http';
import type { ChatMessage, Conversation } from '../chat/types';

export function getConversations(): Promise<Conversation[]> {
  return apiGet<Conversation[]>('/chat/conversations');
}

export function getConversation(id: number): Promise<Conversation> {
  return apiGet<Conversation>(`/chat/conversations/${id}`);
}

/** Historique du fil ; marque au passage les messages reçus comme lus. */
export function getMessages(conversationId: number): Promise<ChatMessage[]> {
  return apiGet<ChatMessage[]>(`/chat/conversations/${conversationId}/messages`);
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
