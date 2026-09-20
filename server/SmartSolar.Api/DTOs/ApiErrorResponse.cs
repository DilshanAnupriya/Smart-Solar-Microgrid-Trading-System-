/*
 * File:        ApiErrorResponse.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       DTOs
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Single error shape returned by every failing endpoint, so both
 *              the React client and the Android client can read errors the same
 *              way no matter which part of the API produced them.
 */

namespace SmartSolar.Api.DTOs;

public class ApiErrorResponse
{
    // Always false, so a client can tell success from failure without reading the status code
    public bool Success { get; set; }

    // Human readable message that the client can show directly to the user
    public string Message { get; set; } = string.Empty;

    // Field level validation messages, keyed by field name; empty for other errors
    public Dictionary<string, string[]> Errors { get; set; } = [];

    /// <summary>
    /// Builds an error response carrying a single message.
    /// </summary>
    public static ApiErrorResponse FromMessage(string message)
    {
        // Used for business rule failures such as a duplicate email
        return new ApiErrorResponse { Success = false, Message = message };
    }
}
