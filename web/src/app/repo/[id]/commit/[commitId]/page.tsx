import Image from "next/image";
import { getBlockIconPath } from "@/lib/blockIcons";
type BlockDelta = {
	id: string; // e.g., minecraft:redstone_wire
	name: string; // e.g., Redstone Dust
	count: number;
};

type CommitDetail = {
	id: string;
	message: string;
	author: string;
	timestamp: string;
	added: BlockDelta[];
	removed: BlockDelta[];
};

const mockCommitDetail = (commitId: string): CommitDetail => ({
	id: commitId,
	message: "Add piston clock and tidy wiring",
	author: "rayyan",
	timestamp: "2025-11-16T18:18:00Z",
	added: [
		{ id: "minecraft:redstone_wire", name: "Redstone Dust", count: 18 },
		{ id: "minecraft:lever", name: "Lever", count: 2 },
		{ id: "minecraft:observer", name: "Observer", count: 1 },
	],
	removed: [
		{ id: "minecraft:cobblestone", name: "Cobblestone", count: 12 },
		{ id: "minecraft:torch", name: "Torch", count: 3 },
	],
});

function DeltaList({
	title,
	items,
}: {
	title: string;
	items: BlockDelta[];
}) {
	return (
		<section className="mt-6">
			<h3 className="text-base font-semibold">{title}</h3>
			{items.length === 0 ? (
				<p className="mt-2 text-sm text-[var(--muted)]">None</p>
			) : (
				<ul className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
					{items.map((b) => (
						<li key={b.id} className="card flex items-center gap-3 p-3">
							<div className="h-10 w-10 flex-shrink-0 overflow-hidden rounded">
								<Image
									src={getBlockIconPath(b.id)}
									alt={b.name}
									width={40}
									height={40}
								/>
							</div>
							<div className="min-w-0">
								<div className="truncate text-sm font-medium">
									{b.name}
								</div>
								<div className="text-xs text-[var(--muted)]">
									{b.count} {b.count === 1 ? "block" : "blocks"}
								</div>
							</div>
						</li>
					))}
				</ul>
			)}
		</section>
	);
}

export default async function CommitPage({
	params,
}: {
	params: Promise<{ id: string; commitId: string }>;
}) {
	const { commitId } = await params;
	const data = mockCommitDetail(commitId);

	return (
		<div className="min-h-screen w-full px-6 py-10">
			<div className="mx-auto max-w-5xl">
				<header className="mb-6">
					<h1 className="heading-mc text-2xl md:text-3xl">
						Commit {data.id}
					</h1>
					<p className="mt-2 text-sm">
						<span className="font-medium">{data.message}</span>
					</p>
					<p className="mt-1 text-xs text-[var(--muted)]">
						by {data.author} •{" "}
						{new Date(data.timestamp).toLocaleString()}
					</p>
				</header>

				<DeltaList title="Added" items={data.added} />
				<DeltaList title="Removed" items={data.removed} />
			</div>
		</div>
	);
}

