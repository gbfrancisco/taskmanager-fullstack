import type { ProjectStatus } from '@/types/api';

export function ProjectStatusBadge({ status }: { status: ProjectStatus }) {
  const styles: Record<ProjectStatus, string> = {
    PLANNING: 'bg-status-planning',
    ACTIVE: 'bg-status-progress',
    ON_HOLD: 'bg-status-on-hold',
    COMPLETED: 'bg-status-complete',
    CANCELLED: 'bg-status-cancelled'
  };

  const labels: Record<ProjectStatus, string> = {
    PLANNING: 'Planning',
    ACTIVE: 'Active',
    ON_HOLD: 'On Hold',
    COMPLETED: 'Completed',
    CANCELLED: 'Cancelled'
  };

  return (
    <span
      className={`px-2 py-1 text-xs text-ink border-2 border-ink shadow-comic-sm ${styles[status]}`}
    >
      {labels[status]}
    </span>
  );
}
