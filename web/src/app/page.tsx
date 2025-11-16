export default function Home() {
  return (
    <main className="min-h-screen w-full px-8 py-16 flex items-center">
      <div className="mx-auto max-w-6xl text-center">
        <h1 className="brand-title text-6xl md:text-7xl">Blockbase</h1>
        <p className="mx-auto mt-5 max-w-2xl text-xl text-[var(--muted)]">
          Version control for Minecraft builds. Track, commit, and review your
          worlds like code.
        </p>
        <div className="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row">
          <a
            href="/repos"
            className="accent-ring inline-flex items-center justify-center rounded-xl px-8 py-5 text-lg font-semibold text-black"
            style={{ backgroundColor: "var(--accent)" }}
          >
            Get Started
          </a>
          <a
            href="/repos"
            className="accent-ring inline-flex items-center justify-center rounded-xl border px-8 py-5 text-lg"
            style={{ borderColor: "var(--accent-weak)" }}
          >
            {/* Placeholder: will become "View Your Repositories" if logged in */}
            Login
          </a>
        </div>
        <div className="mx-auto mt-14 grid max-w-5xl gap-6 md:grid-cols-3">
          <div className="card p-6 text-left">
            <h3 className="text-2xl font-semibold">Track Changes</h3>
            <p className="mt-3 text-base text-[var(--muted)]">
              Block-level tracking for placements, breaks, and state changes.
              Commit and review your world history.
            </p>
          </div>
          <div className="card p-6 text-left">
            <h3 className="text-2xl font-semibold">Visual Diffs</h3>
            <p className="mt-3 text-base text-[var(--muted)]">
              See added/removed blocks by type to understand builds at a glance.
            </p>
          </div>
          <div className="card p-6 text-left">
            <h3 className="text-2xl font-semibold">Git-like Workflow</h3>
            <p className="mt-3 text-base text-[var(--muted)]">
              Add, commit, log, and reset—familiar commands adapted to Minecraft.
            </p>
          </div>
        </div>
      </div>
    </main>
  );
}
