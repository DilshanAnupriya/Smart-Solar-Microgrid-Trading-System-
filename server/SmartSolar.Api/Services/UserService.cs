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
        var username = dto.Username.Trim().ToLowerInvariant();
        var nic = dto.Nic.Trim().ToUpperInvariant();

        // Business rule: the date of birth must be a real past date
        var dateOfBirth = NormaliseDateOfBirth(dto.DateOfBirth);

        // Business rule: email, username and NIC may each belong to one account
        await EnsureIdentifiersAreFreeAsync(email, username, nic);

        var now = DateTime.UtcNow;

        var user = new WebUser
        {
            FullName = dto.FullName.Trim(),
            Email = email,
            Username = username,
            Nic = nic,
            Phone = dto.Phone.Trim(),
            DateOfBirth = dateOfBirth,

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
        var username = dto.Username.Trim().ToLowerInvariant();
        var nic = dto.Nic.Trim().ToUpperInvariant();

        // Business rule: the date of birth must be a real past date
        var dateOfBirth = NormaliseDateOfBirth(dto.DateOfBirth);

        // Business rule: the new values must not belong to a different account
        await EnsureIdentifiersAreFreeAsync(email, username, nic, excludeId: id);

        user.FullName = dto.FullName.Trim();
        user.Email = email;
        user.Username = username;
        user.Nic = nic;
        user.Phone = dto.Phone.Trim();
        user.DateOfBirth = dateOfBirth;
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
    /// Checks that the email, username and NIC are not already taken by a
    /// different account, and reports the first clash it finds.
    /// </summary>
    private async Task EnsureIdentifiersAreFreeAsync(
        string email,
        string username,
        string nic,
        string? excludeId = null)
    {
        // excludeId lets an account keep its own values while being edited
        if (await _repository.EmailExistsAsync(email, excludeId))
        {
            throw new ConflictException("An account with this email address already exists.");
        }

        if (await _repository.UsernameExistsAsync(username, excludeId))
        {
            throw new ConflictException("An account with this username already exists.");
        }

        if (await _repository.NicExistsAsync(nic, excludeId))
        {
            throw new ConflictException("An account with this NIC number already exists.");
        }
    }

    /// <summary>
    /// Validates the date of birth and returns it as a UTC date.
    /// </summary>
    private static DateTime NormaliseDateOfBirth(DateTime? dateOfBirth)
    {
        // [Required] already rejected a missing value, so this is a safety net
        if (!dateOfBirth.HasValue)
        {
            throw new BusinessRuleException("Date of birth is required.");
        }

        // Only the date part matters, and it is stored as UTC midnight
        var value = DateTime.SpecifyKind(dateOfBirth.Value.Date, DateTimeKind.Utc);

        // Business rule: a date of birth cannot be today or in the future
        if (value >= DateTime.UtcNow.Date)
        {
            throw new BusinessRuleException("Date of birth must be in the past.");
        }

        // Business rule: reject an obviously impossible date
        if (value < new DateTime(1900, 1, 1, 0, 0, 0, DateTimeKind.Utc))
        {
            throw new BusinessRuleException("Date of birth must be on or after 1 January 1900.");
        }

        return value;
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
