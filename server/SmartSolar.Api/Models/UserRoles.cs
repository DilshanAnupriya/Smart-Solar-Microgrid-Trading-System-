/*
 * File:        UserRoles.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Models
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Modified:    2026-09-29 by N. Jayasinghe (IT2XXXXXXX) — added the Prosumer
 *              role name used in tokens issued to the Android app.
 * Description: The two web application roles defined by the system. Keeping the
 *              role names in one place stops spelling mistakes between the
 *              database, the [Authorize] attributes and the React client.
 */

namespace SmartSolar.Api.Models;

public static class UserRoles
{
    // Backoffice users have access to system administration functions
    public const string Backoffice = "Backoffice";

    // Grid Operators have access to operational tools only
    public const string GridOperator = "GridOperator";

    // Every valid role, used for validation and for the role filter
    public static readonly string[] All = [Backoffice, GridOperator];

    // Prosumers sign in only from the Android app. This is a token role, not a
    // web user role, so it is deliberately left out of All.
    public const string Prosumer = "Prosumer";

    /// <summary>
    /// Returns true when the supplied text is one of the two supported roles.
    /// </summary>
    public static bool IsValid(string? role)
    {
        // Comparison is case sensitive so the stored value always matches exactly
        return !string.IsNullOrWhiteSpace(role) && All.Contains(role);
    }
}
