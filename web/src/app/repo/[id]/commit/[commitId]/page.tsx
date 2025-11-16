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
	variant = "neutral",
}: {
	title: string;
	items: BlockDelta[];
	variant?: "added" | "removed" | "neutral";
}) {
	const headerBg =
		variant === "added"
			? "rgba(34,197,94,0.12)" // green 500 @ 12%
			: variant === "removed"
			? "rgba(239,68,68,0.12)" // red 500 @ 12%
			: "transparent";
	const headerBorder =
		variant === "added"
			? "rgba(34,197,94,0.35)"
			: variant === "removed"
			? "rgba(239,68,68,0.35)"
			: "var(--accent-weak)";
	return (
		<section className="card overflow-hidden">
			<div
				className="border-b px-5 py-4"
				style={{ background: headerBg, borderColor: headerBorder }}
			>
				<h3 className="text-xl font-semibold">{title}</h3>
			</div>
			{items.length === 0 ? (
				<p className="px-5 py-4 text-base text-[var(--muted)]">None</p>
			) : (
				<ul className="grid gap-2 p-4 sm:grid-cols-2 lg:grid-cols-2">
					{items.map((b) => (
						<li
							key={b.id}
							className="flex items-center gap-3 rounded-md p-3"
							style={{ background: "rgba(255,255,255,0.02)" }}
						>
							<div className="h-12 w-12 flex-shrink-0 overflow-hidden rounded">
								<Image
									src={getBlockIconPath(b.id)}
									alt={b.name}
									width={48}
									height={48}
								/>
							</div>
							<div className="min-w-0">
								<div className="truncate text-base font-medium">
									{b.name}
								</div>
								<div className="text-sm text-[var(--muted)]">
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
		<div className="min-h-screen w-full px-8 py-14">
			<div className="mx-auto max-w-6xl">
				<header className="mb-8">
					<h1 className="text-5xl font-bold">Commit {data.id}</h1>
					<p className="mt-4 text-2xl">
						<span className="font-semibold">{data.message}</span>
					</p>
					<p className="mt-1 text-md text-[var(--muted)]">
						by {data.author} •{" "}
						{new Date(data.timestamp).toLocaleString()}
					</p>
				</header>

				<div className="grid gap-6 md:grid-cols-2">
					<DeltaList title="Added" items={data.added} variant="added" />
					<DeltaList title="Removed" items={data.removed} variant="removed" />
				</div>
			</div>
		</div>
	);
}

