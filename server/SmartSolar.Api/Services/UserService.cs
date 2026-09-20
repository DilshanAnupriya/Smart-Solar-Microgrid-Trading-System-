/*
 * File:        UserService.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Services
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Business logic for creating and managing web application users.
 *              Following the FAT service pattern, every rule lives here and not
 *              in the controllers or in the React client: unique email, valid
 *              role, password hashing, and the rule that a user may not
 *              deactivate their own account.
 */

using SmartSolar.Api.DTOs;
using SmartSolar.Api.Exceptions;
using SmartSolar.Api.Models;
using SmartSolar.Api.Repositories;
using SmartSolar.Api.Security;

namespace SmartSolar.Api.Services;

public class UserService : IUserService
{
    private readonly IWebUserRepository _repository;
    private readonly IPasswordHasher _passwordHasher;

    /// <summary>
    /// Receives the repository and the password hasher by dependency injection.
    /// </summary>
    public UserService(IWebUserRepository repository, IPasswordHasher passwordHasher)
    {
        // Store the collaborators this service needs
        _repository = repository;
        _passwordHasher = passwordHasher;
    }

    /// <summary>
    /// Lists users, optionally filtered by role, status and a search term.
    /// </summary>
    public async Task<List<UserResponseDto>> GetAllAsync(string? role, string? status, string? search)
    {
        // Reject an unknown role early rather than silently returning an empty list
        if (!string.IsNullOrWhiteSpace(role) && !UserRoles.IsValid(role))
        {
            throw new BusinessRuleException($"Role must be one of: {string.Join(", ", UserRoles.All)}.");
        }

        // Translate the status text used by the UI into the boolean stored in MongoDB
        bool? isActive = status?.Trim().ToLowerInvariant() switch
        {
            "active" => true,
            "inactive" => false,
            "deactivated" => false,
            null or "" or "all" => null,
            _ => throw new BusinessRuleException("Status must be 'active', 'inactive' or 'all'.")
        };

        var users = await _repository.GetAllAsync(role, isActive, search);

        return users.Select(ToResponse).ToList();
    }

    /// <summary>
    /// Returns one user, or throws NotFoundException when the id is unknown.
    /// </summary>
    public async Task<UserResponseDto> GetByIdAsync(string id)
    {
        // A missing record is an expected failure, so it throws a typed exception
        var user = await _repository.GetByIdAsync(id)
                   ?? throw new NotFoundException("User not found.");

        return ToResponse(user);
    }

    /// <summary>
    /// Creates a Backoffice or Grid Operator account.
    /// </summary>
    public async Task<UserResponseDto> CreateAsync(CreateUserDto dto, string createdByUserId)
    {
        // Business rule: the role must be one of the two supported roles
        if (!UserRoles.IsValid(dto.Role))
        {
            throw new BusinessRuleException($"Role must be one of: {string.Join(", ", UserRoles.All)}.");
        }

        var email = dto.Email.Trim().ToLowerInvariant();

        // Business rule: an email address may only be used by one account
        if (await _repository.EmailExistsAsync(email))
        {
            throw new ConflictException("An account with this email address already exists.");
        }

        var now = DateTime.UtcNow;

        var user = new WebUser
        {
            FullName = dto.FullName.Trim(),
            Email = email,

            // The plain password is hashed immediately and never stored as typed
            PasswordHash = _passwordHasher.Hash(dto.Password),
            Role = dto.Role,
            IsActive = true,
            CreatedAt = now,
            UpdatedAt = now,
            CreatedBy = createdByUserId
        };

        var created = await _repository.CreateAsync(user);

        return ToResponse(created);
    }

    /// <summary>
    /// Updates an existing account's name, email and role.
    /// </summary>
    public async Task<UserResponseDto> UpdateAsync(string id, UpdateUserDto dto)
    {
        var user = await _repository.GetByIdAsync(id)
                   ?? throw new NotFoundException("User not found.");

        // Business rule: the role must still be one of the two supported roles
        if (!UserRoles.IsValid(dto.Role))
        {
            throw new BusinessRuleException($"Role must be one of: {string.Join(", ", UserRoles.All)}.");
        }

        var email = dto.Email.Trim().ToLowerInvariant();

        // Business rule: the new email must not belong to a different account
        if (await _repository.EmailExistsAsync(email, excludeId: id))
        {
            throw new ConflictException("An account with this email address already exists.");
        }

        user.FullName = dto.FullName.Trim();
        user.Email = email;
        user.Role = dto.Role;
        user.UpdatedAt = DateTime.UtcNow;

        await _repository.UpdateAsync(user);

        return ToResponse(user);
    }

    /// <summary>
    /// Deactivates an account so the user can no longer sign in.
    /// </summary>
    public async Task DeactivateAsync(string id, string currentUserId)
    {
        // Business rule: a signed in user may not deactivate their own account,
        // because that would immediately lock them out of the system
        if (string.Equals(id, currentUserId, StringComparison.OrdinalIgnoreCase))
        {
            throw new BusinessRuleException("You cannot deactivate your own account.");
        }

        var user = await _repository.GetByIdAsync(id)
                   ?? throw new NotFoundException("User not found.");

        // Nothing to do when the account is already deactivated
        if (!user.IsActive)
        {
            throw new BusinessRuleException("This account is already deactivated.");
        }

        await _repository.SetActiveAsync(id, false);
    }

    /// <summary>
    /// Reactivates a previously deactivated account.
    /// </summary>
    public async Task ActivateAsync(string id)
    {
        var user = await _repository.GetByIdAsync(id)
                   ?? throw new NotFoundException("User not found.");

        // Nothing to do when the account is already active
        if (user.IsActive)
        {
            throw new BusinessRuleException("This account is already active.");
        }

        await _repository.SetActiveAsync(id, true);
    }

    /// <summary>
    /// Maps a stored user document onto the safe response shape using the
    /// shared mapper, which has no password field of any kind.
    /// </summary>
    private static UserResponseDto ToResponse(WebUser user)
    {
        // Delegates to UserMapper so every service returns exactly the same shape
        return UserMapper.ToResponse(user);
    }
}
