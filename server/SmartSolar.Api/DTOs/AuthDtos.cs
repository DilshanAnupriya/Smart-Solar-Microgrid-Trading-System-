/*
 * File:        AuthDtos.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       DTOs
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Modified:    2026-09-29 by N. Jayasinghe (IT2XXXXXXX) — added the prosumer
 *              login request and response used by the Android app.
 * Description: Data transfer objects used by the authentication endpoints.
 *              These shapes are what the React client and the Android client
 *              send and receive; the WebUser document itself is never exposed.
 */

using System.ComponentModel.DataAnnotations;
using SmartSolar.Api.DTOs.Prosumers;
using SmartSolar.Api.Models;

namespace SmartSolar.Api.DTOs;

/// <summary>
/// Credentials posted to api/auth/login. The identifier may be the user's
/// email address, their username or their phone number, so no single format
/// can be enforced here; the lookup decides which one it matched.
/// </summary>
public class LoginRequestDto
{
    [Required(ErrorMessage = "Email, username or phone number is required.")]
    public string Identifier { get; set; } = string.Empty;

    [Required(ErrorMessage = "Password is required.")]
    public string Password { get; set; } = string.Empty;
}

/// <summary>
/// Successful login result: the token plus enough user detail for the client
/// to render the navigation bar and decide which home page to show.
/// </summary>
public class LoginResponseDto
{
    public string Token { get; set; } = string.Empty;

    public DateTime ExpiresAt { get; set; }

    public UserResponseDto User { get; set; } = new();
}

/// <summary>
/// Body posted to api/auth/change-password by the signed in user.
/// </summary>
public class ChangePasswordDto
{
    [Required(ErrorMessage = "Current password is required.")]
    public string CurrentPassword { get; set; } = string.Empty;

    [Required(ErrorMessage = "New password is required.")]
    [MinLength(6, ErrorMessage = "New password must be at least 6 characters.")]
    public string NewPassword { get; set; } = string.Empty;
}

/// <summary>
/// Credentials posted to api/auth/prosumer-login by the Android app. The
/// identifier is the prosumer's NIC or the email address they registered with.
/// </summary>
public class ProsumerLoginRequestDto
{
    [Required(ErrorMessage = "NIC or email is required.")]
    public string Identifier { get; set; } = string.Empty;

    [Required(ErrorMessage = "Password is required.")]
    public string Password { get; set; } = string.Empty;
}

/// <summary>
/// Successful prosumer login. Same top-level shape as the web login, plus the
/// role the app stores in SQLite to decide which home screen to open.
/// </summary>
public class ProsumerLoginResponseDto
{
    public string Token { get; set; } = string.Empty;

    public DateTime ExpiresAt { get; set; }

    public string Role { get; set; } = UserRoles.Prosumer;

    public ProsumerResponse User { get; set; } = new();
}
