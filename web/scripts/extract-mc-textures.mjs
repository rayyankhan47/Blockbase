import fs from "node:fs";
import path from "node:path";
import os from "node:os";

function ensureDir(dir) {
	if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
}

function readJson(file) {
	return JSON.parse(fs.readFileSync(file, "utf8"));
}

function writeJson(file, data) {
	fs.writeFileSync(file, JSON.stringify(data, null, 2), "utf8");
}

function copyFile(src, dest) {
	ensureDir(path.dirname(dest));
	fs.copyFileSync(src, dest);
}

function resolveMinecraftAssetsRoot() {
	// macOS default
	const macPath = path.join(
		os.homedir(),
		"Library",
		"Application Support",
		"minecraft",
		"assets"
	);
	if (fs.existsSync(macPath)) return macPath;
	// Linux / Windows defaults
	const home = os.homedir();
	const linuxPath = path.join(home, ".minecraft", "assets");
	if (fs.existsSync(linuxPath)) return linuxPath;
	const winPath = path.join(home, "AppData", "Roaming", ".minecraft", "assets");
	if (fs.existsSync(winPath)) return winPath;
	throw new Error("Could not find Minecraft assets directory.");
}

function pickIndexFile(indexDir, preferred) {
	const preferredFile = path.join(indexDir, `${preferred}.json`);
	if (preferred && fs.existsSync(preferredFile)) return preferredFile;
	// fallback: pick latest modified json in indexes
	const files = fs
		.readdirSync(indexDir)
		.filter((f) => f.endsWith(".json"))
		.map((f) => path.join(indexDir, f))
		.sort((a, b) => fs.statSync(b).mtimeMs - fs.statSync(a).mtimeMs);
	if (!files.length) throw new Error("No asset index files found.");
	return files[0];
}

function buildMapping({ copiedBlocks, copiedItems }) {
	// Prefer block textures over item textures where names overlap
	const map = {};
	for (const basename of copiedBlocks) {
		map[`minecraft:${basename}`] = `/textures/block/${basename}.png`;
	}
	for (const basename of copiedItems) {
		const key = `minecraft:${basename}`;
		if (!map[key]) {
			map[key] = `/textures/item/${basename}.png`;
		}
	}
	// Common aliases
	if (map["minecraft:redstone_dust"] && !map["minecraft:redstone_wire"]) {
		map["minecraft:redstone_wire"] = map["minecraft:redstone_dust"];
	}
	return map;
}

function main() {
	const versionArgIndex = process.argv.indexOf("--version");
	const preferredVersion =
		versionArgIndex !== -1 ? process.argv[versionArgIndex + 1] : "1.18.2";

	const projectRoot = path.resolve(path.join(process.cwd()));
	const publicDir = path.join(projectRoot, "public");
	const outBlockDir = path.join(publicDir, "textures", "block");
	const outItemDir = path.join(publicDir, "textures", "item");
	const outMapJson = path.join(
		projectRoot,
		"src",
		"lib",
		"blockIconMap.json"
	);

	ensureDir(outBlockDir);
	ensureDir(outItemDir);

	const assetsRoot = resolveMinecraftAssetsRoot();
	const indexDir = path.join(assetsRoot, "indexes");
	const indexFile = pickIndexFile(indexDir, preferredVersion);
	const index = readJson(indexFile);

	const objectsDir = path.join(assetsRoot, "objects");
	const objects = index.objects || {};
	const copiedBlocks = [];
	const copiedItems = [];

	const entries = Object.entries(objects);
	for (const [name, meta] of entries) {
		if (
			!name.startsWith("minecraft/") ||
			!name.endsWith(".png") ||
			!meta?.hash
		) {
			continue;
		}
		const rel = name.replace(/^minecraft\//, "");
		if (!rel.startsWith("textures/")) continue;
		const [, category, filename] = rel.split("/");
		if (!["block", "item"].includes(category)) continue;

		const hash = meta.hash;
		const src = path.join(objectsDir, hash.slice(0, 2), hash);
		if (!fs.existsSync(src)) continue;

		const base = path.basename(filename, ".png");
		if (category === "block") {
			const dest = path.join(outBlockDir, `${base}.png`);
			copyFile(src, dest);
			copiedBlocks.push(base);
		} else if (category === "item") {
			const dest = path.join(outItemDir, `${base}.png`);
			copyFile(src, dest);
			copiedItems.push(base);
		}
	}

	const map = buildMapping({ copiedBlocks, copiedItems });
	writeJson(outMapJson, map);

	console.log(
		`Extracted ${copiedBlocks.length} block textures and ${copiedItems.length} item textures.`
	);
	console.log(`Wrote icon map: ${path.relative(projectRoot, outMapJson)}`);
}

main();


