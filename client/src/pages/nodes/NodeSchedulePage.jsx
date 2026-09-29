/*
 * File:        NodeSchedulePage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages / Nodes
 * Author:      Vidura Hewaduwa
 * Created:     2026-09-24
 * Description: Backoffice editor for each node's seven-day operating schedule.
 *              Closed days are saved without opening or closing times.
 */

import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import ErrorAlert from '../../components/ErrorAlert';
import LoadingSpinner from '../../components/LoadingSpinner';
import PageHeader from '../../components/PageHeader';
import { getNodeById, updateNodeSchedule } from '../../api/nodesApi';
import { useToast } from '../../toast/useToast';

const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

function emptySchedule() {
  // Sensible defaults make a first-time schedule quick to complete
  return DAYS.map((dayOfWeek) => ({ dayOfWeek, openingTime: '08:00', closingTime: '17:00', isClosed: false }));
}

export default function NodeSchedulePage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();
  const [node, setNode] = useState(null);
  const [schedule, setSchedule] = useState(emptySchedule);
  const [pageError, setPageError] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  /** Loads saved entries and fills any missing weekday as closed. */
  const loadNode = useCallback(async () => {
    try {
      const current = await getNodeById(id);
      setNode(current);
      if (current.operatingSchedule.length) {
        setSchedule(DAYS.map((day) => {
          const entry = current.operatingSchedule.find((item) => item.dayOfWeek === day);
          return entry ?? { dayOfWeek: day, openingTime: '08:00', closingTime: '17:00', isClosed: true };
        }));
      }
    } catch (error) {
      showError(error.message);
      navigate('/nodes', { replace: true });
    } finally {
      setLoading(false);
    }
  }, [id, navigate, showError]);

  useEffect(() => { loadNode(); }, [loadNode]);

  /** Updates only one day without mutating the existing React state array. */
  function changeEntry(index, field, value) {
    setSchedule((current) => current.map((entry, entryIndex) => (
      entryIndex === index ? { ...entry, [field]: value } : entry
    )));
    setPageError('');
  }

  /** Performs the quick time-order check before the service validates again. */
  async function handleSubmit(event) {
    event.preventDefault();
    const invalid = schedule.find((entry) => !entry.isClosed && entry.openingTime >= entry.closingTime);
    if (invalid) {
      setPageError(`Opening time must be before closing time on ${invalid.dayOfWeek}.`);
      return;
    }

    setSubmitting(true);
    try {
      // Closed days deliberately send null times so the stored document is unambiguous
      await updateNodeSchedule(id, schedule.map((entry) => ({
        ...entry,
        openingTime: entry.isClosed ? null : entry.openingTime,
        closingTime: entry.isClosed ? null : entry.closingTime,
      })));
      showSuccess(`${node.nodeCode}'s operating schedule has been updated.`);
      navigate(`/nodes/${id}`, { replace: true });
    } catch (error) {
      setPageError(error.message);
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <LoadingSpinner message="Loading schedule…" />;

  return (
    <>
      <PageHeader title="Operating schedule" subtitle={`${node?.name} · ${node?.nodeCode}`} />
      <ErrorAlert message={pageError} onDismiss={() => setPageError('')} />
      <div className="card border-0 shadow-sm">
        <div className="card-body p-4">
          <form onSubmit={handleSubmit}>
            <div className="table-responsive">
              <table className="table align-middle">
                <thead>
                  <tr className="text-secondary small text-uppercase">
                    <th>Day</th><th>Opening</th><th>Closing</th><th className="text-center">Closed</th>
                  </tr>
                </thead>
                <tbody>
                  {schedule.map((entry, index) => (
                    <tr key={entry.dayOfWeek}>
                      <th>{entry.dayOfWeek}</th>
                      <td>
                        <input
                          type="time"
                          className="form-control"
                          value={entry.openingTime}
                          disabled={entry.isClosed || submitting}
                          onChange={(event) => changeEntry(index, 'openingTime', event.target.value)}
                        />
                      </td>
                      <td>
                        <input
                          type="time"
                          className="form-control"
                          value={entry.closingTime}
                          disabled={entry.isClosed || submitting}
                          onChange={(event) => changeEntry(index, 'closingTime', event.target.value)}
                        />
                      </td>
                      <td className="text-center">
                        <input
                          type="checkbox"
                          className="form-check-input"
                          checked={entry.isClosed}
                          disabled={submitting}
                          aria-label={`${entry.dayOfWeek} is closed`}
                          onChange={(event) => changeEntry(index, 'isClosed', event.target.checked)}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="d-flex justify-content-end gap-2 mt-3">
              <button type="button" className="btn btn-outline-secondary" onClick={() => navigate(-1)} disabled={submitting}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting && <span className="spinner-border spinner-border-sm me-2" />}
                Save schedule
              </button>
            </div>
          </form>
        </div>
      </div>
    </>
  );
}
