/*
 * File:        AllowedRoleAttribute.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Validators
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Validation attribute that rejects any role other than the two
 *              roles supported by the system, so an invalid role is refused by
 *              model validation before it ever reaches the service layer.
 */

using System.ComponentModel.DataAnnotations;
using SmartSolar.Api.Models;

namespace SmartSolar.Api.Validators;

[AttributeUsage(AttributeTargets.Property, AllowMultiple = false)]
public class AllowedRoleAttribute : ValidationAttribute
{
    /// <summary>
    /// Returns success only when the value is "Backoffice" or "GridOperator".
    /// </summary>
    protected override ValidationResult? IsValid(object? value, ValidationContext validationContext)
    {
        // A missing value is handled by [Required]; this attribute only checks the content
        var role = value as string;

        if (string.IsNullOrWhiteSpace(role))
        {
            return ValidationResult.Success;
        }

        if (UserRoles.IsValid(role))
        {
            return ValidationResult.Success;
        }

        return new ValidationResult(
            $"Role must be one of: {string.Join(", ", UserRoles.All)}.",
            [validationContext.MemberName ?? nameof(WebUser.Role)]);
    }
}
