/**
 * Header Component - Navigation Bar
 *
 * This component demonstrates TanStack Router's navigation features:
 * - <Link> component for declarative navigation
 * - activeProps for styling the currently active route
 * - Type-safe navigation (try changing 'to' to an invalid path!)
 */

import { Link } from '@tanstack/react-router'

/**
 * Navigation links configuration
 *
 * We define our nav items as data to keep the JSX clean.
 * Each item has:
 * - to: The route path (must be a valid route!)
 * - label: Display text
 */
const navLinks = [
  { to: '/', label: 'Home' },
  { to: '/tasks', label: 'Tasks' },
  { to: '/projects', label: 'Projects' },
] as const

export function Header() {
  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-10">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo / Brand */}
          <Link to="/" className="flex items-center gap-2">
            <span className="text-xl font-bold text-gray-800">
              Task Manager
            </span>
          </Link>

          {/* Navigation Links */}
          <nav className="flex gap-1">
            {navLinks.map((link) => (
              <NavLink key={link.to} to={link.to} label={link.label} />
            ))}
          </nav>
        </div>
      </div>
    </header>
  )
}

/**
 * NavLink Component - Individual navigation link
 *
 * This demonstrates the key Link features:
 *
 * 1. `to` prop - The destination route (type-checked!)
 * 2. `activeProps` - Styles applied when this route is active
 * 3. `inactiveProps` - Styles applied when NOT active
 * 4. `activeOptions` - Fine-tune what counts as "active"
 *    - exact: true means /tasks won't be active when on /tasks/123
 */
function NavLink({ to, label }: { to: string; label: string }) {
  return (
    <Link
      to={to}
      /**
       * activeOptions controls when the link is considered "active"
       *
       * - exact: true → Only active on exact match
       *   /tasks is NOT active when viewing /tasks/123
       *
       * - exact: false (default) → Active on partial match
       *   /tasks IS active when viewing /tasks/123
       *
       * For top-level nav, exact:true usually feels right.
       * The home link "/" especially needs exact:true, otherwise
       * it would be "active" on every page!
       */
      activeOptions={{ exact: to === '/' }}
      /**
       * activeProps - Applied when this link's route is active
       *
       * These classes are ONLY added when navigated to this route.
       * Great for visual feedback showing "you are here"
       */
      activeProps={{
        className:
          'px-3 py-2 rounded-md text-sm font-medium bg-gray-100 text-gray-900',
      }}
      /**
       * inactiveProps - Applied when NOT active
       *
       * These classes are removed when the route becomes active.
       * Use for the default/resting state of the link.
       */
      inactiveProps={{
        className:
          'px-3 py-2 rounded-md text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-50',
      }}
    >
      {label}
    </Link>
  )
}
