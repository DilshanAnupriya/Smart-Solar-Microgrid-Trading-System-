/*
 * File:        ITokenService.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Security
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Modified:    2026-09-29 by N. Jayasinghe (IT2XXXXXXX) — added the prosumer
 *              token used by the Android app.
 * Description: Contract for creating the JSON Web Token that a client sends back
 *              on every later request.
 */

using SmartSolar.Api.Models;

namespace SmartSolar.Api.Security;

/// <summary>
/// The signed token and the exact moment it stops being valid.
/// </summary>
public record TokenResult(string Token, DateTime ExpiresAt);

public interface ITokenService
{
    /// <summary>
    /// Builds a signed token that carries the user's id, name, email and role.
    /// </summary>
    TokenResult CreateToken(WebUser user);

    /// <summary>
    /// Builds a signed token for a prosumer: the NIC as the id and the Prosumer role.
    /// </summary>
    TokenResult CreateToken(Prosumer prosumer);
}
