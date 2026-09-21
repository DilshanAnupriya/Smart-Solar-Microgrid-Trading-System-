/*
 * File:        ExceptionHandlingMiddleware.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Middleware
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Catches every exception that escapes a controller and converts it
 *              into a consistent JSON error response. Expected failures use the
 *              status code carried by the exception; anything unexpected becomes
 *              a generic 500 so internal details are never sent to a client.
 */

using System.Text.Json;
using SmartSolar.Api.DTOs;
using SmartSolar.Api.Exceptions;

namespace SmartSolar.Api.Middleware;

public class ExceptionHandlingMiddleware
{
    private readonly RequestDelegate _next;
    private readonly ILogger<ExceptionHandlingMiddleware> _logger;

    /// <summary>
    /// Receives the next middleware in the pipeline and the logger.
    /// </summary>
    public ExceptionHandlingMiddleware(RequestDelegate next, ILogger<ExceptionHandlingMiddleware> logger)
    {
        // Keep both references for use on every request
        _next = next;
        _logger = logger;
    }

    /// <summary>
    /// Runs the rest of the pipeline and handles any exception it throws.
    /// </summary>
    public async Task InvokeAsync(HttpContext context)
    {
        // Let the request run normally, and only step in when it fails
        try
        {
            await _next(context);
        }
        catch (AppException ex)
        {
            // Expected failure: use the status code and message chosen by the service
            _logger.LogWarning("Handled application error: {Message}", ex.Message);
            await WriteErrorAsync(context, ex.StatusCode, ex.Message);
        }
        catch (Exception ex)
        {
            // Unexpected failure: log the detail for the developer, hide it from the client
            _logger.LogError(ex, "Unhandled error while processing {Path}", context.Request.Path);
            await WriteErrorAsync(context, StatusCodes.Status500InternalServerError,
                "An unexpected error occurred. Please try again.");
        }
    }

    /// <summary>
    /// Writes the shared ApiErrorResponse shape to the HTTP response.
    /// </summary>
    private static async Task WriteErrorAsync(HttpContext context, int statusCode, string message)
    {
        // Do nothing if the response has already started being sent to the client
        if (context.Response.HasStarted)
        {
            return;
        }

        context.Response.Clear();
        context.Response.StatusCode = statusCode;
        context.Response.ContentType = "application/json";

        var payload = JsonSerializer.Serialize(
            ApiErrorResponse.FromMessage(message),
            new JsonSerializerOptions { PropertyNamingPolicy = JsonNamingPolicy.CamelCase });

        await context.Response.WriteAsync(payload);
    }
}
