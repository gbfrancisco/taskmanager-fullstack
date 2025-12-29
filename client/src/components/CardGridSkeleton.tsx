/**
 * CardGridSkeleton - Loading placeholder for card grid pages
 *
 * Used on list pages (tasks, projects) to show a loading state
 * while data is being fetched. Features animated pulse effect.
 */

interface CardGridSkeletonProps {
  /** Number of skeleton cards to show (default: 3) */
  count?: number;
}

export function CardGridSkeleton({ count = 3 }: CardGridSkeletonProps) {
  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {/* Header skeleton */}
      <div className="h-16 w-64 bg-paper-dark mb-8 animate-pulse" />

      {/* Card grid skeleton */}
      <div className="grid gap-6 md:grid-cols-3">
        {Array.from({ length: count }, (_, i) => (
          <div
            key={i}
            className="h-48 bg-paper border-comic opacity-50 animate-pulse"
          />
        ))}
      </div>
    </div>
  );
}
