/*
 * File:        IReservationService.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Services
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-22
 * Description: Contract for power trading reservation business logic.
 *              Enforces all domain business rules including the 7-day
 *              scheduling rule and the 12-hour notice rule for updates
 *              and cancellations.
 */

using SmartSolar.Api.DTOs;

namespace SmartSolar.Api.Services;

public interface IReservationService
{
    /// <summary>
    /// Lists reservations matching the optional filters.
    /// </summary>
    Task<List<ReservationResponseDto>> GetAllAsync(
        string? nic,
        string? nodeId,
        string? status,
        DateTime? fromDate,
        DateTime? toDate,
        string? search);

    /// <summary>
    /// Returns a single reservation by id.
    /// </summary>
    Task<ReservationResponseDto> GetByIdAsync(string id);

    /// <summary>
    /// Returns dashboard summary statistics.
    /// </summary>
    Task<ReservationStatsDto> GetStatsAsync();

    /// <summary>
    /// Creates a new power trading reservation enforcing the 7-day rule.
    /// </summary>
    Task<ReservationResponseDto> CreateAsync(CreateReservationDto dto, string createdBy);

    /// <summary>
    /// Reschedules or updates an existing reservation enforcing the 7-day and 12-hour rules.
    /// </summary>
    Task<ReservationResponseDto> UpdateAsync(string id, UpdateReservationDto dto);

    /// <summary>
    /// Cancels an existing reservation enforcing the 12-hour notice rule.
    /// </summary>
    Task<ReservationResponseDto> CancelAsync(string id, CancelReservationDto dto);

    /// <summary>
    /// Updates the status of a reservation (e.g. Approved or Completed).
    /// </summary>
    Task<ReservationResponseDto> UpdateStatusAsync(string id, UpdateReservationStatusDto dto);

    /// <summary>
    /// Verifies if a microgrid node has any active reservations.
    /// Used to block node deactivation if active energy reservations exist.
    /// </summary>
    Task<bool> HasActiveReservationsForNodeAsync(string nodeId);
}
