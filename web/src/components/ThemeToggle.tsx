'use client';

import { useEffect, useState } from 'react';

type Theme = 'dark' | 'light';

export default function ThemeToggle({ className = '' }: { className?: string }) {
  const [theme, setTheme] = useState<Theme>('dark');

  useEffect(() => {
    const stored = (typeof window !== 'undefined' && localStorage.getItem('bb-theme')) as Theme | null;
    const initial: Theme = stored ?? 'dark';
    setTheme(initial);
    if (typeof document !== 'undefined') {
      document.documentElement.classList.toggle('theme-light', initial === 'light');
    }
  }, []);

  const toggle = () => {
    const next: Theme = theme === 'dark' ? 'light' : 'dark';
    setTheme(next);
    if (typeof document !== 'undefined') {
      document.documentElement.classList.toggle('theme-light', next === 'light');
    }
    if (typeof window !== 'undefined') {
      localStorage.setItem('bb-theme', next);
    }
  };

  return (
    <button
      onClick={toggle}
      className={`accent-ring z-50 rounded-full border px-3 py-1.5 text-xs ${className}`}
      style={{ borderColor: 'var(--accent-weak)' }}
      aria-label="Toggle theme"
    >
      {theme === 'dark' ? 'Light' : 'Dark'}
    </button>
  );
}


