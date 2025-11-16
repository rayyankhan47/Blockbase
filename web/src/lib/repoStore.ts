"use client";

export type Repo = {
  id: string;
  name: string;
  worldName: string;
  createdAt: string;
  updatedAt: string;
  commits: number;
  hasReadme: boolean;
  readme?: string; // markdown
  initialized?: boolean; // for future world linkage instructions
};

const KEY = 'bb-repos';

function load(): Repo[] {
  if (typeof window === 'undefined') return [];
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return seed();
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return seed();
    return parsed;
  } catch {
    return seed();
  }
}

function seed(): Repo[] {
  const initial: Repo[] = [
    {
      id: 'alpha',
      name: 'Alpha Base',
      worldName: 'Superflat Testing',
      createdAt: '2025-11-10T12:00:00Z',
      updatedAt: '2025-11-16T18:20:00Z',
      commits: 12,
      hasReadme: true,
      readme: '# Alpha Base\\n\\nA sandbox world for experimenting with builds.',
      initialized: false,
    },
    {
      id: 'redstone-alu',
      name: 'Redstone ALU',
      worldName: 'Redstone Lab',
      createdAt: '2025-11-12T09:30:00Z',
      updatedAt: '2025-11-16T17:55:00Z',
      commits: 7,
      hasReadme: true,
      readme: '# Redstone ALU\\n\\nWIP arithmetic and logic unit built in redstone.',
      initialized: true,
    },
    {
      id: 'sky-island',
      name: 'Sky Island',
      worldName: 'Creative Sandbox',
      createdAt: '2025-11-01T08:10:00Z',
      updatedAt: '2025-11-15T22:15:00Z',
      commits: 21,
      hasReadme: false,
      readme: '',
      initialized: false,
    },
  ];
  save(initial);
  return initial;
}

function save(repos: Repo[]) {
  if (typeof window === 'undefined') return;
  localStorage.setItem(KEY, JSON.stringify(repos));
}

export function getRepos(): Repo[] {
  return load();
}

export function getRepoById(id: string): Repo | undefined {
  return load().find((r) => r.id === id);
}

export function createRepo(name: string, withReadme: boolean): Repo {
  const repos = load();
  const idBase = name.trim().toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
  let id = idBase || `repo-${Math.random().toString(36).slice(2, 8)}`;
  let num = 1;
  while (repos.some((r) => r.id === id)) {
    id = `${idBase}-${num++}`;
  }
  const now = new Date().toISOString();
  const repo: Repo = {
    id,
    name: name || 'Untitled Repo',
    worldName: '',
    createdAt: now,
    updatedAt: now,
    commits: 0,
    hasReadme: withReadme,
    readme: withReadme ? '# README\\n\\nDescribe your project here.' : '',
    initialized: false,
  };
  repos.unshift(repo);
  save(repos);
  return repo;
}

export function updateRepo(id: string, updates: Partial<Repo>): Repo | undefined {
  const repos = load();
  const idx = repos.findIndex((r) => r.id === id);
  if (idx === -1) return;
  const next = { ...repos[idx], ...updates, updatedAt: new Date().toISOString() };
  repos[idx] = next;
  save(repos);
  return next;
}


