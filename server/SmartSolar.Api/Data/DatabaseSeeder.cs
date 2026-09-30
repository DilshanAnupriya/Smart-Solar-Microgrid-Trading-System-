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
    private readonly INodeRepository _nodeRepository;
    private readonly IPasswordHasher _passwordHasher;
    private readonly SeedAdminSettings _settings;
    private readonly ILogger<DatabaseSeeder> _logger;

    /// <summary>
    /// Receives repositories, the hasher, the seed settings and the logger.
    /// </summary>
    public DatabaseSeeder(
        IWebUserRepository repository,
        IReservationRepository reservationRepository,
        INodeRepository nodeRepository,
        IPasswordHasher passwordHasher,
        IOptions<SeedAdminSettings> options,
        ILogger<DatabaseSeeder> logger)
    {
        // Store everything the seeding routine needs
        _repository = repository;
        _reservationRepository = reservationRepository;
        _nodeRepository = nodeRepository;
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
        await _nodeRepository.EnsureIndexesAsync();
        await _reservationRepository.EnsureIndexesAsync();

        await SeedNodesAsync();
        await SeedReservationsAsync();

        var existingUsers = await _repository.CountAsync();
        var now = DateTime.UtcNow;

        // If the collection is empty, seed the initial Backoffice administrator
        if (existingUsers == 0 && !string.IsNullOrWhiteSpace(_settings.Email) && !string.IsNullOrWhiteSpace(_settings.Password))
        {
            var admin = new WebUser
            {
                FullName = string.IsNullOrWhiteSpace(_settings.FullName) ? "System Administrator" : _settings.FullName,
                Email = _settings.Email.Trim().ToLowerInvariant(),
                Username = string.IsNullOrWhiteSpace(_settings.Username)
                    ? "admin"
                    : _settings.Username.Trim().ToLowerInvariant(),
                Nic = string.IsNullOrWhiteSpace(_settings.Nic) ? "198012345678" : _settings.Nic.Trim().ToUpperInvariant(),
                Phone = string.IsNullOrWhiteSpace(_settings.Phone) ? "0770000000" : _settings.Phone.Trim(),
                DateOfBirth = _settings.DateOfBirth.HasValue
                    ? DateTime.SpecifyKind(_settings.DateOfBirth.Value.Date, DateTimeKind.Utc)
                    : null,
                PasswordHash = _passwordHasher.Hash(_settings.Password),
                Role = UserRoles.Backoffice,
                IsActive = true,
                CreatedAt = now,
                UpdatedAt = now,
                CreatedBy = "system"
            };

            await _repository.CreateAsync(admin);
            _logger.LogInformation("Seeded the first Backoffice account: {Email}", admin.Email);
        }

        // Seed a default Grid Operator account if one does not exist yet
        var existingOperator = await _repository.GetByIdentifierAsync("operator@smartsolar.lk");
        if (existingOperator is null)
        {
            var gridOperator = new WebUser
            {
                FullName = "Microgrid Site Operator",
                Email = "operator@smartsolar.lk",
                Username = "operator",
                Nic = "199212345678",
                Phone = "0771234567",
                DateOfBirth = new DateTime(1992, 5, 15, 0, 0, 0, DateTimeKind.Utc),
                PasswordHash = _passwordHasher.Hash("Operator@123"),
                Role = UserRoles.GridOperator,
                IsActive = true,
                CreatedAt = now,
                UpdatedAt = now,
                CreatedBy = "system"
            };

            await _repository.CreateAsync(gridOperator);
            _logger.LogInformation("Seeded default Grid Operator account: {Email}", gridOperator.Email);
        }
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

    /// <summary>
    /// Seeds default microgrid solar hubs with GPS coordinates and battery slots if none exist.
    /// </summary>
    private async Task SeedNodesAsync()
    {
        var existingNodes = await _nodeRepository.GetActiveAsync();
        if (existingNodes.Count > 0)
        {
            return;
        }

        var now = DateTime.UtcNow;
        var sampleNodes = new List<MicrogridNode>
        {
            new()
            {
                NodeCode = "NODE-COLOMBO-01",
                Name = "Colombo Central Hub (120 kWh)",
                Address = "Galle Road, Colombo 03, Western Province",
                Latitude = 6.9271,
                Longitude = 79.8612,
                GenerationCapacityKw = 50.0,
                StorageCapacityKWh = 120.0,
                TotalBatterySlots = 10,
                AvailableBatterySlots = 7,
                IsActive = true,
                CreatedAt = now,
                UpdatedAt = now,
                CreatedBy = "system"
            },
            new()
            {
                NodeCode = "NODE-KANDY-02",
                Name = "Kandy Hillcrest Station (80 kWh)",
                Address = "Peradeniya Road, Kandy, Central Province",
                Latitude = 7.2906,
                Longitude = 80.6337,
                GenerationCapacityKw = 35.0,
                StorageCapacityKWh = 80.0,
                TotalBatterySlots = 8,
                AvailableBatterySlots = 5,
                IsActive = true,
                CreatedAt = now,
                UpdatedAt = now,
                CreatedBy = "system"
            },
            new()
            {
                NodeCode = "NODE-GALLE-01",
                Name = "Galle Coastal Solar Grid (150 kWh)",
                Address = "Matara Road, Galle, Southern Province",
                Latitude = 6.0535,
                Longitude = 80.2210,
                GenerationCapacityKw = 60.0,
                StorageCapacityKWh = 150.0,
                TotalBatterySlots = 12,
                AvailableBatterySlots = 9,
                IsActive = true,
                CreatedAt = now,
                UpdatedAt = now,
                CreatedBy = "system"
            }
        };

        foreach (var node in sampleNodes)
        {
            await _nodeRepository.CreateAsync(node);
        }

        _logger.LogInformation("Seeded {Count} initial microgrid hub stations.", sampleNodes.Count);
    }
}
