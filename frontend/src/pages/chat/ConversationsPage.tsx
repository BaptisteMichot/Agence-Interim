import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getConversations, hideConversation } from '../../api/chat';
import { errorMessage } from '../../api/http';
import { useAuth } from '../../auth/AuthContext';
import { useChat } from '../../chat/ChatContext';
import ConfirmDialog from '../../components/ConfirmDialog';
import EmptyState from '../../components/EmptyState';
import PageHeader from '../../components/PageHeader';
import Pagination from '../../components/Pagination';
import { card, errorBox } from '../../components/ui';
import { usePagedResource } from '../../hooks/usePagedResource';
import type { Conversation } from '../../chat/types';
import { formatDateTime } from '../../profile/format';

/** Liste des conversations de l'utilisateur, la plus active en premier. */
export default function ConversationsPage() {
  const { user } = useAuth();
  const { subscribe } = useChat();
  const {
    items: conversations,
    pageData,
    loading,
    error,
    setError,
    reload,
    goTo,
  } = usePagedResource(getConversations, 'Impossible de charger les conversations.');
  const [removing, setRemoving] = useState<Conversation | null>(null);

  // Seul l'employeur peut retirer une conversation de sa liste ; le candidat la conserve.
  const canRemove = user?.role === 'EMPLOYER';

  // Un message reçu pendant qu'on est sur la liste la fait remonter à jour.
  useEffect(() => subscribe(() => reload()), [subscribe, reload]);

  const confirmRemove = async () => {
    if (!removing) {
      return;
    }
    const id = removing.id;
    setRemoving(null);
    setError(null);
    try {
      await hideConversation(id);
      reload();
    } catch (err) {
      setError(errorMessage(err, 'La conversation n’a pas pu être retirée.'));
    }
  };

  return (
    <section>
      <PageHeader title="Messages" subtitle="Vos échanges autour des candidatures." />

      {error && <p className={errorBox}>{error}</p>}

      <div className={`mt-6 ${card}`}>
        {loading && <p className="text-sm text-muted">Chargement…</p>}
        {!loading && conversations.length === 0 && (
          <EmptyState
            title="Aucune conversation"
            description={
              canRemove
                ? "Ouvrez une conversation depuis les candidatures reçues sur une de vos offres."
                : 'Un employeur peut vous contacter depuis une de vos candidatures.'
            }
          />
        )}

        <ul className="space-y-3">
          {conversations.map((conversation) => (
            <li key={conversation.id} className="flex items-stretch gap-2">
              <Link
                to={`/messages/${conversation.id}`}
                className="flex min-w-0 flex-1 flex-wrap items-center justify-between gap-4 rounded-lg border border-line p-4 transition hover:bg-slate-50"
              >
                <div className="min-w-0">
                  <p className="font-medium text-ink">
                    {conversation.otherPartyName}
                    {conversation.unreadCount > 0 && (
                      <span className="ml-2 rounded-full bg-brand-600 px-2 py-0.5 text-xs font-semibold text-white">
                        {conversation.unreadCount}
                      </span>
                    )}
                  </p>
                  <p className="text-sm text-muted">Offre : {conversation.offerTitle}</p>
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

              {canRemove && (
                <button
                  type="button"
                  onClick={() => setRemoving(conversation)}
                  aria-label={`Retirer la conversation avec ${conversation.otherPartyName}`}
                  title="Retirer de ma liste"
                  className="flex w-10 shrink-0 items-center justify-center rounded-lg border border-red-200 bg-red-50 text-red-600 transition hover:bg-red-100"
                >
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth={1.8}
                    strokeLinecap="round"
                    className="h-4 w-4"
                    aria-hidden="true"
                  >
                    <path d="M6 6l12 12M18 6 6 18" />
                  </svg>
                </button>
              )}
            </li>
          ))}
        </ul>

        <Pagination page={pageData} onChange={goTo} label="conversations" />
      </div>

      <ConfirmDialog
        open={removing !== null}
        title="Retirer la conversation"
        message={`La conversation avec ${removing?.otherPartyName ?? ''} disparaîtra de votre liste. Elle reste visible pour le candidat, et reviendra chez vous s'il vous écrit à nouveau.`}
        confirmLabel="Retirer"
        onConfirm={confirmRemove}
        onCancel={() => setRemoving(null)}
      />
    </section>
  );
}
