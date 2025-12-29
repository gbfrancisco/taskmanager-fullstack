/**
 * ProjectCard - Displays a single project in a grid/list view
 *
 * Features:
 * - Links to project detail page
 * - Visual "spine" on left side colored by status
 * - Shows status badge, title, description preview
 * - Displays task count
 * - Hover effects for interactivity
 */

import { Link } from '@tanstack/react-router';
import { ProjectStatusBadge } from '@/components/ProjectStatusBadge';
import { PROJECT_STATUS_CONFIG } from '@/constants/statusConfig';
import type { Project } from '@/types/api';

interface ProjectCardProps {
  project: Project;
}

export function ProjectCard({ project }: ProjectCardProps) {
  const config = PROJECT_STATUS_CONFIG[project.status];
  const spineColor = config?.bg || 'bg-ink';

  return (
    <Link
      to="/projects/$projectId"
      params={{ projectId: String(project.id) }}
      className="group block bg-paper border-comic shadow-comic-soft-interactive h-full relative overflow-hidden"
    >
      {/* Visual Spine (Left side strip) */}
      <div className={`absolute left-0 top-0 bottom-0 w-3 border-r-2 border-ink ${spineColor}`} />

      <div className="p-5 pl-8 flex flex-col h-full">
        {/* ID and Status row */}
        <div className="flex justify-between items-start mb-2">
          <span className="font-mono text-[10px] font-bold uppercase text-ink-light tracking-widest">
            Arc #{project.id.toString().padStart(3, '0')}
          </span>
          <ProjectStatusBadge status={project.status} size="sm" />
        </div>

        {/* Title */}
        <h3 className="text-display text-2xl leading-tight mb-2 group-hover:text-amber-dark transition-colors">
          {project.name}
        </h3>

        {/* Description (truncated) */}
        <div className="flex-1 mb-6">
          {project.description ? (
            <p className="text-sm text-ink-soft line-clamp-3">
              {project.description}
            </p>
          ) : (
            <p className="text-sm text-ink-light italic">No description available.</p>
          )}
        </div>

        {/* Footer - Task count */}
        <div className="border-t-2 border-dashed border-ink/20 pt-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold uppercase text-ink-light">Tasks:</span>
            <span className="bg-ink text-paper text-xs font-mono px-1.5 py-0.5 rounded-sm">
              {project.taskCount || 0}
            </span>
          </div>

          <span className="text-xs font-bold text-amber-vivid group-hover:translate-x-1 transition-transform">
            Open File &rarr;
          </span>
        </div>
      </div>
    </Link>
  );
}
