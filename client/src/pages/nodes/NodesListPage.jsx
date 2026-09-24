/*
 * File:        NodesListPage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages / Nodes
 * Author:      Vidura Hewaduwa
 * Created:     2026-09-24
 * Description: Staff node directory with server-side search and status filtering.
 *              Creation and editing controls are shown only to Backoffice users.
 */

import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import EmptyState from '../../components/EmptyState';
import LoadingSpinner from '../../components/LoadingSpinner';
import PageHeader from '../../components/PageHeader';
import { NodeStatusBadge } from '../../components/Badges';
import { useAuth } from '../../auth/useAuth';
import { useToast } from '../../toast/useToast';
import { getNodes } from '../../api/nodesApi';
import { ROLES } from '../../utils/constants';

export default function NodesListPage() {
  const { hasRole } = useAuth();
  const { showError } = useToast();
  const navigate = useNavigate();
  // UI visibility improves usability; the API remains the security boundary
  const isBackoffice = hasRole([ROLES.BACKOFFICE]);

  const [nodes, setNodes] = useState([]);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('all');
  const [loading, setLoading] = useState(true);

  /** Fetches the current filtered list from the API. */
  const loadNodes = useCallback(async () => {
    setLoading(true);
    try {
      setNodes(await getNodes({ search, isActive: status }));
    } catch (error) {
      showError(error.message);
    } finally {
      setLoading(false);
    }
  }, [search, status, showError]);

  useEffect(() => {
    // Debouncing prevents one network request for every search keystroke
    const timer = setTimeout(loadNodes, 300);
    return () => clearTimeout(timer);
  }, [loadNodes]);

  return (
    <>
      <PageHeader
        title="Microgrid nodes"
        subtitle="Manage locations, capacities, schedules and battery availability"
        actions={isBackoffice && <Link to="/nodes/new" className="btn btn-primary">Add node</Link>}
      />

      <div className="card border-0 shadow-sm">
        <div className="card-body">
          <div className="row g-2 mb-3">
            <div className="col-md-8">
              <label htmlFor="nodeSearch" className="form-label small text-secondary">Search</label>
              <input
                id="nodeSearch"
                type="search"
                className="form-control"
                placeholder="Search by code, name or address"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
            </div>
            <div className="col-md-4">
              <label htmlFor="nodeStatus" className="form-label small text-secondary">Status</label>
              <select
                id="nodeStatus"
                className="form-select"
                value={status}
                onChange={(event) => setStatus(event.target.value)}
              >
                <option value="all">All statuses</option>
                <option value="true">Active</option>
                <option value="false">Inactive</option>
              </select>
            </div>
          </div>

          {loading ? (
            <LoadingSpinner message="Loading microgrid nodes…" />
          ) : nodes.length === 0 ? (
            <EmptyState
              title="No nodes found"
              description="Clear the filters or create the first microgrid node."
              action={isBackoffice && <Link to="/nodes/new" className="btn btn-sm btn-primary">Add node</Link>}
            />
          ) : (
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0">
                <thead>
                  <tr className="text-secondary small text-uppercase text-nowrap">
                    <th>Code</th>
                    <th>Name</th>
                    <th>Generation</th>
                    <th>Storage</th>
                    <th>Available slots</th>
                    <th>Status</th>
                    <th className="text-end">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {nodes.map((node) => (
                    <tr key={node.id}>
                      <td className="fw-semibold text-nowrap">{node.nodeCode}</td>
                      <td>
                        <div>{node.name}</div>
                        <small className="text-secondary">{node.address}</small>
                      </td>
                      <td className="text-nowrap">{node.generationCapacityKw} kW</td>
                      <td className="text-nowrap">{node.storageCapacityKWh} kWh</td>
                      <td>{node.availableBatterySlots} / {node.totalBatterySlots}</td>
                      <td><NodeStatusBadge isActive={node.isActive} /></td>
                      <td className="text-end">
                        <div className="btn-group btn-group-sm">
                          <button className="btn btn-outline-primary" onClick={() => navigate(`/nodes/${node.id}`)}>
                            View
                          </button>
                          {isBackoffice && (
                            <button className="btn btn-outline-secondary" onClick={() => navigate(`/nodes/${node.id}/edit`)}>
                              Edit
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
