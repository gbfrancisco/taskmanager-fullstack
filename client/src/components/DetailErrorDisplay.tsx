/**
 * DetailErrorDisplay - Error display for detail pages
 *
 * Used when data fetching fails on detail pages (task detail, project detail).
 * Shows a styled error container with a link back to the list page.
 */

import { Link } from '@tanstack/react-router';

interface DetailErrorDisplayProps {
  /** Error title/heading */
  title: string;
  /** Error message to display */
  message: string;
  /** Route to navigate back to (e.g., '/tasks') */
  backTo: string;
  /** Label for the back link (e.g., 'Return to Tasks') */
  backLabel: string;
}

export function DetailErrorDisplay({
  title,
  message,
  backTo,
  backLabel
}: DetailErrorDisplayProps) {
  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <div className="bg-danger-bg border-comic p-6">
        <h2 className="text-display text-2xl text-danger">{title}</h2>
        <p>{message}</p>
        <Link to={backTo} className="underline mt-4 inline-block">
          {backLabel}
        </Link>
      </div>
    </div>
  );
}
