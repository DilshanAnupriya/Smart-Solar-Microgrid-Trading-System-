/*
 * File:        MicrogridNode.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Models
 * Author:      Vidura Hewaduwa
 * Created:     2026-09-24
 * Description: MongoDB document for a physical microgrid node and its embedded
 *              weekly operating-schedule entries. Deactivation is stored as a
 *              status change so historical node information is retained.
 */

using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolar.Api.Models;

/// <summary>
/// A physical microgrid location stored in the MongoDB nodes collection.
/// </summary>
public class MicrogridNode
{
    // MongoDB generates the ObjectId when a new node is inserted
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string Id { get; set; } = string.Empty;

    [BsonElement("nodeCode")]
    public string NodeCode { get; set; } = string.Empty;

    [BsonElement("name")]
    public string Name { get; set; } = string.Empty;

    [BsonElement("address")]
    public string Address { get; set; } = string.Empty;

    [BsonElement("latitude")]
    public double Latitude { get; set; }

    [BsonElement("longitude")]
    public double Longitude { get; set; }

    // Instantaneous solar generation capacity is measured in kilowatts
    [BsonElement("generationCapacityKw")]
    public double GenerationCapacityKw { get; set; }

    // Battery energy storage capacity is measured in kilowatt-hours
    [BsonElement("storageCapacityKWh")]
    public double StorageCapacityKWh { get; set; }

    [BsonElement("totalBatterySlots")]
    public int TotalBatterySlots { get; set; }

    [BsonElement("availableBatterySlots")]
    public int AvailableBatterySlots { get; set; }

    // Stored inside the node document because schedules belong only to this node
    [BsonElement("operatingSchedule")]
    public List<NodeScheduleEntry> OperatingSchedule { get; set; } = [];

    [BsonElement("isActive")]
    public bool IsActive { get; set; } = true;

    [BsonElement("createdAt")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [BsonElement("updatedAt")]
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    // Null while active; populated when the node is soft-deactivated
    [BsonElement("deactivatedAt")]
    public DateTime? DeactivatedAt { get; set; }

    [BsonElement("createdBy")]
    public string CreatedBy { get; set; } = string.Empty;
}

/// <summary>
/// One opening period in a node's weekly schedule. Times use 24-hour HH:mm text.
/// </summary>
public class NodeScheduleEntry
{
    [BsonElement("dayOfWeek")]
    public string DayOfWeek { get; set; } = string.Empty;

    // Times use 24-hour HH:mm text, for example 08:30
    [BsonElement("openingTime")]
    public string? OpeningTime { get; set; }

    [BsonElement("closingTime")]
    public string? ClosingTime { get; set; }

    [BsonElement("isClosed")]
    public bool IsClosed { get; set; }
}
