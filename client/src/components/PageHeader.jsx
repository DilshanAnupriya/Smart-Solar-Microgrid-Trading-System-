/*
 * File:        PageHeader.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Components
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Consistent page title block used at the top of every screen, so
 *              the application looks the same from page to page.
 */

export default function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
      <div>
        <h1 className="h3 fw-semibold mb-1">{title}</h1>
        {subtitle && <p className="text-secondary mb-0">{subtitle}</p>}
      </div>

      {/* Buttons such as "Add User" are passed in by the page */}
      {actions && <div className="d-flex gap-2">{actions}</div>}
    </div>
  );
}
