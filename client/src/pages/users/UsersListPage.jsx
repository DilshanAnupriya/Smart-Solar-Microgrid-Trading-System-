/*
 * File:        UsersListPage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages / Users
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Backoffice screen that lists every web application user with
 *              search, role and status filters, and lets an officer create,
 *              edit, deactivate and reactivate accounts. Every action is sent to
 *              the Web API; nothing is changed locally.
 */

import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import ConfirmModal from '../../components/ConfirmModal';
import EmptyState from '../../components/EmptyState';
import LoadingSpinner from '../../components/LoadingSpinner';
import PageHeader from '../../components/PageHeader';
import UserDetailsModal from '../../components/UserDetailsModal';
import { RoleBadge, StatusBadge } from '../../components/Badges';
import { useAuth } from '../../auth/useAuth';
import { useToast } from '../../toast/useToast';
import { activateUser, deactivateUser, deleteUser, getUsers } from '../../api/usersApi';
import { ROLE_OPTIONS } from '../../utils/constants';
import { formatDate } from '../../utils/formatters';

// Wording of the confirmation dialog for each action the table can start
const ACTION_TEXT = {
  deactivate: {
    title: 'Deactivate account',
    message: (name) => `${name} will no longer be able to sign in. Continue?`,
    confirmLabel: 'Deactivate',
    variant: 'warning',
  },
  activate: {
    title: 'Reactivate account',
    message: (name) => `${name} will be able to sign in again. Continue?`,
    confirmLabel: 'Activate',
    variant: 'success',
  },
  delete: {
    title: 'Delete account',
    message: (name) =>
      `${name} will be permanently removed. This cannot be undone. Continue?`,
    confirmLabel: 'Delete',
    variant: 'danger',
  },
};

export default function UsersListPage() {
  const { user: currentUser } = useAuth();
  const { showSuccess, showError } = useToast();
  const navigate = useNavigate();

  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  // Filter state, applied by the API rather than in the browser
  const [search, setSearch] = useState('');
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('all');

  // The account waiting for a confirmed activate, deactivate or delete decision
  const [pendingAction, setPendingAction] = useState(null);
  const [actionBusy, setActionBusy] = useState(false);

  // The account whose full details are being viewed, or null when none is
  const [viewingUser, setViewingUser] = useState(null);

  /**
   * Fetches the user list using the current filters.
   */
  const loadUsers = useCallback(async () => {
    // Called on first render, whenever a filter changes, and after each action
    setLoading(true);

    try {
      const data = await getUsers({ role, status, search });
      setUsers(data);
    } catch (fetchError) {
      showError(fetchError.message);
    } finally {
      setLoading(false);
    }
  }, [role, status, search, showError]);

  // Debounced so typing in the search box does not fire a request per keystroke
  useEffect(() => {
    const timer = setTimeout(loadUsers, 300);

    return () => clearTimeout(timer);
  }, [loadUsers]);

  /**
   * Sends the confirmed activate or deactivate request to the Web API.
   */
  async function handleConfirmAction() {
    if (!pendingAction) return;

    setActionBusy(true);

    try {
      if (pendingAction.type === 'deactivate') {
        await deactivateUser(pendingAction.user.id);
        showSuccess(`${pendingAction.user.fullName} has been deactivated.`);
      } else if (pendingAction.type === 'delete') {
        await deleteUser(pendingAction.user.id);
        showSuccess(`${pendingAction.user.fullName} has been deleted.`);
      } else {
        await activateUser(pendingAction.user.id);
        showSuccess(`${pendingAction.user.fullName} has been reactivated.`);
      }

      setPendingAction(null);

      // Refetch so the table always reflects what the database now holds
      await loadUsers();
    } catch (actionError) {
      showError(actionError.message);
      setPendingAction(null);
    } finally {
      setActionBusy(false);
    }
  }

  return (
    <>
      <PageHeader
        title="User management"
        subtitle="Create and manage Backoffice and Grid Operator accounts"
        actions={
          <Link to="/users/new" className="btn btn-primary">
            Add user
          </Link>
        }
      />

      <div className="card border-0 shadow-sm">
        <div className="card-body">
          <div className="row g-2 mb-3">
            <div className="col-md-6">
              <label htmlFor="search" className="form-label small text-secondary">Search</label>
              <input
                id="search"
                type="search"
                className="form-control"
                placeholder="Search by name or email"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
            </div>

            <div className="col-md-3">
              <label htmlFor="roleFilter" className="form-label small text-secondary">Role</label>
              <select
                id="roleFilter"
                className="form-select"
                value={role}
                onChange={(event) => setRole(event.target.value)}
              >
                <option value="">All roles</option>
                {ROLE_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </div>

            <div className="col-md-3">
              <label htmlFor="statusFilter" className="form-label small text-secondary">Status</label>
              <select
                id="statusFilter"
                className="form-select"
                value={status}
                onChange={(event) => setStatus(event.target.value)}
              >
                <option value="all">All statuses</option>
                <option value="active">Active</option>
                <option value="inactive">Deactivated</option>
              </select>
            </div>
          </div>

          {loading ? (
            <LoadingSpinner message="Loading users…" />
          ) : users.length === 0 ? (
            <EmptyState
              title="No users found"
              description="Try clearing the filters, or add the first account."
              action={<Link to="/users/new" className="btn btn-sm btn-primary">Add user</Link>}
            />
          ) : (
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0">
                <thead>
                  {/* text-nowrap keeps the headings on one line; the wrapper
                      scrolls sideways on a narrow screen */}
                  {/* Username, NIC, phone and date of birth are deliberately
                      left out here; they are shown in the View dialog */}
                  <tr className="text-secondary small text-uppercase text-nowrap">
                    <th scope="col">Name</th>
                    <th scope="col">Email</th>
                    <th scope="col">Role</th>
                    <th scope="col">Status</th>
                    <th scope="col">Created</th>
                    <th scope="col" className="text-end">Actions</th>
                  </tr>
                </thead>

                <tbody>
                  {users.map((item) => {
                    // A user may not deactivate their own account, so that
                    // button is hidden on their own row
                    const isSelf = item.id === currentUser?.id;

                    return (
                      <tr key={item.id}>
                        <td className="fw-semibold">
                          {item.fullName}
                          {isSelf && <span className="badge text-bg-light border ms-2">You</span>}
                        </td>
                        <td className="text-secondary">{item.email}</td>
                        <td><RoleBadge role={item.role} /></td>
                        <td><StatusBadge isActive={item.isActive} /></td>
                        <td className="text-secondary small">{formatDate(item.createdAt)}</td>
                        <td className="text-end">
                          <div className="btn-group btn-group-sm">
                            <button
                              type="button"
                              className="btn btn-outline-primary"
                              onClick={() => setViewingUser(item)}
                            >
                              View
                            </button>

                            <button
                              type="button"
                              className="btn btn-outline-secondary"
                              onClick={() => navigate(`/users/${item.id}/edit`)}
                            >
                              Edit
                            </button>

                            {item.isActive ? (
                              <button
                                type="button"
                                className="btn btn-outline-warning"
                                disabled={isSelf}
                                title={isSelf ? 'You cannot deactivate your own account' : undefined}
                                onClick={() => setPendingAction({ type: 'deactivate', user: item })}
                              >
                                Deactivate
                              </button>
                            ) : (
                              <button
                                type="button"
                                className="btn btn-outline-success"
                                onClick={() => setPendingAction({ type: 'activate', user: item })}
                              >
                                Activate
                              </button>
                            )}

                            {/* Deleting cannot be undone, so it is the last
                                button and always asks for confirmation */}
                            <button
                              type="button"
                              className="btn btn-outline-danger"
                              disabled={isSelf}
                              title={isSelf ? 'You cannot delete your own account' : undefined}
                              onClick={() => setPendingAction({ type: 'delete', user: item })}
                            >
                              Delete
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      <ConfirmModal
        show={Boolean(pendingAction)}
        busy={actionBusy}
        title={ACTION_TEXT[pendingAction?.type]?.title ?? ''}
        message={
          pendingAction
            ? ACTION_TEXT[pendingAction.type].message(pendingAction.user.fullName)
            : ''
        }
        confirmLabel={ACTION_TEXT[pendingAction?.type]?.confirmLabel ?? 'Confirm'}
        confirmVariant={ACTION_TEXT[pendingAction?.type]?.variant ?? 'danger'}
        onConfirm={handleConfirmAction}
        onCancel={() => setPendingAction(null)}
      />

      {/* Read only dialog holding every detail of the selected account */}
      <UserDetailsModal
        user={viewingUser}
        onClose={() => setViewingUser(null)}
        onEdit={(user) => {
          setViewingUser(null);
          navigate(`/users/${user.id}/edit`);
        }}
      />
    </>
  );
}
