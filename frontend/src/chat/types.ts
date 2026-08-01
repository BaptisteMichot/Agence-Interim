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

/** Trame reçue du serveur sur la WebSocket. */
export interface ServerFrame {
  type: 'MESSAGE' | 'ERROR';
  message: ChatMessage | null;
  error: string | null;
}
