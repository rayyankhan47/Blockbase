"use client";

import ThemeToggle from "@/components/ThemeToggle";
import BackNav from "@/components/BackNav";

export default function FloatingControls() {
  return (
    <div className="fixed left-4 top-4 z-50 flex flex-col items-center gap-3">
      <ThemeToggle />
      <BackNav />
    </div>
  );
}


