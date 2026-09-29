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
        // Search matches NIC, name, email or phone; status is Active or Deactivated
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
    /// PATCH api/prosumers/{nic}/deactivate — deactivate an account (staff, or the prosumer themselves).
    /// </summary>
    [HttpPatch("{nic}/deactivate")]
    [Authorize(Roles = "Backoffice,GridOperator,Prosumer")]
    public async Task<IActionResult> Deactivate(string nic)
    {
        // Records who deactivated the account and when
        var (callerId, callerRole) = GetCaller();
        var result = await _prosumerService.DeactivateAsync(nic, callerId, callerRole);
        return Ok(ApiResponse<ProsumerResponse>.Ok(result, "Prosumer account deactivated."));
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
        // CHECK with Nadeema: id must be in NameIdentifier and role in the Role claim
        var callerId = User.FindFirstValue(ClaimTypes.NameIdentifier) ?? string.Empty;
        var callerRole = User.FindFirstValue(ClaimTypes.Role) ?? string.Empty;
        return (callerId, callerRole);
    }
}