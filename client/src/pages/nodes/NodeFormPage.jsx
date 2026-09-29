/*
 * File:        NodeFormPage.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Pages / Nodes
 * Author:      Vidura Hewaduwa
 * Created:     2026-09-24
 * Description: Shared Backoffice form for creating and editing microgrid nodes.
 *              Client validation gives quick feedback; NodeService repeats every rule.
 */

import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import ErrorAlert from '../../components/ErrorAlert';
import LoadingSpinner from '../../components/LoadingSpinner';
import PageHeader from '../../components/PageHeader';
import { createNode, getNodeById, updateNode } from '../../api/nodesApi';
import { useToast } from '../../toast/useToast';

const EMPTY_FORM = {
  nodeCode: '', name: '', address: '', latitude: '', longitude: '',
  generationCapacityKw: '', storageCapacityKWh: '', totalBatterySlots: '', availableBatterySlots: '',
};

export default function NodeFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();
  // An id in the URL selects edit mode; otherwise the form creates a node
  const isEditMode = Boolean(id);
  const [values, setValues] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [pageError, setPageError] = useState('');
  const [loading, setLoading] = useState(isEditMode);
  const [submitting, setSubmitting] = useState(false);

  /** Loads existing values only when the same form is used for editing. */
  const loadNode = useCallback(async () => {
    try {
      const node = await getNodeById(id);
      setValues({
        nodeCode: node.nodeCode,
        name: node.name,
        address: node.address,
        latitude: String(node.latitude),
        longitude: String(node.longitude),
        generationCapacityKw: String(node.generationCapacityKw),
        storageCapacityKWh: String(node.storageCapacityKWh),
        totalBatterySlots: String(node.totalBatterySlots),
        availableBatterySlots: String(node.availableBatterySlots),
      });
    } catch (error) {
      showError(error.message);
      navigate('/nodes', { replace: true });
    } finally {
      setLoading(false);
    }
  }, [id, navigate, showError]);

  useEffect(() => {
    if (isEditMode) loadNode();
  }, [isEditMode, loadNode]);

  /** Keeps form state and field-level error state synchronized with an input. */
  function handleChange(event) {
    const { name, value } = event.target;
    setValues((current) => ({ ...current, [name]: value }));
    setFieldErrors((current) => ({ ...current, [name]: undefined }));
  }

  /** Mirrors important service rules so users get feedback before submitting. */
  function validate() {
    const errors = {};
    if (!values.nodeCode.trim()) errors.nodeCode = 'Node code is required.';
    if (!values.name.trim()) errors.name = 'Name is required.';
    if (!values.address.trim()) errors.address = 'Address is required.';

    const latitude = Number(values.latitude);
    const longitude = Number(values.longitude);
    const generation = Number(values.generationCapacityKw);
    const storage = Number(values.storageCapacityKWh);
    const total = Number(values.totalBatterySlots);
    const available = Number(values.availableBatterySlots);

    if (values.latitude === '' || latitude < -90 || latitude > 90) errors.latitude = 'Enter a latitude from -90 to 90.';
    if (values.longitude === '' || longitude < -180 || longitude > 180) errors.longitude = 'Enter a longitude from -180 to 180.';
    if (values.generationCapacityKw === '' || generation <= 0) errors.generationCapacityKw = 'Generation capacity must be positive.';
    if (values.storageCapacityKWh === '' || storage <= 0) errors.storageCapacityKWh = 'Storage capacity must be positive.';
    if (!Number.isInteger(total) || total < 0) errors.totalBatterySlots = 'Enter a non-negative whole number.';
    if (!Number.isInteger(available) || available < 0) errors.availableBatterySlots = 'Enter a non-negative whole number.';
    if (!errors.availableBatterySlots && available > total) errors.availableBatterySlots = 'Available slots cannot exceed total slots.';
    return errors;
  }

  /** Converts text input values into the numeric JSON contract expected by the API. */
  async function handleSubmit(event) {
    event.preventDefault();
    const errors = validate();
    setFieldErrors(errors);
    setPageError('');
    if (Object.keys(errors).length) return;

    // Number inputs are strings in browser state and must be converted before sending
    const payload = {
      nodeCode: values.nodeCode.trim().toUpperCase(),
      name: values.name.trim(),
      address: values.address.trim(),
      latitude: Number(values.latitude),
      longitude: Number(values.longitude),
      generationCapacityKw: Number(values.generationCapacityKw),
      storageCapacityKWh: Number(values.storageCapacityKWh),
      totalBatterySlots: Number(values.totalBatterySlots),
      availableBatterySlots: Number(values.availableBatterySlots),
      operatingSchedule: [],
    };

    setSubmitting(true);
    try {
      const saved = isEditMode ? await updateNode(id, payload) : await createNode(payload);
      showSuccess(`Node ${saved.nodeCode} has been ${isEditMode ? 'updated' : 'created'}.`);
      navigate(`/nodes/${saved.id}`, { replace: true });
    } catch (error) {
      setPageError(error.message);
      if (error.status === 409) setFieldErrors((current) => ({ ...current, nodeCode: error.message }));
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <LoadingSpinner message="Loading node…" />;

  const fields = [
    ['nodeCode', 'Node code', 'text', 'e.g. NODE-CMB-01'],
    ['name', 'Node name', 'text', 'e.g. Colombo Central Hub'],
    ['latitude', 'Latitude', 'number', '6.9271'],
    ['longitude', 'Longitude', 'number', '79.8612'],
    ['generationCapacityKw', 'Generation capacity (kW)', 'number', '250'],
    ['storageCapacityKWh', 'Storage capacity (kWh)', 'number', '500'],
    ['totalBatterySlots', 'Total battery slots', 'number', '20'],
    ['availableBatterySlots', 'Available battery slots', 'number', '20'],
  ];

  return (
    <>
      <PageHeader
        title={isEditMode ? 'Edit microgrid node' : 'Add microgrid node'}
        subtitle="Enter the location, capacity and battery-slot information"
      />
      <ErrorAlert message={pageError} onDismiss={() => setPageError('')} />
      <div className="card border-0 shadow-sm">
        <div className="card-body p-4">
          <form onSubmit={handleSubmit} noValidate>
            <div className="row g-3">
              {fields.map(([name, label, type, placeholder]) => (
                <div className="col-md-6" key={name}>
                  <label htmlFor={name} className="form-label">{label}</label>
                  <input
                    id={name}
                    name={name}
                    type={type}
                    step={type === 'number' ? 'any' : undefined}
                    min={name.includes('Slots') ? '0' : undefined}
                    className={`form-control ${fieldErrors[name] ? 'is-invalid' : ''}`}
                    value={values[name]}
                    placeholder={placeholder}
                    onChange={handleChange}
                    disabled={submitting}
                  />
                  {fieldErrors[name] && <div className="invalid-feedback">{fieldErrors[name]}</div>}
                </div>
              ))}
              <div className="col-12">
                <label htmlFor="address" className="form-label">Address</label>
                <textarea
                  id="address"
                  name="address"
                  rows="3"
                  className={`form-control ${fieldErrors.address ? 'is-invalid' : ''}`}
                  value={values.address}
                  onChange={handleChange}
                  disabled={submitting}
                />
                {fieldErrors.address && <div className="invalid-feedback">{fieldErrors.address}</div>}
              </div>
            </div>
            <div className="d-flex justify-content-end gap-2 mt-4">
              <button type="button" className="btn btn-outline-secondary" onClick={() => navigate(-1)} disabled={submitting}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting && <span className="spinner-border spinner-border-sm me-2" />}
                {isEditMode ? 'Save changes' : 'Create node'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </>
  );
}
