/*
 * File:        IProsumerRepository.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Repositories
 * Author:      Dilshan Anupriya (IT22189530)
 * Created:     2026-09-20
 * Description: Contract for reading and writing prosumer documents in MongoDB.
 */

using SmartSolar.Api.Models;

namespace SmartSolar.Api.Repositories;

public interface IProsumerRepository
{
    /// <summary>Returns the prosumer with the given NIC, or null if none exists.</summary>
    Task<Prosumer?> GetByNicAsync(string nic);

    /// <summary>Returns prosumers matching an optional search term and status.</summary>
    Task<List<Prosumer>> GetAllAsync(string? search, ProsumerStatus? status);

    /// <summary>Returns true if a prosumer with this NIC already exists.</summary>
    Task<bool> NicExistsAsync(string nic);

    /// <summary>Returns true if another prosumer already uses this email.</summary>
    Task<bool> EmailExistsAsync(string email, string? excludeNic = null);

    /// <summary>Inserts a new prosumer document.</summary>
    Task CreateAsync(Prosumer prosumer);

    /// <summary>Replaces an existing prosumer document.</summary>
    Task UpdateAsync(Prosumer prosumer);
}