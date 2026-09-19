/*
 * File:        MongoDbContext.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Data
 * Author:      BDA Cooray (IT22189530)
 * Created:     2026-09-19
 * Description: Creates a single MongoClient for the application, provides access
 *              to the SmartSolar database and its collections, and exposes a
 *              ping check used to verify the database connection.
 */

using Microsoft.Extensions.Options;
using MongoDB.Bson;
using MongoDB.Driver;

namespace SmartSolar.Api.Data;

public class MongoDbContext
{
    // Collection names used across the system (kept in one place to avoid typos)
    public const string WebUsersCollection = "webUsers";
    public const string ProsumersCollection = "prosumers";
    public const string NodesCollection = "nodes";
    public const string ReservationsCollection = "reservations";
    public const string EnergyTransfersCollection = "energyTransfers";

    public IMongoDatabase Database { get; }

    /// <summary>
    /// Validates the MongoDB settings, creates the MongoClient and opens the database.
    /// </summary>
    public MongoDbContext(IOptions<MongoDbSettings> options)
    {
        // Read settings and fail early with a clear message if they are missing
        var settings = options.Value;

        if (string.IsNullOrWhiteSpace(settings.ConnectionString))
        {
            throw new InvalidOperationException(
                "MongoDbSettings:ConnectionString is not configured. Set it using dotnet user-secrets.");
        }

        if (string.IsNullOrWhiteSpace(settings.DatabaseName))
        {
            throw new InvalidOperationException(
                "MongoDbSettings:DatabaseName is not configured in appsettings.json.");
        }

        var client = new MongoClient(settings.ConnectionString);
        Database = client.GetDatabase(settings.DatabaseName);
    }

    /// <summary>
    /// Returns a typed MongoDB collection by name.
    /// </summary>
    public IMongoCollection<T> GetCollection<T>(string collectionName)
    {
        // Used by repositories, e.g. GetCollection<Prosumer>(ProsumersCollection)
        return Database.GetCollection<T>(collectionName);
    }

    /// <summary>
    /// Sends a ping command to MongoDB to confirm the connection is working.
    /// </summary>
    public async Task PingAsync()
    {
        // Throws an exception if the server cannot be reached or authentication fails
        await Database.RunCommandAsync((Command<BsonDocument>)"{ ping: 1 }");
    }
}