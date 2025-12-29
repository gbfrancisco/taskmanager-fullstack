/**
 * TaskStatusBadge - Displays task status as a styled badge
 *
 * This component provides two size variants:
 * - 'sm' (default): Small rotated stamp for card views, filled background
 * - 'lg': Large stamp with double border for detail pages
 *
 * The visual style differs between sizes:
 * - Small: Filled background colors, compact, slight rotation
 * - Large: Transparent background, colored double border, more prominent rotation
 */

import type { TaskStatus } from '@/types/api';

interface TaskStatusBadgeProps {
  status: TaskStatus;
  size?: 'sm' | 'lg';
}

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
  const configs: Record<TaskStatus, string> = {
    TODO: 'bg-paper text-ink border-ink rotate-[-2deg]',
    IN_PROGRESS: 'bg-status-planning text-ink border-ink rotate-1',
    COMPLETED: 'bg-status-complete text-ink border-ink rotate-[-2deg]',
    CANCELLED: 'bg-status-cancelled text-ink border-ink rotate-1'
  };

  return (
    <span
      className={`
        px-2 py-0.5 text-[10px] font-black uppercase tracking-wider border-2 shadow-sm
        ${configs[status]}
      `}
    >
      {status.replace('_', ' ')}
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
  const colors: Record<TaskStatus, string> = {
    TODO: 'text-ink-light border-ink-light',
    IN_PROGRESS: 'text-status-progress border-status-progress',
    COMPLETED: 'text-success border-success',
    CANCELLED: 'text-danger border-danger'
  };

  return (
    <div className={`border-4 border-double p-2 rotate-[-5deg] opacity-90 ${colors[status]}`}>
      <span className="text-xl font-black uppercase tracking-widest">
        {status.replace('_', ' ')}
      </span>
    </div>
  );
}
