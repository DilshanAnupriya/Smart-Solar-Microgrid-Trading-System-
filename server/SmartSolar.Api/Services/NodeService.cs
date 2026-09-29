/*
 * File:        NodeService.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Services
 * Author:      Vidura Hewaduwa
 * Created:     2026-09-24
 * Description: FAT service containing every microgrid-node business rule,
 *              including capacity, coordinates, schedules, slots, lifecycle
 *              changes and nearby-distance calculations.
 */

using System.Globalization;
using SmartSolar.Api.DTOs;
using SmartSolar.Api.Exceptions;
using SmartSolar.Api.Models;
using SmartSolar.Api.Repositories;

namespace SmartSolar.Api.Services;

public class NodeService : INodeService
{
    private const double EarthRadiusKm = 6371.0088;
    private readonly INodeRepository _repository;
    private readonly INodeReservationChecker _reservationChecker;

    public NodeService(INodeRepository repository, INodeReservationChecker reservationChecker)
    {
        // Dependencies are injected so business logic can be tested without MongoDB
        _repository = repository;
        _reservationChecker = reservationChecker;
    }

    /// <summary>Lists nodes and maps database documents into safe responses.</summary>
    public async Task<List<NodeResponseDto>> GetAllAsync(string? search, bool? isActive)
    {
        var nodes = await _repository.GetAllAsync(search, isActive);
        return nodes.Select(node => Map(node)).ToList();
    }

    /// <summary>Returns one node or a consistent 404 application error.</summary>
    public async Task<NodeResponseDto> GetByIdAsync(string id)
    {
        return Map(await GetExistingAsync(id));
    }

    /// <summary>Validates uniqueness and node details before inserting a document.</summary>
    public async Task<NodeResponseDto> CreateAsync(CreateNodeDto request, string createdBy)
    {
        ValidateDetails(request);
        var code = NormalizeCode(request.NodeCode);

        // The service check gives a readable 409; the unique index handles race conditions
        if (await _repository.NodeCodeExistsAsync(code))
        {
            throw new ConflictException($"A node with code '{code}' already exists.");
        }

        var now = DateTime.UtcNow;
        var node = new MicrogridNode
        {
            NodeCode = code,
            Name = request.Name.Trim(),
            Address = request.Address.Trim(),
            Latitude = request.Latitude,
            Longitude = request.Longitude,
            GenerationCapacityKw = request.GenerationCapacityKw,
            StorageCapacityKWh = request.StorageCapacityKWh,
            TotalBatterySlots = request.TotalBatterySlots,
            AvailableBatterySlots = request.AvailableBatterySlots,
            OperatingSchedule = NormalizeSchedule(request.OperatingSchedule),
            IsActive = true,
            CreatedAt = now,
            UpdatedAt = now,
            CreatedBy = string.IsNullOrWhiteSpace(createdBy) ? "system" : createdBy
        };

        await _repository.CreateAsync(node);
        return Map(node);
    }

    /// <summary>Updates general information while leaving status and history unchanged.</summary>
    public async Task<NodeResponseDto> UpdateAsync(string id, UpdateNodeDto request)
    {
        ValidateDetails(request);
        var node = await GetExistingAsync(id);
        var code = NormalizeCode(request.NodeCode);

        if (await _repository.NodeCodeExistsAsync(code, id))
        {
            throw new ConflictException($"A node with code '{code}' already exists.");
        }

        node.NodeCode = code;
        node.Name = request.Name.Trim();
        node.Address = request.Address.Trim();
        node.Latitude = request.Latitude;
        node.Longitude = request.Longitude;
        node.GenerationCapacityKw = request.GenerationCapacityKw;
        node.StorageCapacityKWh = request.StorageCapacityKWh;
        node.TotalBatterySlots = request.TotalBatterySlots;
        node.AvailableBatterySlots = request.AvailableBatterySlots;
        node.UpdatedAt = DateTime.UtcNow;

        await _repository.UpdateAsync(node);
        return Map(node);
    }

    /// <summary>Validates and replaces all embedded weekly schedule entries.</summary>
    public async Task<NodeResponseDto> UpdateScheduleAsync(string id, UpdateNodeScheduleDto request)
    {
        var node = await GetExistingAsync(id);
        node.OperatingSchedule = NormalizeSchedule(request.OperatingSchedule);
        node.UpdatedAt = DateTime.UtcNow;

        await _repository.UpdateAsync(node);
        return Map(node);
    }

    /// <summary>Updates the operator-managed available-slot count.</summary>
    public async Task<NodeResponseDto> UpdateSlotsAsync(string id, UpdateNodeSlotsDto request)
    {
        var node = await GetExistingAsync(id);

        if (request.AvailableBatterySlots < 0)
        {
            throw new BusinessRuleException("Available battery slots cannot be negative.");
        }

        if (request.AvailableBatterySlots > node.TotalBatterySlots)
        {
            throw new BusinessRuleException("Available battery slots cannot exceed total battery slots.");
        }

        node.AvailableBatterySlots = request.AvailableBatterySlots;
        node.UpdatedAt = DateTime.UtcNow;

        await _repository.UpdateAsync(node);
        return Map(node);
    }

    /// <summary>Soft-deactivates a node after checking active reservations.</summary>
    public async Task<NodeResponseDto> DeactivateAsync(string id)
    {
        var node = await GetExistingAsync(id);
        if (!node.IsActive)
        {
            throw new BusinessRuleException("This node is already inactive.");
        }

        // Pending and Approved reservations represent work that still needs this node
        if (await _reservationChecker.HasActiveReservationsForNodeAsync(id))
        {
            throw new ConflictException("This node cannot be deactivated while it has Pending or Approved reservations.");
        }

        var now = DateTime.UtcNow;
        node.IsActive = false;
        node.DeactivatedAt = now;
        node.UpdatedAt = now;
        await _repository.UpdateAsync(node);

        return Map(node);
    }

    /// <summary>Restores a soft-deactivated node to operational use.</summary>
    public async Task<NodeResponseDto> ActivateAsync(string id)
    {
        var node = await GetExistingAsync(id);
        if (node.IsActive)
        {
            throw new BusinessRuleException("This node is already active.");
        }

        node.IsActive = true;
        node.DeactivatedAt = null;
        node.UpdatedAt = DateTime.UtcNow;
        await _repository.UpdateAsync(node);

        return Map(node);
    }

    /// <summary>Uses the Haversine formula and returns active nodes nearest first.</summary>
    public async Task<List<NodeResponseDto>> GetNearbyAsync(double latitude, double longitude, double radiusKm)
    {
        ValidateCoordinates(latitude, longitude);
        if (radiusKm <= 0)
        {
            throw new BusinessRuleException("Search radius must be greater than zero.");
        }

        var nodes = await _repository.GetActiveAsync();
        return nodes
            .Select(node => new { Node = node, Distance = DistanceKm(latitude, longitude, node.Latitude, node.Longitude) })
            .Where(item => item.Distance <= radiusKm)
            .OrderBy(item => item.Distance)
            .Select(item => Map(item.Node, Math.Round(item.Distance, 2)))
            .ToList();
    }

    /// <summary>Shared lookup used by every command operating on an existing node.</summary>
    private async Task<MicrogridNode> GetExistingAsync(string id)
    {
        return await _repository.GetByIdAsync(id)
               ?? throw new NotFoundException($"Microgrid node with id '{id}' was not found.");
    }

    /// <summary>Enforces required fields, coordinates, capacity and slot limits.</summary>
    private static void ValidateDetails(CreateNodeDto request)
    {
        if (string.IsNullOrWhiteSpace(request.NodeCode))
            throw new BusinessRuleException("Node code is required.");
        if (string.IsNullOrWhiteSpace(request.Name))
            throw new BusinessRuleException("Name is required.");
        if (string.IsNullOrWhiteSpace(request.Address))
            throw new BusinessRuleException("Address is required.");

        ValidateCoordinates(request.Latitude, request.Longitude);

        if (request.GenerationCapacityKw <= 0)
            throw new BusinessRuleException("Generation capacity must be greater than zero kW.");
        if (request.StorageCapacityKWh <= 0)
            throw new BusinessRuleException("Storage capacity must be greater than zero kWh.");
        if (request.TotalBatterySlots < 0 || request.AvailableBatterySlots < 0)
            throw new BusinessRuleException("Battery slot counts cannot be negative.");
        if (request.AvailableBatterySlots > request.TotalBatterySlots)
            throw new BusinessRuleException("Available battery slots cannot exceed total battery slots.");
    }

    /// <summary>Checks geographic coordinate boundaries used by create and nearby search.</summary>
    private static void ValidateCoordinates(double latitude, double longitude)
    {
        if (latitude is < -90 or > 90)
            throw new BusinessRuleException("Latitude must be between -90 and 90.");
        if (longitude is < -180 or > 180)
            throw new BusinessRuleException("Longitude must be between -180 and 180.");
    }

    /// <summary>Parses HH:mm values, normalizes day names and rejects overlapping periods.</summary>
    private static List<NodeScheduleEntry> NormalizeSchedule(IEnumerable<NodeScheduleEntryDto>? schedule)
    {
        var normalized = new List<(DayOfWeek Day, TimeOnly? Opening, TimeOnly? Closing, NodeScheduleEntry Entry)>();

        foreach (var item in schedule ?? [])
        {
            if (!Enum.TryParse<DayOfWeek>(item.DayOfWeek?.Trim(), true, out var day))
                throw new BusinessRuleException($"'{item.DayOfWeek}' is not a valid day of the week.");

            if (item.IsClosed)
            {
                normalized.Add((day, null, null, new NodeScheduleEntry
                {
                    DayOfWeek = day.ToString(),
                    IsClosed = true
                }));
                continue;
            }

            if (!TimeOnly.TryParseExact(item.OpeningTime, "HH:mm", CultureInfo.InvariantCulture,
                    DateTimeStyles.None, out var opening) ||
                !TimeOnly.TryParseExact(item.ClosingTime, "HH:mm", CultureInfo.InvariantCulture,
                    DateTimeStyles.None, out var closing))
            {
                throw new BusinessRuleException($"Opening and closing times for {day} must use HH:mm format.");
            }

            if (opening >= closing)
                throw new BusinessRuleException($"Opening time must precede closing time on {day}.");

            normalized.Add((day, opening, closing, new NodeScheduleEntry
            {
                DayOfWeek = day.ToString(),
                OpeningTime = opening.ToString("HH:mm", CultureInfo.InvariantCulture),
                ClosingTime = closing.ToString("HH:mm", CultureInfo.InvariantCulture),
                IsClosed = false
            }));
        }

        foreach (var group in normalized.GroupBy(entry => entry.Day))
        {
            // A closed marker represents the whole day, so it cannot coexist with an opening period
            if (group.Any(entry => entry.Entry.IsClosed) && group.Count() > 1)
                throw new BusinessRuleException($"A closed day cannot contain another schedule entry ({group.Key}).");

            // Sorting by opening time means each period only needs comparison with its predecessor
            var periods = group.Where(entry => !entry.Entry.IsClosed).OrderBy(entry => entry.Opening).ToList();
            for (var index = 1; index < periods.Count; index++)
            {
                if (periods[index].Opening < periods[index - 1].Closing)
                    throw new BusinessRuleException($"Schedule entries overlap on {group.Key}.");
            }
        }

        return normalized
            .OrderBy(entry => entry.Day == DayOfWeek.Sunday ? 7 : (int)entry.Day)
            .ThenBy(entry => entry.Opening)
            .Select(entry => entry.Entry)
            .ToList();
    }

    private static string NormalizeCode(string code) => code.Trim().ToUpperInvariant();

    /// <summary>Calculates great-circle distance between two latitude/longitude points.</summary>
    private static double DistanceKm(double latitude1, double longitude1, double latitude2, double longitude2)
    {
        static double Radians(double degrees) => degrees * Math.PI / 180;

        var latitudeDelta = Radians(latitude2 - latitude1);
        var longitudeDelta = Radians(longitude2 - longitude1);
        var a = Math.Pow(Math.Sin(latitudeDelta / 2), 2) +
                Math.Cos(Radians(latitude1)) * Math.Cos(Radians(latitude2)) *
                Math.Pow(Math.Sin(longitudeDelta / 2), 2);

        return EarthRadiusKm * 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));
    }

    /// <summary>Maps the persistence model without exposing MongoDB-specific details.</summary>
    private static NodeResponseDto Map(MicrogridNode node, double? distanceKm = null)
    {
        return new NodeResponseDto
        {
            Id = node.Id,
            NodeCode = node.NodeCode,
            Name = node.Name,
            Address = node.Address,
            Latitude = node.Latitude,
            Longitude = node.Longitude,
            GenerationCapacityKw = node.GenerationCapacityKw,
            StorageCapacityKWh = node.StorageCapacityKWh,
            TotalBatterySlots = node.TotalBatterySlots,
            AvailableBatterySlots = node.AvailableBatterySlots,
            OperatingSchedule = node.OperatingSchedule.Select(entry => new NodeScheduleEntryDto
            {
                DayOfWeek = entry.DayOfWeek,
                OpeningTime = entry.OpeningTime,
                ClosingTime = entry.ClosingTime,
                IsClosed = entry.IsClosed
            }).ToList(),
            IsActive = node.IsActive,
            CreatedAt = node.CreatedAt,
            UpdatedAt = node.UpdatedAt,
            DeactivatedAt = node.DeactivatedAt,
            CreatedBy = node.CreatedBy,
            DistanceKm = distanceKm
        };
    }
}
