/** Message d'une conversation. */
export interface ChatMessage {
  id: number;
  conversationId: number;
  senderId: number;
  senderFirstName: string;
  content: string;
  sentTime: string; // ISO
  read: boolean;
}

/** Conversation vue par un participant. */
export interface Conversation {
  id: number;
  applicationId: number;
  offerId: number;
  offerTitle: string;
  otherPartyId: number;
  otherPartyName: string;
  lastMessage: string | null;
  lastMessageTime: string | null;
  unreadCount: number;
}

/**
 * Un lot d'historique d'un fil, du plus ancien au plus récent.
 * Un fil se lit à l'envers du reste : on ouvre sur les derniers messages et on
 * remonte le temps, d'où un drapeau plutôt qu'un numéro de page.
 */
export interface MessageHistory {
  messages: ChatMessage[];
  hasMore: boolean;
}

/** Trame reçue du serveur sur la WebSocket. */
export interface ServerFrame {
  type: 'MESSAGE' | 'ERROR';
  message: ChatMessage | null;
  error: string | null;
}
