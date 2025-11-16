export function getApiBase(): string {
	// Next.js inlines NEXT_PUBLIC_* at build time; fallback to localhost
	return process.env.NEXT_PUBLIC_BLOCKBASE_API_URL || 'http://localhost:3000/api';
}


