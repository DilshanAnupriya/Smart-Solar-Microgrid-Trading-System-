/*
 * File:        ReservationRepository.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Repositories
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-22
 * Description: MongoDB implementation of IReservationRepository. Handles database
 *              queries and updates for the reservations collection without
 *              containing business rules, strictly upholding the FAT service pattern.
 */

using System.Text.RegularExpressions;
using MongoDB.Bson;
using MongoDB.Driver;
using SmartSolar.Api.Data;
using SmartSolar.Api.DTOs;
using SmartSolar.Api.Models;

namespace SmartSolar.Api.Repositories;

public class ReservationRepository : IReservationRepository
{
    private readonly IMongoCollection<Reservation> _reservations;

    /// <summary>
    /// Resolves the reservations collection from the MongoDbContext.
    /// </summary>
    public ReservationRepository(MongoDbContext context)
    {
        // Obtain collection using the constant from MongoDbContext
        _reservations = context.GetCollection<Reservation>(MongoDbContext.ReservationsCollection);
    }

    /// <summary>
    /// Returns reservations matching the optional filters.
    /// </summary>
    public async Task<List<Reservation>> GetAllAsync(
        string? nic,
        string? nodeId,
        string? status,
        DateTime? fromDate,
        DateTime? toDate,
        string? search)
    {
        // Build filters dynamically
        var builder = Builders<Reservation>.Filter;
        var filter = builder.Empty;

        if (!string.IsNullOrWhiteSpace(nic))
        {
            filter &= builder.Eq(r => r.ProsumerNic, nic.Trim().ToUpperInvariant());
        }

        if (!string.IsNullOrWhiteSpace(nodeId))
        {
            filter &= builder.Eq(r => r.NodeId, nodeId.Trim());
        }

        if (!string.IsNullOrWhiteSpace(status) && !status.Equals("all", StringComparison.OrdinalIgnoreCase))
        {
            filter &= builder.Eq(r => r.Status, status.Trim());
        }

        if (fromDate.HasValue)
        {
            filter &= builder.Gte(r => r.SlotStartTime, fromDate.Value);
        }

        if (toDate.HasValue)
        {
            filter &= builder.Lte(r => r.SlotStartTime, toDate.Value);
        }

        if (!string.IsNullOrWhiteSpace(search))
        {
            var pattern = new BsonRegularExpression(Regex.Escape(search.Trim()), "i");
            filter &= builder.Or(
                builder.Regex(r => r.ReservationNumber, pattern),
                builder.Regex(r => r.ProsumerNic, pattern),
                builder.Regex(r => r.ProsumerName, pattern),
                builder.Regex(r => r.NodeName, pattern));
        }

        return await _reservations
            .Find(filter)
            .SortByDescending(r => r.SlotStartTime)
            .ToListAsync();
    }

    /// <summary>
    /// Returns a single reservation by its MongoDB ObjectId.
    /// </summary>
    public async Task<Reservation?> GetByIdAsync(string id)
    {
        // Lookup by primary key
        return await _reservations.Find(r => r.Id == id).FirstOrDefaultAsync();
    }

    /// <summary>
    /// Returns a single reservation by its unique reservation reference number.
    /// </summary>
    public async Task<Reservation?> GetByNumberAsync(string reservationNumber)
    {
        // Lookup by human-friendly reference number
        return await _reservations.Find(r => r.ReservationNumber == reservationNumber).FirstOrDefaultAsync();
    }

    /// <summary>
    /// Inserts a new reservation document into the collection.
    /// </summary>
    public async Task<Reservation> CreateAsync(Reservation reservation)
    {
        // Persist the reservation document
        await _reservations.InsertOneAsync(reservation);
        return reservation;
    }

    /// <summary>
    /// Replaces/updates an existing reservation document.
    /// </summary>
    public async Task UpdateAsync(Reservation reservation)
    {
        // Replace existing document by id
        await _reservations.ReplaceOneAsync(r => r.Id == reservation.Id, reservation);
    }

    /// <summary>
    /// Permanently removes a reservation from the collection.
    /// </summary>
    public async Task DeleteAsync(string id)
    {
        // Delete document by id
        await _reservations.DeleteOneAsync(r => r.Id == id);
    }

    /// <summary>
    /// Returns active (Pending or Approved) reservations assigned to a node.
    /// Used by Microgrid Node Management to block node deactivation when
    /// active reservations exist.
    /// </summary>
    public async Task<List<Reservation>> GetActiveReservationsByNodeIdAsync(string nodeId)
    {
        // Active reservations are those not completed and not cancelled
        var builder = Builders<Reservation>.Filter;
        var filter = builder.Eq(r => r.NodeId, nodeId) &
                     (builder.Eq(r => r.Status, ReservationStatus.Pending) |
                      builder.Eq(r => r.Status, ReservationStatus.Approved));

        return await _reservations.Find(filter).ToListAsync();
    }

    /// <summary>
    /// Counts overlapping active reservations for a node within a given time range.
    /// </summary>
    public async Task<long> CountOverlappingReservationsAsync(string nodeId, DateTime start, DateTime end, string? excludeId = null)
    {
        // Overlap condition: start < other.SlotEndTime AND end > other.SlotStartTime
        var builder = Builders<Reservation>.Filter;
        var filter = builder.Eq(r => r.NodeId, nodeId) &
                     (builder.Eq(r => r.Status, ReservationStatus.Pending) |
                      builder.Eq(r => r.Status, ReservationStatus.Approved)) &
                     builder.Lt(r => r.SlotStartTime, end) &
                     builder.Gt(r => r.SlotEndTime, start);

        if (!string.IsNullOrWhiteSpace(excludeId))
        {
            filter &= builder.Ne(r => r.Id, excludeId);
        }

        return await _reservations.CountDocumentsAsync(filter);
    }

    /// <summary>
    /// Aggregates reservation counts for operational and administrative dashboards.
    /// </summary>
    public async Task<ReservationStatsDto> GetStatsAsync()
    {
        // Compute dashboard summary counts
        var now = DateTime.UtcNow;
        var todayStart = DateTime.SpecifyKind(DateTime.UtcNow.Date, DateTimeKind.Utc);
        var todayEnd = todayStart.AddDays(1);

        var total = await _reservations.CountDocumentsAsync(Builders<Reservation>.Filter.Empty);
        var pending = await _reservations.CountDocumentsAsync(r => r.Status == ReservationStatus.Pending);
        var approvedFuture = await _reservations.CountDocumentsAsync(r =>
            r.Status == ReservationStatus.Approved && r.SlotStartTime >= now);
        var today = await _reservations.CountDocumentsAsync(r =>
            r.SlotStartTime >= todayStart && r.SlotStartTime < todayEnd &&
            r.Status != ReservationStatus.Cancelled);
        var completed = await _reservations.CountDocumentsAsync(r => r.Status == ReservationStatus.Completed);
        var cancelled = await _reservations.CountDocumentsAsync(r => r.Status == ReservationStatus.Cancelled);

        return new ReservationStatsDto
        {
            TotalReservations = total,
            PendingReservations = pending,
            ApprovedFutureReservations = approvedFuture,
            TodayReservations = today,
            CompletedReservations = completed,
            CancelledReservations = cancelled
        };
    }

    /// <summary>
    /// Ensures necessary database indexes exist for performance and unique constraints.
    /// </summary>
    public async Task EnsureIndexesAsync()
    {
        // Build index definitions
        var indexModels = new List<CreateIndexModel<Reservation>>
        {
            new(
                Builders<Reservation>.IndexKeys.Ascending(r => r.ReservationNumber),
                new CreateIndexOptions { Unique = true, Name = "ux_reservations_number" }
            ),
            new(
                Builders<Reservation>.IndexKeys.Ascending(r => r.ProsumerNic),
                new CreateIndexOptions { Name = "ix_reservations_prosumer_nic" }
            ),
            new(
                Builders<Reservation>.IndexKeys.Ascending(r => r.NodeId).Ascending(r => r.SlotStartTime),
                new CreateIndexOptions { Name = "ix_reservations_node_slot" }
            ),
            new(
                Builders<Reservation>.IndexKeys.Ascending(r => r.Status),
                new CreateIndexOptions { Name = "ix_reservations_status" }
            )
        };

        await _reservations.Indexes.CreateManyAsync(indexModels);
    }
}
