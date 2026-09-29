/*
 * File:        NodeDetailsPage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages / Nodes
 * Author:      Vidura Hewaduwa
 * Created:     2026-09-24
 * Description: Complete node profile with role-specific navigation and confirmed
 *              soft activation/deactivation actions.
 */

import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import ConfirmModal from '../../components/ConfirmModal';
import LoadingSpinner from '../../components/LoadingSpinner';
import PageHeader from '../../components/PageHeader';
import { NodeStatusBadge } from '../../components/Badges';
import { activateNode, deactivateNode, getNodeById } from '../../api/nodesApi';
import { useAuth } from '../../auth/useAuth';
import { useToast } from '../../toast/useToast';
import { ROLES } from '../../utils/constants';
import { formatDateTime } from '../../utils/formatters';

export default function NodeDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const { showSuccess, showError } = useToast();
  // Backoffice maintains definitions; Grid Operators maintain live slot counts
  const isBackoffice = hasRole([ROLES.BACKOFFICE]);
  const isGridOperator = hasRole([ROLES.GRID_OPERATOR]);
  const [node, setNode] = useState(null);
  const [loading, setLoading] = useState(true);
  const [pendingAction, setPendingAction] = useState(null);
  const [actionBusy, setActionBusy] = useState(false);

  /** Retrieves the latest node state so status and slot counts are current. */
  const loadNode = useCallback(async () => {
    try {
      setNode(await getNodeById(id));
    } catch (error) {
      showError(error.message);
      navigate('/nodes', { replace: true });
    } finally {
      setLoading(false);
    }
  }, [id, navigate, showError]);

  useEffect(() => { loadNode(); }, [loadNode]);

  /** Runs only after the confirmation modal accepts the status change. */
  async function handleStatusChange() {
    setActionBusy(true);
    try {
      const updated = pendingAction === 'deactivate' ? await deactivateNode(id) : await activateNode(id);
      setNode(updated);
      showSuccess(`Node ${updated.nodeCode} has been ${updated.isActive ? 'activated' : 'deactivated'}.`);
    } catch (error) {
      showError(error.message);
    } finally {
      setActionBusy(false);
      setPendingAction(null);
    }
  }

  if (loading) return <LoadingSpinner message="Loading node details…" />;
  if (!node) return null;

  return (
    <>
      <PageHeader
        title={node.name}
        subtitle={`Microgrid node · ${node.nodeCode}`}
        actions={
          <>
            <Link to="/nodes" className="btn btn-outline-secondary">Back to list</Link>
            {isBackoffice && <Link to={`/nodes/${id}/edit`} className="btn btn-outline-primary">Edit</Link>}
            {isBackoffice && <Link to={`/nodes/${id}/schedule`} className="btn btn-outline-primary">Schedule</Link>}
            {isGridOperator && <Link to={`/nodes/${id}/slots`} className="btn btn-primary">Update slots</Link>}
            {isBackoffice && (
              <button
                type="button"
                className={`btn btn-${node.isActive ? 'warning' : 'success'}`}
                onClick={() => setPendingAction(node.isActive ? 'deactivate' : 'activate')}
              >
                {node.isActive ? 'Deactivate' : 'Activate'}
              </button>
            )}
          </>
        }
      />

      <div className="card border-0 shadow-sm mb-3">
        <div className="card-body p-4">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h2 className="h6 text-uppercase text-secondary mb-0">Node information</h2>
            <NodeStatusBadge isActive={node.isActive} />
          </div>
          <div className="row g-3">
            <Detail label="Node code" value={node.nodeCode} />
            <Detail label="Address" value={node.address} />
            <Detail label="Coordinates" value={`${node.latitude}, ${node.longitude}`} />
            <Detail label="Generation capacity" value={`${node.generationCapacityKw} kW`} />
            <Detail label="Storage capacity" value={`${node.storageCapacityKWh} kWh`} />
            <Detail label="Battery slots" value={`${node.availableBatterySlots} available of ${node.totalBatterySlots}`} />
          </div>
        </div>
      </div>

      <div className="row g-3">
        <div className="col-lg-7">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4">
              <h2 className="h6 text-uppercase text-secondary mb-3">Operating schedule</h2>
              {node.operatingSchedule.length === 0 ? (
                <p className="text-secondary mb-0">No operating schedule has been configured.</p>
              ) : (
                <div className="table-responsive">
                  <table className="table table-sm align-middle mb-0">
                    <tbody>
                      {node.operatingSchedule.map((entry, index) => (
                        <tr key={`${entry.dayOfWeek}-${index}`}>
                          <th className="ps-0">{entry.dayOfWeek}</th>
                          <td className="text-end pe-0">
                            {entry.isClosed ? <span className="text-secondary">Closed</span> : `${entry.openingTime} – ${entry.closingTime}`}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </div>
        <div className="col-lg-5">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4">
              <h2 className="h6 text-uppercase text-secondary mb-3">Record history</h2>
              <Detail label="Created" value={formatDateTime(node.createdAt)} full />
              <Detail label="Last updated" value={formatDateTime(node.updatedAt)} full />
              <Detail label="Created by" value={node.createdBy || '—'} full />
              {!node.isActive && <Detail label="Deactivated" value={formatDateTime(node.deactivatedAt)} full />}
            </div>
          </div>
        </div>
      </div>

      <ConfirmModal
        show={Boolean(pendingAction)}
        busy={actionBusy}
        title={pendingAction === 'deactivate' ? 'Deactivate node' : 'Activate node'}
        message={pendingAction === 'deactivate'
          ? 'Deactivate this node? Nodes with Pending or Approved reservations cannot be deactivated.'
          : 'Activate this node and make it available for operations again?'}
        confirmLabel={pendingAction === 'deactivate' ? 'Deactivate' : 'Activate'}
        confirmVariant={pendingAction === 'deactivate' ? 'warning' : 'success'}
        onConfirm={handleStatusChange}
        onCancel={() => setPendingAction(null)}
      />
    </>
  );
}

function Detail({ label, value, full = false }) {
  // A small local component keeps every detail label/value pair visually consistent
  return (
    <div className={full ? 'mb-3' : 'col-md-6'}>
      <div className="small text-secondary">{label}</div>
      <div className="fw-medium">{value}</div>
    </div>
  );
}
