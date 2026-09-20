/*
 * File:        AppException.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Exceptions
 * Author:      N. Jayasinghe (IT)
 * Created:     2026-09-19
 * Description: Base class for all expected application errors. Each subclass
 *              carries the HTTP status code the middleware should return.
 */

namespace SmartSolar.Api.Exceptions;

public abstract class AppException : Exception
{
    // HTTP status code returned to the client for this error
    public int StatusCode { get; }

    /// <summary>
    /// Creates an application exception with a message and HTTP status code.
    /// </summary>
    protected AppException(string message, int statusCode) : base(message)
    {
        // Store the status code for the exception handling middleware
        StatusCode = statusCode;
    }
}