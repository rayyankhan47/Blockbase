'use client';

import Link from "next/link";
import { usePathname } from "next/navigation";

function computeBack(pathname: string): { href: string; label: string } | null {
	if (!pathname || pathname === "/") return null;
	// /repo/[id]/commit/[commitId] -> /repo/[id]
	const commitMatch = pathname.match(/^\/repo\/([^/]+)\/commit\/[^/]+/);
	if (commitMatch) {
		return { href: `/repo/${commitMatch[1]}`, label: "Back to Repo" };
	}
	// /repo/[id] -> /repos
	const repoMatch = pathname.match(/^\/repo\/([^/]+)\/?$/);
	if (repoMatch) {
		return { href: "/repos", label: "Back to Repos" };
	}
	// /repos -> /
	if (pathname.startsWith("/repos")) {
		return { href: "/", label: "Back Home" };
	}
	// default: go home
	return { href: "/", label: "Back Home" };
}

export default function BackNav({ className = '' }: { className?: string }) {
	const pathname = usePathname();
	const back = computeBack(pathname);
	if (!back) return null;
	return (
		<Link
			href={back.href}
			aria-label={back.label}
			className={`z-40 inline-flex h-11 w-11 items-center justify-center rounded-full ${className}`}
			style={{ backgroundColor: "var(--accent)", color: "#0a0a0a", boxShadow: "0 6px 18px rgba(0,0,0,0.25)" }}
		>
			<span aria-hidden style={{ fontSize: 22, lineHeight: 1 }}>←</span>
		</Link>
	);
}


