/**
 * Header Component - Navigation Bar
 *
 * Graphic Novel Theme:
 * - Vivid Amber background with thick black border
 * - Bebas Neue font for logo and nav links
 * - Comic-style shadows and interactive press effect
 */

import { Link } from '@tanstack/react-router';

/**
 * Navigation links configuration
 */
const navLinks = [
  { to: '/', label: 'Home' },
  { to: '/tasks', label: 'Tasks' },
  { to: '/projects', label: 'Projects' }
] as const;

export function Header() {
  return (
    <header className="bg-amber-vivid border-comic-heavy sticky top-0 z-10 shadow-comic">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo / Brand */}
          <Link to="/" className="flex items-center gap-2">
            <span className="text-display text-2xl text-ink tracking-wider">
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
 * - Active: Black background with amber text
 * - Inactive: Paper background with interactive shadow
 */
function NavLink({ to, label }: { to: string; label: string }) {
  return (
    <Link
      to={to}
      activeOptions={{ exact: to === '/' }}
      activeProps={{
        className:
          'px-4 py-2 text-display text-sm bg-ink text-amber-vivid border-comic'
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
