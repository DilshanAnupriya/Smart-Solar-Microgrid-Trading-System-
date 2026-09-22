/*
 * File:        AccountDeactivatedException.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Exceptions
 * Author:      N. Jayasinghe (IT)
 * Created:     2026-09-21
 * Description: Thrown when a deactivated web user or prosumer tries to log in.
 *              Returns HTTP 403 Forbidden.
 */

namespace SmartSolar.Api.Exceptions;

public class AccountDeactivatedException : AppException
{
    /// <summary>
    /// Creates the exception with the default deactivated-account message.
    /// </summary>
    public AccountDeactivatedException()
        : base("This account has been deactivated. Please contact a Backoffice officer.", StatusCodes.Status403Forbidden)
    {
        // Default message is used when AuthService throws without a message
    }

    /// <summary>
    /// Creates the exception with a custom message.
    /// </summary>
    public AccountDeactivatedException(string message)
        : base(message, StatusCodes.Status403Forbidden)
    {
        // Custom message supplied by the caller
    }
}