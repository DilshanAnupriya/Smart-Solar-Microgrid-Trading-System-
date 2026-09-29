/*
 * File:        DatabaseSeeder.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Data
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Runs once when the API starts. Creates the unique index on the
 *              user email field and, if the webUsers collection is empty,
 *              inserts the first Backoffice account so the system can be signed
 *              into for the first time.
 */

using Microsoft.Extensions.Options;
using SmartSolar.Api.Models;
using SmartSolar.Api.Repositories;
using SmartSolar.Api.Security;

namespace SmartSolar.Api.Data;

public class DatabaseSeeder
{
    private readonly IWebUserRepository _repository;
    private readonly IPasswordHasher _passwordHasher;
    private readonly SeedAdminSettings _settings;
    private readonly ILogger<DatabaseSeeder> _logger;

    /// <summary>
    /// Receives the repository, the hasher, the seed settings and the logger.
    /// </summary>
    public DatabaseSeeder(
        IWebUserRepository repository,
        IPasswordHasher passwordHasher,
        IOptions<SeedAdminSettings> options,
        ILogger<DatabaseSeeder> logger)
    {
        // Store everything the seeding routine needs
        _repository = repository;
        _passwordHasher = passwordHasher;
        _settings = options.Value;
        _logger = logger;
    }

    /// <summary>
    /// Creates the indexes and seeds the first Backoffice user when required.
    /// </summary>
    public async Task RunAsync()
    {
        // Indexes are created first so the seeded account is also protected by them
        await _repository.EnsureIndexesAsync();

        var existingUsers = await _repository.CountAsync();

        // Only seed into a completely empty collection, so restarting the API
        // never creates duplicate administrators
        if (existingUsers > 0)
        {
            _logger.LogInformation("Seeding skipped: {Count} web user(s) already exist.", existingUsers);
            return;
        }

        // Without a configured password there is nothing safe to insert
        if (string.IsNullOrWhiteSpace(_settings.Email) || string.IsNullOrWhiteSpace(_settings.Password))
        {
            _logger.LogWarning(
                "No web users exist and SeedAdmin is not configured. " +
                "Set SeedAdmin:Email and SeedAdmin:Password to create the first Backoffice account.");
            return;
        }

        var now = DateTime.UtcNow;

        var admin = new WebUser
        {
            FullName = string.IsNullOrWhiteSpace(_settings.FullName) ? "System Administrator" : _settings.FullName,
            Email = _settings.Email.Trim().ToLowerInvariant(),

            // Fall back to sensible defaults so the seed never fails validation
            Username = string.IsNullOrWhiteSpace(_settings.Username)
                ? "admin"
                : _settings.Username.Trim().ToLowerInvariant(),
            Nic = _settings.Nic.Trim().ToUpperInvariant(),
            Phone = _settings.Phone.Trim(),
            DateOfBirth = _settings.DateOfBirth.HasValue
                ? DateTime.SpecifyKind(_settings.DateOfBirth.Value.Date, DateTimeKind.Utc)
                : null,

            PasswordHash = _passwordHasher.Hash(_settings.Password),
            Role = UserRoles.Backoffice,
            IsActive = true,
            CreatedAt = now,
            UpdatedAt = now,

            // Marked as created by the system because no signed in user made it
            CreatedBy = "system"
        };

        await _repository.CreateAsync(admin);

        _logger.LogInformation("Seeded the first Backoffice account: {Email}", admin.Email);
    }
}
