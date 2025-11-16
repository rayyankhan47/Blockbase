type Repo = {
  id: string;
  name: string;
  worldName: string;
  createdAt: string;
  updatedAt: string;
  commits: number;
};

const mockRepos: Repo[] = [
  {
    id: "alpha",
    name: "Alpha Base",
    worldName: "Superflat Testing",
    createdAt: "2025-11-10T12:00:00Z",
    updatedAt: "2025-11-16T18:20:00Z",
    commits: 12,
  },
  {
    id: "redstone-alu",
    name: "Redstone ALU",
    worldName: "Redstone Lab",
    createdAt: "2025-11-12T09:30:00Z",
    updatedAt: "2025-11-16T17:55:00Z",
    commits: 7,
  },
  {
    id: "sky-island",
    name: "Sky Island",
    worldName: "Creative Sandbox",
    createdAt: "2025-11-01T08:10:00Z",
    updatedAt: "2025-11-15T22:15:00Z",
    commits: 21,
  },
];

export default function Home() {
  return (
    <div className="min-h-screen w-full px-6 py-10">
      <div className="mx-auto max-w-5xl">
        <header className="mb-8">
          <h1 className="heading-mc text-3xl md:text-4xl">Blockbase</h1>
          <p className="mt-2 text-sm text-[var(--muted)]">
            Your Minecraft build repos
          </p>
        </header>

        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {mockRepos.map((repo) => (
            <a
              key={repo.id}
              href={`/repo/${repo.id}`}
              className="card block p-4 transition hover:opacity-95"
            >
              <h2 className="text-xl font-semibold">{repo.name}</h2>
              <div className="mt-2 text-sm text-[var(--muted)]">
                <div>World: {repo.worldName}</div>
                <div>Commits: {repo.commits}</div>
              </div>
              <div className="mt-4 text-xs text-[var(--muted)]">
                Updated: {new Date(repo.updatedAt).toLocaleString()}
              </div>
            </a>
          ))}
        </section>
      </div>
    </div>
  );
}
