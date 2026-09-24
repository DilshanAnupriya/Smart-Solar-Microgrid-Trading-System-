/*
 * File:        INodeReservationChecker.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Services
 * Author:      Vidura Hewaduwa
 * Created:     2026-09-24
 * Description: Small integration boundary used to stop nodes with active
 *              reservations from being deactivated.
 */

namespace SmartSolar.Api.Services;

/// <summary>
/// Keeps node deactivation independent from the reservation implementation.
/// It can later delegate to IReservationService without changing NodeService.
/// </summary>
public interface INodeReservationChecker
{
    Task<bool> HasActiveReservationsForNodeAsync(string nodeId);
}
