/**
 * TaskStatusBadge - Displays task status as a styled badge
 *
 * This component provides two size variants:
 * - 'sm' (default): Small rotated stamp for card views, filled background
 * - 'lg': Large stamp with double border for detail pages
 *
 * Colors are centralized in @/constants/statusConfig for consistency.
 */

import type { TaskStatus } from '@/types/api';
import { TASK_STATUS_CONFIG } from '@/constants/statusConfig';

interface TaskStatusBadgeProps {
  status: TaskStatus;
  size?: 'sm' | 'lg';
}

/** Rotation styles per status for visual variety */
const ROTATIONS: Record<TaskStatus, string> = {
  TODO: 'rotate-[-2deg]',
  IN_PROGRESS: 'rotate-1',
  COMPLETED: 'rotate-[-2deg]',
  CANCELLED: 'rotate-1'
};

export function TaskStatusBadge({ status, size = 'sm' }: TaskStatusBadgeProps) {
  if (size === 'lg') {
    return <LargeStamp status={status} />;
  }
  return <SmallStamp status={status} />;
}

/**
 * SmallStamp - Compact status badge for task cards
 *
 * Features filled background colors and slight rotation for a "stamped" effect.
 */
function SmallStamp({ status }: { status: TaskStatus }) {
  const config = TASK_STATUS_CONFIG[status];

  return (
    <span
      className={`
        px-2 py-0.5 text-[10px] font-black uppercase tracking-wider border-2 shadow-sm
        ${config.bg} text-ink border-ink ${ROTATIONS[status]}
      `}
    >
      {config.label}
    </span>
  );
}

/**
 * LargeStamp - Prominent status badge for task detail pages
 *
 * Uses a double border with colored outline for a "stamped document" effect.
 * No background fill - just the border and text in matching colors.
 */
function LargeStamp({ status }: { status: TaskStatus }) {
  const config = TASK_STATUS_CONFIG[status];

  return (
    <div className={`border-4 border-double p-2 rotate-[-5deg] opacity-90 ${config.text} ${config.border}`}>
      <span className="text-xl font-black uppercase tracking-widest">
        {config.label}
      </span>
    </div>
  );
}
