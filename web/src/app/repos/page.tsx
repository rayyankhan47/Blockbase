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

export default function ReposPage() {
	return (
		<div className="min-h-screen w-full px-8 py-14">
			<div className="mx-auto max-w-6xl">
				<header className="mb-10">
					<h1 className="text-5xl font-bold">Your Repositories</h1>
					<p className="mt-3 text-xl text-[var(--muted)]">
						Browse and open a repository to view its commits.
					</p>
				</header>

				<section className="grid gap-7 sm:grid-cols-2 lg:grid-cols-3">
					{mockRepos.map((repo) => (
						<a
							key={repo.id}
							href={`/repo/${repo.id}`}
							className="card block p-7 transition hover:opacity-95"
						>
							<h2 className="text-3xl font-semibold">{repo.name}</h2>
							<div className="mt-4 text-lg text-[var(--muted)]">
								<div>World: {repo.worldName}</div>
								<div>Commits: {repo.commits}</div>
							</div>
							<div className="mt-6 text-md text-[var(--muted)]">
								Updated: {new Date(repo.updatedAt).toLocaleString()}
							</div>
						</a>
					))}
				</section>
			</div>
		</div>
	);
}

