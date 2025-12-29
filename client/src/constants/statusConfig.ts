/**
 * Status Configuration - Single source of truth for status colors and labels
 *
 * This file centralizes all status-related styling to ensure consistency
 * across the application. Colors follow JIRA-style conventions:
 * - Grey: Not started (TODO, PLANNING)
 * - Blue: Active work (IN_PROGRESS, ACTIVE)
 * - Green: Completed
 * - Orange: On Hold
 * - Red: Cancelled
 */

import type { TaskStatus, ProjectStatus } from '@/types/api';

// =============================================================================
// TYPES
// =============================================================================

/**
 * Color configuration for a status
 *
 * Each status has background, text, and border color classes
 * that can be used in different contexts (badges, cards, etc.)
 */
export interface StatusColorConfig {
  /** Background color class (e.g., 'bg-status-grey') */
  bg: string;
  /** Text color class (e.g., 'text-status-grey') */
  text: string;
  /** Border color class (e.g., 'border-status-grey') */
  border: string;
}

/**
 * Complete status configuration including colors and label
 */
export interface StatusConfig extends StatusColorConfig {
  /** Human-readable label (e.g., 'To Do', 'In Progress') */
  label: string;
}

// =============================================================================
// TASK STATUS CONFIGURATION
// =============================================================================

export const TASK_STATUS_CONFIG: Record<TaskStatus, StatusConfig> = {
  TODO: {
    bg: 'bg-status-grey',
    text: 'text-ink-light',     // darker for outline visibility
    border: 'border-ink-light', // darker for outline visibility
    label: 'To Do'
  },
  IN_PROGRESS: {
    bg: 'bg-status-blue',
    text: 'text-status-blue',
    border: 'border-status-blue',
    label: 'In Progress'
  },
  COMPLETED: {
    bg: 'bg-status-green',
    text: 'text-status-green',
    border: 'border-status-green',
    label: 'Completed'
  },
  CANCELLED: {
    bg: 'bg-status-red',
    text: 'text-status-red',
    border: 'border-status-red',
    label: 'Cancelled'
  }
} as const;

// =============================================================================
// PROJECT STATUS CONFIGURATION
// =============================================================================

export const PROJECT_STATUS_CONFIG: Record<ProjectStatus, StatusConfig> = {
  PLANNING: {
    bg: 'bg-status-grey',
    text: 'text-ink-light',     // darker for outline visibility
    border: 'border-ink-light', // darker for outline visibility
    label: 'Planning'
  },
  ACTIVE: {
    bg: 'bg-status-blue',
    text: 'text-status-blue',
    border: 'border-status-blue',
    label: 'Active'
  },
  ON_HOLD: {
    bg: 'bg-status-orange',
    text: 'text-status-orange',
    border: 'border-status-orange',
    label: 'On Hold'
  },
  COMPLETED: {
    bg: 'bg-status-green',
    text: 'text-status-green',
    border: 'border-status-green',
    label: 'Done'
  },
  CANCELLED: {
    bg: 'bg-status-red',
    text: 'text-status-red',
    border: 'border-status-red',
    label: 'Cancelled'
  }
} as const;

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

/**
 * Get the color config for a task status
 */
export function getTaskStatusConfig(status: TaskStatus): StatusConfig {
  return TASK_STATUS_CONFIG[status];
}

/**
 * Get the color config for a project status
 */
export function getProjectStatusConfig(status: ProjectStatus): StatusConfig {
  return PROJECT_STATUS_CONFIG[status];
}
