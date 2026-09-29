/*
 * File:        ProsumerReservationTests.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Tests
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-30
 * Description: Unit tests for the prosumer reservation rules used by the
 *              Android app: bookings start Pending, only the owner can reach a
 *              reservation, the QR code is released only once approved, and the
 *              7-day and 12-hour rules apply exactly as they do for staff.
 */

using Microsoft.Extensions.Logging.Abstractions;
using MongoDB.Bson;
using SmartSolar.Api.DTOs;
using SmartSolar.Api.Exceptions;
using SmartSolar.Api.Models;
using SmartSolar.Api.Repositories;
using SmartSolar.Api.Services;

namespace SmartSolar.Tests;

public class ProsumerReservationTests
{
    private const string Nic = "991234567V";
    private const string OtherNic = "200012345678";

    private readonly FakeReservationRepository _reservations = new();
    private readonly FakeProsumerRepository _prosumers = new();
    private readonly FakeNodeRepository _nodes = new();
    private readonly MicrogridNode _node;
    private readonly ReservationService _service;

    /// <summary>
    /// Every test starts with one active prosumer and one active node.
    /// </summary>
    public ProsumerReservationTests()
    {
        // Real service, in-memory repositories
        _prosumers.Items.Add(new Prosumer { Nic = Nic, FullName = "Nimal Perera", Status = ProsumerStatus.Active });
        _node = new MicrogridNode { Id = ObjectId.GenerateNewId().ToString(), Name = "Colombo Central Hub", IsActive = true };
        _nodes.Items.Add(_node);
        _service = new ReservationService(_reservations, _prosumers, _nodes, NullLogger<ReservationService>.Instance);
    }

    [Fact]
    public async Task CreateForProsumerAsync_StartsPendingForTheTokenNic_WithoutAQrCode()
    {
        // A valid request three days ahead
        var result = await _service.CreateForProsumerAsync(Request(DateTime.UtcNow.AddDays(3)), Nic);

        Assert.Equal(ReservationStatus.Pending, result.Status);
        Assert.Equal(Nic, result.ProsumerNic);
        Assert.Equal("Nimal Perera", result.ProsumerName);
        Assert.Equal(_node.Id, result.NodeId);
        Assert.Equal("Colombo Central Hub", result.NodeName);

        // The token exists on the server, but it is not released until approval
        Assert.Equal(string.Empty, result.TransactionQrCode);
        Assert.NotEqual(string.Empty, _reservations.Items.Single().TransactionQrCode);
    }

    [Fact]
    public async Task CreateForProsumerAsync_MoreThanSevenDaysAhead_IsRejected()
    {
        // The 7-day scheduling window
        var exception = await Assert.ThrowsAsync<BusinessRuleException>(() =>
            _service.CreateForProsumerAsync(Request(DateTime.UtcNow.AddDays(10)), Nic));

        Assert.Contains("7 days", exception.Message);
    }

    [Fact]
    public async Task CreateForProsumerAsync_InactiveOrUnknownNode_IsRejected()
    {
        // A node that is switched off, and one that does not exist
        _node.IsActive = false;

        await Assert.ThrowsAsync<BusinessRuleException>(() =>
            _service.CreateForProsumerAsync(Request(DateTime.UtcNow.AddDays(3)), Nic));

        var unknownNode = Request(DateTime.UtcNow.AddDays(3));
        unknownNode.NodeId = ObjectId.GenerateNewId().ToString();
        await Assert.ThrowsAsync<BusinessRuleException>(() =>
            _service.CreateForProsumerAsync(unknownNode, Nic));
    }

    [Fact]
    public async Task CreateForProsumerAsync_DeactivatedProsumer_IsForbidden()
    {
        // A token can still be valid for a while after the account is deactivated
        _prosumers.Items.Single().Status = ProsumerStatus.Deactivated;

        await Assert.ThrowsAsync<ForbiddenException>(() =>
            _service.CreateForProsumerAsync(Request(DateTime.UtcNow.AddDays(3)), Nic));
    }

    [Fact]
    public async Task CreateForProsumerAsync_UnknownType_IsRejected()
    {
        // Only DropOff and Charging exist
        var request = Request(DateTime.UtcNow.AddDays(3));
        request.ReservationType = "Selling";

        await Assert.ThrowsAsync<BusinessRuleException>(() =>
            _service.CreateForProsumerAsync(request, Nic));
    }

    [Fact]
    public async Task GetForProsumerAsync_ListsOnlyOwnReservations_WithQrOnlyWhenApproved()
    {
        // Two of the prosumer's own and one belonging to someone else
        var approved = Stored(Nic, ReservationStatus.Approved, DateTime.UtcNow.AddDays(2));
        var pending = Stored(Nic, ReservationStatus.Pending, DateTime.UtcNow.AddDays(3));
        Stored(OtherNic, ReservationStatus.Approved, DateTime.UtcNow.AddDays(4));

        var result = await _service.GetForProsumerAsync(Nic, null, null);

        Assert.Equal(2, result.Count);
        Assert.Equal(approved.TransactionQrCode, result.Single(r => r.Id == approved.Id).TransactionQrCode);
        Assert.Equal(string.Empty, result.Single(r => r.Id == pending.Id).TransactionQrCode);
    }

    [Fact]
    public async Task GetForProsumerByIdAsync_SomeoneElsesReservation_IsForbidden()
    {
        // Knowing the id is not enough to read another prosumer's booking
        var theirs = Stored(OtherNic, ReservationStatus.Approved, DateTime.UtcNow.AddDays(2));

        await Assert.ThrowsAsync<ForbiddenException>(() => _service.GetForProsumerByIdAsync(theirs.Id!, Nic));
    }

    [Fact]
    public async Task GetForProsumerByIdAsync_MalformedId_IsNotFound()
    {
        // A malformed id must give 404, not a server error
        await Assert.ThrowsAsync<NotFoundException>(() => _service.GetForProsumerByIdAsync("not-an-id", Nic));
    }

    [Fact]
    public async Task UpdateForProsumerAsync_ApprovedReservation_GoesBackToPending()
    {
        // Moving an approved slot must be approved again, so the old QR code stops working
        var reservation = Stored(Nic, ReservationStatus.Approved, DateTime.UtcNow.AddDays(2));
        var oldQr = reservation.TransactionQrCode;
        var newStart = DateTime.UtcNow.AddDays(3);

        var result = await _service.UpdateForProsumerAsync(reservation.Id!, Update(newStart), Nic);

        Assert.Equal(ReservationStatus.Pending, result.Status);
        Assert.Equal(string.Empty, result.TransactionQrCode);
        Assert.Equal(newStart, reservation.SlotStartTime);
        Assert.NotEqual(oldQr, reservation.TransactionQrCode);
    }

    [Fact]
    public async Task UpdateForProsumerAsync_LessThanTwelveHoursBefore_IsRejected()
    {
        // The 12-hour notice rule
        var reservation = Stored(Nic, ReservationStatus.Approved, DateTime.UtcNow.AddHours(6));

        var exception = await Assert.ThrowsAsync<BusinessRuleException>(() =>
            _service.UpdateForProsumerAsync(reservation.Id!, Update(DateTime.UtcNow.AddDays(2)), Nic));

        Assert.Contains("12 hours", exception.Message);
    }

    [Fact]
    public async Task CancelForProsumerAsync_WithoutReason_RecordsTheStandardReason()
    {
        // The app does not ask for a reason
        var reservation = Stored(Nic, ReservationStatus.Approved, DateTime.UtcNow.AddDays(2));

        var result = await _service.CancelForProsumerAsync(reservation.Id!, new ProsumerCancelReservationDto(), Nic);

        Assert.Equal(ReservationStatus.Cancelled, result.Status);
        Assert.Equal("Cancelled by the prosumer in the mobile app.", result.CancellationReason);
        Assert.Equal(string.Empty, result.TransactionQrCode);
    }

    [Fact]
    public async Task CancelForProsumerAsync_LessThanTwelveHoursBefore_IsRejected()
    {
        // The same 12-hour notice rule as for staff
        var reservation = Stored(Nic, ReservationStatus.Approved, DateTime.UtcNow.AddHours(6));

        await Assert.ThrowsAsync<BusinessRuleException>(() =>
            _service.CancelForProsumerAsync(reservation.Id!, new ProsumerCancelReservationDto(), Nic));
    }

    [Fact]
    public async Task CancelForProsumerAsync_SomeoneElsesReservation_IsForbidden()
    {
        // Only the owner can cancel
        var theirs = Stored(OtherNic, ReservationStatus.Approved, DateTime.UtcNow.AddDays(2));

        await Assert.ThrowsAsync<ForbiddenException>(() =>
            _service.CancelForProsumerAsync(theirs.Id!, new ProsumerCancelReservationDto(), Nic));
    }

    /// <summary>
    /// A two-hour drop-off request at the test node, starting at [start].
    /// </summary>
    private ProsumerReservationRequestDto Request(DateTime start)
    {
        // Same fields the Android app posts
        return new ProsumerReservationRequestDto
        {
            NodeId = _node.Id,
            SlotStartTime = start,
            SlotEndTime = start.AddHours(2),
            EnergyAmountKWh = 12.5,
            ReservationType = ReservationType.DropOff
        };
    }

    /// <summary>
    /// A two-hour update to a new start time.
    /// </summary>
    private static UpdateReservationDto Update(DateTime start)
    {
        // Same fields the Android app puts
        return new UpdateReservationDto { SlotStartTime = start, SlotEndTime = start.AddHours(2), EnergyAmountKWh = 20 };
    }

    /// <summary>
    /// Puts a reservation straight into the fake repository.
    /// </summary>
    private Reservation Stored(string nic, string status, DateTime start)
    {
        // Shaped like a reservation the API created earlier
        var reservation = new Reservation
        {
            Id = ObjectId.GenerateNewId().ToString(),
            ReservationNumber = $"RES-TEST-{_reservations.Items.Count + 1}",
            ProsumerNic = nic,
            NodeId = _node.Id,
            NodeName = _node.Name,
            SlotStartTime = start,
            SlotEndTime = start.AddHours(2),
            EnergyAmountKWh = 10,
            ReservationType = ReservationType.DropOff,
            Status = status,
            TransactionQrCode = $"qr-token-{Guid.NewGuid():N}"
        };
        _reservations.Items.Add(reservation);
        return reservation;
    }

    /// <summary>
    /// In-memory reservations with the filters the prosumer methods use.
    /// </summary>
    private sealed class FakeReservationRepository : IReservationRepository
    {
        public List<Reservation> Items { get; } = [];

        public Task<List<Reservation>> GetAllAsync(string? nic, string? nodeId, string? status, DateTime? fromDate, DateTime? toDate, string? search)
        {
            // Only the NIC and status filters matter to these tests
            return Task.FromResult(Items
                .Where(r => nic is null || r.ProsumerNic == nic)
                .Where(r => status is null || r.Status == status)
                .OrderByDescending(r => r.SlotStartTime)
                .ToList());
        }

        public Task<Reservation?> GetByIdAsync(string id)
        {
            // Primary key lookup
            return Task.FromResult(Items.FirstOrDefault(r => r.Id == id));
        }

        public Task<Reservation?> GetByNumberAsync(string reservationNumber)
        {
            // Reference number lookup
            return Task.FromResult(Items.FirstOrDefault(r => r.ReservationNumber == reservationNumber));
        }

        public Task<Reservation> CreateAsync(Reservation reservation)
        {
            // MongoDB would generate the ObjectId
            reservation.Id ??= ObjectId.GenerateNewId().ToString();
            Items.Add(reservation);
            return Task.FromResult(reservation);
        }

        public Task UpdateAsync(Reservation reservation)
        {
            // The list holds the same object, so there is nothing to copy
            return Task.CompletedTask;
        }

        public Task DeleteAsync(string id)
        {
            // Removes the reservation with this id
            Items.RemoveAll(r => r.Id == id);
            return Task.CompletedTask;
        }

        public Task<List<Reservation>> GetActiveReservationsByNodeIdAsync(string nodeId)
        {
            // Pending and Approved reservations at the node
            return Task.FromResult(Items.Where(r => r.NodeId == nodeId &&
                (r.Status == ReservationStatus.Pending || r.Status == ReservationStatus.Approved)).ToList());
        }

        public Task<long> CountOverlappingReservationsAsync(string nodeId, DateTime start, DateTime end, string? excludeId = null)
        {
            // Not used by the prosumer methods
            return Task.FromResult(0L);
        }

        public Task<ReservationStatsDto> GetStatsAsync()
        {
            // Not used by the prosumer methods
            return Task.FromResult(new ReservationStatsDto { TotalReservations = Items.Count });
        }

        public Task EnsureIndexesAsync()
        {
            // Nothing to index in memory
            return Task.CompletedTask;
        }
    }

    /// <summary>
    /// In-memory prosumers.
    /// </summary>
    private sealed class FakeProsumerRepository : IProsumerRepository
    {
        public List<Prosumer> Items { get; } = [];

        public Task<Prosumer?> GetByNicAsync(string nic)
        {
            // NIC is the primary key
            return Task.FromResult(Items.FirstOrDefault(p => p.Nic == nic));
        }

        public Task<Prosumer?> GetByEmailAsync(string email)
        {
            // Emails are stored in lowercase
            return Task.FromResult(Items.FirstOrDefault(p => p.Email == email));
        }

        public Task<List<Prosumer>> GetAllAsync(string? search, ProsumerStatus? status)
        {
            // Not used by the reservation tests
            return Task.FromResult(Items.ToList());
        }

        public Task<bool> NicExistsAsync(string nic)
        {
            // True when a prosumer already has this NIC
            return Task.FromResult(Items.Any(p => p.Nic == nic));
        }

        public Task<bool> EmailExistsAsync(string email, string? excludeNic = null)
        {
            // True when another prosumer already uses this email
            return Task.FromResult(Items.Any(p => p.Email == email && p.Nic != excludeNic));
        }

        public Task CreateAsync(Prosumer prosumer)
        {
            // Keeps the new prosumer in the list
            Items.Add(prosumer);
            return Task.CompletedTask;
        }

        public Task UpdateAsync(Prosumer prosumer)
        {
            // The list holds the same object, so there is nothing to copy
            return Task.CompletedTask;
        }
    }

    /// <summary>
    /// In-memory grid nodes.
    /// </summary>
    private sealed class FakeNodeRepository : INodeRepository
    {
        public List<MicrogridNode> Items { get; } = [];

        public Task<List<MicrogridNode>> GetAllAsync(string? search, bool? isActive)
        {
            // Status filter only
            return Task.FromResult(Items.Where(n => !isActive.HasValue || n.IsActive == isActive).ToList());
        }

        public Task<List<MicrogridNode>> GetActiveAsync()
        {
            // Nodes that can take reservations
            return Task.FromResult(Items.Where(n => n.IsActive).ToList());
        }

        public Task<MicrogridNode?> GetByIdAsync(string id)
        {
            // Primary key lookup
            return Task.FromResult(Items.FirstOrDefault(n => n.Id == id));
        }

        public Task<bool> NodeCodeExistsAsync(string nodeCode, string? excludeId = null)
        {
            // Not used by the reservation tests
            return Task.FromResult(false);
        }

        public Task CreateAsync(MicrogridNode node)
        {
            // Keeps the new node in the list
            Items.Add(node);
            return Task.CompletedTask;
        }

        public Task UpdateAsync(MicrogridNode node)
        {
            // The list holds the same object, so there is nothing to copy
            return Task.CompletedTask;
        }

        public Task EnsureIndexesAsync()
        {
            // Nothing to index in memory
            return Task.CompletedTask;
        }
    }
}
