/**
 * MetadataList Component - Label-Value Pairs with Aligned Labels
 *
 * Uses CSS Grid to automatically align labels (colons line up).
 * The first column auto-sizes to the widest label, second column takes the rest.
 *
 * Usage:
 * ```tsx
 * <MetadataList>
 *   <MetadataItem label="Task ID">{task.id}</MetadataItem>
 *   <MetadataItem label="Owner">{task.appUser.username}</MetadataItem>
 *   <MetadataItem label="Project">
 *     <Link to="/projects/1">My Project</Link>
 *   </MetadataItem>
 * </MetadataList>
 * ```
 */

import type { ReactNode } from 'react';

interface MetadataListProps {
  children: ReactNode;
  className?: string;
}

/**
 * MetadataList - Container for label-value pairs
 *
 * Renders a <dl> with CSS Grid layout.
 * grid-cols-[auto_1fr] = first column sizes to widest content, second takes rest.
 */
export function MetadataList({ children, className = '' }: MetadataListProps) {
  return (
    <dl className={`grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 text-sm ${className}`}>
      {children}
    </dl>
  );
}

interface MetadataItemProps {
  label: string;
  children: ReactNode;
}

/**
 * MetadataItem - Single label-value row
 *
 * Renders <dt> (label) and <dd> (value) as a fragment.
 * Children can be any ReactNode: text, links, formatted dates, etc.
 */
export function MetadataItem({ label, children }: MetadataItemProps) {
  return (
    <>
      <dt className="text-ink-light">{label}:</dt>
      <dd className="text-ink">{children}</dd>
    </>
  );
}
