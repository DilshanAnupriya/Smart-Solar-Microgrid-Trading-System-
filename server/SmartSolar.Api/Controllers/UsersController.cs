/*
 * File:        UsersController.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Controllers
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Web user management endpoints. The controller is marked
 *              [Authorize(Roles = "Backoffice")] at class level, which is how
 *              the requirement "only Backoffice users have access to system
 *              administration functions" is enforced on the server. A Grid
 *              Operator's token is rejected here with 403 even if the client
 *              somehow shows the page.
 */

using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolar.Api.DTOs;
using SmartSolar.Api.Models;
using SmartSolar.Api.Services;

namespace SmartSolar.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize(Roles = UserRoles.Backoffice)]
public class UsersController : ControllerBase
{
    private readonly IUserService _userService;

    /// <summary>
    /// Receives the user management service through dependency injection.
    /// </summary>
    public UsersController(IUserService userService)
    {
        // Store the service used by every endpoint in this controller
        _userService = userService;
    }

    /// <summary>
    /// GET api/users — lists users, filtered by role, status and search text.
    /// </summary>
    [HttpGet]
    [ProducesResponseType(typeof(List<UserResponseDto>), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetAll(
        [FromQuery] string? role,
        [FromQuery] string? status,
        [FromQuery] string? search)
    {
        // The filters are optional; omitting them returns every account
        var users = await _userService.GetAllAsync(role, status, search);

        return Ok(users);
    }

    /// <summary>
    /// GET api/users/{id} — returns a single user.
    /// </summary>
    [HttpGet("{id}")]
    [ProducesResponseType(typeof(UserResponseDto), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(string id)
    {
        // A missing id becomes a 404 through the exception handling middleware
        var user = await _userService.GetByIdAsync(id);

        return Ok(user);
    }

    /// <summary>
    /// POST api/users — creates a Backoffice or Grid Operator account.
    /// </summary>
    [HttpPost]
    [ProducesResponseType(typeof(UserResponseDto), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status409Conflict)]
    public async Task<IActionResult> Create([FromBody] CreateUserDto dto)
    {
        // The creating officer's id is taken from the token and stored for audit
        var created = await _userService.CreateAsync(dto, GetCurrentUserId());

        return CreatedAtAction(nameof(GetById), new { id = created.Id }, created);
    }

    /// <summary>
    /// PUT api/users/{id} — updates an account's name, email and role.
    /// </summary>
    [HttpPut("{id}")]
    [ProducesResponseType(typeof(UserResponseDto), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status404NotFound)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status409Conflict)]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateUserDto dto)
    {
        // Passwords are not changed here; the owner changes their own password
        var updated = await _userService.UpdateAsync(id, dto);

        return Ok(updated);
    }

    /// <summary>
    /// PATCH api/users/{id}/deactivate — blocks an account from signing in.
    /// </summary>
    [HttpPatch("{id}/deactivate")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Deactivate(string id)
    {
        // The current user's id is passed so the service can refuse self-deactivation
        await _userService.DeactivateAsync(id, GetCurrentUserId());

        return NoContent();
    }

    /// <summary>
    /// PATCH api/users/{id}/activate — restores a deactivated account.
    /// </summary>
    [HttpPatch("{id}/activate")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Activate(string id)
    {
        // Reactivating an already active account is refused by the service
        await _userService.ActivateAsync(id);

        return NoContent();
    }

    /// <summary>
    /// DELETE api/users/{id} — permanently removes an account.
    /// Only Backoffice users reach this endpoint, because of the
    /// [Authorize(Roles = "Backoffice")] attribute on this controller.
    /// </summary>
    [HttpDelete("{id}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiErrorResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Delete(string id)
    {
        // The current user's id is passed so the service can refuse self-deletion
        await _userService.DeleteAsync(id, GetCurrentUserId());

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
