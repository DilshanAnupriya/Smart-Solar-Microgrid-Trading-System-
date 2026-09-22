/*
 * File:        ValidationPatterns.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Validators
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-21
 * Description: Regular expressions and messages used to validate the account
 *              fields. They are declared as constants in one place so that the
 *              same rule is applied by every DTO, and so the React client's
 *              checks can be kept in step with the API.
 */

namespace SmartSolar.Api.Validators;

public static class ValidationPatterns
{
    // Letters, digits, dot, underscore and hyphen; between 3 and 30 characters
    public const string Username = @"^[a-zA-Z0-9._-]{3,30}$";

    public const string UsernameMessage =
        "Username must be 3 to 30 characters and may contain letters, numbers, dot, underscore and hyphen only.";

    // Sri Lankan NIC: either the old format (9 digits then V or X) or the new
    // 12 digit format
    public const string Nic = @"^(\d{9}[VvXx]|\d{12})$";

    public const string NicMessage =
        "NIC must be 9 digits followed by V or X, or 12 digits.";

    // Sri Lankan telephone number: 0 followed by 9 digits, or +94 and 9 digits
    public const string Phone = @"^(0\d{9}|\+94\d{9})$";

    public const string PhoneMessage =
        "Phone number must be 10 digits starting with 0, or +94 followed by 9 digits.";
}
