/*
 * File:        IProsumerService.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Services
 * Author:      Dilshan Anupriya (IT22189530)
 * Created:     2026-09-20
 * Description: Contract for prosumer business logic: registration, profile
 *              updates, deactivation and Backoffice-only reactivation.
 */

using SmartSolar.Api.DTOs.Prosumers;

namespace SmartSolar.Api.Services;

public interface IProsumerService
{
    /// <summary>Creates a prosumer (self-registration or staff creation).</summary>
    Task<ProsumerResponse> CreateAsync(ProsumerCreateRequest request);

    /// <summary>Lists prosumers with optional search and status filter.</summary>
    Task<List<ProsumerResponse>> GetAllAsync(string? search, string? status);

    /// <summary>Returns one prosumer if the caller is allowed to view it.</summary>
    Task<ProsumerResponse> GetByNicAsync(string nic, string callerId, string callerRole);

    /// <summary>Updates profile details if the caller is allowed to edit it.</summary>
    Task<ProsumerResponse> UpdateAsync(string nic, ProsumerUpdateRequest request, string callerId, string callerRole);

    /// <summary>Deactivates a prosumer account.</summary>
    Task<ProsumerResponse> DeactivateAsync(string nic, string callerId, string callerRole);

    /// <summary>Reactivates a deactivated account (Backoffice only).</summary>
    Task<ProsumerResponse> ReactivateAsync(string nic, string callerId, string callerRole);
}