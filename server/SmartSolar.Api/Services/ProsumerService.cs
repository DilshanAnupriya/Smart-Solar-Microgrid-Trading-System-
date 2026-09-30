/*
 * File:        ProsumerService.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Services
 * Author:      BDA Cooray (IT22189530)
 * Created:     2026-09-20
 * Description: Business logic for prosumer management. Enforces NIC format,
 *              unique NIC and email, self-access for prosumers, deactivation
 *              rules and Backoffice-only reactivation. A prosumer cannot
 *              deactivate their own account directly: they send a request,
 *              which blocks the account until a Backoffice officer approves
 *              or rejects it.
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
                throw new BusinessRuleException("Status must be 'Active', 'PendingDeactivation' or 'Deactivated'.");
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

        // Rule: a blocked prosumer (pending request or deactivated) cannot edit their own profile
        if (callerRole == ProsumerRole && prosumer.Status != ProsumerStatus.Active)
        {
            throw new ForbiddenException("Your account is blocked, so your profile cannot be changed.");
        }

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
        // Staff can deactivate anyone; a prosumer can only request it for themselves
        var normalizedNic = NicValidator.Normalize(nic);
        EnsureCanAccess(normalizedNic, callerId, callerRole);

        var prosumer = await GetExistingAsync(normalizedNic);

        // Rule: cannot deactivate an account that is already deactivated
        if (prosumer.Status == ProsumerStatus.Deactivated)
        {
            throw new BusinessRuleException("This prosumer account is already deactivated.");
        }

        var now = DateTime.UtcNow;

        if (callerRole == ProsumerRole)
        {
            // Rule: only one open request at a time
            if (prosumer.Status == ProsumerStatus.PendingDeactivation)
            {
                throw new BusinessRuleException("A deactivation request is already waiting for Backoffice approval.");
            }

            // The account is blocked from now on, but only deactivated once approved
            prosumer.Status = ProsumerStatus.PendingDeactivation;
            prosumer.DeactivationRequestedAt = now;
            prosumer.UpdatedAt = now;

            await _repository.UpdateAsync(prosumer);
            return ProsumerResponse.FromModel(prosumer);
        }

        // Staff deactivation is immediate, and also settles any open request
        MarkDeactivated(prosumer, callerId, now);

        await _repository.UpdateAsync(prosumer);
        return ProsumerResponse.FromModel(prosumer);
    }

    /// <summary>
    /// Approves a prosumer's deactivation request. Only Backoffice users are allowed.
    /// </summary>
    public async Task<ProsumerResponse> ApproveDeactivationAsync(string nic, string callerId, string callerRole)
    {
        // Rule: only Backoffice can decide on a deactivation request
        EnsureBackoffice(callerRole, "Only Backoffice users can approve a deactivation request.");

        var prosumer = await GetPendingRequestAsync(nic);
        MarkDeactivated(prosumer, callerId, DateTime.UtcNow);

        await _repository.UpdateAsync(prosumer);
        return ProsumerResponse.FromModel(prosumer);
    }

    /// <summary>
    /// Rejects a prosumer's deactivation request, unblocking the account. Only Backoffice users are allowed.
    /// </summary>
    public async Task<ProsumerResponse> RejectDeactivationAsync(string nic, string callerId, string callerRole)
    {
        // Rule: only Backoffice can decide on a deactivation request
        EnsureBackoffice(callerRole, "Only Backoffice users can reject a deactivation request.");

        var prosumer = await GetPendingRequestAsync(nic);

        // The prosumer can sign in and trade again
        prosumer.Status = ProsumerStatus.Active;
        prosumer.DeactivationRequestedAt = null;
        prosumer.UpdatedAt = DateTime.UtcNow;

        await _repository.UpdateAsync(prosumer);
        return ProsumerResponse.FromModel(prosumer);
    }

    /// <summary>
    /// Reactivates a deactivated account. Only Backoffice users are allowed.
    /// </summary>
    public async Task<ProsumerResponse> ReactivateAsync(string nic, string callerId, string callerRole)
    {
        // Rule: only Backoffice can reactivate (checked here as well as on the controller)
        EnsureBackoffice(callerRole, "Only Backoffice users can reactivate a prosumer account.");

        var prosumer = await GetExistingAsync(NicValidator.Normalize(nic));

        // Rule: cannot reactivate an account that is already active
        if (prosumer.Status == ProsumerStatus.Active)
        {
            throw new BusinessRuleException("This prosumer account is already active.");
        }

        // Rule: an open request is settled with approve or reject, not reactivate
        if (prosumer.Status == ProsumerStatus.PendingDeactivation)
        {
            throw new BusinessRuleException("This account has a pending deactivation request. Approve or reject it instead.");
        }

        prosumer.Status = ProsumerStatus.Active;
        prosumer.DeactivationRequestedAt = null;
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
    /// Loads a prosumer that has an open deactivation request, or throws.
    /// </summary>
    private async Task<Prosumer> GetPendingRequestAsync(string nic)
    {
        // Approve and reject only make sense while a request is waiting
        var prosumer = await GetExistingAsync(NicValidator.Normalize(nic));
        if (prosumer.Status != ProsumerStatus.PendingDeactivation)
        {
            throw new BusinessRuleException("This prosumer has no pending deactivation request.");
        }
        return prosumer;
    }

    /// <summary>
    /// Sets the account to Deactivated and records who did it and when.
    /// </summary>
    private static void MarkDeactivated(Prosumer prosumer, string callerId, DateTime now)
    {
        // DeactivationRequestedAt is kept, so the history still shows when the prosumer asked
        prosumer.Status = ProsumerStatus.Deactivated;
        prosumer.DeactivatedAt = now;
        prosumer.DeactivatedBy = callerId;
        prosumer.UpdatedAt = now;
    }

    /// <summary>
    /// Throws ForbiddenException unless the caller is a Backoffice user.
    /// </summary>
    private static void EnsureBackoffice(string callerRole, string message)
    {
        // Checked here as well as by [Authorize] on the controller
        if (callerRole != BackofficeRole)
        {
            throw new ForbiddenException(message);
        }
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