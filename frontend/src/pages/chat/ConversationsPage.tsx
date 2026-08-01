import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getConversations } from '../../api/chat';
import { useChat } from '../../chat/ChatContext';
import { errorBox } from '../../components/ui';
import type { Conversation } from '../../chat/types';
import { formatDateTime } from '../../profile/format';

/** Liste des conversations de l'utilisateur, la plus active en premier. */
export default function ConversationsPage() {
  const { subscribe, setActiveConversation } = useChat();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setError(null);
    try {
      setConversations(await getConversations());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Impossible de charger les conversations.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    setActiveConversation(null);
    reload();
  }, [reload, setActiveConversation]);

  // Un message reçu pendant qu'on est sur la liste la fait remonter à jour.
  useEffect(() => subscribe(() => reload()), [subscribe, reload]);

  return (
    <section>
      <h1 className="text-2xl font-semibold text-slate-900">Messages</h1>
      <p className="mt-1 text-slate-600">Vos échanges autour des candidatures.</p>

      {error && <p className={`mt-4 ${errorBox}`}>{error}</p>}

      <div className="mt-6 rounded-xl border border-slate-200 bg-white p-6">
        {loading && <p className="text-sm text-slate-500">Chargement…</p>}
        {!loading && conversations.length === 0 && (
          <p className="text-sm text-slate-500">
            Aucune conversation. Un employeur peut vous contacter depuis une de vos candidatures.
          </p>
        )}

        <ul className="space-y-3">
          {conversations.map((conversation) => (
            <li key={conversation.id}>
              <Link
                to={`/messages/${conversation.id}`}
                className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-slate-200 p-4 transition hover:bg-slate-50"
              >
                <div className="min-w-0">
                  <p className="font-medium text-slate-900">
                    {conversation.otherPartyName}
                    {conversation.unreadCount > 0 && (
                      <span className="ml-2 rounded-full bg-indigo-600 px-2 py-0.5 text-xs font-semibold text-white">
                        {conversation.unreadCount}
                      </span>
                    )}
                  </p>
                  <p className="text-sm text-slate-500">Offre : {conversation.offerTitle}</p>
                  <p className="truncate text-sm text-slate-400">
                    {conversation.lastMessage ?? 'Aucun message pour le moment.'}
                  </p>
                </div>
                {conversation.lastMessageTime && (
                  <span className="shrink-0 text-xs text-slate-400">
                    {formatDateTime(conversation.lastMessageTime)}
                  </span>
                )}
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
