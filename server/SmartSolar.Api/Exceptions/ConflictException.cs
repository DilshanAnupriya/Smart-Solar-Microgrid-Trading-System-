/*
 * File:        ConflictException.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Exceptions
 * Author:      N. Jayasinghe (IT)
 * Created:     2026-09-19
 * Description: Thrown when a request conflicts with existing data, e.g. a
 *              duplicate NIC or deactivating a node with active reservations.
 *              Returns HTTP 409 Conflict.
 */

namespace SmartSolar.Api.Exceptions;

public class ConflictException : AppException
{
    /// <summary>
    /// Creates a conflict exception that maps to HTTP 409.
    /// </summary>
    public ConflictException(string message) : base(message, StatusCodes.Status409Conflict)
    {
        // No extra logic; status code is set by the base class
    }
}