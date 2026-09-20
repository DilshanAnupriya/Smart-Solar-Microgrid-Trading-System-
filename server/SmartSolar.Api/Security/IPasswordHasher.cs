/*
 * File:        IPasswordHasher.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Security
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Contract for hashing and verifying user passwords, so that the
 *              services never depend on a specific hashing library directly.
 */

namespace SmartSolar.Api.Security;

public interface IPasswordHasher
{
    /// <summary>
    /// Turns a plain text password into a one-way hash for storage.
    /// </summary>
    string Hash(string password);

    /// <summary>
    /// Checks a plain text password against a stored hash.
    /// </summary>
    bool Verify(string password, string passwordHash);
}
