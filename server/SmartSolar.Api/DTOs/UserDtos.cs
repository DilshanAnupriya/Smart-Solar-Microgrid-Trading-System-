/*
 * File:        UserDtos.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       DTOs
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Modified:    2026-09-21 — added username, NIC, phone and date of birth.
 * Description: Data transfer objects for web user management. UserResponseDto
 *              deliberately has no password field, so a password hash can never
 *              leave the API.
 */

using System.ComponentModel.DataAnnotations;
using SmartSolar.Api.Validators;

namespace SmartSolar.Api.DTOs;

/// <summary>
/// Body posted to api/users to create a Backoffice or Grid Operator account.
/// </summary>
public class CreateUserDto
{
    [Required(ErrorMessage = "Full name is required.")]
    [StringLength(100, MinimumLength = 3, ErrorMessage = "Full name must be between 3 and 100 characters.")]
    public string FullName { get; set; } = string.Empty;

    [Required(ErrorMessage = "Email is required.")]
    [EmailAddress(ErrorMessage = "Email must be a valid email address.")]
    public string Email { get; set; } = string.Empty;

    [Required(ErrorMessage = "Username is required.")]
    [RegularExpression(ValidationPatterns.Username, ErrorMessage = ValidationPatterns.UsernameMessage)]
    public string Username { get; set; } = string.Empty;

    [Required(ErrorMessage = "NIC number is required.")]
    [RegularExpression(ValidationPatterns.Nic, ErrorMessage = ValidationPatterns.NicMessage)]
    public string Nic { get; set; } = string.Empty;

    [Required(ErrorMessage = "Phone number is required.")]
    [RegularExpression(ValidationPatterns.Phone, ErrorMessage = ValidationPatterns.PhoneMessage)]
    public string Phone { get; set; } = string.Empty;

    [Required(ErrorMessage = "Date of birth is required.")]
    public DateTime? DateOfBirth { get; set; }

    [Required(ErrorMessage = "Role is required.")]
    [AllowedRole]
    public string Role { get; set; } = string.Empty;

    [Required(ErrorMessage = "Password is required.")]
    [MinLength(6, ErrorMessage = "Password must be at least 6 characters.")]
    public string Password { get; set; } = string.Empty;
}

/// <summary>
/// Body sent to api/users/{id} to update an existing account. The password is
/// not changed here; the account owner changes it from the profile page.
/// </summary>
public class UpdateUserDto
{
    [Required(ErrorMessage = "Full name is required.")]
    [StringLength(100, MinimumLength = 3, ErrorMessage = "Full name must be between 3 and 100 characters.")]
    public string FullName { get; set; } = string.Empty;

    [Required(ErrorMessage = "Email is required.")]
    [EmailAddress(ErrorMessage = "Email must be a valid email address.")]
    public string Email { get; set; } = string.Empty;

    [Required(ErrorMessage = "Username is required.")]
    [RegularExpression(ValidationPatterns.Username, ErrorMessage = ValidationPatterns.UsernameMessage)]
    public string Username { get; set; } = string.Empty;

    [Required(ErrorMessage = "NIC number is required.")]
    [RegularExpression(ValidationPatterns.Nic, ErrorMessage = ValidationPatterns.NicMessage)]
    public string Nic { get; set; } = string.Empty;

    [Required(ErrorMessage = "Phone number is required.")]
    [RegularExpression(ValidationPatterns.Phone, ErrorMessage = ValidationPatterns.PhoneMessage)]
    public string Phone { get; set; } = string.Empty;

    [Required(ErrorMessage = "Date of birth is required.")]
    public DateTime? DateOfBirth { get; set; }

    [Required(ErrorMessage = "Role is required.")]
    [AllowedRole]
    public string Role { get; set; } = string.Empty;
}

/// <summary>
/// The safe, public view of a web user returned by every endpoint.
/// </summary>
public class UserResponseDto
{
    public string Id { get; set; } = string.Empty;

    public string FullName { get; set; } = string.Empty;

    public string Email { get; set; } = string.Empty;

    public string Username { get; set; } = string.Empty;

    public string Nic { get; set; } = string.Empty;

    public string Phone { get; set; } = string.Empty;

    public DateTime? DateOfBirth { get; set; }

    public string Role { get; set; } = string.Empty;

    public bool IsActive { get; set; }

    public DateTime CreatedAt { get; set; }

    public DateTime UpdatedAt { get; set; }
}
