/*
 * File:        NodeRepository.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Repositories
 * Author:      Vidura Hewaduwa
 * Created:     2026-09-24
 * Description: MongoDB implementation of INodeRepository. This layer performs
 *              data access only; validation stays inside NodeService.
 */

using System.Text.RegularExpressions;
using MongoDB.Bson;
using MongoDB.Driver;
using SmartSolar.Api.Data;
using SmartSolar.Api.Models;

namespace SmartSolar.Api.Repositories;

public class NodeRepository : INodeRepository
{
    private readonly IMongoCollection<MicrogridNode> _nodes;

    public NodeRepository(MongoDbContext context)
    {
        // Use the shared collection constant to avoid spelling differences
        _nodes = context.GetCollection<MicrogridNode>(MongoDbContext.NodesCollection);
    }

    /// <summary>Lists nodes with optional search and status filtering.</summary>
    public async Task<List<MicrogridNode>> GetAllAsync(string? search, bool? isActive)
    {
        var builder = Builders<MicrogridNode>.Filter;
        var filter = builder.Empty;

        if (isActive.HasValue)
        {
            filter &= builder.Eq(node => node.IsActive, isActive.Value);
        }

        if (!string.IsNullOrWhiteSpace(search))
        {
            // Escaping makes characters such as * and . behave as normal text
            var pattern = new BsonRegularExpression(Regex.Escape(search.Trim()), "i");
            filter &= builder.Or(
                builder.Regex(node => node.NodeCode, pattern),
                builder.Regex(node => node.Name, pattern),
                builder.Regex(node => node.Address, pattern));
        }

        return await _nodes.Find(filter)
            .SortBy(node => node.Name)
            .ToListAsync();
    }

    /// <summary>Returns only active nodes for mobile nearby searches.</summary>
    public async Task<List<MicrogridNode>> GetActiveAsync()
    {
        return await _nodes.Find(node => node.IsActive)
            .SortBy(node => node.Name)
            .ToListAsync();
    }

    /// <summary>Finds one node after safely validating the ObjectId text.</summary>
    public async Task<MicrogridNode?> GetByIdAsync(string id)
    {
        if (!ObjectId.TryParse(id, out _))
        {
            return null;
        }

        return await _nodes.Find(node => node.Id == id).FirstOrDefaultAsync();
    }

    /// <summary>Checks node-code uniqueness, excluding the current node on edit.</summary>
    public async Task<bool> NodeCodeExistsAsync(string nodeCode, string? excludeId = null)
    {
        var builder = Builders<MicrogridNode>.Filter;
        var filter = builder.Eq(node => node.NodeCode, nodeCode.Trim().ToUpperInvariant());

        if (!string.IsNullOrWhiteSpace(excludeId))
        {
            filter &= builder.Ne(node => node.Id, excludeId);
        }

        return await _nodes.Find(filter).AnyAsync();
    }

    /// <summary>Inserts a new node and lets MongoDB generate its ObjectId.</summary>
    public async Task CreateAsync(MicrogridNode node)
    {
        await _nodes.InsertOneAsync(node);
    }

    /// <summary>Writes all current node fields back to the matching document.</summary>
    public async Task UpdateAsync(MicrogridNode node)
    {
        await _nodes.ReplaceOneAsync(existing => existing.Id == node.Id, node);
    }

    /// <summary>Protects node-code uniqueness even when writes happen concurrently.</summary>
    public async Task EnsureIndexesAsync()
    {
        var model = new CreateIndexModel<MicrogridNode>(
            Builders<MicrogridNode>.IndexKeys.Ascending(node => node.NodeCode),
            new CreateIndexOptions { Unique = true, Name = "ux_nodes_nodeCode" });

        await _nodes.Indexes.CreateOneAsync(model);
    }
}
