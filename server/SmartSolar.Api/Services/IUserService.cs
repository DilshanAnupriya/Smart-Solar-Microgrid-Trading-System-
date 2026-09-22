/*
 * File:        IUserService.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Services
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Contract for the web user management business logic used by the
 *              Backoffice administration screens.
 */

using SmartSolar.Api.DTOs;

namespace SmartSolar.Api.Services;

public interface IUserService
{
    /// <summary>
    /// Lists users, optionally filtered by role, status and a search term.
    /// </summary>
    Task<List<UserResponseDto>> GetAllAsync(string? role, string? status, string? search);

    /// <summary>
    /// Returns one user, or throws NotFoundException when the id is unknown.
    /// </summary>
    Task<UserResponseDto> GetByIdAsync(string id);

    /// <summary>
    /// Creates a Backoffice or Grid Operator account.
    /// </summary>
    Task<UserResponseDto> CreateAsync(CreateUserDto dto, string createdByUserId);

    /// <summary>
    /// Updates an existing account's name, email and role.
    /// </summary>
    Task<UserResponseDto> UpdateAsync(string id, UpdateUserDto dto);

    /// <summary>
    /// Deactivates an account so the user can no longer sign in.
    /// </summary>
    Task DeactivateAsync(string id, string currentUserId);

    /// <summary>
    /// Reactivates a previously deactivated account.
    /// </summary>
    Task ActivateAsync(string id);

    /// <summary>
    /// Permanently deletes an account. Only a Backoffice officer may do this,
    /// which is enforced by the [Authorize] attribute on the controller.
    /// </summary>
    Task DeleteAsync(string id, string currentUserId);
}
