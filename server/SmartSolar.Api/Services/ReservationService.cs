/*
 * File:        ReservationService.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Services
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-22
 * Modified:    2026-09-30 by Cooray B.D.A (IT22189530) — added the prosumer
 *              operations for the Android app (own reservations only, Pending
 *              until approved, QR code released only once approved, node must
 *              be active) and moved the update and cancel rules into shared
 *              helpers so staff and prosumers follow exactly the same rules.
 * Description: FAT service implementation of IReservationService. Centralizes all
 *              business logic for energy slot reservations, strictly enforcing
 *              the 7-day scheduling window and the 12-hour advance notice
 *              requirement for updates and cancellations.
 */

using System.Text;
using MongoDB.Bson;
using SmartSolar.Api.DTOs;
using SmartSolar.Api.Exceptions;
using SmartSolar.Api.Models;
using SmartSolar.Api.Repositories;
using SmartSolar.Api.Validators;

namespace SmartSolar.Api.Services;

public class ReservationService : IReservationService
{
    private readonly IReservationRepository _repository;
    private readonly IProsumerRepository _prosumerRepository;
    private readonly INodeRepository _nodeRepository;
    private readonly ILogger<ReservationService> _logger;

    /// <summary>
    /// Receives the reservation, prosumer and node repositories and the logger.
    /// </summary>
    public ReservationService(
        IReservationRepository repository,
        IProsumerRepository prosumerRepository,
        INodeRepository nodeRepository,
        ILogger<ReservationService> logger)
    {
        // Store dependencies injected by the container
        _repository = repository;
        _prosumerRepository = prosumerRepository;
        _nodeRepository = nodeRepository;
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
        var reservationNumber = NewReservationNumber(now);

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

        // Business Rules 2 and 3 and the 7-day window, shared with prosumer updates
        ApplyUpdate(reservation, dto);

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

        // Business Rules 4 and 5, shared with prosumer cancellations
        ApplyCancel(reservation, dto.Reason);

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

    // -------------------------------------------------------------------------
    // Prosumer operations (Android app). The NIC always comes from the token.
    // -------------------------------------------------------------------------

    /// <summary>
    /// Lists the prosumer's own reservations, newest slot first.
    /// </summary>
    public async Task<List<ReservationResponseDto>> GetForProsumerAsync(string nic, string? status, string? search)
    {
        // Filtering by the token's NIC means a prosumer can only ever list their own bookings
        var reservations = await _repository.GetAllAsync(NicValidator.Normalize(nic), null, status, null, null, search);
        return reservations.Select(MapForProsumer).ToList();
    }

    /// <summary>
    /// Returns one of the prosumer's own reservations.
    /// </summary>
    public async Task<ReservationResponseDto> GetForProsumerByIdAsync(string id, string nic)
    {
        // Another prosumer's reservation is refused rather than shown
        var reservation = await GetOwnedAsync(id, nic);
        return MapForProsumer(reservation);
    }

    /// <summary>
    /// Requests a new reservation for the prosumer. It starts as Pending, and its QR code is
    /// only released to the app once a Backoffice or Grid Operator user approves it.
    /// </summary>
    public async Task<ReservationResponseDto> CreateForProsumerAsync(ProsumerReservationRequestDto dto, string nic)
    {
        // Rule: only an active prosumer may book (a token can outlive a deactivation)
        var prosumer = await _prosumerRepository.GetByNicAsync(NicValidator.Normalize(nic))
                       ?? throw new NotFoundException("Your prosumer profile was not found.");
        if (prosumer.Status != ProsumerStatus.Active)
        {
            throw new ForbiddenException("A deactivated account cannot make reservations.");
        }

        // Rule: the node must exist and be active; its name is taken from the node itself
        var node = await _nodeRepository.GetByIdAsync(dto.NodeId.Trim());
        if (node is null || !node.IsActive)
        {
            throw new BusinessRuleException("The selected grid node is not available for reservations.");
        }

        // Rule: only drop-off and charging reservations exist
        var reservationType = ParseReservationType(dto.ReservationType);

        // Business Rule 1: within 7 days, not in the past, and at most 12 hours long
        var startUtc = ToUtc(dto.SlotStartTime);
        var endUtc = ToUtc(dto.SlotEndTime);
        ValidateSlotTimeBounds(startUtc, endUtc);

        var now = DateTime.UtcNow;
        var reservationNumber = NewReservationNumber(now);

        var reservation = new Reservation
        {
            ReservationNumber = reservationNumber,
            ProsumerNic = prosumer.Nic,
            ProsumerName = prosumer.FullName,
            NodeId = node.Id,
            NodeName = node.Name,
            SlotStartTime = startUtc,
            SlotEndTime = endUtc,
            EnergyAmountKWh = dto.EnergyAmountKWh,
            ReservationType = reservationType,
            // Rule: a prosumer's request waits for approval before it can be used
            Status = ReservationStatus.Pending,
            TransactionQrCode = GenerateQrToken(reservationNumber, prosumer.Nic, node.Id, startUtc, dto.EnergyAmountKWh),
            Notes = dto.Notes?.Trim(),
            CreatedAt = now,
            UpdatedAt = now,
            CreatedBy = prosumer.Nic
        };

        var created = await _repository.CreateAsync(reservation);
        _logger.LogInformation("Prosumer {Nic} requested energy reservation {Number}", created.ProsumerNic, created.ReservationNumber);

        return MapForProsumer(created);
    }

    /// <summary>
    /// Changes the slot or energy of the prosumer's own reservation. The same rules as a staff
    /// update apply, and the reservation goes back to Pending so the old QR code stops working.
    /// </summary>
    public async Task<ReservationResponseDto> UpdateForProsumerAsync(string id, UpdateReservationDto dto, string nic)
    {
        // Only the owner may change it; then the shared rules (state, 12-hour notice, 7-day window)
        var reservation = await GetOwnedAsync(id, nic);
        ApplyUpdate(reservation, dto);

        // Rule: a changed booking must be approved again before a QR code is released
        reservation.Status = ReservationStatus.Pending;

        await _repository.UpdateAsync(reservation);
        _logger.LogInformation("Prosumer {Nic} updated energy reservation {Number}", reservation.ProsumerNic, reservation.ReservationNumber);

        return MapForProsumer(reservation);
    }

    /// <summary>
    /// Cancels the prosumer's own reservation, with the same 12-hour rule as for staff.
    /// </summary>
    public async Task<ReservationResponseDto> CancelForProsumerAsync(string id, ProsumerCancelReservationDto dto, string nic)
    {
        // Only the owner may cancel; the reason is optional for prosumers
        var reservation = await GetOwnedAsync(id, nic);
        var reason = string.IsNullOrWhiteSpace(dto.Reason)
            ? "Cancelled by the prosumer in the mobile app."
            : dto.Reason;
        ApplyCancel(reservation, reason);

        await _repository.UpdateAsync(reservation);
        _logger.LogInformation("Prosumer {Nic} cancelled energy reservation {Number}", reservation.ProsumerNic, reservation.ReservationNumber);

        return MapForProsumer(reservation);
    }

    /// <summary>
    /// Checks the update rules, then applies the new slot and energy and refreshes the QR token.
    /// Shared by staff and prosumer updates so both follow exactly the same rules.
    /// </summary>
    private static void ApplyUpdate(Reservation reservation, UpdateReservationDto dto)
    {
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
        var newStartUtc = ToUtc(dto.SlotStartTime);
        var newEndUtc = ToUtc(dto.SlotEndTime);
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
    }

    /// <summary>
    /// Checks the cancellation rules, then marks the reservation cancelled.
    /// Shared by staff and prosumer cancellations.
    /// </summary>
    private static void ApplyCancel(Reservation reservation, string reason)
    {
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
        reservation.CancellationReason = reason.Trim();
        reservation.CancelledAt = now;
        reservation.UpdatedAt = now;
    }

    /// <summary>
    /// Loads a reservation and checks that it belongs to the prosumer with this NIC.
    /// </summary>
    private async Task<Reservation> GetOwnedAsync(string id, string nic)
    {
        // A malformed id cannot match any reservation, so it is simply "not found"
        var reservation = ObjectId.TryParse(id, out _) ? await _repository.GetByIdAsync(id) : null;
        if (reservation is null)
        {
            throw new NotFoundException($"Reservation with id '{id}' was not found.");
        }

        // Rule: a prosumer may only see and change their own reservations
        if (!string.Equals(reservation.ProsumerNic, NicValidator.Normalize(nic), StringComparison.Ordinal))
        {
            throw new ForbiddenException("You can only access your own reservations.");
        }

        return reservation;
    }

    /// <summary>
    /// Maps a reservation for its prosumer. The QR code is only dispatched once the reservation
    /// is approved; while it is pending, and after it is cancelled or completed, it is left empty.
    /// </summary>
    private static ReservationResponseDto MapForProsumer(Reservation reservation)
    {
        // Same projection as for staff, minus a QR code that must not be used yet
        var response = MapToResponse(reservation);
        if (reservation.Status != ReservationStatus.Approved)
        {
            response.TransactionQrCode = string.Empty;
        }

        return response;
    }

    /// <summary>
    /// Accepts the two reservation types in any letter case and returns the stored spelling.
    /// </summary>
    private static string ParseReservationType(string? value)
    {
        // Anything else is refused, so the database only ever holds the two known types
        var trimmed = value?.Trim();
        if (string.Equals(trimmed, ReservationType.DropOff, StringComparison.OrdinalIgnoreCase))
        {
            return ReservationType.DropOff;
        }

        if (string.Equals(trimmed, ReservationType.Charging, StringComparison.OrdinalIgnoreCase))
        {
            return ReservationType.Charging;
        }

        throw new BusinessRuleException("Reservation type must be DropOff or Charging.");
    }

    /// <summary>
    /// Makes a request time UTC. A time sent with "Z" already is; one sent with an offset
    /// arrives as local time and is converted rather than relabelled.
    /// </summary>
    private static DateTime ToUtc(DateTime value)
    {
        // Times without any zone are taken as UTC, which is what the clients send
        return value.Kind switch
        {
            DateTimeKind.Utc => value,
            DateTimeKind.Local => value.ToUniversalTime(),
            _ => DateTime.SpecifyKind(value, DateTimeKind.Utc)
        };
    }

    /// <summary>
    /// Human-friendly reference number, e.g. "RES-20260930-8F2A1C".
    /// </summary>
    private static string NewReservationNumber(DateTime now)
    {
        // Date plus six random hex characters; the unique index guards against a rare clash
        return $"RES-{now:yyyyMMdd}-{Guid.NewGuid().ToString("N")[..6].ToUpperInvariant()}";
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
