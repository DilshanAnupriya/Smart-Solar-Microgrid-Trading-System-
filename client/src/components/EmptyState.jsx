/*
 * File:        EmptyState.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Components
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Placeholder shown when a list has no rows, so the user sees a
 *              clear explanation instead of an empty table.
 */

import Icon from './Icon';

export default function EmptyState({ title, description, action }) {
  return (
    <div className="text-center py-5">
      <span className="empty-state-icon mb-3">
        <Icon name="search" size={24} />
      </span>
      <h6 className="fw-semibold mb-1">{title}</h6>
      {description && <p className="text-secondary small mb-3">{description}</p>}
      {action}
    </div>
  );
}
