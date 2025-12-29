/**
 * TaskStrip - Compact task item for sidebar lists
 *
 * Used in project detail pages to show tasks belonging to the project.
 * Features:
 * - Checkbox visual indicating completion status
 * - Task title with strikethrough for completed/cancelled
 * - Mini ID badge
 * - Links to task detail page
 */

import { Link } from '@tanstack/react-router';
import type { Task } from '@/types/api';

interface TaskStripProps {
  task: Task;
}

export function TaskStrip({ task }: TaskStripProps) {
  const isComplete = task.status === 'COMPLETED';
  const isCancelled = task.status === 'CANCELLED';

  return (
    <Link
      to="/tasks/$taskId"
      params={{ taskId: String(task.id) }}
      className={`
        group block bg-paper border-comic p-3 shadow-comic-soft-interactive
        ${isComplete ? 'opacity-70' : ''}
      `}
    >
      <div className="flex items-center gap-3">
        {/* Checkbox Visual */}
        <div className={`
          w-5 h-5 border-2 border-ink flex items-center justify-center
          ${isComplete ? 'bg-success' : isCancelled ? 'bg-danger' : 'bg-paper'}
        `}>
          {isComplete && <span className="text-paper text-xs font-black">✓</span>}
          {isCancelled && <span className="text-paper text-xs font-black">✕</span>}
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0">
          <div className={`text-sm font-bold truncate ${isComplete || isCancelled ? 'line-through text-ink-light' : 'text-ink'}`}>
            {task.title}
          </div>
        </div>

        {/* Mini Badge */}
        <span className="text-[10px] font-mono border border-ink px-1 bg-paper-dark">
          #{task.id}
        </span>
      </div>
    </Link>
  );
}
