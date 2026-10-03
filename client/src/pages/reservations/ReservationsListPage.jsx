/*
 * File:        ReservationsListPage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages / Reservations
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-22
 * Description: Main power trading reservation management console for Backoffice
 *              officers and Grid Operators. Lists bookings with search, status,
 *              and date filters, enforces the 7-day and 12-hour rules visually,
 *              and provides inspection, modification, approval, and cancellation workflows.
 */

import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import EmptyState from '../../components/EmptyState';
import LoadingSpinner from '../../components/LoadingSpinner';
import PageHeader from '../../components/PageHeader';
import { ReservationStatusBadge, ReservationTypeBadge } from '../../components/Badges';
import ReservationDetailsModal from './ReservationDetailsModal';
import { useToast } from '../../toast/useToast';
import {
  cancelReservation,
  getReservations,
  getReservationStats,
  updateReservationStatus,
} from '../../api/reservationsApi';
import { RESERVATION_STATUS } from '../../utils/constants';
import { formatDate } from '../../utils/formatters';

const STATUS_TABS = [
  { key: 'all', label: 'All Bookings' },
  { key: RESERVATION_STATUS.APPROVED, label: 'Approved' },
  { key: RESERVATION_STATUS.PENDING, label: 'Pending Approval' },
  { key: RESERVATION_STATUS.COMPLETED, label: 'Completed' },
  { key: RESERVATION_STATUS.CANCELLED, label: 'Cancelled' },
];

export default function ReservationsListPage() {
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();

  const [reservations, setReservations] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  // Filters
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('all');
  const [selectedDate, setSelectedDate] = useState('');

  // Modals state
  const [viewingReservation, setViewingReservation] = useState(null);
  const [cancellingReservation, setCancellingReservation] = useState(null);
  const [cancelReason, setCancelReason] = useState('');
  const [actionBusy, setActionBusy] = useState(false);

  /**
   * Fetches the reservation list and statistics from the API.
   */
  const loadData = useCallback(async () => {
    setLoading(true);

    try {
      const fromDate = selectedDate ? new Date(`${selectedDate}T00:00:00Z`).toISOString() : undefined;
      const toDate = selectedDate ? new Date(`${selectedDate}T23:59:59Z`).toISOString() : undefined;

      const [resData, statsData] = await Promise.all([
        getReservations({ status, search, fromDate, toDate }),
        getReservationStats(),
      ]);

      setReservations(resData);
      setStats(statsData);
    } catch (err) {
      showError(err.message || 'Failed to load reservations.');
    } finally {
      setLoading(false);
    }
  }, [status, search, selectedDate, showError]);

  useEffect(() => {
    const timer = setTimeout(loadData, 250);
    return () => clearTimeout(timer);
  }, [loadData]);

  /**
   * Approves a pending reservation.
   */
  async function handleApprove(res) {
    setActionBusy(true);
    try {
      await updateReservationStatus(res.id, { status: RESERVATION_STATUS.APPROVED });
      showSuccess(`Reservation ${res.reservationNumber} approved successfully.`);
      setViewingReservation(null);
      await loadData();
    } catch (err) {
      showError(err.message);
    } finally {
      setActionBusy(false);
    }
  }

  /**
   * Confirms cancellation with a required reason.
   */
  async function handleConfirmCancel(e) {
    e.preventDefault();
    if (!cancellingReservation) return;

    if (!cancelReason.trim()) {
      showError('Please provide a reason for cancelling this reservation.');
      return;
    }

    setActionBusy(true);
    try {
      await cancelReservation(cancellingReservation.id, { reason: cancelReason.trim() });
      showSuccess(`Reservation ${cancellingReservation.reservationNumber} was cancelled.`);
      setCancellingReservation(null);
      setViewingReservation(null);
      setCancelReason('');
      await loadData();
    } catch (err) {
      showError(err.message);
    } finally {
      setActionBusy(false);
    }
  }

  return (
    <>
      <PageHeader
        title="Energy Slot Reservations"
        subtitle="Manage power trading schedules, slot allocations, and prosumer bookings"
        action={
          <Link to="/reservations/new" className="btn btn-primary btn-sm d-flex align-items-center gap-1">
            <span aria-hidden="true">+</span>
            <span>New Reservation</span>
          </Link>
        }
      />

      {/* Assignment Business Rules Alert */}
      <div className="alert alert-light border border-primary-subtle shadow-sm mb-4">
        <div className="d-flex align-items-center gap-2 mb-1">
          <span className="badge text-bg-primary">FAT Service Rules</span>
          <strong className="text-dark small">Automated Policy Enforcement:</strong>
        </div>
        <ul className="text-secondary small mb-0 ps-3">
          <li><strong>7-Day Booking Rule:</strong> Power trading slots can only be scheduled within 7 days from today.</li>
          <li><strong>12-Hour Notice Rule:</strong> Rescheduling, updates, and cancellations require at least 12 hours advance notice before the scheduled slot start time.</li>
        </ul>
      </div>

      {/* Live KPIs */}
      {stats && (
        <div className="row g-3 mb-4">
          <div className="col-6 col-md-3">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <span className="text-secondary small text-uppercase">Total Bookings</span>
                <h3 className="h4 fw-bold mt-1 mb-0">{stats.totalReservations}</h3>
              </div>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <span className="text-secondary small text-uppercase">Pending Approval</span>
                <h3 className="h4 fw-bold text-warning mt-1 mb-0">{stats.pendingReservations}</h3>
              </div>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <span className="text-secondary small text-uppercase">Approved Future</span>
                <h3 className="h4 fw-bold text-success mt-1 mb-0">{stats.approvedFutureReservations}</h3>
              </div>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <span className="text-secondary small text-uppercase">Today's Transfers</span>
                <h3 className="h4 fw-bold text-info mt-1 mb-0">{stats.todayReservations}</h3>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Filter Toolbar */}
      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body">
          <div className="row g-3 align-items-center">
            <div className="col-lg-5">
              <input
                type="search"
                className="form-control form-control-sm"
                placeholder="Search reference #, prosumer NIC, or microgrid hub…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            <div className="col-sm-6 col-lg-3">
              <input
                type="date"
                className="form-control form-control-sm"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
                title="Filter by slot date"
              />
            </div>
            {selectedDate && (
              <div className="col-auto">
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm"
                  onClick={() => setSelectedDate('')}
                >
                  Clear Date
                </button>
              </div>
            )}
          </div>

          <ul className="nav nav-pills mt-3 gap-1">
            {STATUS_TABS.map((tab) => (
              <li className="nav-item" key={tab.key}>
                <button
                  type="button"
                  className={`nav-link py-1 px-3 small ${status === tab.key ? 'active' : ''}`}
                  onClick={() => setStatus(tab.key)}
                >
                  {tab.label}
                </button>
              </li>
            ))}
          </ul>
        </div>
      </div>

      {/* Reservations Table */}
      {loading ? (
        <LoadingSpinner message="Loading energy slot reservations…" />
      ) : reservations.length === 0 ? (
        <EmptyState
          title="No reservations found"
          message="No power trading reservations match your filter criteria."
          action={
            <Link to="/reservations/new" className="btn btn-primary btn-sm">
              Create First Booking
            </Link>
          }
        />
      ) : (
        <div className="card border-0 shadow-sm">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light small text-secondary">
                <tr>
                  <th scope="col">Reference</th>
                  <th scope="col">Prosumer</th>
                  <th scope="col">Microgrid Hub</th>
                  <th scope="col">Slot Date & Time</th>
                  <th scope="col">Energy</th>
                  <th scope="col">Type</th>
                  <th scope="col">Status</th>
                  <th scope="col" className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody className="small">
                {reservations.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <button
                        type="button"
                        className="btn btn-link p-0 text-decoration-none fw-semibold small"
                        onClick={() => setViewingReservation(item)}
                      >
                        {item.reservationNumber}
                      </button>
                    </td>
                    <td>
                      <div className="fw-medium">{item.prosumerName || 'Prosumer'}</div>
                      <span className="text-secondary smaller">{item.prosumerNic}</span>
                    </td>
                    <td>
                      <div>{item.nodeName}</div>
                      <span className="text-secondary smaller">{item.nodeId}</span>
                    </td>
                    <td>
                      <div>{formatDate(item.slotStartTime)}</div>
                      <span className="text-secondary smaller">
                        {new Date(item.slotStartTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        {' - '}
                        {new Date(item.slotEndTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </td>
                    <td>
                      <span className="fw-medium">{item.energyAmountKWh}</span> <span className="text-secondary smaller">kWh</span>
                    </td>
                    <td>
                      <ReservationTypeBadge type={item.reservationType} />
                    </td>
                    <td>
                      <ReservationStatusBadge status={item.status} />
                    </td>
                    <td className="text-end">
                      <div className="btn-group btn-group-sm">
                        <button
                          type="button"
                          className="btn btn-outline-secondary"
                          title="View reservation details and QR code"
                          onClick={() => setViewingReservation(item)}
                        >
                          View
                        </button>

                        <button
                          type="button"
                          className="btn btn-outline-secondary"
                          title={
                            item.canModify
                              ? 'Edit slot details'
                              : 'Editing locked: requires at least 12 hours notice'
                          }
                          disabled={!item.canModify}
                          onClick={() => navigate(`/reservations/${item.id}/edit`)}
                        >
                          Edit
                        </button>

                        <button
                          type="button"
                          className="btn btn-outline-danger"
                          title={
                            item.canCancel
                              ? 'Cancel booking'
                              : 'Cancellation locked: requires at least 12 hours notice'
                          }
                          disabled={!item.canCancel}
                          onClick={() => setCancellingReservation(item)}
                        >
                          Cancel
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Details Modal */}
      {viewingReservation && (
        <ReservationDetailsModal
          reservation={viewingReservation}
          onClose={() => setViewingReservation(null)}
          onEdit={(res) => {
            setViewingReservation(null);
            navigate(`/reservations/${res.id}/edit`);
          }}
          onCancelClick={(res) => {
            setViewingReservation(null);
            setCancellingReservation(res);
          }}
          onApproveClick={handleApprove}
        />
      )}

      {/* Cancel Confirmation Dialog */}
      {cancellingReservation && (
        <>
          <div className="modal fade show d-block" tabIndex="-1" role="dialog">
            <div className="modal-dialog modal-dialog-centered">
              <div className="modal-content">
                <form onSubmit={handleConfirmCancel}>
                  <div className="modal-header">
                    <h5 className="modal-title text-danger">Cancel Reservation</h5>
                    <button
                      type="button"
                      className="btn-close"
                      aria-label="Close"
                      onClick={() => setCancellingReservation(null)}
                    />
                  </div>

                  <div className="modal-body">
                    <p className="small mb-2">
                      Are you sure you want to cancel reservation{' '}
                      <strong>{cancellingReservation.reservationNumber}</strong>?
                    </p>

                    <div className="alert alert-info py-2 small mb-3">
                      <strong>12-Hour Rule Check Passed:</strong> This slot is scheduled in{' '}
                      <strong>{cancellingReservation.hoursUntilSlot} hours</strong>, satisfying the
                      12-hour minimum notice requirement.
                    </div>

                    <div className="mb-3">
                      <label htmlFor="cancelReason" className="form-label small fw-semibold">
                        Cancellation Reason <span className="text-danger">*</span>
                      </label>
                      <textarea
                        id="cancelReason"
                        className="form-control form-control-sm"
                        rows="3"
                        placeholder="State why this power trading booking is being cancelled…"
                        value={cancelReason}
                        onChange={(e) => setCancelReason(e.target.value)}
                        required
                      />
                    </div>
                  </div>

                  <div className="modal-footer">
                    <button
                      type="button"
                      className="btn btn-outline-secondary btn-sm"
                      onClick={() => setCancellingReservation(null)}
                      disabled={actionBusy}
                    >
                      Keep Booking
                    </button>
                    <button
                      type="submit"
                      className="btn btn-danger btn-sm"
                      disabled={actionBusy}
                    >
                      {actionBusy ? 'Cancelling…' : 'Confirm Cancellation'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
          <div className="modal-backdrop fade show" />
        </>
      )}
    </>
  );
}
