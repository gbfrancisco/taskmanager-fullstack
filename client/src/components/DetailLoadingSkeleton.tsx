/**
 * DetailLoadingSkeleton - Loading placeholder for detail pages
 *
 * Used on detail pages (task detail, project detail) to show a
 * loading state while data is being fetched.
 *
 * Two variants:
 * - 'centered': Single centered card (task detail style)
 * - 'split': Two-column layout (project detail style)
 */

interface DetailLoadingSkeletonProps {
  variant?: 'centered' | 'split';
}

export function DetailLoadingSkeleton({ variant = 'centered' }: DetailLoadingSkeletonProps) {
  if (variant === 'split') {
    return (
      <div className="max-w-6xl mx-auto px-4 py-8">
        {/* Back link skeleton */}
        <div className="h-8 w-32 bg-paper-dark mb-6 animate-pulse" />

        {/* Two-column grid skeleton */}
        <div className="grid lg:grid-cols-12 gap-8">
          <div className="lg:col-span-8 h-96 bg-paper border-comic animate-pulse" />
          <div className="lg:col-span-4 h-64 bg-paper border-comic animate-pulse" />
        </div>
      </div>
    );
  }

  // Default: centered variant
  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <div className="bg-paper border-comic p-12 animate-pulse flex flex-col items-center">
        <div className="w-16 h-16 bg-paper-dark rounded-full mb-4" />
        <div className="h-4 w-1/2 bg-paper-dark mb-2" />
        <div className="h-4 w-1/3 bg-paper-dark" />
      </div>
    </div>
  );
}
