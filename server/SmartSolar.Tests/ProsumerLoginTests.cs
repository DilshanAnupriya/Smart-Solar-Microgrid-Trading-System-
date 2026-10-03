/*
 * File:        ProsumerLoginTests.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Tests
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Description: Unit tests for the prosumer sign-in used by the Android app:
 *              NIC or email lookup, one shared message for bad credentials,
 *              blocking deactivated accounts, and the claims in the token.
 */

using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using Microsoft.Extensions.Options;
using SmartSolar.Api.DTOs;
using SmartSolar.Api.Exceptions;
using SmartSolar.Api.Models;
using SmartSolar.Api.Repositories;
using SmartSolar.Api.Security;
using SmartSolar.Api.Services;

namespace SmartSolar.Tests;

public class ProsumerLoginTests
{
    private const string Nic = "991234567V";
    private const string Password = "Secret@123";

    [Fact]
    public async Task ProsumerLoginAsync_WithNic_ReturnsTokenCarryingNicAndProsumerRole()
    {
        // A registered prosumer signs in with their NIC
        var service = CreateService(ActiveProsumer());

        var result = await service.ProsumerLoginAsync(Request(Nic, Password));

        Assert.Equal("Prosumer", result.Role);
        Assert.Equal(Nic, result.User.Nic);
        Assert.True(result.ExpiresAt > DateTime.UtcNow);

        // ProsumersController reads the NIC and the role back out of these claims
        var token = new JwtSecurityTokenHandler().ReadJwtToken(result.Token);
        Assert.Equal(Nic, ClaimValue(token, "nameid", ClaimTypes.NameIdentifier));
        Assert.Equal("Prosumer", ClaimValue(token, "role", ClaimTypes.Role));
    }

    [Fact]
    public async Task ProsumerLoginAsync_WithEmailInAnyCase_FindsTheProsumer()
    {
        // Emails are stored in lowercase, so the typed case must not matter
        var service = CreateService(ActiveProsumer());

        var result = await service.ProsumerLoginAsync(Request("Nimal.Perera@Example.LK", Password));

        Assert.Equal(Nic, result.User.Nic);
    }

    [Fact]
    public async Task ProsumerLoginAsync_NormalizesTheNic()
    {
        // Spaces and a lowercase v are accepted, as they are at registration
        var service = CreateService(ActiveProsumer());

        var result = await service.ProsumerLoginAsync(Request("  991234567v ", Password));

        Assert.Equal(Nic, result.User.Nic);
    }

    [Fact]
    public async Task ProsumerLoginAsync_WrongPassword_ThrowsInvalidCredentials()
    {
        // A correct NIC with the wrong password must be refused
        var service = CreateService(ActiveProsumer());

        await Assert.ThrowsAsync<InvalidCredentialsException>(() =>
            service.ProsumerLoginAsync(Request(Nic, "wrong-password")));
    }

    [Fact]
    public async Task ProsumerLoginAsync_UnknownAccount_GivesSameMessageAsWrongPassword()
    {
        // Identical messages stop an attacker from discovering which NICs are registered
        var service = CreateService(ActiveProsumer());

        var unknown = await Assert.ThrowsAsync<InvalidCredentialsException>(() =>
            service.ProsumerLoginAsync(Request("200012345678", Password)));
        var wrongPassword = await Assert.ThrowsAsync<InvalidCredentialsException>(() =>
            service.ProsumerLoginAsync(Request(Nic, "wrong-password")));

        Assert.Equal(wrongPassword.Message, unknown.Message);
    }

    [Fact]
    public async Task ProsumerLoginAsync_DeactivatedAccount_ThrowsAccountDeactivated()
    {
        // Even the correct password cannot open a deactivated account
        var prosumer = ActiveProsumer();
        prosumer.Status = ProsumerStatus.Deactivated;
        var service = CreateService(prosumer);

        await Assert.ThrowsAsync<AccountDeactivatedException>(() =>
            service.ProsumerLoginAsync(Request(Nic, Password)));
    }

    /// <summary>
    /// Builds an AuthService around the given prosumers, with a real TokenService.
    /// </summary>
    private static AuthService CreateService(params Prosumer[] prosumers)
    {
        // Test signing settings; the key only needs to be at least 32 characters
        var settings = Options.Create(new JwtSettings
        {
            Issuer = "SmartSolar.Api",
            Audience = "SmartSolar.Clients",
            ExpiryMinutes = 120,
            SecretKey = "unit-test-signing-key-that-is-long-enough-2026"
        });

        var prosumerRepository = new FakeProsumerRepository();
        prosumerRepository.Prosumers.AddRange(prosumers);

        return new AuthService(
            new UnusedWebUserRepository(),
            prosumerRepository,
            new FakePasswordHasher(),
            new TokenService(settings));
    }

    /// <summary>
    /// An active prosumer whose password is <see cref="Password"/>.
    /// </summary>
    private static Prosumer ActiveProsumer()
    {
        // Stored the way ProsumerService saves it: normalised NIC, lowercase email
        return new Prosumer
        {
            Nic = Nic,
            FullName = "Nimal Perera",
            Email = "nimal.perera@example.lk",
            Phone = "0771234567",
            Address = "12 Galle Road, Colombo 03",
            PasswordHash = new FakePasswordHasher().Hash(Password),
            Status = ProsumerStatus.Active
        };
    }

    /// <summary>
    /// Builds the request body the Android app posts.
    /// </summary>
    private static ProsumerLoginRequestDto Request(string identifier, string password)
    {
        // Same two fields as the login form on the phone
        return new ProsumerLoginRequestDto { Identifier = identifier, Password = password };
    }

    /// <summary>
    /// Returns the value of the first claim with any of the given types.
    /// </summary>
    private static string? ClaimValue(JwtSecurityToken token, params string[] claimTypes)
    {
        // The token handler may write a claim under its short JWT name or its long .NET name
        return token.Claims.FirstOrDefault(claim => claimTypes.Contains(claim.Type))?.Value;
    }

    /// <summary>
    /// Readable stand-in for BCrypt so the tests stay fast.
    /// </summary>
    private sealed class FakePasswordHasher : IPasswordHasher
    {
        public string Hash(string password)
        {
            // Prefix the password so a hash never equals the plain text
            return $"hashed:{password}";
        }

        public bool Verify(string password, string passwordHash)
        {
            // Matches only a hash produced by Hash() for the same password
            return passwordHash == Hash(password);
        }
    }

    /// <summary>
    /// In-memory prosumer store with the same lookups as ProsumerRepository.
    /// </summary>
    private sealed class FakeProsumerRepository : IProsumerRepository
    {
        public List<Prosumer> Prosumers { get; } = [];

        public Task<Prosumer?> GetByNicAsync(string nic)
        {
            // NIC is the primary key, so an exact match
            return Task.FromResult(Prosumers.FirstOrDefault(p => p.Nic == nic));
        }

        public Task<Prosumer?> GetByEmailAsync(string email)
        {
            // Emails are stored in lowercase, so an exact match on the lowercase value
            return Task.FromResult(Prosumers.FirstOrDefault(p => p.Email == email));
        }

        public Task<List<Prosumer>> GetAllAsync(string? search, ProsumerStatus? status)
        {
            // Filtering is not needed by the login tests
            return Task.FromResult(Prosumers.ToList());
        }

        public Task<bool> NicExistsAsync(string nic)
        {
            // True when a prosumer already has this NIC
            return Task.FromResult(Prosumers.Any(p => p.Nic == nic));
        }

        public Task<bool> EmailExistsAsync(string email, string? excludeNic = null)
        {
            // True when another prosumer already uses this email
            return Task.FromResult(Prosumers.Any(p => p.Email == email && p.Nic != excludeNic));
        }

        public Task CreateAsync(Prosumer prosumer)
        {
            // Keeps the new prosumer in the list
            Prosumers.Add(prosumer);
            return Task.CompletedTask;
        }

        public Task UpdateAsync(Prosumer prosumer)
        {
            // The list holds the same object, so there is nothing to copy
            return Task.CompletedTask;
        }
    }

    /// <summary>
    /// Prosumer sign-in never touches web users, so every call here is a test failure.
    /// </summary>
    private sealed class UnusedWebUserRepository : IWebUserRepository
    {
        public Task<List<WebUser>> GetAllAsync(string? role, bool? isActive, string? search)
        {
            // Not used by prosumer sign-in
            throw new NotSupportedException();
        }

        public Task<WebUser?> GetByIdAsync(string id)
        {
            // Not used by prosumer sign-in
            throw new NotSupportedException();
        }

        public Task<WebUser?> GetByEmailAsync(string email)
        {
            // Not used by prosumer sign-in
            throw new NotSupportedException();
        }

        public Task<WebUser?> GetByIdentifierAsync(string identifier)
        {
            // Not used by prosumer sign-in
            throw new NotSupportedException();
        }

        public Task<bool> EmailExistsAsync(string email, string? excludeId = null)
        {
            // Not used by prosumer sign-in
            throw new NotSupportedException();
        }

        public Task<bool> UsernameExistsAsync(string username, string? excludeId = null)
        {
            // Not used by prosumer sign-in
            throw new NotSupportedException();
        }

        public Task<bool> NicExistsAsync(string nic, string? excludeId = null)
        {
            // Not used by prosumer sign-in
            throw new NotSupportedException();
        }

        public Task<WebUser> CreateAsync(WebUser user)
        {
            // Not used by prosumer sign-in
            throw new NotSupportedException();
        }

        public Task UpdateAsync(WebUser user)
        {
            // Not used by prosumer sign-in
            throw new NotSupportedException();
        }

        public Task SetActiveAsync(string id, bool isActive)
        {
            // Not used by prosumer sign-in
            throw new NotSupportedException();
        }

        public Task DeleteAsync(string id)
        {
            // Not used by prosumer sign-in
            throw new NotSupportedException();
        }

        public Task<long> CountActiveByRoleAsync(string role)
        {
            // Not used by prosumer sign-in
            throw new NotSupportedException();
        }

        public Task<long> CountAsync()
        {
            // Not used by prosumer sign-in
            throw new NotSupportedException();
        }

        public Task EnsureIndexesAsync()
        {
            // Not used by prosumer sign-in
            throw new NotSupportedException();
        }
    }
}
