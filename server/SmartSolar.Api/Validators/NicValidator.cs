/*
 * File:        NicValidator.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Validators
 * Author:      Dilshan Anupriya (IT22189530)
 * Created:     2026-09-20
 * Description: Validates and normalises Sri Lankan National Identity Card
 *              numbers. Supports the old format (9 digits + V/X) and the
 *              new format (12 digits).
 */

using System.Text.RegularExpressions;

namespace SmartSolar.Api.Validators;

public static class NicValidator
{
    // Old NIC: 9 digits followed by V or X (e.g. 991234567V)
    private static readonly Regex OldNicPattern = new(@"^[0-9]{9}[VX]$", RegexOptions.Compiled);

    // New NIC: 12 digits (e.g. 199912345678)
    private static readonly Regex NewNicPattern = new(@"^[0-9]{12}$", RegexOptions.Compiled);

    /// <summary>
    /// Trims spaces and converts the NIC to uppercase so v/x and V/X are treated the same.
    /// </summary>
    public static string Normalize(string? nic)
    {
        // Null input becomes an empty string so callers never get a null back
        return (nic ?? string.Empty).Trim().ToUpperInvariant();
    }

    /// <summary>
    /// Returns true if the NIC matches either the old or the new Sri Lankan format.
    /// </summary>
    public static bool IsValid(string? nic)
    {
        // Normalise first so lowercase v/x and surrounding spaces are accepted
        var normalized = Normalize(nic);
        return OldNicPattern.IsMatch(normalized) || NewNicPattern.IsMatch(normalized);
    }
}