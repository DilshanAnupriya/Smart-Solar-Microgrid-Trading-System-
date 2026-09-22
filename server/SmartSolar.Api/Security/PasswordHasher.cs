/*
 * File:        PasswordHasher.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Security
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: BCrypt implementation of IPasswordHasher. BCrypt is used because
 *              it is deliberately slow and salts every hash automatically, which
 *              makes stored passwords far harder to crack than a plain hash.
 */

namespace SmartSolar.Api.Security;

public class PasswordHasher : IPasswordHasher
{
    // Work factor controls how slow the hash is; 11 is a good balance for this project
    private const int WorkFactor = 11;

    /// <summary>
    /// Creates a salted BCrypt hash of the given plain text password.
    /// </summary>
    public string Hash(string password)
    {
        // BCrypt generates a random salt and embeds it inside the returned hash string
        return BCrypt.Net.BCrypt.HashPassword(password, WorkFactor);
    }

    /// <summary>
    /// Verifies a plain text password against a previously stored BCrypt hash.
    /// </summary>
    public bool Verify(string password, string passwordHash)
    {
        // Returns false instead of throwing when the stored hash is empty or malformed
        try
        {
            return BCrypt.Net.BCrypt.Verify(password, passwordHash);
        }
        catch
        {
            return false;
        }
    }
}
