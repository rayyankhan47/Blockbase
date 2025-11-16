// Manual overrides for common ID/name mismatches or missing textures in packs.
// Values are public-relative paths that should exist after extraction.
export const ICON_ALIASES: Record<string, string> = {
	// Vanilla torch ID often maps to torch_on texture in packs
	"minecraft:torch": "/textures/block/torch_on.png",
	// Observer may be missing in older packs; fallback to a reasonable redstone block
	"minecraft:observer": "/textures/block/redstone_block.png",
};


