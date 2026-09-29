/*
 * File:        MyReservationsController.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Controllers
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-30
 * Description: Reservation endpoints for a signed-in prosumer (Android app).
 *              The NIC is always read from the JWT, never from the request, so
 *              a prosumer can only see and change their own reservations. All
 *              business rules are enforced in ReservationService.
 */

using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.ModelBinding;
using SmartSolar.Api.Common;
using SmartSolar.Api.DTOs;
using SmartSolar.Api.Models;
using SmartSolar.Api.Services;

namespace SmartSolar.Api.Controllers;

[ApiController]
[Route("api/reservations/my")]
[Authorize(Roles = UserRoles.Prosumer)]
public class MyReservationsController : ControllerBase
{
    private readonly IReservationService _reservationService;

    /// <summary>
    /// Receives the reservation service through dependency injection.
    /// </summary>
    public MyReservationsController(IReservationService reservationService)
    {
        // Store the service used by every endpoint
        _reservationService = reservationService;
    }

    /// <summary>
    /// GET api/reservations/my?status=&amp;search= — the prosumer's own reservations.
    /// </summary>
    [HttpGet]
    [ProducesResponseType(typeof(ApiResponse<List<ReservationResponseDto>>), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetMine([FromQuery] string? status, [FromQuery] string? search)
    {
        // Newest slot first; the QR code is only included for approved reservations
        var result = await _reservationService.GetForProsumerAsync(GetCallerNic(), status, search);
        return Ok(ApiResponse<List<ReservationResponseDto>>.Ok(result, $"{result.Count} reservation(s) found."));
    }

    /// <summary>
    /// GET api/reservations/my/{id} — one of the prosumer's own reservations.
    /// </summary>
    [HttpGet("{id}")]
    [ProducesResponseType(typeof(ApiResponse<ReservationResponseDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetMineById(string id)
    {
        // Another prosumer's reservation gives 403
        var result = await _reservationService.GetForProsumerByIdAsync(id, GetCallerNic());
        return Ok(ApiResponse<ReservationResponseDto>.Ok(result, "Reservation retrieved successfully."));
    }

    /// <summary>
    /// POST api/reservations/my — requests a new reservation (7-day rule enforced); it starts as Pending.
    /// </summary>
    [HttpPost]
    [ProducesResponseType(typeof(ApiResponse<ReservationResponseDto>), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Create([FromBody] ProsumerReservationRequestDto dto)
    {
        // The booking is made for the NIC in the token, whatever the body says
        var created = await _reservationService.CreateForProsumerAsync(dto, GetCallerNic());
        return CreatedAtAction(nameof(GetMineById), new { id = created.Id },
            ApiResponse<ReservationResponseDto>.Ok(created,
                "Reservation requested. Your QR code will be issued once it is approved."));
    }

    /// <summary>
    /// PUT api/reservations/my/{id} — changes the slot or energy (7-day and 12-hour rules enforced).
    /// </summary>
    [HttpPut("{id}")]
    [ProducesResponseType(typeof(ApiResponse<ReservationResponseDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateReservationDto dto)
    {
        // A changed reservation goes back to Pending until it is approved again
        var updated = await _reservationService.UpdateForProsumerAsync(id, dto, GetCallerNic());
        return Ok(ApiResponse<ReservationResponseDto>.Ok(updated,
            "Reservation updated. It needs approval again before a QR code is issued."));
    }

    /// <summary>
    /// PATCH api/reservations/my/{id}/cancel — cancels the reservation (12-hour rule enforced).
    /// The body, and the reason inside it, are optional.
    /// </summary>
    [HttpPatch("{id}/cancel")]
    [ProducesResponseType(typeof(ApiResponse<ReservationResponseDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Cancel(
        string id,
        [FromBody(EmptyBodyBehavior = EmptyBodyBehavior.Allow)] ProsumerCancelReservationDto? dto)
    {
        // A missing body is treated the same as one without a reason
        var cancelled = await _reservationService.CancelForProsumerAsync(id, dto ?? new ProsumerCancelReservationDto(), GetCallerNic());
        return Ok(ApiResponse<ReservationResponseDto>.Ok(cancelled, "Reservation cancelled."));
    }

    /// <summary>
    /// Reads the signed-in prosumer's NIC from the JWT.
    /// </summary>
    private string GetCallerNic()
    {
        // Prosumer tokens carry the NIC in NameIdentifier (see TokenService)
        return User.FindFirstValue(ClaimTypes.NameIdentifier) ?? string.Empty;
    }
}
