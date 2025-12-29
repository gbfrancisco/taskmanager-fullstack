/**
 * MetaItem - Displays a label/value pair for metadata sections
 *
 * Used in detail pages to show information like:
 * - Operative/Commander (user)
 * - Project assignment
 * - Dates (created, deadline)
 *
 * Supports two variants:
 * - 'sidebar': Semantic dt/dd, supports links, regular font (task detail)
 * - 'grid': Compact divs, monospace font, no links (project detail)
 */

import { Link } from '@tanstack/react-router';

interface MetaItemProps {
  label: string;
  value: string;
  link?: string;
  highlight?: boolean;
  variant?: 'sidebar' | 'grid';
}

export function MetaItem({
  label,
  value,
  link,
  highlight,
  variant = 'sidebar'
}: MetaItemProps) {
  if (variant === 'grid') {
    return (
      <div>
        <div className="text-[10px] uppercase font-bold text-ink-light">{label}</div>
        <div className={`font-mono text-sm font-bold truncate ${highlight ? 'text-amber-dark' : 'text-ink'}`}>
          {value}
        </div>
      </div>
    );
  }

  // Default: sidebar variant
  return (
    <div>
      <dt className="text-xs text-ink-light uppercase font-bold">{label}</dt>
      <dd className={`font-bold ${highlight ? 'text-amber-dark' : 'text-ink'}`}>
        {link ? (
          <Link to={link} className="underline decoration-wavy hover:text-amber-vivid">
            {value}
          </Link>
        ) : (
          value
        )}
      </dd>
    </div>
  );
}
