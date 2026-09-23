/*
 * File:        ReservationDetailsModal.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages / Reservations
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-22
 * Description: Modal displaying full details of an energy trading reservation,
 *              including prosumer info, time slot window, transaction QR code,
 *              and notice period status (enforcing the 12-hour rule).
 */

import { ReservationStatusBadge, ReservationTypeBadge } from '../../components/Badges';
import { formatDateTime } from '../../utils/formatters';

export default function ReservationDetailsModal({
  reservation,
  onClose,
  onEdit,
  onCancelClick,
  onApproveClick,
}) {
  if (!reservation) return null;

  const isPending = reservation.status === 'Pending';
  const isCancelled = reservation.status === 'Cancelled';
  const isCompleted = reservation.status === 'Completed';

  return (
    <>
      <div className="modal fade show d-block" tabIndex="-1" role="dialog">
        <div className="modal-dialog modal-dialog-centered modal-lg">
          <div className="modal-content">
            <div className="modal-header">
              <div>
                <h5 className="modal-title mb-0">Reservation Details</h5>
                <span className="text-secondary small">Reference: {reservation.reservationNumber}</span>
              </div>
              <button type="button" className="btn-close" aria-label="Close" onClick={onClose} />
            </div>

            <div className="modal-body">
              {/* Notice rule indicator */}
              {!isCancelled && !isCompleted && (
                <div
                  className={`alert ${
                    reservation.canCancel ? 'alert-info' : 'alert-warning'
                  } d-flex align-items-center justify-content-between py-2 px-3 mb-3`}
                  role="status"
                >
                  <div className="small">
                    <strong>12-Hour Rule Notice:</strong>{' '}
                    {reservation.canCancel ? (
                      <span>
                        Slot is scheduled in <strong>{reservation.hoursUntilSlot} hrs</strong>.
                        Modifications and cancellations are permitted.
                      </span>
                    ) : (
                      <span>
                        Slot is scheduled in <strong>{reservation.hoursUntilSlot} hrs</strong>.
                        Modifications and cancellations are <strong>locked</strong> (requires &gt; 12 hours notice).
                      </span>
                    )}
                  </div>
                  <span className={`badge ${reservation.canCancel ? 'text-bg-info' : 'text-bg-warning'}`}>
                    {reservation.canCancel ? 'Editable' : 'Locked'}
                  </span>
                </div>
              )}

              <div className="row g-3">
                <DetailItem label="Prosumer NIC" value={reservation.prosumerNic} />
                <DetailItem label="Prosumer Name" value={reservation.prosumerName} />

                <DetailItem label="Microgrid Station" value={reservation.nodeName} />
                <DetailItem label="Node ID" value={reservation.nodeId} />

                <DetailItem label="Slot Start Time" value={formatDateTime(reservation.slotStartTime)} />
                <DetailItem label="Slot End Time" value={formatDateTime(reservation.slotEndTime)} />

                <DetailItem label="Energy Reserved" value={`${reservation.energyAmountKWh} kW/h`} />

                <div className="col-md-6">
                  <p className="text-secondary small mb-1">Trading Type</p>
                  <ReservationTypeBadge type={reservation.reservationType} />
                </div>

                <div className="col-md-6">
                  <p className="text-secondary small mb-1">Status</p>
                  <ReservationStatusBadge status={reservation.status} />
                </div>

                <DetailItem label="Booked By" value={reservation.createdBy || 'System'} />
                <DetailItem label="Created On" value={formatDateTime(reservation.createdAt)} />

                {reservation.notes && (
                  <div className="col-12">
                    <p className="text-secondary small mb-1">Operational Notes</p>
                    <div className="p-2 bg-light rounded small border">{reservation.notes}</div>
                  </div>
                )}

                {isCancelled && (
                  <div className="col-12">
                    <div className="alert alert-danger mb-0 py-2">
                      <p className="fw-semibold small mb-1">Cancellation Reason:</p>
                      <p className="small mb-1">{reservation.cancellationReason || 'No reason specified'}</p>
                      <span className="text-secondary smaller">
                        Cancelled at: {formatDateTime(reservation.cancelledAt)}
                      </span>
                    </div>
                  </div>
                )}

                {/* QR Code Dispatch Info for Grid Operators */}
                {reservation.transactionQrCode && !isCancelled && (
                  <div className="col-12">
                    <div className="card bg-light border-dashed">
                      <div className="card-body d-flex flex-column flex-sm-row align-items-center gap-3">
                        <div className="p-2 bg-white rounded border text-center" style={{ minWidth: 90 }}>
                          <span style={{ fontSize: '2.5rem' }} role="img" aria-label="QR Code">
                            🏁
                          </span>
                          <div className="smaller text-muted">SECURE QR</div>
                        </div>
                        <div className="w-100 overflow-hidden">
                          <p className="fw-semibold small mb-1">Transaction QR Dispatch Token</p>
                          <p className="text-muted smaller mb-1">
                            Present on mobile client or scan on-site by Grid Operator to finalize transfer.
                          </p>
                          <code className="smaller text-break text-dark bg-white px-2 py-1 rounded d-block border">
                            {reservation.transactionQrCode}
                          </code>
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="modal-footer justify-content-between">
              <div>
                {isPending && onApproveClick && (
                  <button
                    type="button"
                    className="btn btn-success btn-sm"
                    onClick={() => onApproveClick(reservation)}
                  >
                    Approve Reservation
                  </button>
                )}
              </div>

              <div className="d-flex gap-2">
                <button type="button" className="btn btn-outline-secondary btn-sm" onClick={onClose}>
                  Close
                </button>

                {reservation.canCancel && onCancelClick && (
                  <button
                    type="button"
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => onCancelClick(reservation)}
                  >
                    Cancel Booking
                  </button>
                )}

                {reservation.canModify && onEdit && (
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    onClick={() => onEdit(reservation)}
                  >
                    Edit Booking
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="modal-backdrop fade show" />
    </>
  );
}

function DetailItem({ label, value }) {
  return (
    <div className="col-md-6">
      <p className="text-secondary small mb-1">{label}</p>
      <p className="mb-0 fw-medium">{value || '—'}</p>
    </div>
  );
}
