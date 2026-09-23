/*
 * File:        AccountDeactivatedException.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Exceptions
 * Author:      N. Jayasinghe (IT)
 * Created:     2026-09-19
 * Description: Thrown when a deactivated user attempts to log in.
 *              Returns HTTP 403 Forbidden.
 */

namespace SmartSolar.Api.Exceptions;

public class AccountDeactivatedException : AppException
{
    /// <summary>
    /// Creates an account deactivated exception that maps to HTTP 403.
    /// </summary>
    public AccountDeactivatedException(string message) : base(message, StatusCodes.Status403Forbidden)
    {
        // No extra logic; status code is set by the base class
    }
}
