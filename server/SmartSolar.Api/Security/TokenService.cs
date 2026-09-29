/*
 * File:        TokenService.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Security
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Builds the signed JWT issued at login. The role is written into a
 *              ClaimTypes.Role claim, which is what makes the
 *              [Authorize(Roles = "Backoffice")] attribute work on a controller.
 */

using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using SmartSolar.Api.Models;

namespace SmartSolar.Api.Security;

public class TokenService : ITokenService
{
    private readonly JwtSettings _settings;

    /// <summary>
    /// Reads the JWT settings and fails early if the signing key is missing.
    /// </summary>
    public TokenService(IOptions<JwtSettings> options)
    {
        // Validate configuration on start-up rather than on the first login attempt
        _settings = options.Value;

        if (string.IsNullOrWhiteSpace(_settings.SecretKey))
        {
            throw new InvalidOperationException(
                "JwtSettings:SecretKey is not configured. Set it using dotnet user-secrets.");
        }

        // HMAC-SHA256 needs a key of at least 256 bits (32 characters)
        if (Encoding.UTF8.GetByteCount(_settings.SecretKey) < 32)
        {
            throw new InvalidOperationException(
                "JwtSettings:SecretKey must be at least 32 characters long.");
        }
    }

    /// <summary>
    /// Creates a signed token describing the supplied user.
    /// </summary>
    public TokenResult CreateToken(WebUser user)
    {
        // The claims are the facts about the user that travel inside the token
        var expiresAt = DateTime.UtcNow.AddMinutes(_settings.ExpiryMinutes);

        var claims = new List<Claim>
        {
            // Unique id of this token, useful when debugging
            new(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString()),

            // Subject: the user id, read back by the API to identify the caller
            new(JwtRegisteredClaimNames.Sub, user.Id ?? string.Empty),
            new(ClaimTypes.NameIdentifier, user.Id ?? string.Empty),

            new(ClaimTypes.Name, user.FullName),
            new(ClaimTypes.Email, user.Email),

            // The role claim checked by [Authorize(Roles = "...")]
            new(ClaimTypes.Role, user.Role)
        };

        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_settings.SecretKey));
        var credentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

        var token = new JwtSecurityToken(
            issuer: _settings.Issuer,
            audience: _settings.Audience,
            claims: claims,
            expires: expiresAt,
            signingCredentials: credentials);

        return new TokenResult(new JwtSecurityTokenHandler().WriteToken(token), expiresAt);
    }
}
