/*
 * File:        ReservationFormPage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages / Reservations
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-22
 * Description: Form page to create a new power trading reservation or reschedule
 *              an existing one. Strictly limits scheduling to a 7-day window and
 *              warns/blocks modifications within 12 hours of the slot start.
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import LoadingSpinner from '../../components/LoadingSpinner';
import PageHeader from '../../components/PageHeader';
import { useToast } from '../../toast/useToast';
import {
  createReservation,
  getReservationById,
  updateReservation,
} from '../../api/reservationsApi';
import {
  REFERENCE_NODES,
  RESERVATION_TYPES,
  RESERVATION_TYPE_LABELS,
} from '../../utils/constants';

function getTodayString() {
  return new Date().toISOString().slice(0, 10);
}

function getMaxDateString() {
  const max = new Date();
  max.setDate(max.getDate() + 7);
  return max.toISOString().slice(0, 10);
}

const EMPTY_FORM = {
  prosumerNic: '',
  prosumerName: '',
  nodeId: REFERENCE_NODES[0].id,
  slotDate: getTodayString(),
  startTime: '09:00',
  endTime: '11:00',
  energyAmountKWh: 20.0,
  reservationType: RESERVATION_TYPES.DROP_OFF,
  notes: '',
};

export default function ReservationFormPage() {
  const { id } = useParams();
  const isEditMode = Boolean(id);
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();

  const [values, setValues] = useState(EMPTY_FORM);
  const [loading, setLoading] = useState(isEditMode);
  const [submitting, setSubmitting] = useState(false);
  const [existingReservation, setExistingReservation] = useState(null);
  const [serverError, setServerError] = useState('');

  // 7-day limits
  const minDate = useMemo(() => getTodayString(), []);
  const maxDate = useMemo(() => getMaxDateString(), []);

  /**
   * Loads the existing reservation in edit mode.
   */
  const loadExisting = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getReservationById(id);
      setExistingReservation(res);

      const startDate = new Date(res.slotStartTime);
      const endDate = new Date(res.slotEndTime);

      setValues({
        prosumerNic: res.prosumerNic,
        prosumerName: res.prosumerName,
        nodeId: res.nodeId,
        slotDate: startDate.toISOString().slice(0, 10),
        startTime: startDate.toTimeString().slice(0, 5),
        endTime: endDate.toTimeString().slice(0, 5),
        energyAmountKWh: res.energyAmountKWh,
        reservationType: res.reservationType,
        notes: res.notes || '',
      });
    } catch (err) {
      showError(err.message || 'Failed to load reservation.');
      navigate('/reservations');
    } finally {
      setLoading(false);
    }
  }, [id, showError, navigate]);

  useEffect(() => {
    if (isEditMode) {
      loadExisting();
    }
  }, [isEditMode, loadExisting]);

  function handleChange(e) {
    const { name, value } = e.target;
    setValues((prev) => ({ ...prev, [name]: value }));
    setServerError('');
  }

  /**
   * Handles form submit with FAT business rule validations.
   */
  async function handleSubmit(e) {
    e.preventDefault();
    setServerError('');

    // Combine date and time into UTC ISO timestamps
    const startIso = new Date(`${values.slotDate}T${values.startTime}:00Z`).toISOString();
    const endIso = new Date(`${values.slotDate}T${values.endTime}:00Z`).toISOString();

    if (new Date(endIso) <= new Date(startIso)) {
      setServerError('Slot end time must be after slot start time.');
      return;
    }

    const selectedNode = REFERENCE_NODES.find((n) => n.id === values.nodeId);

    setSubmitting(true);
    try {
      if (isEditMode) {
        await updateReservation(id, {
          slotStartTime: startIso,
          slotEndTime: endIso,
          energyAmountKWh: parseFloat(values.energyAmountKWh),
          notes: values.notes,
        });
        showSuccess(`Reservation ${existingReservation?.reservationNumber} updated successfully.`);
      } else {
        await createReservation({
          prosumerNic: values.prosumerNic.trim().toUpperCase(),
          prosumerName: values.prosumerName.trim() || undefined,
          nodeId: values.nodeId,
          nodeName: selectedNode ? selectedNode.name : values.nodeId,
          slotStartTime: startIso,
          slotEndTime: endIso,
          energyAmountKWh: parseFloat(values.energyAmountKWh),
          reservationType: values.reservationType,
          notes: values.notes,
        });
        showSuccess('Power trading reservation created successfully.');
      }

      navigate('/reservations');
    } catch (err) {
      setServerError(err.message || 'An error occurred while saving the reservation.');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <LoadingSpinner message="Loading reservation details…" />;
  }

  const isNoticeViolated = isEditMode && existingReservation && !existingReservation.canModify;

  return (
    <>
      <PageHeader
        title={isEditMode ? `Edit Reservation: ${existingReservation?.reservationNumber}` : 'Create Energy Reservation'}
        subtitle={
          isEditMode
            ? 'Reschedule or modify power trading energy amount'
            : 'Book a power trading slot on behalf of a solar prosumer'
        }
        action={
          <Link to="/reservations" className="btn btn-outline-secondary btn-sm">
            Back to Reservations
          </Link>
        }
      />

      <div className="row justify-content-center">
        <div className="col-lg-8">
          {/* 12-Hour notice rule warning when editing an imminent slot */}
          {isNoticeViolated && (
            <div className="alert alert-danger shadow-sm mb-4" role="alert">
              <h6 className="alert-heading fw-bold mb-1">12-Hour Notice Rule Warning</h6>
              <p className="small mb-0">
                This reservation is scheduled in <strong>{existingReservation.hoursUntilSlot} hours</strong>.
                Updates require at least 12 hours notice prior to the slot. The server will reject this modification
                unless rescheduled by a system administrator.
              </p>
            </div>
          )}

          {/* Server Error Alert */}
          {serverError && (
            <div className="alert alert-danger shadow-sm mb-4" role="alert">
              <strong>Action Rejected:</strong> {serverError}
            </div>
          )}

          <div className="card border-0 shadow-sm">
            <div className="card-body p-4">
              <form onSubmit={handleSubmit}>
                <div className="row g-3">
                  {/* Prosumer NIC */}
                  <div className="col-md-6">
                    <label htmlFor="prosumerNic" className="form-label small fw-semibold">
                      Prosumer NIC <span className="text-danger">*</span>
                    </label>
                    <input
                      type="text"
                      id="prosumerNic"
                      name="prosumerNic"
                      className="form-control form-control-sm"
                      placeholder="e.g. 200012345678 or 951234567V"
                      value={values.prosumerNic}
                      onChange={handleChange}
                      disabled={isEditMode}
                      required
                    />
                    <div className="form-text smaller">Primary key matching the Prosumer profile.</div>
                  </div>

                  {/* Prosumer Full Name */}
                  <div className="col-md-6">
                    <label htmlFor="prosumerName" className="form-label small fw-semibold">
                      Prosumer Full Name
                    </label>
                    <input
                      type="text"
                      id="prosumerName"
                      name="prosumerName"
                      className="form-control form-control-sm"
                      placeholder="e.g. Sunimal Jayasuriya"
                      value={values.prosumerName}
                      onChange={handleChange}
                      disabled={isEditMode}
                    />
                  </div>

                  {/* Microgrid Node Selection */}
                  <div className="col-12">
                    <label htmlFor="nodeId" className="form-label small fw-semibold">
                      Microgrid Station Hub <span className="text-danger">*</span>
                    </label>
                    <select
                      id="nodeId"
                      name="nodeId"
                      className="form-select form-select-sm"
                      value={values.nodeId}
                      onChange={handleChange}
                      disabled={isEditMode}
                      required
                    >
                      {REFERENCE_NODES.map((node) => (
                        <option key={node.id} value={node.id}>
                          {node.name} — Capacity: {node.capacity} kWh, Slots: {node.slots}
                        </option>
                      ))}
                    </select>
                    <div className="form-text smaller">
                      Select the destination microgrid battery hub.
                    </div>
                  </div>

                  {/* Slot Date (7-day rule restricted) */}
                  <div className="col-md-4">
                    <label htmlFor="slotDate" className="form-label small fw-semibold">
                      Slot Date <span className="text-danger">*</span>
                    </label>
                    <input
                      type="date"
                      id="slotDate"
                      name="slotDate"
                      className="form-control form-control-sm"
                      min={minDate}
                      max={maxDate}
                      value={values.slotDate}
                      onChange={handleChange}
                      required
                    />
                    <div className="form-text smaller text-primary">
                      Must be scheduled within 7 days.
                    </div>
                  </div>

                  {/* Slot Start Time */}
                  <div className="col-md-4">
                    <label htmlFor="startTime" className="form-label small fw-semibold">
                      Start Time <span className="text-danger">*</span>
                    </label>
                    <input
                      type="time"
                      id="startTime"
                      name="startTime"
                      className="form-control form-control-sm"
                      value={values.startTime}
                      onChange={handleChange}
                      required
                    />
                  </div>

                  {/* Slot End Time */}
                  <div className="col-md-4">
                    <label htmlFor="endTime" className="form-label small fw-semibold">
                      End Time <span className="text-danger">*</span>
                    </label>
                    <input
                      type="time"
                      id="endTime"
                      name="endTime"
                      className="form-control form-control-sm"
                      value={values.endTime}
                      onChange={handleChange}
                      required
                    />
                  </div>

                  {/* Energy Amount (kWh) */}
                  <div className="col-md-6">
                    <label htmlFor="energyAmountKWh" className="form-label small fw-semibold">
                      Energy Volume (kW/h) <span className="text-danger">*</span>
                    </label>
                    <div className="input-group input-group-sm">
                      <input
                        type="number"
                        id="energyAmountKWh"
                        name="energyAmountKWh"
                        className="form-control"
                        step="0.5"
                        min="0.5"
                        max="5000"
                        value={values.energyAmountKWh}
                        onChange={handleChange}
                        required
                      />
                      <span className="input-group-text">kW/h</span>
                    </div>
                    <div className="form-text smaller">Power traded during this reservation slot.</div>
                  </div>

                  {/* Trading Type */}
                  <div className="col-md-6">
                    <label htmlFor="reservationType" className="form-label small fw-semibold">
                      Trading Transaction Type <span className="text-danger">*</span>
                    </label>
                    <select
                      id="reservationType"
                      name="reservationType"
                      className="form-select form-select-sm"
                      value={values.reservationType}
                      onChange={handleChange}
                      disabled={isEditMode}
                      required
                    >
                      <option value={RESERVATION_TYPES.DROP_OFF}>
                        {RESERVATION_TYPE_LABELS[RESERVATION_TYPES.DROP_OFF]}
                      </option>
                      <option value={RESERVATION_TYPES.CHARGING}>
                        {RESERVATION_TYPE_LABELS[RESERVATION_TYPES.CHARGING]}
                      </option>
                    </select>
                  </div>

                  {/* Operational Notes */}
                  <div className="col-12">
                    <label htmlFor="notes" className="form-label small fw-semibold">
                      Operational Remarks / Instructions
                    </label>
                    <textarea
                      id="notes"
                      name="notes"
                      className="form-control form-control-sm"
                      rows="3"
                      placeholder="Optional notes for grid operators on duty…"
                      value={values.notes}
                      onChange={handleChange}
                    />
                  </div>
                </div>

                <div className="d-flex justify-content-end gap-2 mt-4 pt-3 border-top">
                  <Link to="/reservations" className="btn btn-outline-secondary btn-sm">
                    Cancel
                  </Link>
                  <button
                    type="submit"
                    className="btn btn-primary btn-sm px-4"
                    disabled={submitting}
                  >
                    {submitting
                      ? 'Saving…'
                      : isEditMode
                      ? 'Update Reservation'
                      : 'Confirm & Create Booking'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
