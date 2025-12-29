/**
 * PageErrorState - Error display for list pages
 *
 * Used when data fetching fails on list pages (tasks, projects).
 * Shows a styled error container with customizable title and message.
 */

interface PageErrorStateProps {
  /** Error title/heading */
  title: string;
  /** The error object or message */
  error: unknown;
  /** Default message if error is not an Error instance */
  defaultMessage?: string;
}

export function PageErrorState({
  title,
  error,
  defaultMessage = 'An error occurred.'
}: PageErrorStateProps) {
  const message = error instanceof Error ? error.message : defaultMessage;

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="bg-danger-bg border-comic-heavy p-6 shadow-comic-soft">
        <h1 className="text-display text-2xl text-danger mb-2">{title}</h1>
        <p className="font-mono text-sm">{message}</p>
      </div>
    </div>
  );
}
