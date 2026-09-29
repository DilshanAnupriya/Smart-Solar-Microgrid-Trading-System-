/*
 * File:        AuthDtos.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       DTOs
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Data transfer objects used by the authentication endpoints.
 *              These shapes are what the React client and the Android client
 *              send and receive; the WebUser document itself is never exposed.
 */

using System.ComponentModel.DataAnnotations;

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
