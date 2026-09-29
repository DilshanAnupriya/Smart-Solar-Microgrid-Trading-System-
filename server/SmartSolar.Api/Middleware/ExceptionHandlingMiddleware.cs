/*
 * File:        ExceptionHandlingMiddleware.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Middleware
 * Author:      N. Jayasinghe (IT)
 * Created:     2026-09-19
 * Description: Global error handler. Converts application exceptions into
 *              ApiResponse JSON with the correct HTTP status code and hides
 *              unexpected server errors behind a generic 500 message.
 */

using SmartSolar.Api.Common;
using SmartSolar.Api.Exceptions;

namespace SmartSolar.Api.Middleware;

public class ExceptionHandlingMiddleware
{
    private readonly RequestDelegate _next;
    private readonly ILogger<ExceptionHandlingMiddleware> _logger;

    /// <summary>
    /// Receives the next middleware in the pipeline and a logger.
    /// </summary>
    public ExceptionHandlingMiddleware(RequestDelegate next, ILogger<ExceptionHandlingMiddleware> logger)
    {
        // Store dependencies for use when handling requests
        _next = next;
        _logger = logger;
    }

    /// <summary>
    /// Runs the rest of the pipeline and catches any exception it throws.
    /// </summary>
    public async Task InvokeAsync(HttpContext context)
    {
        // Expected errors return their own status; anything else returns 500
        try
        {
            await _next(context);
        }
        catch (AppException ex)
        {
            _logger.LogWarning("Handled application error ({StatusCode}): {Message}", ex.StatusCode, ex.Message);
            await WriteErrorResponseAsync(context, ex.StatusCode, ex.Message);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Unhandled exception while processing {Path}", context.Request.Path);
            await WriteErrorResponseAsync(context, StatusCodes.Status500InternalServerError,
                "An unexpected error occurred. Please try again later.");
        }
    }

    /// <summary>
    /// Writes an ApiResponse failure envelope as JSON with the given status code.
    /// </summary>
    private static async Task WriteErrorResponseAsync(HttpContext context, int statusCode, string message)
    {
        // Cannot change the response once it has started streaming to the client
        if (context.Response.HasStarted)
        {
            return;
        }

        context.Response.Clear();
        context.Response.StatusCode = statusCode;
        context.Response.ContentType = "application/json";

        var response = ApiResponse<object>.Fail(message);
        await context.Response.WriteAsJsonAsync(response);
    }
}