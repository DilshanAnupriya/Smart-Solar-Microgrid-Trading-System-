/*
 * File:        ProsumerUpdateRequest.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       DTOs
 * Author:      Dilshan Anupriya (IT22189530)
 * Created:     2026-09-20
 * Description: Request body for updating a prosumer profile. The NIC cannot
 *              be changed and the password is not updated here.
 */

using System.ComponentModel.DataAnnotations;

namespace SmartSolar.Api.DTOs.Prosumers;

public class ProsumerUpdateRequest
{
    [Required(ErrorMessage = "Full name is required.")]
    [StringLength(100, ErrorMessage = "Full name must be 100 characters or fewer.")]
    public string FullName { get; set; } = string.Empty;

    [Required(ErrorMessage = "Email is required.")]
    [EmailAddress(ErrorMessage = "Email address is not valid.")]
    public string Email { get; set; } = string.Empty;

    [Required(ErrorMessage = "Phone number is required.")]
    [RegularExpression(@"^0[0-9]{9}$", ErrorMessage = "Phone number must be 10 digits starting with 0.")]
    public string Phone { get; set; } = string.Empty;

    [Required(ErrorMessage = "Address is required.")]
    [StringLength(250, ErrorMessage = "Address must be 250 characters or fewer.")]
    public string Address { get; set; } = string.Empty;
}