/*
 * File:        IReservationRepository.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Repositories
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-22
 * Description: Contract for MongoDB data access operations on the reservations
 *              collection. Exposes filtering, CRUD operations, capacity overlap
 *              queries, active reservation checks for node deactivation, and
 *              dashboard aggregations.
 */

using SmartSolar.Api.DTOs;
using SmartSolar.Api.Models;

namespace SmartSolar.Api.Repositories;

public interface IReservationRepository
{
    /// <summary>
    /// Returns reservations matching the optional filters.
    /// </summary>
    Task<List<Reservation>> GetAllAsync(
        string? nic,
        string? nodeId,
        string? status,
        DateTime? fromDate,
        DateTime? toDate,
        string? search);

    /// <summary>
    /// Returns a single reservation by its MongoDB ObjectId.
    /// </summary>
    Task<Reservation?> GetByIdAsync(string id);

    /// <summary>
    /// Returns a single reservation by its unique reservation reference number.
    /// </summary>
    Task<Reservation?> GetByNumberAsync(string reservationNumber);

    /// <summary>
    /// Inserts a new reservation document into the collection.
    /// </summary>
    Task<Reservation> CreateAsync(Reservation reservation);

    /// <summary>
    /// Replaces/updates an existing reservation document.
    /// </summary>
    Task UpdateAsync(Reservation reservation);

    /// <summary>
    /// Permanently removes a reservation from the collection.
    /// </summary>
    Task DeleteAsync(string id);

    /// <summary>
    /// Returns active (Pending or Approved) reservations assigned to a node.
    /// Used by Microgrid Node Management to block node deactivation when
    /// active reservations exist.
    /// </summary>
    Task<List<Reservation>> GetActiveReservationsByNodeIdAsync(string nodeId);

    /// <summary>
    /// Counts overlapping active reservations for a node within a given time range.
    /// </summary>
    Task<long> CountOverlappingReservationsAsync(string nodeId, DateTime start, DateTime end, string? excludeId = null);

    /// <summary>
    /// Aggregates reservation counts for operational and administrative dashboards.
    /// </summary>
    Task<ReservationStatsDto> GetStatsAsync();

    /// <summary>
    /// Ensures necessary database indexes exist for performance and unique constraints.
    /// </summary>
    Task EnsureIndexesAsync();
}
