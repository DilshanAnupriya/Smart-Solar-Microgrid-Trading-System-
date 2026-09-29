/*
 * File:        BusinessRuleException.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Exceptions
 * Author:      N. Jayasinghe (IT)
 * Created:     2026-09-19
 * Description: Thrown when a business rule is violated, e.g. a reservation
 *              outside the 7-day window. Returns HTTP 400 Bad Request.
 */

namespace SmartSolar.Api.Exceptions;

public class BusinessRuleException : AppException
{
    /// <summary>
    /// Creates a business rule exception that maps to HTTP 400.
    /// </summary>
    public BusinessRuleException(string message) : base(message, StatusCodes.Status400BadRequest)
    {
        // No extra logic; status code is set by the base class
    }
}