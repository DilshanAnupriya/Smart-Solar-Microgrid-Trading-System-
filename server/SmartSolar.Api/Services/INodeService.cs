/*
 * File:        INodeService.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Services
 * Author:      Vidura Hewaduwa
 * Created:     2026-09-24
 * Description: Contract for node business rules, status changes, schedules,
 *              battery availability and nearby searches.
 */

using SmartSolar.Api.DTOs;

namespace SmartSolar.Api.Services;

public interface INodeService
{
    /// <summary>Lists nodes for the staff management console.</summary>
    Task<List<NodeResponseDto>> GetAllAsync(string? search, bool? isActive);
    /// <summary>Returns a single node or throws a not-found error.</summary>
    Task<NodeResponseDto> GetByIdAsync(string id);
    /// <summary>Validates and creates a new active node.</summary>
    Task<NodeResponseDto> CreateAsync(CreateNodeDto request, string createdBy);
    /// <summary>Validates and updates general node details.</summary>
    Task<NodeResponseDto> UpdateAsync(string id, UpdateNodeDto request);
    /// <summary>Validates and replaces the weekly operating schedule.</summary>
    Task<NodeResponseDto> UpdateScheduleAsync(string id, UpdateNodeScheduleDto request);
    /// <summary>Updates live available slots without changing the total.</summary>
    Task<NodeResponseDto> UpdateSlotsAsync(string id, UpdateNodeSlotsDto request);
    /// <summary>Soft-deactivates a node when no active reservation exists.</summary>
    Task<NodeResponseDto> DeactivateAsync(string id);
    /// <summary>Restores an inactive node.</summary>
    Task<NodeResponseDto> ActivateAsync(string id);
    /// <summary>Returns active nodes inside a radius, nearest first.</summary>
    Task<List<NodeResponseDto>> GetNearbyAsync(double latitude, double longitude, double radiusKm);
}
