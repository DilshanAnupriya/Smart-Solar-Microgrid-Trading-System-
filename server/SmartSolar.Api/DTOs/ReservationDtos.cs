/*
 * File:        ReservationDtos.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       DTOs
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-22
 * Description: Data transfer objects for energy slot reservation management.
 *              Includes request validation attributes and response projections
 *              with computed eligibility flags for modification and cancellation.
 */

using System.ComponentModel.DataAnnotations;
using SmartSolar.Api.Models;
using SmartSolar.Api.Validators;

namespace SmartSolar.Api.DTOs;

/// <summary>
/// Body posted to api/reservations to create a power trading booking.
/// </summary>
public class CreateReservationDto
{
    [Required(ErrorMessage = "Prosumer NIC is required.")]
    [RegularExpression(ValidationPatterns.Nic, ErrorMessage = ValidationPatterns.NicMessage)]
    public string ProsumerNic { get; set; } = string.Empty;

    public string? ProsumerName { get; set; }

    [Required(ErrorMessage = "Microgrid Node ID is required.")]
    public string NodeId { get; set; } = string.Empty;

    public string? NodeName { get; set; }

    [Required(ErrorMessage = "Slot start time is required.")]
    public DateTime SlotStartTime { get; set; }

    [Required(ErrorMessage = "Slot end time is required.")]
    public DateTime SlotEndTime { get; set; }

    [Required(ErrorMessage = "Energy amount is required.")]
    [Range(0.1, 10000.0, ErrorMessage = "Energy amount must be between 0.1 and 10,000 kW/h.")]
    public double EnergyAmountKWh { get; set; }

    [Required(ErrorMessage = "Reservation type is required.")]
    public string ReservationType { get; set; } = Models.ReservationType.DropOff;

    [StringLength(500, ErrorMessage = "Notes cannot exceed 500 characters.")]
    public string? Notes { get; set; }
}

/// <summary>
/// Body sent to api/reservations/{id} to reschedule or adjust a booking.
/// Subject to the 7-day scheduling rule and the 12-hour notice rule.
/// </summary>
public class UpdateReservationDto
{
    [Required(ErrorMessage = "Slot start time is required.")]
    public DateTime SlotStartTime { get; set; }

    [Required(ErrorMessage = "Slot end time is required.")]
    public DateTime SlotEndTime { get; set; }

    [Required(ErrorMessage = "Energy amount is required.")]
    [Range(0.1, 10000.0, ErrorMessage = "Energy amount must be between 0.1 and 10,000 kW/h.")]
    public double EnergyAmountKWh { get; set; }

    [StringLength(500, ErrorMessage = "Notes cannot exceed 500 characters.")]
    public string? Notes { get; set; }
}

/// <summary>
/// Body sent to api/reservations/{id}/cancel to record cancellation.
/// Subject to the 12-hour notice rule.
/// </summary>
public class CancelReservationDto
{
    [Required(ErrorMessage = "Cancellation reason is required.")]
    [StringLength(300, MinimumLength = 3, ErrorMessage = "Cancellation reason must be between 3 and 300 characters.")]
    public string Reason { get; set; } = string.Empty;
}

/// <summary>
/// Body sent to api/reservations/{id}/status to approve or complete a reservation.
/// </summary>
public class UpdateReservationStatusDto
{
    [Required(ErrorMessage = "Status is required.")]
    public string Status { get; set; } = string.Empty;

    public string? Notes { get; set; }
}

/// <summary>
/// Returned to callers when inspecting one or more power trading reservations.
/// Includes computed rules so the UI knows if edit/cancel options should be enabled.
/// </summary>
public class ReservationResponseDto
{
    public string Id { get; set; } = string.Empty;
    public string ReservationNumber { get; set; } = string.Empty;
    public string ProsumerNic { get; set; } = string.Empty;
    public string ProsumerName { get; set; } = string.Empty;
    public string NodeId { get; set; } = string.Empty;
    public string NodeName { get; set; } = string.Empty;
    public DateTime SlotStartTime { get; set; }
    public DateTime SlotEndTime { get; set; }
    public double EnergyAmountKWh { get; set; }
    public string ReservationType { get; set; } = string.Empty;
    public string Status { get; set; } = string.Empty;
    public string TransactionQrCode { get; set; } = string.Empty;
    public string? CancellationReason { get; set; }
    public DateTime? CancelledAt { get; set; }
    public string? Notes { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public string CreatedBy { get; set; } = string.Empty;

    // Computed rule properties
    public bool CanModify { get; set; }
    public bool CanCancel { get; set; }
    public double HoursUntilSlot { get; set; }
}

/// <summary>
/// Aggregated statistics returned for Backoffice and Grid Operator dashboards.
/// </summary>
public class ReservationStatsDto
{
    public long TotalReservations { get; set; }
    public long PendingReservations { get; set; }
    public long ApprovedFutureReservations { get; set; }
    public long TodayReservations { get; set; }
    public long CompletedReservations { get; set; }
    public long CancelledReservations { get; set; }
}
