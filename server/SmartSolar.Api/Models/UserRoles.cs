/*
 * File:        UserRoles.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Models
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
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

    /// <summary>
    /// Returns true when the supplied text is one of the two supported roles.
    /// </summary>
    public static bool IsValid(string? role)
    {
        // Comparison is case sensitive so the stored value always matches exactly
        return !string.IsNullOrWhiteSpace(role) && All.Contains(role);
    }
}
