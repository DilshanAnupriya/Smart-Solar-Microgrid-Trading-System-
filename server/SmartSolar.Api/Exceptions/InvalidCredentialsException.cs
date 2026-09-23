/*
 * File:        InvalidCredentialsException.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Exceptions
 * Author:      N. Jayasinghe (IT)
 * Created:     2026-09-19
 * Description: Thrown when login credentials do not match an active user.
 *              Returns HTTP 401 Unauthorized.
 */

namespace SmartSolar.Api.Exceptions;

public class InvalidCredentialsException : AppException
{
    /// <summary>
    /// Creates an invalid credentials exception that maps to HTTP 401.
    /// </summary>
    public InvalidCredentialsException(string message) : base(message, StatusCodes.Status401Unauthorized)
    {
        // No extra logic; status code is set by the base class
    }
}
