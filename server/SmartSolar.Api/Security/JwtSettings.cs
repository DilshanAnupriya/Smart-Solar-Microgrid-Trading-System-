/*
 * File:        JwtSettings.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Security
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Strongly typed settings for JSON Web Token creation and
 *              validation, bound from the "JwtSettings" section of
 *              appsettings.json and user-secrets.
 */

namespace SmartSolar.Api.Security;

public class JwtSettings
{
    // Name of the service that issues the token (checked when a token is validated)
    public string Issuer { get; set; } = string.Empty;

    // Name of the clients allowed to use the token (web app and mobile app)
    public string Audience { get; set; } = string.Empty;

    // How long a token stays valid after login, in minutes
    public int ExpiryMinutes { get; set; }

    // Signing key used to sign and verify tokens (kept in user-secrets, never in Git)
    public string SecretKey { get; set; } = string.Empty;
}
