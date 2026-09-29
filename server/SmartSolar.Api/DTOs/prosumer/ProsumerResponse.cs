/*
 * File:        ProsumerResponse.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       DTOs
 * Author:      Dilshan Anupriya (IT22189530)
 * Created:     2026-09-20
 * Description: Prosumer data returned to clients. Never includes the
 *              password hash.
 */

using SmartSolar.Api.Models;

namespace SmartSolar.Api.DTOs.Prosumers;

public class ProsumerResponse
{
    public string Nic { get; set; } = string.Empty;
    public string FullName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string Phone { get; set; } = string.Empty;
    public string Address { get; set; } = string.Empty;
    public string Status { get; set; } = string.Empty;
    public DateTime? DeactivatedAt { get; set; }
    public string? DeactivatedBy { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }

    /// <summary>
    /// Maps a Prosumer document to a response, leaving out the password hash.
    /// </summary>
    public static ProsumerResponse FromModel(Prosumer prosumer)
    {
        // Copy only the fields that are safe to send to clients
        return new ProsumerResponse
        {
            Nic = prosumer.Nic,
            FullName = prosumer.FullName,
            Email = prosumer.Email,
            Phone = prosumer.Phone,
            Address = prosumer.Address,
            Status = prosumer.Status.ToString(),
            DeactivatedAt = prosumer.DeactivatedAt,
            DeactivatedBy = prosumer.DeactivatedBy,
            CreatedAt = prosumer.CreatedAt,
            UpdatedAt = prosumer.UpdatedAt
        };
    }
}