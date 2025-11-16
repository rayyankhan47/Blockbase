type Commit = {
	id: string;
	author: string;
	message: string;
	timestamp: string;
};

const mockRepoMeta = (id: string) => ({
	id,
	name:
		id === "alpha"
			? "Alpha Base"
			: id === "redstone-alu"
			? "Redstone ALU"
			: "Sky Island",
	worldName:
		id === "alpha"
			? "Superflat Testing"
			: id === "redstone-alu"
			? "Redstone Lab"
			: "Creative Sandbox",
	createdAt: "2025-11-10T12:00:00Z",
	updatedAt: "2025-11-16T18:20:00Z",
});

const mockCommits: Commit[] = [
	{
		id: "b7f3a21",
		author: "rayyan",
		message: "Add piston clock and tidy wiring",
		timestamp: "2025-11-16T18:18:00Z",
	},
	{
		id: "a152c9e",
		author: "rayyan",
		message: "Lay foundation for control room",
		timestamp: "2025-11-16T17:40:00Z",
	},
	{
		id: "5f91d0a",
		author: "rayyan",
		message: "Initial repo setup",
		timestamp: "2025-11-16T16:05:00Z",
	},
];

export default async function RepoPage({
	params,
}: {
	params: Promise<{ id: string }>;
}) {
	const { id } = await params;
	const meta = mockRepoMeta(id);

	return (
		<div className="min-h-screen w-full px-8 py-14">
			<div className="mx-auto max-w-6xl">
				<header className="mb-8">
					<h1 className="text-5xl font-bold">{meta.name}</h1>
					<p className="mt-3 text-xl text-[var(--muted)]">
						World: {meta.worldName}
					</p>
					<p className="mt-1 text-md text-[var(--muted)]">
						Updated: {new Date(meta.updatedAt).toLocaleString()}
					</p>
				</header>

				<section className="space-y-5">
					<h2 className="text-3xl font-semibold">Commits</h2>
					<ul className="space-y-4">
						{mockCommits.map((c) => (
							<li key={c.id} className="card p-6">
								<a
									href={`/repo/${id}/commit/${c.id}`}
									className="block"
								>
									<div className="flex items-center justify-between">
										<div className="min-w-0">
											<div className="truncate text-xl font-medium">
												{c.message}
											</div>
											<div className="mt-1 text-md text-[var(--muted)]">
												<span className="font-mono">
													{c.id}
												</span>{" "}
												by {c.author} •{" "}
												{new Date(
													c.timestamp
												).toLocaleString()}
											</div>
										</div>
										<div className="text-md text-[var(--muted)]">
											View
										</div>
									</div>
								</a>
							</li>
						))}
					</ul>
				</section>
			</div>
		</div>
	);
}

