/*
 * File:        WebUserRepository.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Repositories
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: MongoDB implementation of IWebUserRepository. Builds the queries
 *              against the "webUsers" collection and contains no business rules,
 *              which keeps all logic in the service layer as the FAT service
 *              pattern requires.
 */

using MongoDB.Driver;
using SmartSolar.Api.Data;
using SmartSolar.Api.Models;

namespace SmartSolar.Api.Repositories;

public class WebUserRepository : IWebUserRepository
{
    private readonly IMongoCollection<WebUser> _users;

    /// <summary>
    /// Resolves the webUsers collection from the shared MongoDB context.
    /// </summary>
    public WebUserRepository(MongoDbContext context)
    {
        // The collection name comes from the constant defined in MongoDbContext
        _users = context.GetCollection<WebUser>(MongoDbContext.WebUsersCollection);
    }

    /// <summary>
    /// Returns users matching the optional role, status and search filters.
    /// </summary>
    public async Task<List<WebUser>> GetAllAsync(string? role, bool? isActive, string? search)
    {
        // Start with a filter that matches everything, then narrow it down
        var builder = Builders<WebUser>.Filter;
        var filter = builder.Empty;

        if (!string.IsNullOrWhiteSpace(role))
        {
            filter &= builder.Eq(u => u.Role, role);
        }

        if (isActive.HasValue)
        {
            filter &= builder.Eq(u => u.IsActive, isActive.Value);
        }

        if (!string.IsNullOrWhiteSpace(search))
        {
            // Case insensitive "contains" match across every identifying field
            var pattern = new MongoDB.Bson.BsonRegularExpression(
                System.Text.RegularExpressions.Regex.Escape(search.Trim()), "i");

            filter &= builder.Or(
                builder.Regex(u => u.FullName, pattern),
                builder.Regex(u => u.Email, pattern),
                builder.Regex(u => u.Username, pattern),
                builder.Regex(u => u.Nic, pattern),
                builder.Regex(u => u.Phone, pattern));
        }

        return await _users.Find(filter)
            .SortByDescending(u => u.CreatedAt)
            .ToListAsync();
    }

    /// <summary>
    /// Returns a single user by id, or null when the id does not exist.
    /// </summary>
    public async Task<WebUser?> GetByIdAsync(string id)
    {
        // An id that is not a valid ObjectId would throw, so it is checked first
        if (!MongoDB.Bson.ObjectId.TryParse(id, out _))
        {
            return null;
        }

        return await _users.Find(u => u.Id == id).FirstOrDefaultAsync();
    }

    /// <summary>
    /// Returns a single user by email address, or null when there is no match.
    /// </summary>
    public async Task<WebUser?> GetByEmailAsync(string email)
    {
        // Emails are always stored lowercase so the lookup is normalised too
        var normalised = email.Trim().ToLowerInvariant();

        return await _users.Find(u => u.Email == normalised).FirstOrDefaultAsync();
    }

    /// <summary>
    /// Returns true when the email is already used by another user.
    /// </summary>
    public async Task<bool> EmailExistsAsync(string email, string? excludeId = null)
    {
        // excludeId lets a user keep their own email while being edited
        var normalised = email.Trim().ToLowerInvariant();

        var builder = Builders<WebUser>.Filter;
        var filter = builder.Eq(u => u.Email, normalised);

        if (!string.IsNullOrWhiteSpace(excludeId))
        {
            filter &= builder.Ne(u => u.Id, excludeId);
        }

        return await _users.Find(filter).AnyAsync();
    }

    /// <summary>
    /// Returns true when the username is already used by another user.
    /// </summary>
    public async Task<bool> UsernameExistsAsync(string username, string? excludeId = null)
    {
        // Usernames are stored lowercase so the comparison is normalised too
        var normalised = username.Trim().ToLowerInvariant();

        var builder = Builders<WebUser>.Filter;
        var filter = builder.Eq(u => u.Username, normalised);

        if (!string.IsNullOrWhiteSpace(excludeId))
        {
            filter &= builder.Ne(u => u.Id, excludeId);
        }

        return await _users.Find(filter).AnyAsync();
    }

    /// <summary>
    /// Returns true when the NIC number is already used by another user.
    /// </summary>
    public async Task<bool> NicExistsAsync(string nic, string? excludeId = null)
    {
        // NIC numbers are stored uppercase so "123456789v" matches "123456789V"
        var normalised = nic.Trim().ToUpperInvariant();

        var builder = Builders<WebUser>.Filter;
        var filter = builder.Eq(u => u.Nic, normalised);

        if (!string.IsNullOrWhiteSpace(excludeId))
        {
            filter &= builder.Ne(u => u.Id, excludeId);
        }

        return await _users.Find(filter).AnyAsync();
    }

    /// <summary>
    /// Inserts a new user document and returns it with the generated id.
    /// </summary>
    public async Task<WebUser> CreateAsync(WebUser user)
    {
        // MongoDB fills in the Id property during the insert
        await _users.InsertOneAsync(user);

        return user;
    }

    /// <summary>
    /// Updates the editable fields of an existing user.
    /// </summary>
    public async Task UpdateAsync(WebUser user)
    {
        // Only the fields a Backoffice officer may edit are written back
        var update = Builders<WebUser>.Update
            .Set(u => u.FullName, user.FullName)
            .Set(u => u.Email, user.Email)
            .Set(u => u.Username, user.Username)
            .Set(u => u.Nic, user.Nic)
            .Set(u => u.Phone, user.Phone)
            .Set(u => u.DateOfBirth, user.DateOfBirth)
            .Set(u => u.Role, user.Role)
            .Set(u => u.PasswordHash, user.PasswordHash)
            .Set(u => u.UpdatedAt, user.UpdatedAt);

        await _users.UpdateOneAsync(u => u.Id == user.Id, update);
    }

    /// <summary>
    /// Activates or deactivates a user without touching any other field.
    /// </summary>
    public async Task SetActiveAsync(string id, bool isActive)
    {
        // Deactivation is a status change only; the record itself is never deleted
        var update = Builders<WebUser>.Update
            .Set(u => u.IsActive, isActive)
            .Set(u => u.UpdatedAt, DateTime.UtcNow);

        await _users.UpdateOneAsync(u => u.Id == id, update);
    }

    /// <summary>
    /// Counts all user documents, used to decide whether seeding is needed.
    /// </summary>
    public async Task<long> CountAsync()
    {
        // Used once at start-up by the database seeder
        return await _users.CountDocumentsAsync(Builders<WebUser>.Filter.Empty);
    }

    /// <summary>
    /// Creates the unique indexes on the email, username and NIC fields.
    /// </summary>
    public async Task EnsureIndexesAsync()
    {
        // A unique index makes the database itself reject a duplicate value.
        // Username and NIC are sparse as well, so that user documents created
        // before those fields existed do not clash with one another.
        var models = new[]
        {
            new CreateIndexModel<WebUser>(
                Builders<WebUser>.IndexKeys.Ascending(u => u.Email),
                new CreateIndexOptions { Unique = true, Name = "ux_webUsers_email" }),

            new CreateIndexModel<WebUser>(
                Builders<WebUser>.IndexKeys.Ascending(u => u.Username),
                new CreateIndexOptions { Unique = true, Sparse = true, Name = "ux_webUsers_username" }),

            new CreateIndexModel<WebUser>(
                Builders<WebUser>.IndexKeys.Ascending(u => u.Nic),
                new CreateIndexOptions { Unique = true, Sparse = true, Name = "ux_webUsers_nic" })
        };

        await _users.Indexes.CreateManyAsync(models);
    }
}
