/*
 * File:        INodeRepository.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Repositories
 * Author:      Vidura Hewaduwa
 * Created:     2026-09-24
 * Description: Contract for reading and writing node documents in MongoDB.
 */

using SmartSolar.Api.Models;

namespace SmartSolar.Api.Repositories;

public interface INodeRepository
{
    /// <summary>Returns nodes matching optional search and active-status filters.</summary>
    Task<List<MicrogridNode>> GetAllAsync(string? search, bool? isActive);

    /// <summary>Returns active nodes used by the nearby-node search.</summary>
    Task<List<MicrogridNode>> GetActiveAsync();

    /// <summary>Returns a node by MongoDB id, or null when it does not exist.</summary>
    Task<MicrogridNode?> GetByIdAsync(string id);

    /// <summary>Checks whether another node already uses a normalized node code.</summary>
    Task<bool> NodeCodeExistsAsync(string nodeCode, string? excludeId = null);

    /// <summary>Inserts a new node document.</summary>
    Task CreateAsync(MicrogridNode node);

    /// <summary>Replaces an existing node document.</summary>
    Task UpdateAsync(MicrogridNode node);

    /// <summary>Creates the unique node-code database index.</summary>
    Task EnsureIndexesAsync();
}
