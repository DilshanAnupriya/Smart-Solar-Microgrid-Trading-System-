/*
 * File:        SeedAdminSettings.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Data
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Details of the first Backoffice account created automatically on
 *              an empty database. Only a Backoffice user can create other users,
 *              so without this seed there would be no way to sign in at all.
 */

namespace SmartSolar.Api.Data;

public class SeedAdminSettings
{
    // Display name of the first administrator
    public string FullName { get; set; } = string.Empty;

    // Login email of the first administrator
    public string Email { get; set; } = string.Empty;

    // Plain password, hashed before it is stored (keep the real one in user-secrets)
    public string Password { get; set; } = string.Empty;
}
