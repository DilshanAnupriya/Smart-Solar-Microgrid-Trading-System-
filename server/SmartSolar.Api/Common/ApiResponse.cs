/*
 * File:        ApiResponse.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Common
 * Author:      BDA Cooray (IT22189530)
 * Created:     2026-09-19
 * Description: Standard response envelope returned by every API endpoint so
 *              that web and mobile clients can handle results consistently.
 */

namespace SmartSolar.Api.Common;

public class ApiResponse<T>
{
    // True when the request completed successfully
    public bool Success { get; set; }

    // Human-readable message shown by the web and mobile clients
    public string Message { get; set; } = string.Empty;

    // Payload returned on success (null on failure)
    public T? Data { get; set; }

    // Optional list of validation error messages
    public List<string>? Errors { get; set; }

    /// <summary>
    /// Creates a successful response containing data.
    /// </summary>
    public static ApiResponse<T> Ok(T data, string message = "Request completed successfully")
    {
        // Build a success envelope with the given data and message
        return new ApiResponse<T>
        {
            Success = true,
            Message = message,
            Data = data
        };
    }

    /// <summary>
    /// Creates a failed response with a message and optional validation errors.
    /// </summary>
    public static ApiResponse<T> Fail(string message, List<string>? errors = null)
    {
        // Build a failure envelope with no data
        return new ApiResponse<T>
        {
            Success = false,
            Message = message,
            Errors = errors
        };
    }
}