import Image from "next/image";
import { getBlockIconPath } from "@/lib/blockIcons";
import { getApiBase } from "@/lib/config";

type BlockDelta = {
	id: string; // e.g., minecraft:redstone_wire
	name: string; // e.g., Redstone Dust
	count: number;
};

type CommitFromApi = {
	id: string;
	repo_id: string;
	message: string;
	author: string;
	timestamp: string;
	changes: RawChange[];
};

type RawChange = {
	oldStateId?: string | null;
	newStateId?: string | null;
	type?: "PLACED" | "BROKEN" | "MODIFIED" | string;
};

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
	const { id, commitId } = await params;

	const res = await fetch(`${getApiBase()}/repos/${id}/commits/${commitId}`, {
		cache: "no-store",
	});

	if (!res.ok) {
		return (
			<div className="min-h-screen w-full px-8 py-14">
				<div className="mx-auto max-w-6xl">
					<p>Commit not found.</p>
				</div>
			</div>
		);
	}

	const apiCommit = (await res.json()) as CommitFromApi;
	const { added, removed } = summarizeChanges(apiCommit.changes);

	const data = {
		id: apiCommit.id,
		message: apiCommit.message,
		author: apiCommit.author,
		timestamp: apiCommit.timestamp,
		added,
		removed,
	};

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

function summarizeChanges(changes: RawChange[]): {
	added: BlockDelta[];
	removed: BlockDelta[];
} {
	const addedMap = new Map<string, number>();
	const removedMap = new Map<string, number>();

	for (const ch of changes || []) {
		const t = ch.type || "";
		if (t === "PLACED" && ch.newStateId) {
			const id = ch.newStateId;
			addedMap.set(id, (addedMap.get(id) || 0) + 1);
		} else if (t === "BROKEN" && ch.oldStateId) {
			const id = ch.oldStateId;
			removedMap.set(id, (removedMap.get(id) || 0) + 1);
		} else if (t === "MODIFIED") {
			// Optional: treat modified as removed old + added new
			if (ch.oldStateId) {
				const id = ch.oldStateId;
				removedMap.set(id, (removedMap.get(id) || 0) + 1);
			}
			if (ch.newStateId) {
				const id = ch.newStateId;
				addedMap.set(id, (addedMap.get(id) || 0) + 1);
			}
		}
	}

	const toDeltas = (m: Map<string, number>): BlockDelta[] =>
		Array.from(m.entries()).map(([id, count]) => ({
			id,
			name: humanizeId(id),
			count,
		}));

	return {
		added: toDeltas(addedMap),
		removed: toDeltas(removedMap),
	};
}

function humanizeId(id: string): string {
	const bare = id.includes(":") ? id.split(":")[1] : id;
	return bare
		.split("_")
		.map((p) => p.charAt(0).toUpperCase() + p.slice(1))
		.join(" ");
}

