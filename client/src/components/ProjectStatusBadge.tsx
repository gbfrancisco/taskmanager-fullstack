/**
 * ProjectStatusBadge - Displays project status as a styled badge
 *
 * This component provides three size variants:
 * - 'default': Simple filled badge (original style)
 * - 'sm': Small rotated stamp for card views, border only
 * - 'lg': Large stamp with double border for detail pages
 *
 * Colors are centralized in @/constants/statusConfig for consistency.
 */

import type { ProjectStatus } from '@/types/api';
import { PROJECT_STATUS_CONFIG } from '@/constants/statusConfig';

interface ProjectStatusBadgeProps {
  status: ProjectStatus;
  size?: 'default' | 'sm' | 'lg';
}

/** Rotation styles per status for visual variety (large stamps) */
const LARGE_ROTATIONS: Record<ProjectStatus, string> = {
  PLANNING: 'rotate-[-10deg]',
  ACTIVE: 'rotate-[5deg]',
  ON_HOLD: 'rotate-[-5deg]',
  COMPLETED: 'rotate-[-10deg]',
  CANCELLED: 'rotate-[-10deg]'
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
  const config = PROJECT_STATUS_CONFIG[status];

  return (
    <span
      className={`px-2 py-1 text-xs text-ink border-2 border-ink shadow-comic-sm ${config.bg}`}
    >
      {config.label}
    </span>
  );
}

/**
 * SmallStamp - Compact status stamp for project cards
 *
 * Uses border-only styling with colored text for a lighter appearance.
 */
function SmallStamp({ status }: { status: ProjectStatus }) {
  const config = PROJECT_STATUS_CONFIG[status];

  return (
    <span
      className={`
        text-[10px] font-black uppercase tracking-wider border-2 px-1 rotate-[-2deg]
        ${config.text} ${config.border}
      `}
    >
      {config.label}
    </span>
  );
}

/**
 * LargeStamp - Prominent status stamp for project detail pages
 *
 * Uses double border with varied rotation per status for visual interest.
 */
function LargeStamp({ status }: { status: ProjectStatus }) {
  const config = PROJECT_STATUS_CONFIG[status];

  return (
    <div
      className={`
        border-4 border-double px-2 py-1 uppercase font-black tracking-widest text-lg opacity-80
        ${config.text} ${config.border} ${LARGE_ROTATIONS[status]}
      `}
    >
      {config.label}
    </div>
  );
}
