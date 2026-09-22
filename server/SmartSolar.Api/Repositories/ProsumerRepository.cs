/*
 * File:        ProsumerRepository.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Repositories
 * Author:      Dilshan Anupriya (IT22189530)
 * Created:     2026-09-20
 * Description: MongoDB implementation of IProsumerRepository. Handles data
 *              access only; all business rules live in ProsumerService.
 */

using System.Text.RegularExpressions;
using MongoDB.Bson;
using MongoDB.Driver;
using SmartSolar.Api.Data;
using SmartSolar.Api.Models;

namespace SmartSolar.Api.Repositories;

public class ProsumerRepository : IProsumerRepository
{
    private readonly IMongoCollection<Prosumer> _prosumers;

    /// <summary>
    /// Gets the prosumers collection from the shared MongoDbContext.
    /// </summary>
    public ProsumerRepository(MongoDbContext context)
    {
        // Collection name comes from the constant in MongoDbContext
        _prosumers = context.GetCollection<Prosumer>(MongoDbContext.ProsumersCollection);
    }

    /// <summary>
    /// Finds a single prosumer by NIC.
    /// </summary>
    public async Task<Prosumer?> GetByNicAsync(string nic)
    {
        // NIC is the _id, so this is a primary key lookup
        return await _prosumers.Find(p => p.Nic == nic).FirstOrDefaultAsync();
    }

    /// <summary>
    /// Lists prosumers, optionally filtered by a search term and status, newest first.
    /// </summary>
    public async Task<List<Prosumer>> GetAllAsync(string? search, ProsumerStatus? status)
    {
        // Start with "match everything" and add filters only when provided
        var builder = Builders<Prosumer>.Filter;
        var filter = builder.Empty;

        if (status.HasValue)
        {
            filter &= builder.Eq(p => p.Status, status.Value);
        }

        if (!string.IsNullOrWhiteSpace(search))
        {
            // Escape the input so characters like "." or "*" are treated literally
            var pattern = new BsonRegularExpression(Regex.Escape(search.Trim()), "i");

            filter &= builder.Or(
                builder.Regex(p => p.Nic, pattern),
                builder.Regex(p => p.FullName, pattern),
                builder.Regex(p => p.Email, pattern),
                builder.Regex(p => p.Phone, pattern));
        }

        return await _prosumers.Find(filter)
            .SortByDescending(p => p.CreatedAt)
            .ToListAsync();
    }

    /// <summary>
    /// Checks whether a prosumer with this NIC already exists.
    /// </summary>
    public async Task<bool> NicExistsAsync(string nic)
    {
        // CountDocuments with a limit of 1 stops as soon as a match is found
        var count = await _prosumers.CountDocumentsAsync(
            p => p.Nic == nic, new CountOptions { Limit = 1 });
        return count > 0;
    }

    /// <summary>
    /// Checks whether any other prosumer already uses this email address.
    /// </summary>
    public async Task<bool> EmailExistsAsync(string email, string? excludeNic = null)
    {
        // When updating, the prosumer's own record is excluded from the check
        var builder = Builders<Prosumer>.Filter;
        var filter = builder.Eq(p => p.Email, email.Trim().ToLowerInvariant());

        if (!string.IsNullOrWhiteSpace(excludeNic))
        {
            filter &= builder.Ne(p => p.Nic, excludeNic);
        }

        var count = await _prosumers.CountDocumentsAsync(filter, new CountOptions { Limit = 1 });
        return count > 0;
    }

    /// <summary>
    /// Inserts a new prosumer document.
    /// </summary>
    public async Task CreateAsync(Prosumer prosumer)
    {
        // Insert fails if the NIC (_id) already exists
        await _prosumers.InsertOneAsync(prosumer);
    }

    /// <summary>
    /// Replaces the stored prosumer document with the updated version.
    /// </summary>
    public async Task UpdateAsync(Prosumer prosumer)
    {
        // Match on NIC and replace the whole document
        await _prosumers.ReplaceOneAsync(p => p.Nic == prosumer.Nic, prosumer);
    }
}