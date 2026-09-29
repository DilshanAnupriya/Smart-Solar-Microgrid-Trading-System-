/*
 * File:        ProsumerListPage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages / Prosumers
 * Author:      B.D.A.Cooray (IT22189530)
 * Created:     2026-09-22
 * Description: Staff screen listing every solar prosumer, with a search box and
 *              a status filter. Both filters are applied by the Web API through
 *              GET /api/prosumers?search=&status= rather than in the browser,
 *              so the table always shows what the database actually holds. Each
 *              row links to the profile page, where the deactivate and
 *              reactivate actions live. The API answers this route only for the
 *              Backoffice and Grid Operator roles.
 */

import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import EmptyState from '../../components/EmptyState';
import LoadingSpinner from '../../components/LoadingSpinner';
import PageHeader from '../../components/PageHeader';
import { ProsumerStatusBadge } from '../../components/Badges';
import { useToast } from '../../toast/useToast';
import { getProsumers } from '../../api/prosumerApi';
import { PROSUMER_STATUS_OPTIONS } from '../../utils/constants';
import { formatDate } from '../../utils/formatters';

export default function ProsumerListPage() {
  const { showError } = useToast();
  const navigate = useNavigate();

  const [prosumers, setProsumers] = useState([]);
  const [loading, setLoading] = useState(true);

  // Filter state; "all" is a value used only by the dropdown, never sent
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('all');

  /**
   * Fetches the prosumer list using the current search term and status.
   */
  const loadProsumers = useCallback(async () => {
    // Runs on first render and again whenever either filter changes
    setLoading(true);

    try {
      const data = await getProsumers({ search, status });
      setProsumers(data);
    } catch (fetchError) {
      showError(fetchError.message);
    } finally {
      setLoading(false);
    }
  }, [search, status, showError]);

  // Debounced so typing in the search box does not fire a request per keystroke
  useEffect(() => {
    const timer = setTimeout(loadProsumers, 300);

    return () => clearTimeout(timer);
  }, [loadProsumers]);

  /**
   * Clears both filters and lets the effect above reload the full list.
   */
  function handleClearFilters() {
    // Shown on the empty state so a too-narrow search is easy to undo
    setSearch('');
    setStatus('all');
  }

  return (
    <>
      <PageHeader
        title="Prosumer management"
        subtitle="Search, review and maintain solar prosumer accounts"
        actions={
          <Link to="/prosumers/new" className="btn btn-primary">
            Add prosumer
          </Link>
        }
      />

      <div className="card border-0 shadow-sm">
        <div className="card-body">
          <div className="row g-2 mb-3">
            <div className="col-md-8">
              <label htmlFor="search" className="form-label small text-secondary">Search</label>
              <input
                id="search"
                type="search"
                className="form-control"
                placeholder="Search by NIC, name, email or phone"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
            </div>

            <div className="col-md-4">
              <label htmlFor="statusFilter" className="form-label small text-secondary">Status</label>
              <select
                id="statusFilter"
                className="form-select"
                value={status}
                onChange={(event) => setStatus(event.target.value)}
              >
                <option value="all">All statuses</option>
                {PROSUMER_STATUS_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </div>
          </div>

          {loading ? (
            <LoadingSpinner message="Loading prosumers…" />
          ) : prosumers.length === 0 ? (
            <EmptyState
              title="No prosumers found"
              description="Try clearing the filters, or register the first prosumer."
              action={
                <div className="d-flex gap-2 justify-content-center">
                  <button type="button" className="btn btn-sm btn-outline-secondary" onClick={handleClearFilters}>
                    Clear filters
                  </button>
                  <Link to="/prosumers/new" className="btn btn-sm btn-primary">Add prosumer</Link>
                </div>
              }
            />
          ) : (
            <>
              {/* A short count so staff can see how far a search narrowed the list */}
              <p className="text-secondary small mb-2">
                {prosumers.length} prosumer{prosumers.length === 1 ? '' : 's'} found
              </p>

              <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">
                  <thead>
                    {/* text-nowrap keeps the headings on one line; the wrapper
                        scrolls sideways on a narrow screen */}
                    <tr className="text-secondary small text-uppercase text-nowrap">
                      <th scope="col">NIC</th>
                      <th scope="col">Full name</th>
                      <th scope="col">Email</th>
                      <th scope="col">Phone</th>
                      <th scope="col">Status</th>
                      <th scope="col">Registered</th>
                      <th scope="col" className="text-end">Actions</th>
                    </tr>
                  </thead>

                  <tbody>
                    {prosumers.map((item) => (
                      // The NIC is the primary key, so it is also the row key
                      <tr key={item.nic}>
                        <td className="fw-semibold text-nowrap">{item.nic}</td>
                        <td>{item.fullName}</td>
                        <td className="text-secondary">{item.email}</td>
                        <td className="text-secondary text-nowrap">{item.phone}</td>
                        <td><ProsumerStatusBadge status={item.status} /></td>
                        <td className="text-secondary small text-nowrap">{formatDate(item.createdAt)}</td>
                        <td className="text-end">
                          <div className="btn-group btn-group-sm">
                            {/* Deactivate and reactivate live on the profile
                                page, so the row only needs these two links */}
                            <button
                              type="button"
                              className="btn btn-outline-primary"
                              onClick={() => navigate(`/prosumers/${item.nic}`)}
                            >
                              View
                            </button>

                            <button
                              type="button"
                              className="btn btn-outline-secondary"
                              onClick={() => navigate(`/prosumers/${item.nic}/edit`)}
                            >
                              Edit
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>
      </div>
    </>
  );
}
