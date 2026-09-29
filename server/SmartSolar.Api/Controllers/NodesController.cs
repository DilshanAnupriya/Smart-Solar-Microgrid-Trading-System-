/*
 * File:        NodesController.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Controllers
 * Author:      Vidura Hewaduwa
 * Created:     2026-09-24
 * Description: REST endpoints for microgrid node management. Role attributes
 *              enforce permissions while NodeService enforces business rules.
 */

using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolar.Api.Common;
using SmartSolar.Api.DTOs;
using SmartSolar.Api.Services;

namespace SmartSolar.Api.Controllers;

[ApiController]
[Route("api/nodes")]
[Authorize]
public class NodesController : ControllerBase
{
    private readonly INodeService _nodeService;

    public NodesController(INodeService nodeService)
    {
        // The controller delegates all business decisions to the service
        _nodeService = nodeService;
    }

    /// <summary>GET api/nodes — staff list with optional search and status filters.</summary>
    [HttpGet]
    [Authorize(Roles = "Backoffice,GridOperator")]
    public async Task<IActionResult> GetAll([FromQuery] string? search, [FromQuery] bool? isActive)
    {
        var result = await _nodeService.GetAllAsync(search, isActive);
        return Ok(ApiResponse<List<NodeResponseDto>>.Ok(result, $"{result.Count} node(s) found."));
    }

    /// <summary>GET api/nodes/nearby — active nodes near a mobile prosumer.</summary>
    [HttpGet("nearby")]
    [Authorize(Roles = "Prosumer")]
    public async Task<IActionResult> GetNearby(
        [FromQuery] double latitude,
        [FromQuery] double longitude,
        [FromQuery] double radiusKm = 10)
    {
        var result = await _nodeService.GetNearbyAsync(latitude, longitude, radiusKm);
        return Ok(ApiResponse<List<NodeResponseDto>>.Ok(result, $"{result.Count} nearby node(s) found."));
    }

    /// <summary>GET api/nodes/{id} — full node information for either web role.</summary>
    [HttpGet("{id}")]
    [Authorize(Roles = "Backoffice,GridOperator")]
    public async Task<IActionResult> GetById(string id)
    {
        var result = await _nodeService.GetByIdAsync(id);
        return Ok(ApiResponse<NodeResponseDto>.Ok(result, "Node retrieved successfully."));
    }

    /// <summary>POST api/nodes — Backoffice creates a node.</summary>
    [HttpPost]
    [Authorize(Roles = "Backoffice")]
    public async Task<IActionResult> Create([FromBody] CreateNodeDto request)
    {
        // Audit the staff account responsible for creating the node
        var createdBy = User.FindFirstValue(ClaimTypes.NameIdentifier) ?? "system";
        var result = await _nodeService.CreateAsync(request, createdBy);
        return CreatedAtAction(nameof(GetById), new { id = result.Id },
            ApiResponse<NodeResponseDto>.Ok(result, "Node created successfully."));
    }

    /// <summary>PUT api/nodes/{id} — Backoffice updates general details.</summary>
    [HttpPut("{id}")]
    [Authorize(Roles = "Backoffice")]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateNodeDto request)
    {
        var result = await _nodeService.UpdateAsync(id, request);
        return Ok(ApiResponse<NodeResponseDto>.Ok(result, "Node updated successfully."));
    }

    /// <summary>PUT api/nodes/{id}/schedule — Backoffice replaces the schedule.</summary>
    [HttpPut("{id}/schedule")]
    [Authorize(Roles = "Backoffice")]
    public async Task<IActionResult> UpdateSchedule(string id, [FromBody] UpdateNodeScheduleDto request)
    {
        var result = await _nodeService.UpdateScheduleAsync(id, request);
        return Ok(ApiResponse<NodeResponseDto>.Ok(result, "Operating schedule updated successfully."));
    }

    /// <summary>PATCH api/nodes/{id}/slots — Grid Operator updates live availability.</summary>
    [HttpPatch("{id}/slots")]
    [Authorize(Roles = "GridOperator")]
    public async Task<IActionResult> UpdateSlots(string id, [FromBody] UpdateNodeSlotsDto request)
    {
        var result = await _nodeService.UpdateSlotsAsync(id, request);
        return Ok(ApiResponse<NodeResponseDto>.Ok(result, "Available battery slots updated successfully."));
    }

    /// <summary>PATCH api/nodes/{id}/deactivate — Backoffice soft-deactivates a node.</summary>
    [HttpPatch("{id}/deactivate")]
    [Authorize(Roles = "Backoffice")]
    public async Task<IActionResult> Deactivate(string id)
    {
        var result = await _nodeService.DeactivateAsync(id);
        return Ok(ApiResponse<NodeResponseDto>.Ok(result, "Node deactivated successfully."));
    }

    /// <summary>PATCH api/nodes/{id}/activate — Backoffice restores a node.</summary>
    [HttpPatch("{id}/activate")]
    [Authorize(Roles = "Backoffice")]
    public async Task<IActionResult> Activate(string id)
    {
        var result = await _nodeService.ActivateAsync(id);
        return Ok(ApiResponse<NodeResponseDto>.Ok(result, "Node activated successfully."));
    }
}
