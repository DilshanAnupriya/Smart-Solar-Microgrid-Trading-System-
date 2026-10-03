/*
 * File:        ProsumerDetailPage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages / Prosumers
 * Author:      B.D.A.Cooray (IT22189530)
 * Created:     2026-09-22
 * Description: Read only profile for one prosumer, loaded with
 *              GET /api/prosumers/{nic}. An active account can be deactivated
 *              by either staff role through PATCH .../deactivate, while a
 *              deactivated account can only be restored through
 *              PATCH .../reactivate, which the API allows for the Backoffice
 *              role alone. The reactivate button is therefore hidden from Grid
 *              Operators, but that is only a convenience: ProsumerService
 *              refuses the call regardless of what the screen shows. A
 *              prosumer who asks to deactivate from the mobile app is blocked
 *              with the PendingDeactivation status; a Backoffice officer then
 *              approves (PATCH .../deactivation/approve) or rejects
 *              (PATCH .../deactivation/reject) the request here. Every action
 *              asks for confirmation first, so an account is never changed by
 *              an accidental click.
 */

import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import ConfirmModal from '../../components/ConfirmModal';
import LoadingSpinner from '../../components/LoadingSpinner';
import PageHeader from '../../components/PageHeader';
import { ProsumerStatusBadge } from '../../components/Badges';
import { useAuth } from '../../auth/useAuth';
import { useToast } from '../../toast/useToast';
import {
  approveDeactivationRequest,
  deactivateProsumer,
  getProsumerByNic,
  reactivateProsumer,
  rejectDeactivationRequest,
} from '../../api/prosumerApi';
import { PROSUMER_STATUS, ROLES } from '../../utils/constants';
import { formatDateTime } from '../../utils/formatters';

// Wording of the confirmation dialog, the API call and the success toast for
// each action the page can start. Every call returns the updated profile.
const ACTIONS = {
  deactivate: {
    title: 'Deactivate prosumer account',
    message: (name) =>
      `${name} will no longer be able to sign in or trade energy. Only a Backoffice officer can reactivate the account afterwards. Continue?`,
    confirmLabel: 'Deactivate',
    variant: 'warning',
    run: deactivateProsumer,
    success: (name) => `${name}'s account has been deactivated.`,
  },
  reactivate: {
    title: 'Reactivate prosumer account',
    message: (name) => `${name} will be able to sign in and trade energy again. Continue?`,
    confirmLabel: 'Reactivate',
    variant: 'success',
    run: reactivateProsumer,
    success: (name) => `${name}'s account has been reactivated.`,
  },
  approve: {
    title: 'Approve deactivation request',
    message: (name) =>
      `${name}'s account will be deactivated as they requested. Only a Backoffice officer can reactivate it afterwards. Continue?`,
    confirmLabel: 'Approve',
    variant: 'danger',
    run: approveDeactivationRequest,
    success: (name) => `${name}'s deactivation request was approved. The account is now deactivated.`,
  },
  reject: {
    title: 'Reject deactivation request',
    message: (name) =>
      `${name}'s account will stay active and they will be able to sign in and trade energy again. Continue?`,
    confirmLabel: 'Reject request',
    variant: 'primary',
    run: rejectDeactivationRequest,
    success: (name) => `${name}'s deactivation request was rejected. The account is active again.`,
  },
};

/**
 * One labelled line in the profile card, used for every detail shown.
 */
function DetailRow({ label, children }) {
  // Kept as a small component so each row lines up the same way
  return (
    <div className="col-md-6">
      <div className="small text-secondary text-uppercase">{label}</div>
      <div className="fw-medium">{children}</div>
    </div>
  );
}

export default function ProsumerDetailPage() {
  const { nic } = useParams();
  const { hasRole } = useAuth();
  const { showSuccess, showError } = useToast();
  const navigate = useNavigate();

  const [prosumer, setProsumer] = useState(null);
  const [loading, setLoading] = useState(true);

  // Which action is waiting for a confirmed decision, or null when none is
  const [pendingAction, setPendingAction] = useState(null);
  const [actionBusy, setActionBusy] = useState(false);

  // Reactivation and deciding on deactivation requests are Backoffice powers,
  // so those buttons are hidden from operators
  const isBackoffice = hasRole([ROLES.BACKOFFICE]);

  /**
   * Loads the prosumer profile named in the address.
   */
  const loadProsumer = useCallback(async () => {
    // Runs on first render and again after a status change is confirmed
    setLoading(true);

    try {
      const data = await getProsumerByNic(nic);
      setProsumer(data);
    } catch (fetchError) {
      // An unknown NIC or a refused profile leaves nothing to show
      showError(fetchError.message);
      navigate('/prosumers', { replace: true });
    } finally {
      setLoading(false);
    }
  }, [nic, showError, navigate]);

  useEffect(() => {
    loadProsumer();
  }, [loadProsumer]);

  /**
   * Sends the confirmed action (deactivate, reactivate, approve or reject) to the Web API.
   */
  async function handleConfirmAction() {
    if (!pendingAction) return;

    setActionBusy(true);

    try {
      // Every endpoint returns the updated profile, so the page is refreshed
      // from the API's answer rather than from a guess made in the browser
      const action = ACTIONS[pendingAction];
      const updated = await action.run(nic);
      setProsumer(updated);
      showSuccess(action.success(prosumer.fullName));

      setPendingAction(null);
    } catch (actionError) {
      showError(actionError.message);
      setPendingAction(null);
    } finally {
      setActionBusy(false);
    }
  }

  if (loading) {
    return <LoadingSpinner message="Loading prosumer profile…" />;
  }

  // The failed load already redirected, so there is nothing left to render
  if (!prosumer) return null;

  const isActive = prosumer.status === PROSUMER_STATUS.ACTIVE;
  const isPending = prosumer.status === PROSUMER_STATUS.PENDING_DEACTIVATION;
  const isDeactivated = prosumer.status === PROSUMER_STATUS.DEACTIVATED;

  return (
    <>
      <PageHeader
        title={prosumer.fullName}
        subtitle={`Prosumer profile · NIC ${prosumer.nic}`}
        actions={
          <>
            <Link to="/prosumers" className="btn btn-outline-secondary">
              Back to list
            </Link>

            <Link to={`/prosumers/${prosumer.nic}/edit`} className="btn btn-outline-primary">
              Edit
            </Link>

            {/* An active account can be deactivated by either staff role */}
            {isActive && (
              <button
                type="button"
                className="btn btn-warning"
                onClick={() => setPendingAction('deactivate')}
              >
                Deactivate
              </button>
            )}

            {/* A deactivation request is decided by Backoffice only */}
            {isPending && isBackoffice && (
              <>
                <button
                  type="button"
                  className="btn btn-outline-primary"
                  onClick={() => setPendingAction('reject')}
                >
                  Reject request
                </button>
                <button
                  type="button"
                  className="btn btn-danger"
                  onClick={() => setPendingAction('approve')}
                >
                  Approve deactivation
                </button>
              </>
            )}

            {/* Reactivation is Backoffice only, so a Grid Operator sees
                nothing here rather than a button the API would refuse */}
            {isDeactivated && isBackoffice && (
              <button
                type="button"
                className="btn btn-success"
                onClick={() => setPendingAction('reactivate')}
              >
                Reactivate
              </button>
            )}
          </>
        }
      />

      {/* The prosumer is blocked until someone decides on the request */}
      {isPending && (
        <div className="alert alert-warning" role="status">
          {prosumer.fullName} asked to deactivate this account on{' '}
          {formatDateTime(prosumer.deactivationRequestedAt)}. The account is blocked until{' '}
          {isBackoffice
            ? 'you approve or reject the request.'
            : 'a Backoffice officer approves or rejects the request.'}
        </div>
      )}

      {/* Explains to a Grid Operator why no reactivate button is offered */}
      {isDeactivated && !isBackoffice && (
        <div className="alert alert-info" role="status">
          This account is deactivated. Only a Backoffice officer can reactivate it.
        </div>
      )}

      <div className="card border-0 shadow-sm mb-3">
        <div className="card-body p-4">
          <h2 className="h6 text-uppercase text-secondary mb-3">Account details</h2>

          <div className="row g-3">
            <DetailRow label="NIC number">{prosumer.nic}</DetailRow>
            <DetailRow label="Status"><ProsumerStatusBadge status={prosumer.status} /></DetailRow>
            <DetailRow label="Full name">{prosumer.fullName}</DetailRow>
            <DetailRow label="Email address">{prosumer.email}</DetailRow>
            <DetailRow label="Phone number">{prosumer.phone}</DetailRow>
            <DetailRow label="Installation address">{prosumer.address}</DetailRow>
          </div>
        </div>
      </div>

      <div className="card border-0 shadow-sm">
        <div className="card-body p-4">
          <h2 className="h6 text-uppercase text-secondary mb-3">Record history</h2>

          <div className="row g-3">
            <DetailRow label="Registered">{formatDateTime(prosumer.createdAt)}</DetailRow>
            <DetailRow label="Last updated">{formatDateTime(prosumer.updatedAt)}</DetailRow>

            {/* Shown while a request is open, and kept after it is approved */}
            {prosumer.deactivationRequestedAt && !isActive && (
              <DetailRow label="Deactivation requested">
                {formatDateTime(prosumer.deactivationRequestedAt)}
              </DetailRow>
            )}

            {/* The API records who deactivated the account and when, so those
                two fields are only worth showing once it has happened */}
            {isDeactivated && (
              <>
                <DetailRow label="Deactivated on">{formatDateTime(prosumer.deactivatedAt)}</DetailRow>
                <DetailRow label="Deactivated by">{prosumer.deactivatedBy || '—'}</DetailRow>
              </>
            )}
          </div>
        </div>
      </div>

      {/* Nothing is sent to the API until this dialog is confirmed */}
      <ConfirmModal
        show={Boolean(pendingAction)}
        busy={actionBusy}
        title={ACTIONS[pendingAction]?.title ?? ''}
        message={pendingAction ? ACTIONS[pendingAction].message(prosumer.fullName) : ''}
        confirmLabel={ACTIONS[pendingAction]?.confirmLabel ?? 'Confirm'}
        confirmVariant={ACTIONS[pendingAction]?.variant ?? 'danger'}
        onConfirm={handleConfirmAction}
        onCancel={() => setPendingAction(null)}
      />
    </>
  );
}
