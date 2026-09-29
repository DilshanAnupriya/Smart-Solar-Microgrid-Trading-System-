/*
 * File:        NodeServiceTests.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Tests
 * Author:      Vidura Hewaduwa
 * Created:     2026-09-24
 * Description: Unit tests for the most important node-management business rules.
 */

using SmartSolar.Api.DTOs;
using SmartSolar.Api.Exceptions;
using SmartSolar.Api.Models;
using SmartSolar.Api.Repositories;
using SmartSolar.Api.Services;

namespace SmartSolar.Tests;

public class NodeServiceTests
{
    // The fakes at the bottom keep these tests fast and independent from MongoDB
    [Fact]
    public async Task CreateAsync_RejectsDuplicateNodeCode()
    {
        var repository = new FakeNodeRepository
        {
            Nodes = { ValidNode("NODE-01") }
        };
        var service = new NodeService(repository, new FakeReservationChecker());

        await Assert.ThrowsAsync<ConflictException>(() =>
            service.CreateAsync(ValidCreateRequest("node-01"), "user-1"));
    }

    [Fact]
    public async Task CreateAsync_RejectsAvailableSlotsGreaterThanTotal()
    {
        var request = ValidCreateRequest();
        request.TotalBatterySlots = 4;
        request.AvailableBatterySlots = 5;
        var service = new NodeService(new FakeNodeRepository(), new FakeReservationChecker());

        var exception = await Assert.ThrowsAsync<BusinessRuleException>(() =>
            service.CreateAsync(request, "user-1"));

        Assert.Contains("cannot exceed", exception.Message);
    }

    [Fact]
    public async Task UpdateScheduleAsync_RejectsOverlappingPeriods()
    {
        var node = ValidNode();
        var repository = new FakeNodeRepository { Nodes = { node } };
        var service = new NodeService(repository, new FakeReservationChecker());
        var request = new UpdateNodeScheduleDto
        {
            OperatingSchedule =
            [
                new() { DayOfWeek = "Monday", OpeningTime = "08:00", ClosingTime = "12:00" },
                new() { DayOfWeek = "Monday", OpeningTime = "11:30", ClosingTime = "15:00" }
            ]
        };

        var exception = await Assert.ThrowsAsync<BusinessRuleException>(() =>
            service.UpdateScheduleAsync(node.Id, request));

        Assert.Contains("overlap", exception.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task DeactivateAsync_IsBlockedByActiveReservations()
    {
        var node = ValidNode();
        var repository = new FakeNodeRepository { Nodes = { node } };
        var service = new NodeService(repository, new FakeReservationChecker { HasActiveReservations = true });

        await Assert.ThrowsAsync<ConflictException>(() => service.DeactivateAsync(node.Id));
        Assert.True(node.IsActive);
    }

    [Fact]
    public async Task DeactivateAsync_PerformsSoftDeletion()
    {
        var node = ValidNode();
        var repository = new FakeNodeRepository { Nodes = { node } };
        var service = new NodeService(repository, new FakeReservationChecker());

        var result = await service.DeactivateAsync(node.Id);

        Assert.False(result.IsActive);
        Assert.NotNull(result.DeactivatedAt);
        Assert.Single(repository.Nodes);
    }

    [Fact]
    public async Task GetNearbyAsync_ReturnsOnlyActiveNodesInsideRadiusInDistanceOrder()
    {
        var near = ValidNode("NEAR", 6.9271, 79.8612);
        var farther = ValidNode("FARTHER", 6.90, 79.87);
        var inactive = ValidNode("INACTIVE", 6.9272, 79.8613);
        inactive.IsActive = false;
        var repository = new FakeNodeRepository { Nodes = { farther, inactive, near } };
        var service = new NodeService(repository, new FakeReservationChecker());

        var result = await service.GetNearbyAsync(6.9271, 79.8612, 10);

        Assert.Equal(2, result.Count);
        Assert.Equal("NEAR", result[0].NodeCode);
        Assert.DoesNotContain(result, item => item.NodeCode == "INACTIVE");
    }

    private static CreateNodeDto ValidCreateRequest(string code = "NODE-01") => new()
    {
        NodeCode = code,
        Name = "Central Node",
        Address = "Colombo",
        Latitude = 6.9271,
        Longitude = 79.8612,
        GenerationCapacityKw = 250,
        StorageCapacityKWh = 500,
        TotalBatterySlots = 20,
        AvailableBatterySlots = 10
    };

    private static MicrogridNode ValidNode(
        string code = "NODE-01",
        double latitude = 6.9271,
        double longitude = 79.8612) => new()
    {
        Id = MongoDB.Bson.ObjectId.GenerateNewId().ToString(),
        NodeCode = code,
        Name = code,
        Address = "Colombo",
        Latitude = latitude,
        Longitude = longitude,
        GenerationCapacityKw = 250,
        StorageCapacityKWh = 500,
        TotalBatterySlots = 20,
        AvailableBatterySlots = 10,
        IsActive = true,
        CreatedAt = DateTime.UtcNow,
        UpdatedAt = DateTime.UtcNow,
        CreatedBy = "test"
    };

    private sealed class FakeReservationChecker : INodeReservationChecker
    {
        public bool HasActiveReservations { get; set; }
        public Task<bool> HasActiveReservationsForNodeAsync(string nodeId) =>
            Task.FromResult(HasActiveReservations);
    }

    private sealed class FakeNodeRepository : INodeRepository
    {
        public List<MicrogridNode> Nodes { get; } = [];

        public Task<List<MicrogridNode>> GetAllAsync(string? search, bool? isActive) =>
            Task.FromResult(Nodes.Where(node => !isActive.HasValue || node.IsActive == isActive).ToList());

        public Task<List<MicrogridNode>> GetActiveAsync() =>
            Task.FromResult(Nodes.Where(node => node.IsActive).ToList());

        public Task<MicrogridNode?> GetByIdAsync(string id) =>
            Task.FromResult(Nodes.FirstOrDefault(node => node.Id == id));

        public Task<bool> NodeCodeExistsAsync(string nodeCode, string? excludeId = null) =>
            Task.FromResult(Nodes.Any(node =>
                node.Id != excludeId && node.NodeCode.Equals(nodeCode, StringComparison.OrdinalIgnoreCase)));

        public Task CreateAsync(MicrogridNode node)
        {
            if (string.IsNullOrEmpty(node.Id)) node.Id = MongoDB.Bson.ObjectId.GenerateNewId().ToString();
            Nodes.Add(node);
            return Task.CompletedTask;
        }

        public Task UpdateAsync(MicrogridNode node) => Task.CompletedTask;
        public Task EnsureIndexesAsync() => Task.CompletedTask;
    }
}
