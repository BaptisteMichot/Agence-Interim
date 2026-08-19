import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getConversation, getMessages } from '../../api/chat';
import { errorMessage } from '../../api/http';
import { useAuth } from '../../auth/AuthContext';
import { useChat } from '../../chat/ChatContext';
import { btnPrimary, errorBox, inputClass, linkBack } from '../../components/ui';
import type { ChatMessage, Conversation } from '../../chat/types';
import { formatDateTime } from '../../profile/format';

/** Fil de discussion d'une conversation, alimenté en temps réel par la WebSocket. */
export default function ConversationThreadPage() {
  const { id } = useParams();
  const conversationId = Number(id);
  const { user } = useAuth();
  const { connected, subscribe, setActiveConversation, refreshUnread, sendMessage } = useChat();

  const [conversation, setConversation] = useState<Conversation | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [draft, setDraft] = useState('');
  const [sending, setSending] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const bottomRef = useRef<HTMLDivElement | null>(null);

  /** Recharge l'historique : le serveur marque au passage les messages reçus comme lus. */
  const reloadMessages = useCallback(async () => {
    const history = await getMessages(conversationId);
    setMessages(history);
    refreshUnread();
  }, [conversationId, refreshUnread]);

  useEffect(() => {
    setActiveConversation(conversationId);
    setLoading(true);
    setError(null);
    Promise.all([getConversation(conversationId), reloadMessages()])
      .then(([detail]) => setConversation(detail))
      .catch((err) =>
        setError(errorMessage(err, 'Impossible de charger la conversation.')),
      )
      .finally(() => setLoading(false));

    return () => setActiveConversation(null);
  }, [conversationId, reloadMessages, setActiveConversation]);

  // Message entrant : on recharge le fil, ce qui l'affiche et le marque comme lu.
  useEffect(
    () =>
      subscribe((message) => {
        if (message.conversationId === conversationId) {
          reloadMessages().catch(() => setError('Impossible de rafraîchir la conversation.'));
        }
      }),
    [subscribe, conversationId, reloadMessages],
  );

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: 'end' });
  }, [messages]);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    const content = draft.trim();
    if (!content) {
      return;
    }
    setError(null);
    setSending(true);
    try {
      await sendMessage(conversationId, content);
      setDraft('');
      // On rafraîchit toujours : l'affichage de son propre message ne doit pas dépendre
      // de l'écho temps réel (WebSocket fermée, en reconnexion, ou écho perdu). Si l'écho
      // arrive quand même, il déclenche le même rechargement — le résultat est identique.
      await reloadMessages();
    } catch (err) {
      setError(errorMessage(err, "Le message n'a pas pu être envoyé."));
    } finally {
      setSending(false);
    }
  };

  if (loading) {
    return <p className="text-slate-500">Chargement…</p>;
  }

  if (!conversation) {
    return <p className={errorBox}>{error ?? 'Conversation introuvable.'}</p>;
  }

  return (
    <section className="space-y-4">
      <div>
        <Link to="/messages" className={linkBack}>
          ← Retour aux messages
        </Link>
        <h1 className="mt-2 text-2xl font-semibold text-slate-900">{conversation.otherPartyName}</h1>
        <p className="text-slate-600">Offre : {conversation.offerTitle}</p>
        {!connected && (
          <p className="mt-1 text-xs text-amber-600">
            Connexion temps réel interrompue — les messages partent quand même, la reconnexion est
            automatique.
          </p>
        )}
      </div>

      {error && <p className={errorBox}>{error}</p>}

      <div className="h-[26rem] overflow-y-auto rounded-xl border border-slate-200 bg-white p-4">
        {messages.length === 0 && (
          <p className="text-sm text-slate-500">
            Aucun message. Écrivez le premier ci-dessous.
          </p>
        )}
        <ul className="space-y-3">
          {messages.map((message) => {
            const mine = message.senderId === user?.userId;
            return (
              <li key={message.id} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                <div
                  className={`max-w-[75%] rounded-2xl px-4 py-2 ${
                    mine ? 'bg-brand-600 text-white' : 'bg-slate-100 text-slate-800'
                  }`}
                >
                  <p className="whitespace-pre-line break-words">{message.content}</p>
                  <p className={`mt-1 text-xs ${mine ? 'text-brand-200' : 'text-slate-400'}`}>
                    {formatDateTime(message.sentTime)}
                  </p>
                </div>
              </li>
            );
          })}
        </ul>
        <div ref={bottomRef} />
      </div>

      <form onSubmit={submit} className="flex gap-2">
        <input
          type="text"
          className={inputClass}
          placeholder="Votre message…"
          value={draft}
          maxLength={2000}
          onChange={(e) => setDraft(e.target.value)}
        />
        <button type="submit" className={btnPrimary} disabled={sending || draft.trim() === ''}>
          Envoyer
        </button>
      </form>
    </section>
  );
}
