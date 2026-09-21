/*
 * File:        IWebUserRepository.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Repositories
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Contract for reading and writing web user documents. The
 *              repository performs data access only; every business rule lives
 *              in the service layer.
 */

using SmartSolar.Api.Models;

namespace SmartSolar.Api.Repositories;

public interface IWebUserRepository
{
    /// <summary>
    /// Returns users matching the optional role, status and search filters.
    /// </summary>
    Task<List<WebUser>> GetAllAsync(string? role, bool? isActive, string? search);

    /// <summary>
    /// Returns a single user by id, or null when the id does not exist.
    /// </summary>
    Task<WebUser?> GetByIdAsync(string id);

    /// <summary>
    /// Returns a single user by email address, or null when there is no match.
    /// </summary>
    Task<WebUser?> GetByEmailAsync(string email);

    /// <summary>
    /// Returns a single user whose email address, username or phone number
    /// matches the supplied login identifier, or null when nothing matches.
    /// </summary>
    Task<WebUser?> GetByIdentifierAsync(string identifier);

    /// <summary>
    /// Returns true when the email is already used by another user.
    /// </summary>
    Task<bool> EmailExistsAsync(string email, string? excludeId = null);

    /// <summary>
    /// Returns true when the username is already used by another user.
    /// </summary>
    Task<bool> UsernameExistsAsync(string username, string? excludeId = null);

    /// <summary>
    /// Returns true when the NIC number is already used by another user.
    /// </summary>
    Task<bool> NicExistsAsync(string nic, string? excludeId = null);

    /// <summary>
    /// Inserts a new user document and returns it with the generated id.
    /// </summary>
    Task<WebUser> CreateAsync(WebUser user);

    /// <summary>
    /// Updates the editable fields of an existing user.
    /// </summary>
    Task UpdateAsync(WebUser user);

    /// <summary>
    /// Activates or deactivates a user without touching any other field.
    /// </summary>
    Task SetActiveAsync(string id, bool isActive);

    /// <summary>
    /// Counts all user documents, used to decide whether seeding is needed.
    /// </summary>
    Task<long> CountAsync();

    /// <summary>
    /// Creates the unique indexes on the email, username and NIC fields.
    /// </summary>
    Task EnsureIndexesAsync();
}
