'use client';
import { use, useEffect, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { getApiBase } from '@/lib/config';

type Commit = {
  id: string;
  author: string;
  message: string;
  timestamp: string;
};

type Repo = {
  id: string;
  name: string;
  default_branch: string;
  created_at: string;
};

export default function RepoPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [repo, setRepo] = useState<Repo | undefined>(undefined);
  const [tab, setTab] = useState<'readme' | 'commits'>('readme');
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState('');
  const [commits, setCommits] = useState<Commit[]>([]);
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    const load = async () => {
      try {
        const [repoRes, readmeRes, commitsRes] = await Promise.all([
          fetch(`${getApiBase()}/repos/${id}`, { cache: 'no-store' }),
          fetch(`${getApiBase()}/repos/${id}/readme`, { cache: 'no-store' }),
          fetch(`${getApiBase()}/repos/${id}/commits`, { cache: 'no-store' }),
        ]);
        if (repoRes.ok) {
          const r = await repoRes.json();
          setRepo(r);
        }
        if (readmeRes.ok) {
          const rd = await readmeRes.json();
          if (typeof rd?.content === 'string') setDraft(rd.content);
        }
        if (commitsRes.ok) {
          const cs = await commitsRes.json();
          if (Array.isArray(cs)) setCommits(cs);
        }
      } catch {
        // no-op
      }
      // We don't track "initialized" server-side yet; keep false to show connect block for now.
      setInitialized(false);
    };
    load();
  }, [id]);

  if (!repo) {
    return (
      <div className="min-h-screen w-full px-8 py-14">
        <div className="mx-auto max-w-6xl">
          <p>Repository not found.</p>
        </div>
      </div>
    );
  }

  const saveReadme = async () => {
    try {
      await fetch(`${getApiBase()}/repos/${id}/readme`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: draft }),
      });
      setEditing(false);
    } catch {
      // no-op
    }
  };

  return (
    <div className="min-h-screen w-full px-8 py-14">
      <div className="mx-auto max-w-6xl">
        <header className="mb-8">
          <h1 className="text-5xl font-bold">{repo.name}</h1>
          <p className="mt-3 text-xl text-[var(--muted)]">Default branch: {repo.default_branch}</p>
          <p className="mt-1 text-md text-[var(--muted)]">Created: {new Date(repo.created_at).toLocaleString()}</p>
        </header>

        <div className="mb-6 flex items-center gap-3">
          <button
            className={`rounded-md px-4 py-2 ${tab === 'readme' ? '' : 'border'}`}
            style={tab === 'readme' ? { backgroundColor: 'var(--accent)', color: '#0a0a0a' } : { borderColor: 'var(--accent-weak)' }}
            onClick={() => setTab('readme')}
          >
            ReadMe
          </button>
          <button
            className={`rounded-md px-4 py-2 ${tab === 'commits' ? '' : 'border'}`}
            style={tab === 'commits' ? { backgroundColor: 'var(--accent)', color: '#0a0a0a' } : { borderColor: 'var(--accent-weak)' }}
            onClick={() => setTab('commits')}
          >
            Commits
          </button>
        </div>

        {tab === 'readme' ? (
          <section className="space-y-4">
            {!initialized && (
              <ConnectBlock />
            )}
            <div className="card p-5">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="text-3xl font-semibold">README.md</h2>
                {!editing ? (
                  <button
                    className="rounded-md border px-4 py-2"
                    style={{ borderColor: 'var(--accent-weak)' }}
                    onClick={() => {
                      setDraft(repo.readme || '');
                      setEditing(true);
                    }}
                  >
                    Edit
                  </button>
                ) : (
                  <div className="flex gap-2">
                    <button
                      className="rounded-md border px-4 py-2"
                      style={{ borderColor: 'var(--accent-weak)' }}
                      onClick={() => setEditing(false)}
                    >
                      Cancel
                    </button>
                    <button
                      className="rounded-md px-4 py-2 font-semibold"
                      style={{ backgroundColor: 'var(--accent)', color: '#0a0a0a' }}
                      onClick={saveReadme}
                    >
                      Save
                    </button>
                  </div>
                )}
              </div>
              {!editing ? (
                <article className="max-w-none">
                  <ReactMarkdown
                    remarkPlugins={[remarkGfm]}
                    components={{
                      h1: ({ node, ...props }) => (
                        <h1 className="mt-6 mb-3 text-4xl font-extrabold" {...props} />
                      ),
                      h2: ({ node, ...props }) => (
                        <h2 className="mt-5 mb-3 text-3xl font-bold" {...props} />
                      ),
                      h3: ({ node, ...props }) => (
                        <h3 className="mt-4 mb-2 text-2xl font-semibold" {...props} />
                      ),
                      p: ({ node, ...props }) => <p className="my-3 leading-8" {...props} />,
                      ul: ({ node, ...props }) => <ul className="my-3 ml-6 list-disc" {...props} />,
                      ol: ({ node, ...props }) => <ol className="my-3 ml-6 list-decimal" {...props} />,
                      code: ({ inline, children, ...props }: any) =>
                        inline
                          ? (
                            <code className="rounded px-1.5 py-0.5" {...props}>{children}</code>
                          )
                          : (
                            <pre className="my-3 overflow-auto rounded p-4">
                              <code {...props}>{children}</code>
                            </pre>
                          ),
                      a: ({ node, ...props }) => (
                        <a className="text-[var(--accent)] underline" {...props} />
                      ),
                      blockquote: ({ node, ...props }) => (
                        <blockquote className="my-3 border-l-4 pl-4 text-[var(--muted)]" {...props} />
                      ),
                      hr: ({ node, ...props }) => <hr className="my-6 border-[var(--accent-weak)]" {...props} />,
                    }}
                  >
                    {draft || '_No README yet._'}
                  </ReactMarkdown>
                </article>
              ) : (
                <textarea
                  className="mt-2 h-[360px] w-full resize-vertical rounded-md border bg-transparent p-3"
                  style={{ borderColor: 'var(--accent-weak)' }}
                  value={draft}
                  onChange={(e) => setDraft(e.target.value)}
                />
              )}
            </div>
          </section>
        ) : (
          <section className="space-y-5">
            <h2 className="text-3xl font-semibold">Commits</h2>
            <ul className="space-y-4">
              {commits.map((c) => (
                <li key={c.id} className="card p-6">
                  <a href={`/repo/${repo.id}/commit/${c.id}`} className="block">
                    <div className="flex items-center justify-between">
                      <div className="min-w-0">
                        <div className="truncate text-xl font-medium">{c.message}</div>
                        <div className="mt-1 text-md text-[var(--muted)]">
                          <span className="font-mono">{c.id}</span> by {c.author} • {new Date(c.timestamp).toLocaleString()}
                        </div>
                      </div>
                      <div className="text-md text-[var(--muted)]">View</div>
                    </div>
                  </a>
                </li>
              ))}
            </ul>
          </section>
        )}
      </div>
    </div>
  );
}

function ConnectBlock() {
  const apiBase = getApiBase();
  const repo = typeof window !== 'undefined' ? window.location.pathname.split('/').filter(Boolean)[1] : '';
  const remote = `${apiBase}/repos/${repo}`;
  const [copied, setCopied] = useState(false);
  const script = `/bb init
/bb add .
/bb commit "Initial commmit"
/bb remote add origin ${remote}
/bb push`;

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(remote);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 3000);
    } catch {
      // fallback: no-op
    }
  };

  return (
    <div className="card p-5">
      <h3 className="text-2xl font-semibold">Connect this repo</h3>
      <p className="mt-2 text-[var(--muted)]">
        Run these commands inside your Minecraft world to link it to this remote.
      </p>
      <div className="mt-3 rounded-md bg-black/30 p-4 text-sm theme-light:bg-black/10">
        <pre>{script}</pre>
      </div>
      <div className="mt-3">
        <button
          onClick={copy}
          className="accent-ring rounded-md px-4 py-2 font-semibold"
          style={{ backgroundColor: 'var(--accent)', color: '#0a0a0a' }}
          disabled={copied}
        >
          {copied ? 'Copied!' : 'Copy remote link'}
        </button>
      </div>
    </div>
  );
}

