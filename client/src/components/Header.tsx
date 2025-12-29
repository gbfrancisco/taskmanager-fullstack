/**
 * Header Component - Navigation Bar
 *
 * Features:
 * - Conditional navigation based on auth state
 * - Authenticated: Home, Tasks, Projects, username, Logout button
 * - Guest: Home, Login, Register
 *
 * Graphic Novel Theme:
 * - Vivid Amber background with thick black border (bottom only)
 * - Outfit font for logo with ink splash effect
 * - Comic-style nav buttons with press effect
 */

import { Link, useNavigate } from '@tanstack/react-router';
import { useAuth } from '@/contexts/AuthContext';

export function Header() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  /**
   * Handle logout - clear auth state and navigate to home
   */
  async function handleLogout() {
    await logout();
    navigate({ to: '/login' });
  }

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

          {/* Navigation - changes based on auth state */}
          <nav className="flex items-center gap-2">
            {/* Home link - hidden on mobile (logo serves same purpose) */}
            <NavLink to="/" label="HOME" className="hidden sm:block" />

            {isAuthenticated ? (
              <>
                {/* Authenticated navigation */}
                <NavLink to="/tasks" label="TASKS" />
                <NavLink to="/projects" label="PROJECTS" />

                {/* User info - hidden on mobile */}
                <span className="hidden sm:block px-3 py-2 text-display text-sm text-ink">
                  {user?.username}
                </span>

                {/* Logout button */}
                <button
                  onClick={handleLogout}
                  className="px-4 py-2 text-display text-sm bg-paper text-ink border-comic shadow-comic-interactive"
                >
                  LOGOUT
                </button>
              </>
            ) : (
              <>
                {/* Guest navigation */}
                <NavLink to="/login" label="LOGIN" />
                <NavLink to="/register" label="REGISTER" />
              </>
            )}
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
function NavLink({ to, label, className = '' }: { to: string; label: string; className?: string }) {
  const baseClasses = 'px-4 py-2 text-display text-sm border-comic';

  return (
    <Link
      to={to}
      activeOptions={{ exact: to === '/' }}
      activeProps={{
        className: `${baseClasses} bg-ink text-amber-vivid translate-x-1 translate-y-1 ${className}`
      }}
      inactiveProps={{
        className: `${baseClasses} bg-paper text-ink shadow-comic-interactive ${className}`
      }}
    >
      {label}
    </Link>
  );
}
