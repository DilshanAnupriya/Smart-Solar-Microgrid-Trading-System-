/*
 * File:        UserMapper.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Services
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Single place where a stored WebUser document is converted into
 *              the UserResponseDto sent to clients. Because every service maps
 *              through this one method, and the DTO has no password field, a
 *              password hash can never leave the API.
 */

using SmartSolar.Api.DTOs;
using SmartSolar.Api.Models;

namespace SmartSolar.Api.Services;

public static class UserMapper
{
    /// <summary>
    /// Copies the safe fields of a user document into the response shape.
    /// </summary>
    public static UserResponseDto ToResponse(WebUser user)
    {
        // PasswordHash is deliberately not copied
        return new UserResponseDto
        {
            Id = user.Id ?? string.Empty,
            FullName = user.FullName,
            Email = user.Email,
            Role = user.Role,
            IsActive = user.IsActive,
            CreatedAt = user.CreatedAt,
            UpdatedAt = user.UpdatedAt
        };
    }
}
