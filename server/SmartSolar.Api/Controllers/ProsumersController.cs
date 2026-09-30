/*
 * File:        ProsumersController.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Controllers
 * Author:      Dilshan Anupriya (IT22189530)
 * Created:     2026-09-20
 * Description: REST endpoints for prosumer management. Reads the caller's id
 *              and role from the JWT and passes them to ProsumerService,
 *              which enforces all business rules.
 */

using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolar.Api.Common;
using SmartSolar.Api.DTOs.Prosumers;
using SmartSolar.Api.Models;
using SmartSolar.Api.Services;

namespace SmartSolar.Api.Controllers;

[ApiController]
[Route("api/prosumers")]
[Authorize]
public class ProsumersController : ControllerBase
{
    private readonly IProsumerService _prosumerService;

    /// <summary>
    /// Receives the prosumer service through dependency injection.
    /// </summary>
    public ProsumersController(IProsumerService prosumerService)
    {
        // Store the service used by every endpoint
        _prosumerService = prosumerService;
    }

    /// <summary>
    /// POST api/prosumers/register — mobile self-registration (no login required).
    /// </summary>
    [HttpPost("register")]
    [AllowAnonymous]
    public async Task<IActionResult> Register([FromBody] ProsumerCreateRequest request)
    {
        // Create the account and return 201 with a link to the new profile
        var result = await _prosumerService.CreateAsync(request);
        return CreatedAtAction(nameof(GetByNic), new { nic = result.Nic },
            ApiResponse<ProsumerResponse>.Ok(result, "Registration successful."));
    }

    /// <summary>
    /// POST api/prosumers — staff create a prosumer profile from the web app.
    /// </summary>
    [HttpPost]
    [Authorize(Roles = "Backoffice,GridOperator")]
    public async Task<IActionResult> Create([FromBody] ProsumerCreateRequest request)
    {
        // Same rules as self-registration, but only staff can call this route
        var result = await _prosumerService.CreateAsync(request);
        return CreatedAtAction(nameof(GetByNic), new { nic = result.Nic },
            ApiResponse<ProsumerResponse>.Ok(result, "Prosumer created successfully."));
    }

    /// <summary>
    /// GET api/prosumers?search=&amp;status= — list prosumers for staff.
    /// </summary>
    [HttpGet]
    [Authorize(Roles = "Backoffice,GridOperator")]
    public async Task<IActionResult> GetAll([FromQuery] string? search, [FromQuery] string? status)
    {
        // Search matches NIC, name, email or phone; status is Active, PendingDeactivation or Deactivated
        var result = await _prosumerService.GetAllAsync(search, status);
        return Ok(ApiResponse<List<ProsumerResponse>>.Ok(result, $"{result.Count} prosumer(s) found."));
    }

    /// <summary>
    /// GET api/prosumers/{nic} — view one profile (staff, or the prosumer themselves).
    /// </summary>
    [HttpGet("{nic}")]
    [Authorize(Roles = "Backoffice,GridOperator,Prosumer")]
    public async Task<IActionResult> GetByNic(string nic)
    {
        // The service decides whether this caller may see this profile
        var (callerId, callerRole) = GetCaller();
        var result = await _prosumerService.GetByNicAsync(nic, callerId, callerRole);
        return Ok(ApiResponse<ProsumerResponse>.Ok(result, "Prosumer retrieved successfully."));
    }

    /// <summary>
    /// PUT api/prosumers/{nic} — update profile details (staff, or the prosumer themselves).
    /// </summary>
    [HttpPut("{nic}")]
    [Authorize(Roles = "Backoffice,GridOperator,Prosumer")]
    public async Task<IActionResult> Update(string nic, [FromBody] ProsumerUpdateRequest request)
    {
        // NIC comes from the URL and cannot be changed through the body
        var (callerId, callerRole) = GetCaller();
        var result = await _prosumerService.UpdateAsync(nic, request, callerId, callerRole);
        return Ok(ApiResponse<ProsumerResponse>.Ok(result, "Prosumer updated successfully."));
    }

    /// <summary>
    /// PATCH api/prosumers/{nic}/deactivate — staff deactivate an account straight away; a
    /// prosumer calling it for themselves only sends a request, which blocks the account
    /// until a Backoffice officer approves or rejects it.
    /// </summary>
    [HttpPatch("{nic}/deactivate")]
    [Authorize(Roles = "Backoffice,GridOperator,Prosumer")]
    public async Task<IActionResult> Deactivate(string nic)
    {
        // The service decides between a request and an immediate deactivation
        var (callerId, callerRole) = GetCaller();
        var result = await _prosumerService.DeactivateAsync(nic, callerId, callerRole);
        var message = result.Status == nameof(ProsumerStatus.PendingDeactivation)
            ? "Deactivation request sent. The account is blocked until a Backoffice officer reviews it."
            : "Prosumer account deactivated.";
        return Ok(ApiResponse<ProsumerResponse>.Ok(result, message));
    }

    /// <summary>
    /// PATCH api/prosumers/{nic}/deactivation/approve — approve a deactivation request (Backoffice only).
    /// </summary>
    [HttpPatch("{nic}/deactivation/approve")]
    [Authorize(Roles = "Backoffice")]
    public async Task<IActionResult> ApproveDeactivation(string nic)
    {
        // Role is checked here and again in the service
        var (callerId, callerRole) = GetCaller();
        var result = await _prosumerService.ApproveDeactivationAsync(nic, callerId, callerRole);
        return Ok(ApiResponse<ProsumerResponse>.Ok(result, "Deactivation request approved."));
    }

    /// <summary>
    /// PATCH api/prosumers/{nic}/deactivation/reject — reject a deactivation request (Backoffice only).
    /// </summary>
    [HttpPatch("{nic}/deactivation/reject")]
    [Authorize(Roles = "Backoffice")]
    public async Task<IActionResult> RejectDeactivation(string nic)
    {
        // The account becomes Active again, so the prosumer can sign in
        var (callerId, callerRole) = GetCaller();
        var result = await _prosumerService.RejectDeactivationAsync(nic, callerId, callerRole);
        return Ok(ApiResponse<ProsumerResponse>.Ok(result, "Deactivation request rejected. The account is active again."));
    }

    /// <summary>
    /// PATCH api/prosumers/{nic}/reactivate — reactivate an account (Backoffice only).
    /// </summary>
    [HttpPatch("{nic}/reactivate")]
    [Authorize(Roles = "Backoffice")]
    public async Task<IActionResult> Reactivate(string nic)
    {
        // Role is checked here and again in the service
        var (callerId, callerRole) = GetCaller();
        var result = await _prosumerService.ReactivateAsync(nic, callerId, callerRole);
        return Ok(ApiResponse<ProsumerResponse>.Ok(result, "Prosumer account reactivated."));
    }

    /// <summary>
    /// Reads the caller's id (user id or NIC) and role from the JWT claims.
    /// </summary>
    private (string CallerId, string CallerRole) GetCaller()
    {
        // Prosumer tokens (TokenService) put the NIC in NameIdentifier and "Prosumer" in Role
        var callerId = User.FindFirstValue(ClaimTypes.NameIdentifier) ?? string.Empty;
        var callerRole = User.FindFirstValue(ClaimTypes.Role) ?? string.Empty;
        return (callerId, callerRole);
    }
}