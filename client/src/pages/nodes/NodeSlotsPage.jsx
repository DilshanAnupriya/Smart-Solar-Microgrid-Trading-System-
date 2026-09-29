/*
 * File:        NodeSlotsPage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages / Nodes
 * Author:      Vidura Hewaduwa
 * Created:     2026-09-24
 * Description: Grid Operator screen for updating live battery-slot availability
 *              without changing a node's configured total slot capacity.
 */

import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import ErrorAlert from '../../components/ErrorAlert';
import LoadingSpinner from '../../components/LoadingSpinner';
import PageHeader from '../../components/PageHeader';
import { getNodeById, updateNodeSlots } from '../../api/nodesApi';
import { useToast } from '../../toast/useToast';

export default function NodeSlotsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();
  const [node, setNode] = useState(null);
  const [available, setAvailable] = useState('');
  const [pageError, setPageError] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  /** Loads the node so the form knows the allowed total and current availability. */
  const loadNode = useCallback(async () => {
    try {
      const current = await getNodeById(id);
      setNode(current);
      setAvailable(String(current.availableBatterySlots));
    } catch (error) {
      showError(error.message);
      navigate('/nodes', { replace: true });
    } finally {
      setLoading(false);
    }
  }, [id, navigate, showError]);

  useEffect(() => { loadNode(); }, [loadNode]);

  /** Validates the whole-number range before calling the protected endpoint. */
  async function handleSubmit(event) {
    event.preventDefault();
    const count = Number(available);
    if (!Number.isInteger(count) || count < 0 || count > node.totalBatterySlots) {
      setPageError(`Enter a whole number from 0 to ${node.totalBatterySlots}.`);
      return;
    }

    setSubmitting(true);
    try {
      const updated = await updateNodeSlots(id, count);
      showSuccess(`${updated.nodeCode} now has ${updated.availableBatterySlots} available slots.`);
      navigate(`/nodes/${id}`, { replace: true });
    } catch (error) {
      setPageError(error.message);
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <LoadingSpinner message="Loading battery slots…" />;

  return (
    <>
      <PageHeader title="Update battery slots" subtitle={`${node.name} · ${node.nodeCode}`} />
      <ErrorAlert message={pageError} onDismiss={() => setPageError('')} />
      <div className="card border-0 shadow-sm" style={{ maxWidth: '42rem' }}>
        <div className="card-body p-4">
          <div className="alert alert-info">
            This node has <strong>{node.totalBatterySlots}</strong> total battery slots.
          </div>
          <form onSubmit={handleSubmit}>
            <label htmlFor="availableSlots" className="form-label">Currently available slots</label>
            <input
              id="availableSlots"
              type="number"
              min="0"
              max={node.totalBatterySlots}
              step="1"
              className="form-control"
              value={available}
              onChange={(event) => { setAvailable(event.target.value); setPageError(''); }}
              disabled={submitting}
            />
            <div className="d-flex justify-content-end gap-2 mt-4">
              <button type="button" className="btn btn-outline-secondary" onClick={() => navigate(-1)} disabled={submitting}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting && <span className="spinner-border spinner-border-sm me-2" />}
                Update slots
              </button>
            </div>
          </form>
        </div>
      </div>
    </>
  );
}
