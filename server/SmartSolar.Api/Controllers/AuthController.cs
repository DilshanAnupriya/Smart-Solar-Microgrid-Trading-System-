/*
 * File:        AuthController.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Controllers
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Authentication endpoints used by both the React web client and
 *              the Android client. The controller only reads the request, calls
 *              the service and returns the result; all rules are in AuthService.
 */

using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolar.Api.DTOs;
using SmartSolar.Api.Services;

namespace SmartSolar.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AuthController : ControllerBase
{
    private readonly IAuthService _authService;

    /// <summary>
    /// Receives the authentication service through dependency injection.
    /// </summary>
    public AuthController(IAuthService authService)
    {
        // Store the service used by every endpoint in this controller
        _authService = authService;
    }

    /// <summary>
    /// POST api/auth/login — verifies credentials and returns a JWT. The
    /// identifier may be the user's email address, username or phone number.
    /// </summary>
    [HttpPost("login")]
    [AllowAnonymous]
    [ProducesResponseType(typeof(LoginResponseDto), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status403Forbidden)]
    public async Task<IActionResult> Login([FromBody] LoginRequestDto dto)
    {
        // Invalid credentials and deactivated accounts are turned into 401 and
        // 403 by the exception handling middleware
        var result = await _authService.LoginAsync(dto);

        return Ok(result);
    }

    /// <summary>
    /// GET api/auth/me — returns the profile of the signed in user.
    /// </summary>
    [HttpGet("me")]
    [Authorize]
    [ProducesResponseType(typeof(UserResponseDto), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetCurrentUser()
    {
        // The id is read from the validated token, never from the request body
        var user = await _authService.GetCurrentUserAsync(GetCurrentUserId());

        return Ok(user);
    }

    /// <summary>
    /// POST api/auth/change-password — changes the signed in user's password.
    /// </summary>
    [HttpPost("change-password")]
    [Authorize]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> ChangePassword([FromBody] ChangePasswordDto dto)
    {
        // A user may only ever change their own password with this endpoint
        await _authService.ChangePasswordAsync(GetCurrentUserId(), dto);

        return NoContent();
    }

    /// <summary>
    /// Reads the signed in user's id out of the JWT claims.
    /// </summary>
    private string GetCurrentUserId()
    {
        // The claim was written by TokenService when the token was created
        return User.FindFirstValue(ClaimTypes.NameIdentifier) ?? string.Empty;
    }
}
