import map from "./blockIconMap.json";

type BlockIconMap = Record<string, string>;
const BLOCK_ICON_MAP = map as BlockIconMap;
const DEFAULT_ICON = "/window.svg";

function idToName(id: string): string {
	// "minecraft:redstone_wire" -> "redstone_wire"
	const colonIdx = id.indexOf(":");
	return colonIdx !== -1 ? id.slice(colonIdx + 1) : id;
}

function generateNameCandidates(baseName: string): string[] {
	const cands = new Set<string>();
	cands.add(baseName);

	// Common alternates
	// redstone_wire -> redstone_dust
	if (baseName === "redstone_wire") cands.add("redstone_dust");
	// torch -> torch_on/torch_off
	if (baseName === "torch") {
		cands.add("torch_on");
		cands.add("redstone_torch_on");
	}
	// door variants (oak_door -> door_wood)
	if (baseName.endsWith("_door")) cands.add("door_wood");
	// singular/plural heuristics
	if (baseName.endsWith("s")) cands.add(baseName.slice(0, -1));
	else cands.add(`${baseName}s`);
	// hyphen/underscore variants (defensive)
	cands.add(baseName.replace(/-/g, "_"));
	cands.add(baseName.replace(/_/g, "-"));

	return Array.from(cands);
}

function searchInMapByCandidates(candidates: string[]): string | null {
	// 1) exact key match with namespace
	for (const cand of candidates) {
		const k = `minecraft:${cand}`;
		if (BLOCK_ICON_MAP[k]) return BLOCK_ICON_MAP[k];
	}
	// 2) endsWith match on keys (looser)
	for (const [k, v] of Object.entries(BLOCK_ICON_MAP)) {
		const name = idToName(k);
		if (candidates.some((c) => name === c || name.endsWith(c))) {
			return v;
		}
	}
	// 3) contains match
	for (const [k, v] of Object.entries(BLOCK_ICON_MAP)) {
		const name = idToName(k);
		if (candidates.some((c) => name.includes(c))) {
			return v;
		}
	}
	return null;
}

export function getBlockIconPath(blockId: string): string {
	// 0) direct hit
	const direct = BLOCK_ICON_MAP[blockId];
	if (direct) return direct;

	// 1) try by name and common alternates
	const base = idToName(blockId);
	const candidates = generateNameCandidates(base);
	const found = searchInMapByCandidates(candidates);
	if (found) return found;

	// 2) fallback to a common generic if present
	if (BLOCK_ICON_MAP["minecraft:stone"]) return BLOCK_ICON_MAP["minecraft:stone"];
	if (BLOCK_ICON_MAP["minecraft:cobblestone"]) return BLOCK_ICON_MAP["minecraft:cobblestone"];

	// 3) final fallback
	return DEFAULT_ICON;
}


