/*
 * File:        InvalidCredentialsException.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Exceptions
 * Author:      N. Jayasinghe (IT)
 * Created:     2026-09-21
 * Description: Thrown when a login attempt uses a wrong username, NIC or
 *              password. Returns HTTP 401 Unauthorized.
 */

namespace SmartSolar.Api.Exceptions;

public class InvalidCredentialsException : AppException
{
    /// <summary>
    /// Creates the exception with a generic message that does not reveal which field was wrong.
    /// </summary>
    public InvalidCredentialsException()
        : base("Invalid username or password.", StatusCodes.Status401Unauthorized)
    {
        // Default message is used when AuthService throws without a message
    }

    /// <summary>
    /// Creates the exception with a custom message.
    /// </summary>
    public InvalidCredentialsException(string message)
        : base(message, StatusCodes.Status401Unauthorized)
    {
        // Custom message supplied by the caller
    }
}