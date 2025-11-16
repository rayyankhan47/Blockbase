import fs from "node:fs";
import path from "node:path";
import AdmZip from "adm-zip";

function ensureDir(dir) {
	if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
}

function writeJson(file, data) {
	fs.writeFileSync(file, JSON.stringify(data, null, 2), "utf8");
}

function usageAndExit() {
	console.error("Usage: node scripts/extract-pack-textures.mjs --zip /path/to/pack.zip");
	process.exit(1);
}

function buildMapping({ copiedBlocks, copiedItems }) {
	const map = {};
	for (const base of copiedBlocks) {
		map[`minecraft:${base}`] = `/textures/block/${base}.png`;
	}
	for (const base of copiedItems) {
		const key = `minecraft:${base}`;
		if (!map[key]) map[key] = `/textures/item/${base}.png`;
	}
	if (map["minecraft:redstone_dust"] && !map["minecraft:redstone_wire"]) {
		map["minecraft:redstone_wire"] = map["minecraft:redstone_dust"];
	}
	return map;
}

function main() {
	const zipIdx = process.argv.indexOf("--zip");
	if (zipIdx === -1 || !process.argv[zipIdx + 1]) {
		usageAndExit();
	}
	const zipPath = path.resolve(process.argv[zipIdx + 1]);
	if (!fs.existsSync(zipPath)) {
		console.error(`Zip not found: ${zipPath}`);
		process.exit(1);
	}

	const projectRoot = process.cwd();
	const publicDir = path.join(projectRoot, "public");
	const outBlockDir = path.join(publicDir, "textures", "block");
	const outItemDir = path.join(publicDir, "textures", "item");
	const outMapJson = path.join(projectRoot, "src", "lib", "blockIconMap.json");
	ensureDir(outBlockDir);
	ensureDir(outItemDir);

	const zip = new AdmZip(zipPath);
	const entries = zip.getEntries();
	const copiedBlocks = [];
	const copiedItems = [];

	for (const e of entries) {
		if (e.isDirectory) continue;
		const name = e.entryName.replace(/\\/g, "/");
		if (!name.toLowerCase().endsWith(".png")) continue;

		// Find textures path anywhere within the zip (handles top-level wrapper folders)
		const lower = name.toLowerCase();
		const texturesIdx = lower.indexOf("assets/minecraft/textures/");
		if (texturesIdx === -1) continue;
		const rel = name.slice(texturesIdx); // assets/minecraft/textures/...

		const parts = rel.split("/");
		// parts: ["assets","minecraft","textures", "<category>", ... "<file>.png"]
		if (parts.length < 5) continue;
		let category = parts[3]; // might be block/blocks or item/items
		const isBlock =
			category === "block" ||
			category === "blocks"; // accept plural
		const isItem =
			category === "item" ||
			category === "items"; // accept plural
		if (!isBlock && !isItem) continue;

		const filename = parts[parts.length - 1];
		const base = path.basename(filename, ".png");

		const dest =
			isBlock
				? path.join(outBlockDir, `${base}.png`)
				: path.join(outItemDir, `${base}.png`);
		ensureDir(path.dirname(dest));
		fs.writeFileSync(dest, e.getData());

		if (isBlock) copiedBlocks.push(base);
		else if (isItem) copiedItems.push(base);
	}

	const map = buildMapping({ copiedBlocks, copiedItems });
	writeJson(outMapJson, map);

	console.log(
		`Extracted ${copiedBlocks.length} block textures and ${copiedItems.length} item textures from pack.`
	);
	console.log(`Wrote icon map: ${path.relative(projectRoot, outMapJson)}`);
}

main();


