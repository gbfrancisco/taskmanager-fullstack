/**
 * Footer Component - Page Footer
 *
 * Graphic Novel Theme:
 * - Vivid Amber background with thick black border (top only)
 * - Matches Header styling for consistency
 * - Centered copyright text
 */

export function Footer() {
  return (
    <footer className="bg-amber-vivid border-t-4 border-ink">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-center items-center h-12">
          <span className="text-display text-sm text-ink">
            © 2025 Task Manager. All rights reserved.
          </span>
        </div>
      </div>
    </footer>
  );
}
