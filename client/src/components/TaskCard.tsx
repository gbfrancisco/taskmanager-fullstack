/**
 * TaskCard - Displays a single task in a grid/list view
 *
 * Features:
 * - Links to task detail page
 * - Shows status badge, title, description preview
 * - Displays project assignment and due date
 * - Hover effects for interactivity
 */

import { Link } from '@tanstack/react-router';
import { TaskStatusBadge } from '@/components/TaskStatusBadge';
import type { Task } from '@/types/api';

interface TaskCardProps {
  task: Task;
}

export function TaskCard({ task }: TaskCardProps) {
  return (
    <Link
      to="/tasks/$taskId"
      params={{ taskId: String(task.id) }}
      className="group block bg-paper border-comic shadow-comic-soft-interactive hover:bg-white transition-all h-full flex flex-col"
    >
      {/* Top accent bar - changes color on hover */}
      <div className="h-2 bg-ink w-full group-hover:bg-amber-vivid transition-colors" />

      <div className="p-5 flex-1 flex flex-col">
        {/* ID and Status row */}
        <div className="flex justify-between items-start mb-3">
          <span className="font-mono text-xs text-ink-light border border-ink-light px-1">
            #{task.id.toString().padStart(4, '0')}
          </span>
          <TaskStatusBadge status={task.status} />
        </div>

        {/* Title */}
        <h3 className="text-display text-xl leading-tight mb-2 group-hover:underline decoration-2 underline-offset-2">
          {task.title}
        </h3>

        {/* Description (truncated) */}
        {task.description && (
          <p className="text-sm text-ink-soft line-clamp-2 mb-4 flex-1 font-medium">
            {task.description}
          </p>
        )}

        {/* Footer - Project and Due Date */}
        <div className="mt-4 pt-3 border-t-2 border-dashed border-ink-light/30 flex items-center justify-between text-xs font-bold text-ink-light uppercase">
          <div>
            {task.project ? (
              <span className="text-amber-dark">📂 {task.project.name}</span>
            ) : (
              <span>📂 Unclassified</span>
            )}
          </div>
          {task.dueDate && (
            <div className={new Date(task.dueDate) < new Date() ? 'text-danger' : ''}>
              ⏱ {formatDateShort(task.dueDate)}
            </div>
          )}
        </div>
      </div>
    </Link>
  );
}

/**
 * Format date for compact card display
 *
 * Shows abbreviated month and day only (e.g., "Jan 15")
 */
function formatDateShort(isoString: string): string {
  const date = new Date(isoString);
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}
