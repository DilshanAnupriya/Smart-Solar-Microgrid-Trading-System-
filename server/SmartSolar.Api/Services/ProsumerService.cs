/*
 * File:        ProsumerService.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Services
 * Author:      BDA Cooray (IT22189530)
 * Created:     2026-09-20
 * Description: Business logic for prosumer management. Enforces NIC format,
 *              unique NIC and email, self-access for prosumers, deactivation
 *              rules and Backoffice-only reactivation.
 */

using SmartSolar.Api.DTOs.Prosumers;
using SmartSolar.Api.Exceptions;
using SmartSolar.Api.Models;
using SmartSolar.Api.Repositories;
using SmartSolar.Api.Security;
using SmartSolar.Api.Validators;

namespace SmartSolar.Api.Services;

public class ProsumerService : IProsumerService
{
    // Role names as stored in the JWT role claim
    private const string BackofficeRole = "Backoffice";
    private const string GridOperatorRole = "GridOperator";
    private const string ProsumerRole = "Prosumer";

    private readonly IProsumerRepository _repository;
    private readonly IPasswordHasher _passwordHasher;

    /// <summary>
    /// Receives the prosumer repository and password hasher through dependency injection.
    /// </summary>
    public ProsumerService(IProsumerRepository repository, IPasswordHasher passwordHasher)
    {
        // Store dependencies for use in the service methods
        _repository = repository;
        _passwordHasher = passwordHasher;
    }

    /// <summary>
    /// Validates the NIC, checks NIC and email are unique, hashes the password and saves the prosumer.
    /// </summary>
    public async Task<ProsumerResponse> CreateAsync(ProsumerCreateRequest request)
    {
        // Rule: NIC must be a valid Sri Lankan old or new format
        var nic = NicValidator.Normalize(request.Nic);
        if (!NicValidator.IsValid(nic))
        {
            throw new BusinessRuleException("NIC must be 9 digits followed by V or X, or 12 digits.");
        }

        // Rule: NIC is the primary key, so it must be unique
        if (await _repository.NicExistsAsync(nic))
        {
            throw new ConflictException("A prosumer with this NIC is already registered.");
        }

        // Rule: email must not belong to another prosumer
        var email = request.Email.Trim().ToLowerInvariant();
        if (await _repository.EmailExistsAsync(email))
        {
            throw new ConflictException("This email address is already used by another prosumer.");
        }

        var now = DateTime.UtcNow;
        var prosumer = new Prosumer
        {
            Nic = nic,
            FullName = request.FullName.Trim(),
            Email = email,
            Phone = request.Phone.Trim(),
            Address = request.Address.Trim(),
            // Uses the shared IPasswordHasher so prosumer and web user hashing are identical
            PasswordHash = _passwordHasher.Hash(request.Password),
            Status = ProsumerStatus.Active,
            CreatedAt = now,
            UpdatedAt = now
        };

        await _repository.CreateAsync(prosumer);
        return ProsumerResponse.FromModel(prosumer);
    }

    /// <summary>
    /// Returns all prosumers matching the optional search term and status.
    /// </summary>
    public async Task<List<ProsumerResponse>> GetAllAsync(string? search, string? status)
    {
        // Convert the status text to the enum, rejecting unknown values
        ProsumerStatus? statusFilter = null;
        if (!string.IsNullOrWhiteSpace(status))
        {
            if (!Enum.TryParse<ProsumerStatus>(status, ignoreCase: true, out var parsed))
            {
                throw new BusinessRuleException("Status must be 'Active' or 'Deactivated'.");
            }
            statusFilter = parsed;
        }

        var prosumers = await _repository.GetAllAsync(search, statusFilter);
        return prosumers.Select(ProsumerResponse.FromModel).ToList();
    }

    /// <summary>
    /// Returns a single prosumer after checking the caller may view it.
    /// </summary>
    public async Task<ProsumerResponse> GetByNicAsync(string nic, string callerId, string callerRole)
    {
        // Prosumers may only view their own profile; staff may view any
        var normalizedNic = NicValidator.Normalize(nic);
        EnsureCanAccess(normalizedNic, callerId, callerRole);

        var prosumer = await GetExistingAsync(normalizedNic);
        return ProsumerResponse.FromModel(prosumer);
    }

    /// <summary>
    /// Updates name, email, phone and address after access and email checks.
    /// </summary>
    public async Task<ProsumerResponse> UpdateAsync(string nic, ProsumerUpdateRequest request, string callerId, string callerRole)
    {
        // Prosumers may only edit their own profile; NIC itself cannot change
        var normalizedNic = NicValidator.Normalize(nic);
        EnsureCanAccess(normalizedNic, callerId, callerRole);

        var prosumer = await GetExistingAsync(normalizedNic);

        // Rule: new email must not belong to a different prosumer
        var email = request.Email.Trim().ToLowerInvariant();
        if (await _repository.EmailExistsAsync(email, excludeNic: normalizedNic))
        {
            throw new ConflictException("This email address is already used by another prosumer.");
        }

        prosumer.FullName = request.FullName.Trim();
        prosumer.Email = email;
        prosumer.Phone = request.Phone.Trim();
        prosumer.Address = request.Address.Trim();
        prosumer.UpdatedAt = DateTime.UtcNow;

        await _repository.UpdateAsync(prosumer);
        return ProsumerResponse.FromModel(prosumer);
    }

    /// <summary>
    /// Deactivates an active account and records who did it and when.
    /// </summary>
    public async Task<ProsumerResponse> DeactivateAsync(string nic, string callerId, string callerRole)
    {
        // Staff can deactivate anyone; a prosumer can only deactivate themselves
        var normalizedNic = NicValidator.Normalize(nic);
        EnsureCanAccess(normalizedNic, callerId, callerRole);

        var prosumer = await GetExistingAsync(normalizedNic);

        // Rule: cannot deactivate an account that is already deactivated
        if (prosumer.Status == ProsumerStatus.Deactivated)
        {
            throw new BusinessRuleException("This prosumer account is already deactivated.");
        }

        var now = DateTime.UtcNow;
        prosumer.Status = ProsumerStatus.Deactivated;
        prosumer.DeactivatedAt = now;
        prosumer.DeactivatedBy = callerId;
        prosumer.UpdatedAt = now;

        await _repository.UpdateAsync(prosumer);
        return ProsumerResponse.FromModel(prosumer);
    }

    /// <summary>
    /// Reactivates a deactivated account. Only Backoffice users are allowed.
    /// </summary>
    public async Task<ProsumerResponse> ReactivateAsync(string nic, string callerId, string callerRole)
    {
        // Rule: only Backoffice can reactivate (checked here as well as on the controller)
        if (callerRole != BackofficeRole)
        {
            throw new ForbiddenException("Only Backoffice users can reactivate a prosumer account.");
        }

        var prosumer = await GetExistingAsync(NicValidator.Normalize(nic));

        // Rule: cannot reactivate an account that is already active
        if (prosumer.Status == ProsumerStatus.Active)
        {
            throw new BusinessRuleException("This prosumer account is already active.");
        }

        prosumer.Status = ProsumerStatus.Active;
        prosumer.DeactivatedAt = null;
        prosumer.DeactivatedBy = null;
        prosumer.UpdatedAt = DateTime.UtcNow;

        await _repository.UpdateAsync(prosumer);
        return ProsumerResponse.FromModel(prosumer);
    }

    /// <summary>
    /// Loads a prosumer by NIC or throws NotFoundException.
    /// </summary>
    private async Task<Prosumer> GetExistingAsync(string nic)
    {
        // Shared lookup used by every method that works on one prosumer
        return await _repository.GetByNicAsync(nic)
               ?? throw new NotFoundException($"No prosumer found with NIC {nic}.");
    }

    /// <summary>
    /// Allows staff to access any profile and prosumers to access only their own.
    /// </summary>
    private static void EnsureCanAccess(string nic, string callerId, string callerRole)
    {
        // Staff roles are allowed through without further checks
        if (callerRole == BackofficeRole || callerRole == GridOperatorRole)
        {
            return;
        }

        // A prosumer's NIC in the token must match the NIC in the URL
        if (callerRole == ProsumerRole && string.Equals(NicValidator.Normalize(callerId), nic, StringComparison.Ordinal))
        {
            return;
        }

        throw new ForbiddenException("You can only access your own prosumer profile.");
    }
}