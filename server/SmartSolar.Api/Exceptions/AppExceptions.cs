/*
 * File:        AppExceptions.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Exceptions
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Application exceptions that each carry the HTTP status code they
 *              should produce. Services throw these, and the exception handling
 *              middleware turns them into the matching HTTP response, so the
 *              controllers stay free of error handling code.
 */

namespace SmartSolar.Api.Exceptions;

/// <summary>
/// Base class for every expected (non bug) failure in the application.
/// </summary>
public abstract class AppException : Exception
{
    // HTTP status code the middleware should return for this failure
    public int StatusCode { get; }

    protected AppException(string message, int statusCode) : base(message)
    {
        // Store the status code chosen by the derived exception type
        StatusCode = statusCode;
    }
}

/// <summary>
/// The requested record does not exist (HTTP 404).
/// </summary>
public class NotFoundException : AppException
{
    public NotFoundException(string message) : base(message, StatusCodes.Status404NotFound)
    {
    }
}

/// <summary>
/// The request clashes with data that already exists, such as a duplicate
/// email address (HTTP 409).
/// </summary>
public class ConflictException : AppException
{
    public ConflictException(string message) : base(message, StatusCodes.Status409Conflict)
    {
    }
}

/// <summary>
/// The request broke a business rule, such as deactivating your own account
/// (HTTP 400).
/// </summary>
public class BusinessRuleException : AppException
{
    public BusinessRuleException(string message) : base(message, StatusCodes.Status400BadRequest)
    {
    }
}

/// <summary>
/// The credentials supplied were not valid (HTTP 401).
/// </summary>
public class InvalidCredentialsException : AppException
{
    public InvalidCredentialsException(string message) : base(message, StatusCodes.Status401Unauthorized)
    {
    }
}

/// <summary>
/// The account exists but is not allowed to sign in or act (HTTP 403).
/// </summary>
public class AccountDeactivatedException : AppException
{
    public AccountDeactivatedException(string message) : base(message, StatusCodes.Status403Forbidden)
    {
    }
}
