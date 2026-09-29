/*
 * File:        ForbiddenException.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Exceptions
 * Author:      N. Jayasinghe (IT)
 * Created:     2026-09-19
 * Description: Thrown when a logged-in user is not allowed to perform an
 *              action, e.g. a Grid Operator reactivating a prosumer.
 *              Returns HTTP 403 Forbidden.
 */

namespace SmartSolar.Api.Exceptions;

public class ForbiddenException : AppException
{
    /// <summary>
    /// Creates a forbidden exception that maps to HTTP 403.
    /// </summary>
    public ForbiddenException(string message) : base(message, StatusCodes.Status403Forbidden)
    {
        // No extra logic; status code is set by the base class
    }
}