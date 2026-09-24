/*
 * File:        NodeDtos.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       DTOs
 * Author:      Vidura Hewaduwa
 * Created:     2026-09-24
 * Description: Request and response contracts for microgrid node management.
 *              Clients never control audit fields such as CreatedAt or CreatedBy.
 */

using System.ComponentModel.DataAnnotations;

namespace SmartSolar.Api.DTOs;

/// <summary>Body posted to api/nodes when Backoffice creates a node.</summary>
public class CreateNodeDto
{
    [Required(ErrorMessage = "Node code is required.")]
    [StringLength(30, ErrorMessage = "Node code must be 30 characters or fewer.")]
    public string NodeCode { get; set; } = string.Empty;

    [Required(ErrorMessage = "Name is required.")]
    [StringLength(100, ErrorMessage = "Name must be 100 characters or fewer.")]
    public string Name { get; set; } = string.Empty;

    [Required(ErrorMessage = "Address is required.")]
    [StringLength(250, ErrorMessage = "Address must be 250 characters or fewer.")]
    public string Address { get; set; } = string.Empty;

    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public double GenerationCapacityKw { get; set; }
    public double StorageCapacityKWh { get; set; }
    public int TotalBatterySlots { get; set; }
    public int AvailableBatterySlots { get; set; }
    public List<NodeScheduleEntryDto> OperatingSchedule { get; set; } = [];
}

/// <summary>Body sent to api/nodes/{id} when Backoffice edits node details.</summary>
public class UpdateNodeDto : CreateNodeDto;

/// <summary>Replaces the complete weekly operating schedule of a node.</summary>
public class UpdateNodeScheduleDto
{
    [Required(ErrorMessage = "Operating schedule is required.")]
    public List<NodeScheduleEntryDto> OperatingSchedule { get; set; } = [];
}

/// <summary>Updates live slot availability without changing the total capacity.</summary>
public class UpdateNodeSlotsDto
{
    public int AvailableBatterySlots { get; set; }
}

/// <summary>One opening period or closed day in the weekly schedule.</summary>
public class NodeScheduleEntryDto
{
    [Required(ErrorMessage = "Day of week is required.")]
    public string DayOfWeek { get; set; } = string.Empty;
    public string? OpeningTime { get; set; }
    public string? ClosingTime { get; set; }
    public bool IsClosed { get; set; }
}

/// <summary>Safe node information returned to web and mobile clients.</summary>
public class NodeResponseDto
{
    public string Id { get; set; } = string.Empty;
    public string NodeCode { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string Address { get; set; } = string.Empty;
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public double GenerationCapacityKw { get; set; }
    public double StorageCapacityKWh { get; set; }
    public int TotalBatterySlots { get; set; }
    public int AvailableBatterySlots { get; set; }
    public List<NodeScheduleEntryDto> OperatingSchedule { get; set; } = [];
    public bool IsActive { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public DateTime? DeactivatedAt { get; set; }
    public string CreatedBy { get; set; } = string.Empty;
    // Populated only by the nearby endpoint
    public double? DistanceKm { get; set; }
}
