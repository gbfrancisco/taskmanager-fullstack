/**
 * Header Component - Navigation Bar
 *
 * Features:
 * - Conditional navigation based on auth state
 * - Authenticated: Home, Tasks, Projects, User dropdown (with logout)
 * - Guest: Home, Login, Register
 *
 * Graphic Novel Theme:
 * - Vivid Amber background with thick black border (bottom only)
 * - Outfit font for logo with ink splash effect
 * - Comic-style nav buttons with press effect
 * - User dropdown with avatar and menu
 */

import { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from '@tanstack/react-router';
import { useAuth } from '@/contexts/AuthContext';

export function Header() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  /**
   * Handle logout - clear auth state and navigate to login
   */
  async function handleLogout() {
    await logout();
    navigate({ to: '/login' });
    setIsMenuOpen(false);
  }

  /**
   * Close menu when clicking outside
   */
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsMenuOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <header className="bg-amber-vivid border-b-4 border-ink sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">

          {/* Logo / Brand - with ink splash effect */}
          <Link to="/" className="group relative flex items-center">
            {/* Ink splash background - skews and scales on hover */}
            <span className="absolute -inset-x-3 -inset-y-1 bg-ink skew-x-[-3deg] transition-transform group-hover:skew-x-0 group-hover:scale-105" />
            <span className="relative text-display text-2xl text-amber-vivid px-1 z-10">
              Task Manager
            </span>
          </Link>

          {/* Navigation - changes based on auth state */}
          <nav className="flex items-center gap-3">
            {/* Home link - hidden on mobile (logo serves same purpose) */}
            <NavLink to="/" label="HOME" className="hidden sm:block" />

            {isAuthenticated ? (
              <>
                {/* Authenticated navigation */}
                <NavLink to="/tasks" label="TASKS" />
                <NavLink to="/projects" label="PROJECTS" />

                {/* User dropdown menu - replaces simple logout button */}
                <div className="relative ml-2" ref={menuRef}>
                  {/*
                    User button styling:
                    - Closed: shadow-comic-interactive (hover/press physics)
                    - Open: translated + no shadow (locked in pressed state)
                  */}
                  <button
                    onClick={() => setIsMenuOpen(!isMenuOpen)}
                    className={`
                      flex items-center gap-2 pl-1 pr-3 py-1.5 border-comic transition-all outline-none
                      ${isMenuOpen
                        ? 'bg-paper text-ink shadow-comic-interactive'
                        : 'bg-paper text-ink shadow-comic-interactive'
                      }
                    `}
                    aria-expanded={isMenuOpen}
                    aria-haspopup="true"
                  >
                    {/* Compact Avatar */}
                    <div className={`
                      w-7 h-7 border-2 font-display font-black text-sm flex items-center justify-center transition-colors
                      ${isMenuOpen ? 'bg-amber-vivid text-ink border-amber-vivid' : 'bg-ink text-white border-ink'}
                    `}>
                      {user?.username.charAt(0).toUpperCase()}
                    </div>

                    {/* Single Line Username */}
                    <span className="hidden md:block font-display text-sm uppercase tracking-wide">
                      {user?.username}
                    </span>

                    {/* Dropdown Arrow */}
                    <span className={`text-[10px] transform transition-transform duration-200 ${isMenuOpen ? 'rotate-180' : ''}`}>
                      ▼
                    </span>
                  </button>

                  {/* Dropdown Content */}
                  {isMenuOpen && (
                    <div className="absolute right-0 mt-2 w-56 bg-paper border-comic shadow-comic-soft overflow-hidden animate-in fade-in slide-in-from-top-1 duration-200 z-50">

                      {/* Header with Details */}
                      <div className="bg-amber-light p-3 border-b-2 border-ink">
                        <div className="flex justify-between items-baseline">
                          <span className="text-[10px] uppercase font-bold text-ink-light">Operative</span>
                          <span className="text-[10px] font-mono text-ink-light">Lvl 1</span>
                        </div>
                        <p className="font-display text-ink truncate text-lg">{user?.username}</p>
                      </div>

                      {/* Logout */}
                      <div className="p-2 bg-paper-dark">
                        <button
                          onClick={handleLogout}
                          className="w-full text-left px-3 py-3 text-sm font-black text-danger uppercase hover:bg-danger hover:text-white transition-colors border-2 border-transparent hover:border-ink flex justify-between items-center group"
                        >
                          <span>Log Out</span>
                          <span className="group-hover:translate-x-1 transition-transform">→</span>
                        </button>
                      </div>
                    </div>
                  )}
                </div>
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
 * - Active: Blue background (status-blue) with paper text
 * - Inactive: Paper background with shadow-comic-interactive (hover/press effect)
 */
function NavLink({ to, label, className = '' }: { to: string; label: string; className?: string }) {
  const baseClasses = 'px-4 py-2 text-display text-sm border-comic';

  return (
    <Link
      to={to}
      activeOptions={{ exact: to === '/' }}
      activeProps={{
        className: `${baseClasses} bg-status-blue text-paper shadow-comic-interactive ${className}`
      }}
      inactiveProps={{
        className: `${baseClasses} bg-paper text-ink shadow-comic-interactive ${className}`
      }}
    >
      {label}
    </Link>
  );
}
