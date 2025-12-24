/**
 * Format an ISO date string for display
 *
 * Shows time only if it's not midnight (00:00).
 * This matches our form behavior where users can optionally include time.
 */
export function formatDate(isoString: string, showMidnight = false): string {
  const date = new Date(isoString);

  const dateStr = date.toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  });

  // Check if time is midnight (meaning no specific time was set)
  if (!showMidnight) {
    const hours = date.getHours();
    const minutes = date.getMinutes();
    if (hours === 0 && minutes === 0) {
      return dateStr;
    }
  }

  // Include time if it was explicitly set
  const timeStr = date.toLocaleTimeString('en-US', {
    hour: 'numeric',
    minute: '2-digit'
  });

  return `${dateStr} at ${timeStr}`;
}
