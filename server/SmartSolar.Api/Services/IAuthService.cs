/*
 * File:        IAuthService.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Services
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Modified:    2026-09-29 by N. Jayasinghe (IT2XXXXXXX) — added prosumer
 *              login for the Android app.
 * Description: Contract for authentication: signing a web user or a prosumer
 *              in, reading the signed in user, and changing a password.
 */

using SmartSolar.Api.DTOs;

namespace SmartSolar.Api.Services;

public interface IAuthService
{
    /// <summary>
    /// Verifies credentials and returns a signed token on success.
    /// </summary>
    Task<LoginResponseDto> LoginAsync(LoginRequestDto dto);

    /// <summary>
    /// Verifies a prosumer's NIC (or email) and password and returns a signed token.
    /// </summary>
    Task<ProsumerLoginResponseDto> ProsumerLoginAsync(ProsumerLoginRequestDto dto);

    /// <summary>
    /// Returns the profile of the user identified by the supplied token id.
    /// </summary>
    Task<UserResponseDto> GetCurrentUserAsync(string userId);

    /// <summary>
    /// Changes the signed in user's own password.
    /// </summary>
    Task ChangePasswordAsync(string userId, ChangePasswordDto dto);
}
