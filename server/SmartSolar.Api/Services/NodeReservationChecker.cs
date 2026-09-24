/*
 * File:        NodeReservationChecker.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Services
 * Author:      Vidura Hewaduwa
 * Created:     2026-09-24
 * Description: Checks the reservations collection for Pending or Approved
 *              reservations before NodeService permits deactivation.
 */

using MongoDB.Bson;
using MongoDB.Driver;
using SmartSolar.Api.Data;

namespace SmartSolar.Api.Services;

/// <summary>
/// Reads only the reservation fields needed by the node deactivation rule.
/// Pending and Approved are the only statuses considered active.
/// </summary>
public class NodeReservationChecker : INodeReservationChecker
{
    private readonly IMongoCollection<BsonDocument> _reservations;

    public NodeReservationChecker(MongoDbContext context)
    {
        // BsonDocument avoids coupling node management to reservation model details
        _reservations = context.GetCollection<BsonDocument>(MongoDbContext.ReservationsCollection);
    }

    public async Task<bool> HasActiveReservationsForNodeAsync(string nodeId)
    {
        // Completed and Cancelled reservations must not block deactivation
        var builder = Builders<BsonDocument>.Filter;
        var filter = builder.Eq("nodeId", nodeId) &
                     builder.In("status", new[] { "Pending", "Approved" });

        return await _reservations.Find(filter).Limit(1).AnyAsync();
    }
}
