import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { useAuth } from '../auth/AuthContext';
import { getUnreadCount, sendMessageHttp } from '../api/chat';
import type { ChatMessage, ServerFrame } from './types';

type MessageListener = (message: ChatMessage) => void;

interface ChatContextValue {
  /** Vrai quand la WebSocket est ouverte (l'envoi bascule sinon sur HTTP). */
  connected: boolean;
  /** Total des messages non lus, pour le badge de la barre de navigation. */
  unreadCount: number;
  /** Recharge le compteur depuis le serveur (après avoir ouvert un fil, par exemple). */
  refreshUnread: () => void;
  /** Conversation actuellement affichée : ses messages entrants n'incrémentent pas le badge. */
  setActiveConversation: (conversationId: number | null) => void;
  /** S'abonne aux messages entrants ; retourne la fonction de désabonnement. */
  subscribe: (listener: MessageListener) => () => void;
  sendMessage: (conversationId: number, content: string) => Promise<void>;
}

const ChatContext = createContext<ChatContextValue | undefined>(undefined);

/**
 * URL de la WebSocket, sur la même origine que le frontend (proxifiée vers le backend
 * en dev). Le cookie de session accompagne la poignée de main comme n'importe quelle
 * requête : le jeton n'a plus à transiter par l'URL, où il finissait dans les journaux
 * d'accès et l'historique du navigateur.
 */
function socketUrl(): string {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return `${protocol}://${window.location.host}/ws/chat`;
}

export function ChatProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated, user } = useAuth();
  const [connected, setConnected] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  const socketRef = useRef<WebSocket | null>(null);
  const listenersRef = useRef(new Set<MessageListener>());
  const activeConversationRef = useRef<number | null>(null);
  const currentUserIdRef = useRef<number | null>(null);

  currentUserIdRef.current = user?.userId ?? null;

  const refreshUnread = useCallback(() => {
    if (!isAuthenticated) {
      return;
    }
    getUnreadCount()
      .then((result) => setUnreadCount(result.count))
      .catch(() => {
        // Compteur indisponible : on garde la valeur précédente plutôt que d'afficher une erreur.
      });
  }, [isAuthenticated]);

  useEffect(() => {
    if (!isAuthenticated) {
      setUnreadCount(0);
      return;
    }
    refreshUnread();

    let closedByEffect = false;
    let reconnectTimer: number | undefined;
    let attempts = 0;

    const connect = () => {
      const socket = new WebSocket(socketUrl());
      socketRef.current = socket;

      socket.onopen = () => {
        attempts = 0;
        setConnected(true);
      };

      socket.onmessage = (event) => {
        let frame: ServerFrame;
        try {
          frame = JSON.parse(event.data as string) as ServerFrame;
        } catch {
          return;
        }
        if (frame.type !== 'MESSAGE' || !frame.message) {
          return;
        }
        const message = frame.message;
        listenersRef.current.forEach((listener) => listener(message));
        const fromSomeoneElse = message.senderId !== currentUserIdRef.current;
        const threadNotOpen = message.conversationId !== activeConversationRef.current;
        if (fromSomeoneElse && threadNotOpen) {
          setUnreadCount((count) => count + 1);
        }
      };

      socket.onclose = () => {
        setConnected(false);
        socketRef.current = null;
        if (closedByEffect) {
          return;
        }
        // Reconnexion avec un délai croissant, plafonné à 10 s.
        attempts += 1;
        reconnectTimer = window.setTimeout(connect, Math.min(1000 * attempts, 10000));
      };
    };

    connect();

    return () => {
      closedByEffect = true;
      window.clearTimeout(reconnectTimer);
      socketRef.current?.close();
      socketRef.current = null;
      setConnected(false);
    };
  }, [isAuthenticated, refreshUnread]);

  const subscribe = useCallback((listener: MessageListener) => {
    listenersRef.current.add(listener);
    return () => {
      listenersRef.current.delete(listener);
    };
  }, []);

  const setActiveConversation = useCallback((conversationId: number | null) => {
    activeConversationRef.current = conversationId;
  }, []);

  const sendMessage = useCallback(async (conversationId: number, content: string) => {
    const socket = socketRef.current;
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ type: 'SEND', conversationId, content }));
      return;
    }
    // WebSocket coupée : on passe par l'API, le destinataire sera notifié par le serveur.
    await sendMessageHttp(conversationId, content);
  }, []);

  const value = useMemo<ChatContextValue>(
    () => ({
      connected,
      unreadCount,
      refreshUnread,
      setActiveConversation,
      subscribe,
      sendMessage,
    }),
    [connected, unreadCount, refreshUnread, setActiveConversation, subscribe, sendMessage],
  );

  return <ChatContext value={value}>{children}</ChatContext>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useChat(): ChatContextValue {
  const context = useContext(ChatContext);
  if (context === undefined) {
    throw new Error("useChat doit être utilisé à l'intérieur d'un ChatProvider.");
  }
  return context;
}
