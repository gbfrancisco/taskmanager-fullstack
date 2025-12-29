/**
 * ProjectStatusBadge - Displays project status as a styled badge
 *
 * This component provides three size variants:
 * - 'default': Simple filled badge (original style)
 * - 'sm': Small rotated stamp for card views, border only
 * - 'lg': Large stamp with double border for detail pages
 *
 * The visual style differs between sizes:
 * - Default: Filled background, no rotation
 * - Small: Border only, colored text, slight rotation
 * - Large: Double border, more prominent, varied rotations per status
 */

import type { ProjectStatus } from '@/types/api';

interface ProjectStatusBadgeProps {
  status: ProjectStatus;
  size?: 'default' | 'sm' | 'lg';
}

const labels: Record<ProjectStatus, string> = {
  PLANNING: 'Planning',
  ACTIVE: 'Active',
  ON_HOLD: 'On Hold',
  COMPLETED: 'Done',
  CANCELLED: 'Cancelled'
};

export function ProjectStatusBadge({ status, size = 'default' }: ProjectStatusBadgeProps) {
  if (size === 'lg') {
    return <LargeStamp status={status} />;
  }
  if (size === 'sm') {
    return <SmallStamp status={status} />;
  }
  return <DefaultBadge status={status} />;
}

/**
 * DefaultBadge - Simple filled badge (original implementation)
 */
function DefaultBadge({ status }: { status: ProjectStatus }) {
  const styles: Record<ProjectStatus, string> = {
    PLANNING: 'bg-status-planning',
    ACTIVE: 'bg-status-progress',
    ON_HOLD: 'bg-status-on-hold',
    COMPLETED: 'bg-status-complete',
    CANCELLED: 'bg-status-cancelled'
  };

  return (
    <span
      className={`px-2 py-1 text-xs text-ink border-2 border-ink shadow-comic-sm ${styles[status]}`}
    >
      {labels[status]}
    </span>
  );
}

/**
 * SmallStamp - Compact status stamp for project cards
 *
 * Uses border-only styling with colored text for a lighter appearance.
 */
function SmallStamp({ status }: { status: ProjectStatus }) {
  const styles: Record<ProjectStatus, string> = {
    PLANNING: 'text-ink-light border-ink-light',
    ACTIVE: 'text-success border-success',
    ON_HOLD: 'text-status-on-hold border-status-on-hold',
    COMPLETED: 'text-status-progress border-status-progress',
    CANCELLED: 'text-danger border-danger'
  };

  return (
    <span
      className={`
        text-[10px] font-black uppercase tracking-wider border-2 px-1 rotate-[-2deg]
        ${styles[status] || 'text-ink border-ink'}
      `}
    >
      {labels[status] || status}
    </span>
  );
}

/**
 * LargeStamp - Prominent status stamp for project detail pages
 *
 * Uses double border with varied rotation per status for visual interest.
 */
function LargeStamp({ status }: { status: ProjectStatus }) {
  const styles: Record<ProjectStatus, string> = {
    PLANNING: 'text-ink-light border-ink-light rotate-[-10deg]',
    ACTIVE: 'text-success border-success rotate-[5deg]',
    ON_HOLD: 'text-status-on-hold border-status-on-hold rotate-[-5deg]',
    COMPLETED: 'text-status-progress border-status-progress rotate-[-10deg]',
    CANCELLED: 'text-danger border-danger rotate-[-10deg]'
  };

  return (
    <div
      className={`
        border-4 border-double px-2 py-1 uppercase font-black tracking-widest text-lg opacity-80
        ${styles[status] || 'text-ink border-ink'}
      `}
    >
      {status.replace('_', ' ')}
    </div>
  );
}
