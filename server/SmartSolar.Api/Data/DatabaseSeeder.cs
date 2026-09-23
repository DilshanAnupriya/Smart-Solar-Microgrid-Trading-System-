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
    private readonly IReservationRepository _reservationRepository;
    private readonly IPasswordHasher _passwordHasher;
    private readonly SeedAdminSettings _settings;
    private readonly ILogger<DatabaseSeeder> _logger;

    /// <summary>
    /// Receives repositories, the hasher, the seed settings and the logger.
    /// </summary>
    public DatabaseSeeder(
        IWebUserRepository repository,
        IReservationRepository reservationRepository,
        IPasswordHasher passwordHasher,
        IOptions<SeedAdminSettings> options,
        ILogger<DatabaseSeeder> logger)
    {
        // Store everything the seeding routine needs
        _repository = repository;
        _reservationRepository = reservationRepository;
        _passwordHasher = passwordHasher;
        _settings = options.Value;
        _logger = logger;
    }

    /// <summary>
    /// Creates the indexes and seeds initial test data when required.
    /// </summary>
    public async Task RunAsync()
    {
        // Indexes are created first so all documents are protected by them
        await _repository.EnsureIndexesAsync();
        await _reservationRepository.EnsureIndexesAsync();

        await SeedReservationsAsync();

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

    /// <summary>
    /// Seeds sample power trading reservations if the collection is empty.
    /// </summary>
    private async Task SeedReservationsAsync()
    {
        // Avoid inserting duplicate sample data if reservations already exist
        var stats = await _reservationRepository.GetStatsAsync();
        if (stats.TotalReservations > 0)
        {
            return;
        }

        var now = DateTime.UtcNow;

        var sampleReservations = new List<Reservation>
        {
            new()
            {
                ReservationNumber = $"RES-{now:yyyyMMdd}-A101",
                ProsumerNic = "200012345678",
                ProsumerName = "Kasun Perera",
                NodeId = "NODE-COLOMBO-01",
                NodeName = "Colombo Central Hub (120 kWh)",
                SlotStartTime = now.AddDays(2).Date.AddHours(10), // in 2 days at 10:00 (> 12h notice)
                SlotEndTime = now.AddDays(2).Date.AddHours(12),
                EnergyAmountKWh = 25.5,
                ReservationType = ReservationType.DropOff,
                Status = ReservationStatus.Approved,
                TransactionQrCode = Convert.ToBase64String(System.Text.Encoding.UTF8.GetBytes($"RES-{now:yyyyMMdd}-A101|200012345678|NODE-COLOMBO-01")),
                Notes = "Excess residential solar feed-in",
                CreatedAt = now,
                UpdatedAt = now,
                CreatedBy = "system"
            },
            new()
            {
                ReservationNumber = $"RES-{now:yyyyMMdd}-B202",
                ProsumerNic = "199856789012",
                ProsumerName = "Nimali Silva",
                NodeId = "NODE-KANDY-02",
                NodeName = "Kandy Hillcrest Station (80 kWh)",
                SlotStartTime = now.AddDays(4).Date.AddHours(14), // in 4 days (> 12h notice)
                SlotEndTime = now.AddDays(4).Date.AddHours(16),
                EnergyAmountKWh = 40.0,
                ReservationType = ReservationType.Charging,
                Status = ReservationStatus.Approved,
                TransactionQrCode = Convert.ToBase64String(System.Text.Encoding.UTF8.GetBytes($"RES-{now:yyyyMMdd}-B202|199856789012|NODE-KANDY-02")),
                Notes = "EV commercial battery fast charge",
                CreatedAt = now,
                UpdatedAt = now,
                CreatedBy = "system"
            },
            new()
            {
                ReservationNumber = $"RES-{now:yyyyMMdd}-C303",
                ProsumerNic = "199544332211",
                ProsumerName = "Chaminda Bandara",
                NodeId = "NODE-GALLE-01",
                NodeName = "Galle Coastal Solar Grid (150 kWh)",
                SlotStartTime = now.AddHours(3), // in 3 hours (< 12h notice: demonstrate rule enforcement!)
                SlotEndTime = now.AddHours(5),
                EnergyAmountKWh = 18.0,
                ReservationType = ReservationType.DropOff,
                Status = ReservationStatus.Approved,
                TransactionQrCode = Convert.ToBase64String(System.Text.Encoding.UTF8.GetBytes($"RES-{now:yyyyMMdd}-C303|199544332211|NODE-GALLE-01")),
                Notes = "Notice window is under 12 hours (editing/cancellation blocked)",
                CreatedAt = now.AddDays(-1),
                UpdatedAt = now.AddDays(-1),
                CreatedBy = "system"
            },
            new()
            {
                ReservationNumber = $"RES-{now:yyyyMMdd}-D404",
                ProsumerNic = "200199887766",
                ProsumerName = "Dilshan Fernando",
                NodeId = "NODE-COLOMBO-01",
                NodeName = "Colombo Central Hub (120 kWh)",
                SlotStartTime = now.AddDays(6).Date.AddHours(9),
                SlotEndTime = now.AddDays(6).Date.AddHours(11),
                EnergyAmountKWh = 30.0,
                ReservationType = ReservationType.DropOff,
                Status = ReservationStatus.Pending,
                TransactionQrCode = Convert.ToBase64String(System.Text.Encoding.UTF8.GetBytes($"RES-{now:yyyyMMdd}-D404|200199887766|NODE-COLOMBO-01")),
                Notes = "Awaiting grid operator verification",
                CreatedAt = now,
                UpdatedAt = now,
                CreatedBy = "system"
            }
        };

        foreach (var reservation in sampleReservations)
        {
            await _reservationRepository.CreateAsync(reservation);
        }

        _logger.LogInformation("Seeded {Count} initial power trading reservations.", sampleReservations.Count);
    }
}
