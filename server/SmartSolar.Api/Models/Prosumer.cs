/*
 * File:        Prosumer.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Models
 * Author:      Dilshan Anupriya (IT22189530)
 * Created:     2026-09-20
 * Description: MongoDB document for a solar prosumer. The National Identity
 *              Card number is used as the primary key (_id).
 */

using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolar.Api.Models;

public class Prosumer
{
    // NIC is the primary key, stored as the MongoDB _id
    [BsonId]
    public string Nic { get; set; } = string.Empty;

    [BsonElement("fullName")]
    public string FullName { get; set; } = string.Empty;

    // Stored in lowercase so duplicate checks are case-insensitive
    [BsonElement("email")]
    public string Email { get; set; } = string.Empty;

    [BsonElement("phone")]
    public string Phone { get; set; } = string.Empty;

    [BsonElement("address")]
    public string Address { get; set; } = string.Empty;

    // BCrypt hash only; the plain password is never stored
    [BsonElement("passwordHash")]
    public string PasswordHash { get; set; } = string.Empty;

    // Saved as "Active" / "Deactivated" text so it is readable in Atlas
    [BsonElement("status")]
    [BsonRepresentation(BsonType.String)]
    public ProsumerStatus Status { get; set; } = ProsumerStatus.Active;

    [BsonElement("deactivatedAt")]
    public DateTime? DeactivatedAt { get; set; }

    // Id of the staff user or NIC of the prosumer who deactivated the account
    [BsonElement("deactivatedBy")]
    public string? DeactivatedBy { get; set; }

    [BsonElement("createdAt")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [BsonElement("updatedAt")]
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}