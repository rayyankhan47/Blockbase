type BlockIconMap = Record<string, string>;

// Temporary mapping to local placeholder svgs in /public.
// We can swap paths to real Minecraft textures later.
const BLOCK_ICON_MAP: BlockIconMap = {
	"minecraft:redstone_wire": "/globe.svg",
	"minecraft:lever": "/window.svg",
	"minecraft:observer": "/file.svg",
	"minecraft:cobblestone": "/vercel.svg",
	"minecraft:torch": "/next.svg",
};

const DEFAULT_ICON = "/window.svg";

export function getBlockIconPath(blockId: string): string {
	return BLOCK_ICON_MAP[blockId] ?? DEFAULT_ICON;
}


