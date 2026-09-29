/*
 * File:        NotFoundException.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Exceptions
 * Author:      N. Jayasinghe (IT)
 * Created:     2026-09-19
 * Description: Thrown when a requested record (prosumer, node, reservation,
 *              user) does not exist. Returns HTTP 404 Not Found.
 */

namespace SmartSolar.Api.Exceptions;

public class NotFoundException : AppException
{
    /// <summary>
    /// Creates a not-found exception that maps to HTTP 404.
    /// </summary>
    public NotFoundException(string message) : base(message, StatusCodes.Status404NotFound)
    {
        // No extra logic; status code is set by the base class
    }
}