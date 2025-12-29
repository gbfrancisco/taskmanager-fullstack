/**
 * Home Page Route - /
 *
 * This is the index route (home page):
 * - File location: src/routes/index.tsx
 * - URL path: / (root)
 *
 * The filename "index.tsx" is special - it represents the root of its parent.
 * In this case, it's directly in routes/, so it maps to "/"
 */

import { createFileRoute, Link } from '@tanstack/react-router';
import { useAuth } from '@/contexts/AuthContext';

export const Route = createFileRoute('/')({
  component: HomePage
});

function HomePage() {
  const { user, isAuthenticated } = useAuth();

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

      {/* COMIC LAYOUT GRID
        We use a grid to create a "Panel" effect common in comics.
      */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">

        {/* --- PANEL 1: HERO / INTRO (Span 8 cols) --- */}
        <section className="lg:col-span-8 flex flex-col gap-6">

          {/* Main Title Panel */}
          <div className="bg-paper border-comic-heavy shadow-comic-soft-lg p-8 relative overflow-hidden">
            {/* Decorative background stripes */}
            <div className="absolute top-0 right-0 w-32 h-32 bg-amber-vivid opacity-20 rotate-45 transform translate-x-16 -translate-y-16" />

            <div className="relative z-10">
              <div className="comic-caption mb-4 text-xs md:text-sm">
                Issue #1: The Beginning
              </div>

              <h1 className="text-display text-5xl md:text-7xl text-ink uppercase leading-none mb-4">
                Task<br />
                <span className="text-amber-dark">Manager</span>
              </h1>

              <p className="text-ink-soft text-xl font-medium max-w-lg border-l-4 border-amber-vivid pl-4 py-1">
                {isAuthenticated
                  ? `Welcome back, ${user?.username}. Your mission log is ready for review.`
                  : "Organize your life with the punchy power of a graphic novel."}
              </p>
            </div>
          </div>

          {/* Action Buttons Row */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            <Link
              to="/tasks"
              className="group block relative"
            >
              <div className="bg-ink text-paper border-comic shadow-comic-soft-interactive p-6 h-full flex flex-col justify-between">
                <div>
                  <h2 className="text-display text-2xl text-amber-vivid mb-2 group-hover:underline decoration-4 decoration-amber-vivid underline-offset-4">
                    Your Tasks
                  </h2>
                  <p className="text-paper-dark text-sm">Access your current (and inactive) tasks and objectives.</p>
                </div>
                <div className="mt-4 self-end">
                  <span className="bg-amber-vivid text-ink text-xs font-bold px-2 py-1 uppercase border-2 border-paper">
                    Start
                  </span>
                </div>
              </div>
            </Link>

            <Link
              to="/projects"
              className="group block relative"
            >
              <div className="bg-paper text-ink border-comic shadow-comic-soft-interactive p-6 h-full flex flex-col justify-between">
                <div>
                  <h2 className="text-display text-2xl mb-2 group-hover:underline decoration-4 decoration-ink underline-offset-4">
                    Your projects
                  </h2>
                  <p className="text-ink-soft text-sm">Manage larger projects and long-term goals.</p>
                </div>
                <div className="mt-4 self-end">
                  <span className="bg-ink text-paper text-xs font-bold px-2 py-1 uppercase">
                    View
                  </span>
                </div>
              </div>
            </Link>
          </div>
        </section>

        {/* --- PANEL 2: SIDEBAR / META INFO (Span 4 cols) --- */}
        <aside className="lg:col-span-4 space-y-6">

          {/* Status Box - Looks like a narrator box */}
          <div className="bg-amber-light border-comic shadow-comic-soft p-6">
            <h3 className="text-display text-xl text-ink mb-3 border-b-2 border-ink pb-2">
              System Status
            </h3>
            <ul className="space-y-3">
              <li className="flex items-center gap-2">
                <div className="w-3 h-3 bg-success border-2 border-ink rounded-full" />
                <span className="font-bold text-sm">Server: Online</span>
              </li>
              <li className="flex items-center gap-2">
                <div className="w-3 h-3 bg-amber-vivid border-2 border-ink rounded-full" />
                <span className="font-bold text-sm">Database: Connected</span>
              </li>
              <li className="flex items-center gap-2">
                <div className="w-3 h-3 bg-status-progress border-2 border-ink rounded-full" />
                <span className="font-bold text-sm">Theme: Graphic Novel</span>
              </li>
            </ul>
          </div>

          {/* "Ad" Space / Learning Project Info */}
          <div className="bg-halftone border-comic p-6 relative">
            <div className="absolute -top-3 -left-3 bg-paper border-2 border-ink px-2 py-1 text-xs font-bold transform -rotate-2">
              DEV NOTE
            </div>
            <p className="text-sm font-medium leading-relaxed mt-2">
              <span className="font-bold">Did you know?</span> This application is built using React, Spring Boot, and TanStack Router to demonstrate modern full-stack patterns.
            </p>
          </div>

          {!isAuthenticated && (
            <div className="text-center pt-4">
              <p className="text-sm text-ink-light mb-2">Ready to join the team?</p>
              <Link
                to="/register"
                className="inline-block w-full text-center bg-status-progress text-paper font-bold border-comic shadow-comic-soft-interactive py-3 px-4 hover:brightness-110"
              >
                CREATE ACCOUNT
              </Link>
            </div>
          )}

        </aside>
      </div>
    </div>
  );
}
