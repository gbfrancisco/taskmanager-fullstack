/**
 * Header Component - Navigation Bar
 *
 * Graphic Novel Theme:
 * - Vivid Amber background with thick black border (bottom only)
 * - Outfit font for logo with ink splash effect
 * - Comic-style nav buttons with press effect
 */

import { Link } from '@tanstack/react-router';

/**
 * Navigation links configuration
 */
const navLinks = [
  { to: '/', label: 'Home' },
  { to: '/tasks', label: 'Tasks' },
  { to: '/projects', label: 'Projects' },
  { to: '/login', label: 'Login' }
] as const;

export function Header() {
  return (
    <header className="bg-amber-vivid border-b-4 border-ink sticky top-0 z-10">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo / Brand - with ink splash effect */}
          <Link to="/" className="group relative flex items-center">
            {/* Ink splash background */}
            <span className="absolute -inset-x-3 -inset-y-1 bg-ink skew-x-[-3deg] transition-transform group-hover:skew-x-0 group-hover:scale-105" />
            <span className="relative text-display text-2xl text-amber-vivid px-1">
              Task Manager
            </span>
          </Link>

          {/* Navigation Links */}
          <nav className="flex gap-2">
            {navLinks.map((link) => (
              <NavLink key={link.to} to={link.to} label={link.label} />
            ))}
          </nav>
        </div>
      </div>
    </header>
  );
}

/**
 * NavLink Component - Comic-style navigation button
 *
 * Features:
 * - Thick black border on all states
 * - Active: Black background with amber text (pressed look)
 * - Inactive: Paper background with hover/active press effect
 */
function NavLink({ to, label }: { to: string; label: string }) {
  return (
    <Link
      to={to}
      activeOptions={{ exact: to === '/' }}
      activeProps={{
        className:
          'px-4 py-2 text-display text-sm bg-ink text-amber-vivid border-comic translate-x-1 translate-y-1'
      }}
      inactiveProps={{
        className:
          'px-4 py-2 text-display text-sm bg-paper text-ink border-comic shadow-comic-interactive'
      }}
    >
      {label}
    </Link>
  );
}
