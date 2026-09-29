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
 *              refuses the call regardless of what the screen shows. Both
 *              actions ask for confirmation first, so an account is never
 *              changed by an accidental click.
 */

import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import ConfirmModal from '../../components/ConfirmModal';
import LoadingSpinner from '../../components/LoadingSpinner';
import PageHeader from '../../components/PageHeader';
import { ProsumerStatusBadge } from '../../components/Badges';
import { useAuth } from '../../auth/useAuth';
import { useToast } from '../../toast/useToast';
import { deactivateProsumer, getProsumerByNic, reactivateProsumer } from '../../api/prosumerApi';
import { PROSUMER_STATUS, ROLES } from '../../utils/constants';
import { formatDateTime } from '../../utils/formatters';

// Wording of the confirmation dialog for each action the page can start
const ACTION_TEXT = {
  deactivate: {
    title: 'Deactivate prosumer account',
    message: (name) =>
      `${name} will no longer be able to sign in or trade energy. Only a Backoffice officer can reactivate the account afterwards. Continue?`,
    confirmLabel: 'Deactivate',
    variant: 'warning',
  },
  reactivate: {
    title: 'Reactivate prosumer account',
    message: (name) => `${name} will be able to sign in and trade energy again. Continue?`,
    confirmLabel: 'Reactivate',
    variant: 'success',
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

  // Reactivation is a Backoffice power, so the button is hidden from operators
  const canReactivate = hasRole([ROLES.BACKOFFICE]);

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
   * Sends the confirmed deactivate or reactivate request to the Web API.
   */
  async function handleConfirmAction() {
    if (!pendingAction) return;

    setActionBusy(true);

    try {
      // Both endpoints return the updated profile, so the page is refreshed
      // from the API's answer rather than from a guess made in the browser
      if (pendingAction === 'deactivate') {
        const updated = await deactivateProsumer(nic);
        setProsumer(updated);
        showSuccess(`${prosumer.fullName}'s account has been deactivated.`);
      } else {
        const updated = await reactivateProsumer(nic);
        setProsumer(updated);
        showSuccess(`${prosumer.fullName}'s account has been reactivated.`);
      }

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
            {isActive ? (
              <button
                type="button"
                className="btn btn-warning"
                onClick={() => setPendingAction('deactivate')}
              >
                Deactivate
              </button>
            ) : (
              // Reactivation is Backoffice only, so a Grid Operator sees
              // nothing here rather than a button the API would refuse
              canReactivate && (
                <button
                  type="button"
                  className="btn btn-success"
                  onClick={() => setPendingAction('reactivate')}
                >
                  Reactivate
                </button>
              )
            )}
          </>
        }
      />

      {/* Explains to a Grid Operator why no reactivate button is offered */}
      {!isActive && !canReactivate && (
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

            {/* The API records who deactivated the account and when, so those
                two fields are only worth showing once it has happened */}
            {!isActive && (
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
        title={ACTION_TEXT[pendingAction]?.title ?? ''}
        message={pendingAction ? ACTION_TEXT[pendingAction].message(prosumer.fullName) : ''}
        confirmLabel={ACTION_TEXT[pendingAction]?.confirmLabel ?? 'Confirm'}
        confirmVariant={ACTION_TEXT[pendingAction]?.variant ?? 'danger'}
        onConfirm={handleConfirmAction}
        onCancel={() => setPendingAction(null)}
      />
    </>
  );
}
