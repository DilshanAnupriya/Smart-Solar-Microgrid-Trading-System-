/*
 * File:        ReservationService.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Services
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-22
 * Description: FAT service implementation of IReservationService. Centralizes all
 *              business logic for energy slot reservations, strictly enforcing
 *              the 7-day scheduling window and the 12-hour advance notice
 *              requirement for updates and cancellations.
 */

using System.Text;
using SmartSolar.Api.DTOs;
using SmartSolar.Api.Exceptions;
using SmartSolar.Api.Models;
using SmartSolar.Api.Repositories;

namespace SmartSolar.Api.Services;

public class ReservationService : IReservationService
{
    private readonly IReservationRepository _repository;
    private readonly ILogger<ReservationService> _logger;

    /// <summary>
    /// Receives repository and logger dependencies.
    /// </summary>
    public ReservationService(IReservationRepository repository, ILogger<ReservationService> logger)
    {
        // Store dependencies injected by the container
        _repository = repository;
        _logger = logger;
    }

    /// <summary>
    /// Lists reservations matching the optional filters.
    /// </summary>
    public async Task<List<ReservationResponseDto>> GetAllAsync(
        string? nic,
        string? nodeId,
        string? status,
        DateTime? fromDate,
        DateTime? toDate,
        string? search)
    {
        // Query the repository and map all entities to response DTOs
        var reservations = await _repository.GetAllAsync(nic, nodeId, status, fromDate, toDate, search);
        return reservations.Select(MapToResponse).ToList();
    }

    /// <summary>
    /// Returns a single reservation by id.
    /// </summary>
    public async Task<ReservationResponseDto> GetByIdAsync(string id)
    {
        // Look up by id and throw 404 if not found
        var reservation = await _repository.GetByIdAsync(id);
        if (reservation is null)
        {
            throw new NotFoundException($"Reservation with id '{id}' was not found.");
        }

        return MapToResponse(reservation);
    }

    /// <summary>
    /// Returns dashboard summary statistics.
    /// </summary>
    public async Task<ReservationStatsDto> GetStatsAsync()
    {
        // Aggregate statistics from the repository
        return await _repository.GetStatsAsync();
    }

    /// <summary>
    /// Creates a new power trading reservation enforcing the 7-day rule.
    /// </summary>
    public async Task<ReservationResponseDto> CreateAsync(CreateReservationDto dto, string createdBy)
    {
        // Convert input dates to UTC
        var startUtc = DateTime.SpecifyKind(dto.SlotStartTime, DateTimeKind.Utc);
        var endUtc = DateTime.SpecifyKind(dto.SlotEndTime, DateTimeKind.Utc);

        // Business Rule 1: Must be scheduled within 7 days from today and not in the past
        ValidateSlotTimeBounds(startUtc, endUtc);

        var now = DateTime.UtcNow;
        var reservationNumber = $"RES-{now:yyyyMMdd}-{Guid.NewGuid().ToString("N")[..6].ToUpperInvariant()}";

        // Generate preliminary transaction QR code payload for prosumer/operator dispatch
        var qrData = GenerateQrToken(reservationNumber, dto.ProsumerNic, dto.NodeId, startUtc, dto.EnergyAmountKWh);

        var reservation = new Reservation
        {
            ReservationNumber = reservationNumber,
            ProsumerNic = dto.ProsumerNic.Trim().ToUpperInvariant(),
            ProsumerName = string.IsNullOrWhiteSpace(dto.ProsumerName) ? "Solar Prosumer" : dto.ProsumerName.Trim(),
            NodeId = dto.NodeId.Trim(),
            NodeName = string.IsNullOrWhiteSpace(dto.NodeName) ? $"Grid Node {dto.NodeId}" : dto.NodeName.Trim(),
            SlotStartTime = startUtc,
            SlotEndTime = endUtc,
            EnergyAmountKWh = dto.EnergyAmountKWh,
            ReservationType = dto.ReservationType.Trim(),
            Status = ReservationStatus.Approved, // Auto-approved on creation or pending confirmation
            TransactionQrCode = qrData,
            Notes = dto.Notes?.Trim(),
            CreatedAt = now,
            UpdatedAt = now,
            CreatedBy = createdBy
        };

        var created = await _repository.CreateAsync(reservation);
        _logger.LogInformation("Created energy reservation {Number} for prosumer {Nic}", created.ReservationNumber, created.ProsumerNic);

        return MapToResponse(created);
    }

    /// <summary>
    /// Reschedules or updates an existing reservation enforcing the 7-day and 12-hour rules.
    /// </summary>
    public async Task<ReservationResponseDto> UpdateAsync(string id, UpdateReservationDto dto)
    {
        // Fetch existing reservation
        var reservation = await _repository.GetByIdAsync(id);
        if (reservation is null)
        {
            throw new NotFoundException($"Reservation with id '{id}' was not found.");
        }

        // Business Rule 2: Cannot update completed or cancelled reservations
        if (reservation.Status == ReservationStatus.Cancelled)
        {
            throw new BusinessRuleException("Cannot modify a reservation that has already been cancelled.");
        }

        if (reservation.Status == ReservationStatus.Completed)
        {
            throw new BusinessRuleException("Cannot modify a reservation that has already been completed.");
        }

        // Business Rule 3: Updates require at least 12 hours' notice prior to original slot start
        ValidateNoticeWindow(reservation, "update");

        // Convert new slot times to UTC and validate the 7-day window
        var newStartUtc = DateTime.SpecifyKind(dto.SlotStartTime, DateTimeKind.Utc);
        var newEndUtc = DateTime.SpecifyKind(dto.SlotEndTime, DateTimeKind.Utc);
        ValidateSlotTimeBounds(newStartUtc, newEndUtc);

        reservation.SlotStartTime = newStartUtc;
        reservation.SlotEndTime = newEndUtc;
        reservation.EnergyAmountKWh = dto.EnergyAmountKWh;
        reservation.Notes = dto.Notes?.Trim();
        reservation.UpdatedAt = DateTime.UtcNow;

        // Refresh QR code token with updated slot time and energy amount
        reservation.TransactionQrCode = GenerateQrToken(
            reservation.ReservationNumber,
            reservation.ProsumerNic,
            reservation.NodeId,
            newStartUtc,
            dto.EnergyAmountKWh);

        await _repository.UpdateAsync(reservation);
        _logger.LogInformation("Updated energy reservation {Number} with 12h notice satisfied", reservation.ReservationNumber);

        return MapToResponse(reservation);
    }

    /// <summary>
    /// Cancels an existing reservation enforcing the 12-hour notice rule.
    /// </summary>
    public async Task<ReservationResponseDto> CancelAsync(string id, CancelReservationDto dto)
    {
        // Fetch existing reservation
        var reservation = await _repository.GetByIdAsync(id);
        if (reservation is null)
        {
            throw new NotFoundException($"Reservation with id '{id}' was not found.");
        }

        // Business Rule 4: Cannot cancel an already cancelled or completed reservation
        if (reservation.Status == ReservationStatus.Cancelled)
        {
            throw new BusinessRuleException("This reservation is already cancelled.");
        }

        if (reservation.Status == ReservationStatus.Completed)
        {
            throw new BusinessRuleException("Cannot cancel a completed energy transfer.");
        }

        // Business Rule 5: Cancellations require at least 12 hours' notice prior to slot start
        ValidateNoticeWindow(reservation, "cancellation");

        var now = DateTime.UtcNow;
        reservation.Status = ReservationStatus.Cancelled;
        reservation.CancellationReason = dto.Reason.Trim();
        reservation.CancelledAt = now;
        reservation.UpdatedAt = now;

        await _repository.UpdateAsync(reservation);
        _logger.LogInformation("Cancelled energy reservation {Number} with reason: {Reason}", reservation.ReservationNumber, dto.Reason);

        return MapToResponse(reservation);
    }

    /// <summary>
    /// Updates the status of a reservation (e.g. Approved or Completed).
    /// </summary>
    public async Task<ReservationResponseDto> UpdateStatusAsync(string id, UpdateReservationStatusDto dto)
    {
        // Fetch existing reservation
        var reservation = await _repository.GetByIdAsync(id);
        if (reservation is null)
        {
            throw new NotFoundException($"Reservation with id '{id}' was not found.");
        }

        var normalizedStatus = dto.Status.Trim();
        if (normalizedStatus != ReservationStatus.Pending &&
            normalizedStatus != ReservationStatus.Approved &&
            normalizedStatus != ReservationStatus.Completed &&
            normalizedStatus != ReservationStatus.Cancelled)
        {
            throw new BusinessRuleException($"Invalid reservation status '{dto.Status}'.");
        }

        if (reservation.Status == ReservationStatus.Cancelled)
        {
            throw new BusinessRuleException("Cannot change status of a cancelled reservation.");
        }

        reservation.Status = normalizedStatus;
        if (!string.IsNullOrWhiteSpace(dto.Notes))
        {
            reservation.Notes = dto.Notes.Trim();
        }
        reservation.UpdatedAt = DateTime.UtcNow;

        await _repository.UpdateAsync(reservation);
        return MapToResponse(reservation);
    }

    /// <summary>
    /// Verifies if a microgrid node has any active reservations.
    /// Used to block node deactivation if active energy reservations exist.
    /// </summary>
    public async Task<bool> HasActiveReservationsForNodeAsync(string nodeId)
    {
        // Check for pending or approved reservations assigned to the hub
        var active = await _repository.GetActiveReservationsByNodeIdAsync(nodeId);
        return active.Count > 0;
    }

    /// <summary>
    /// Validates that a slot falls within 7 days from now and is not in the past.
    /// </summary>
    private static void ValidateSlotTimeBounds(DateTime startTime, DateTime endTime)
    {
        // Basic chronology validation
        if (endTime <= startTime)
        {
            throw new BusinessRuleException("Slot end time must be after slot start time.");
        }

        var now = DateTime.UtcNow;

        // Allow 5 minutes grace for network/form latency
        if (startTime < now.AddMinutes(-5))
        {
            throw new BusinessRuleException("Reservations cannot be scheduled in the past.");
        }

        // Rule: Must be scheduled within 7 days
        if (startTime > now.AddDays(7))
        {
            throw new BusinessRuleException("Reservations must be scheduled within 7 days from today.");
        }

        // Reasonable single slot duration cap
        if ((endTime - startTime).TotalHours > 12)
        {
            throw new BusinessRuleException("A single reservation slot cannot exceed 12 hours.");
        }
    }

    /// <summary>
    /// Validates that at least 12 hours remain before the scheduled start time.
    /// </summary>
    private static void ValidateNoticeWindow(Reservation reservation, string operation)
    {
        // Calculate remaining hours until slot
        var now = DateTime.UtcNow;
        var hoursRemaining = (reservation.SlotStartTime - now).TotalHours;

        if (hoursRemaining < 12.0)
        {
            throw new BusinessRuleException(
                $"Reservation {operation} requires at least 12 hours' notice prior to the scheduled slot. " +
                $"This booking is scheduled in {Math.Max(0, hoursRemaining):F1} hours.");
        }
    }

    /// <summary>
    /// Generates a base64 encoded QR token representation for mobile operator verification.
    /// </summary>
    private static string GenerateQrToken(string refNum, string nic, string nodeId, DateTime start, double energy)
    {
        // Encode reservation attributes for scanning and verification
        var payload = $"{refNum}|{nic}|{nodeId}|{start:yyyy-MM-ddTHH:mm:ssZ}|{energy:F2}";
        return Convert.ToBase64String(Encoding.UTF8.GetBytes(payload));
    }

    /// <summary>
    /// Projects an entity to a response DTO with calculated rules.
    /// </summary>
    private static ReservationResponseDto MapToResponse(Reservation r)
    {
        // Compute hours remaining and actionability flags
        var now = DateTime.UtcNow;
        var hoursRemaining = (r.SlotStartTime - now).TotalHours;
        var canAct = hoursRemaining >= 12.0 &&
                     r.Status != ReservationStatus.Cancelled &&
                     r.Status != ReservationStatus.Completed;

        return new ReservationResponseDto
        {
            Id = r.Id ?? string.Empty,
            ReservationNumber = r.ReservationNumber,
            ProsumerNic = r.ProsumerNic,
            ProsumerName = r.ProsumerName,
            NodeId = r.NodeId,
            NodeName = r.NodeName,
            SlotStartTime = r.SlotStartTime,
            SlotEndTime = r.SlotEndTime,
            EnergyAmountKWh = r.EnergyAmountKWh,
            ReservationType = r.ReservationType,
            Status = r.Status,
            TransactionQrCode = r.TransactionQrCode,
            CancellationReason = r.CancellationReason,
            CancelledAt = r.CancelledAt,
            Notes = r.Notes,
            CreatedAt = r.CreatedAt,
            UpdatedAt = r.UpdatedAt,
            CreatedBy = r.CreatedBy,
            CanModify = canAct,
            CanCancel = canAct,
            HoursUntilSlot = Math.Round(hoursRemaining, 1)
        };
    }
}
