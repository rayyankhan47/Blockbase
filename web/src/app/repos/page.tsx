"use client";
import { useEffect, useState } from 'react';
import { createRepo, getRepos, Repo } from '@/lib/repoStore';

export default function ReposPage() {
  const [repos, setRepos] = useState<Repo[]>([]);
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [withReadme, setWithReadme] = useState(true);

  useEffect(() => {
    setRepos(getRepos());
  }, []);

  const onCreate = () => {
    const r = createRepo(name.trim(), withReadme);
    setRepos(getRepos());
    setOpen(false);
    setName('');
    setWithReadme(true);
    window.location.href = `/repo/${r.id}`;
  };

  return (
    <div className="min-h-screen w-full px-8 py-14">
      <div className="mx-auto max-w-6xl">
        <header className="mb-10 flex items-end justify-between">
          <div>
            <h1 className="text-5xl font-bold">Your Repositories</h1>
            <p className="mt-3 text-xl text-[var(--muted)]">
              Browse and open a repository to view its commits.
            </p>
          </div>
          <button
            onClick={() => setOpen(true)}
            className="accent-ring rounded-xl px-5 py-3 text-lg font-semibold"
            style={{ backgroundColor: 'var(--accent)', color: '#0a0a0a' }}
          >
            Create +
          </button>
        </header>

        <section className="grid gap-7 sm:grid-cols-2 lg:grid-cols-3">
          {repos.map((repo) => (
            <a
              key={repo.id}
              href={`/repo/${repo.id}`}
              className="card block p-7 transition hover:opacity-95"
            >
              <h2 className="text-3xl font-semibold">{repo.name}</h2>
              <div className="mt-4 text-lg text-[var(--muted)]">
                <div>World: {repo.worldName || '—'}</div>
                <div>Commits: {repo.commits}</div>
              </div>
              <div className="mt-6 text-md text-[var(--muted)]">
                Updated: {new Date(repo.updatedAt).toLocaleString()}
              </div>
            </a>
          ))}
        </section>
      </div>

      {open && (
        <div className="fixed inset-0 z-50 grid place-items-center" role="dialog" aria-modal>
          <div className="absolute inset-0 bg-black/60" onClick={() => setOpen(false)} />
          <div className="card relative z-10 w-[min(96vw,560px)] p-6">
            <h2 className="text-2xl font-semibold">Create Repository</h2>
            <label className="mt-5 block text-lg">
              Name
              <input
                className="mt-2 w-full rounded-md border bg-transparent px-3 py-2"
                style={{ borderColor: 'var(--accent-weak)' }}
                placeholder="My Awesome Build"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </label>
            <label className="mt-4 flex items-center gap-3 text-lg">
              <input
                type="checkbox"
                checked={withReadme}
                onChange={(e) => setWithReadme(e.target.checked)}
              />
              Initialize with README
            </label>
            <div className="mt-6 flex justify-end gap-3">
              <button
                onClick={() => setOpen(false)}
                className="rounded-md border px-4 py-2"
                style={{ borderColor: 'var(--accent-weak)' }}
              >
                Cancel
              </button>
              <button
                onClick={onCreate}
                className="rounded-md px-5 py-2 font-semibold"
                style={{ backgroundColor: 'var(--accent)', color: '#0a0a0a' }}
                disabled={!name.trim()}
              >
                Create
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

