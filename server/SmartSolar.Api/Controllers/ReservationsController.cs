/*
 * File:        ReservationsController.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Controllers
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-22
 * Description: Energy slot reservation management endpoints. Accessible to both
 *              Backoffice and Grid Operator users. Enforces all business rules
 *              (7-day scheduling window, 12-hour notice for changes/cancellations)
 *              at the service layer as required by the FAT service pattern.
 */

using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolar.Api.DTOs;
using SmartSolar.Api.Models;
using SmartSolar.Api.Services;

namespace SmartSolar.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize(Roles = $"{UserRoles.Backoffice},{UserRoles.GridOperator}")]
public class ReservationsController : ControllerBase
{
    private readonly IReservationService _reservationService;

    /// <summary>
    /// Receives the reservation service through dependency injection.
    /// </summary>
    public ReservationsController(IReservationService reservationService)
    {
        // Store the injected service instance
        _reservationService = reservationService;
    }

    /// <summary>
    /// GET api/reservations — lists reservations with optional search and filters.
    /// </summary>
    [HttpGet]
    [ProducesResponseType(typeof(List<ReservationResponseDto>), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetAll(
        [FromQuery] string? nic,
        [FromQuery] string? nodeId,
        [FromQuery] string? status,
        [FromQuery] DateTime? fromDate,
        [FromQuery] DateTime? toDate,
        [FromQuery] string? search)
    {
        // Query reservations matching filter criteria
        var results = await _reservationService.GetAllAsync(nic, nodeId, status, fromDate, toDate, search);
        return Ok(results);
    }

    /// <summary>
    /// GET api/reservations/{id} — retrieves details of a single reservation.
    /// </summary>
    [HttpGet("{id}")]
    [ProducesResponseType(typeof(ReservationResponseDto), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(string id)
    {
        // Retrieve single reservation by id
        var reservation = await _reservationService.GetByIdAsync(id);
        return Ok(reservation);
    }

    /// <summary>
    /// GET api/reservations/stats — returns aggregated counts for dashboards.
    /// </summary>
    [HttpGet("stats")]
    [ProducesResponseType(typeof(ReservationStatsDto), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetStats()
    {
        // Query dashboard statistics
        var stats = await _reservationService.GetStatsAsync();
        return Ok(stats);
    }

    /// <summary>
    /// POST api/reservations — creates a power trading reservation (7-day rule enforced).
    /// </summary>
    [HttpPost]
    [ProducesResponseType(typeof(ReservationResponseDto), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Create([FromBody] CreateReservationDto dto)
    {
        // Identify the user making the booking
        var username = User.FindFirstValue(ClaimTypes.Name) ??
                       User.FindFirstValue(ClaimTypes.Email) ??
                       "operator";

        var created = await _reservationService.CreateAsync(dto, username);
        return CreatedAtAction(nameof(GetById), new { id = created.Id }, created);
    }

    /// <summary>
    /// PUT api/reservations/{id} — updates a reservation (7-day and 12-hour rules enforced).
    /// </summary>
    [HttpPut("{id}")]
    [ProducesResponseType(typeof(ReservationResponseDto), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateReservationDto dto)
    {
        // Update slot details with 12-hour notice validation
        var updated = await _reservationService.UpdateAsync(id, dto);
        return Ok(updated);
    }

    /// <summary>
    /// PATCH api/reservations/{id}/cancel — cancels a reservation (12-hour rule enforced).
    /// </summary>
    [HttpPatch("{id}/cancel")]
    [ProducesResponseType(typeof(ReservationResponseDto), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Cancel(string id, [FromBody] CancelReservationDto dto)
    {
        // Cancel reservation with 12-hour notice validation
        var cancelled = await _reservationService.CancelAsync(id, dto);
        return Ok(cancelled);
    }

    /// <summary>
    /// PATCH api/reservations/{id}/status — updates reservation status (e.g. Approved, Completed).
    /// </summary>
    [HttpPatch("{id}/status")]
    [ProducesResponseType(typeof(ReservationResponseDto), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> UpdateStatus(string id, [FromBody] UpdateReservationStatusDto dto)
    {
        // Advance or update lifecycle state
        var result = await _reservationService.UpdateStatusAsync(id, dto);
        return Ok(result);
    }

    /// <summary>
    /// GET api/reservations/node/{nodeId}/active-check — checks if node has active reservations.
    /// Used to block microgrid node deactivation.
    /// </summary>
    [HttpGet("node/{nodeId}/active-check")]
    [ProducesResponseType(typeof(object), StatusCodes.Status200OK)]
    public async Task<IActionResult> CheckActiveNodeReservations(string nodeId)
    {
        // Check for active reservations assigned to this hub
        var hasActive = await _reservationService.HasActiveReservationsForNodeAsync(nodeId);
        return Ok(new { nodeId, hasActiveReservations = hasActive });
    }
}
