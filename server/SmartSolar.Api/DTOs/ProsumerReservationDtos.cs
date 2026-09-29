/*
 * File:        ProsumerReservationDtos.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       DTOs
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-30
 * Description: Request bodies used by a prosumer booking from the Android app.
 *              They carry no NIC or name: the API takes those from the token and
 *              the prosumer profile, so nobody can book on someone else's behalf.
 */

using System.ComponentModel.DataAnnotations;
using SmartSolar.Api.Models;

namespace SmartSolar.Api.DTOs;

/// <summary>
/// Body posted to api/reservations/my to request a new energy slot.
/// The 7-day window and the other slot rules are checked by ReservationService.
/// </summary>
public class ProsumerReservationRequestDto
{
    // Id of an active node from GET api/nodes
    [Required(ErrorMessage = "Please choose a grid node.")]
    public string NodeId { get; set; } = string.Empty;

    // UTC times, e.g. "2026-10-02T04:30:00Z"
    [Required(ErrorMessage = "Slot start time is required.")]
    public DateTime SlotStartTime { get; set; }

    [Required(ErrorMessage = "Slot end time is required.")]
    public DateTime SlotEndTime { get; set; }

    [Required(ErrorMessage = "Energy amount is required.")]
    [Range(0.1, 10000.0, ErrorMessage = "Energy amount must be between 0.1 and 10,000 kWh.")]
    public double EnergyAmountKWh { get; set; }

    // "DropOff" or "Charging"
    [Required(ErrorMessage = "Reservation type is required.")]
    public string ReservationType { get; set; } = Models.ReservationType.DropOff;

    [StringLength(500, ErrorMessage = "Notes cannot exceed 500 characters.")]
    public string? Notes { get; set; }
}

/// <summary>
/// Optional body for api/reservations/my/{id}/cancel. The reason may be left
/// out; a standard one is recorded instead.
/// </summary>
public class ProsumerCancelReservationDto
{
    [StringLength(300, ErrorMessage = "Cancellation reason cannot exceed 300 characters.")]
    public string? Reason { get; set; }
}
