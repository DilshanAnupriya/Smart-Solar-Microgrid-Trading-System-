/*
 * File:        AuthService.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Services
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Modified:    2026-09-29 by N. Jayasinghe (IT2XXXXXXX) — added prosumer
 *              login (NIC or email) for the Android app.
 * Description: Authentication business logic. Verifies credentials, blocks
 *              deactivated accounts from signing in, issues the JWT that carries
 *              the user's role, and handles password changes. All of these rules
 *              live here rather than in the clients, as the FAT service pattern
 *              requires.
 */

using SmartSolar.Api.DTOs;
using SmartSolar.Api.DTOs.Prosumers;
using SmartSolar.Api.Exceptions;
using SmartSolar.Api.Models;
using SmartSolar.Api.Repositories;
using SmartSolar.Api.Security;
using SmartSolar.Api.Validators;

namespace SmartSolar.Api.Services;

public class AuthService : IAuthService
{
    private readonly IWebUserRepository _repository;
    private readonly IProsumerRepository _prosumerRepository;
    private readonly IPasswordHasher _passwordHasher;
    private readonly ITokenService _tokenService;

    /// <summary>
    /// Receives the web user and prosumer repositories, the password hasher and
    /// the token service.
    /// </summary>
    public AuthService(
        IWebUserRepository repository,
        IProsumerRepository prosumerRepository,
        IPasswordHasher passwordHasher,
        ITokenService tokenService)
    {
        // Store the collaborators this service needs
        _repository = repository;
        _prosumerRepository = prosumerRepository;
        _passwordHasher = passwordHasher;
        _tokenService = tokenService;
    }

    /// <summary>
    /// Verifies credentials and returns a signed token on success.
    /// </summary>
    public async Task<LoginResponseDto> LoginAsync(LoginRequestDto dto)
    {
        // The identifier may be an email address, a username or a phone number;
        // the repository decides which one it matched
        var user = await _repository.GetByIdentifierAsync(dto.Identifier);

        // Business rule: an unknown account and a wrong password must produce
        // the exact same message, so an attacker cannot discover which emails,
        // usernames or phone numbers exist
        if (user is null || !_passwordHasher.Verify(dto.Password, user.PasswordHash))
        {
            throw new InvalidCredentialsException("Invalid credentials. Please check your details and try again.");
        }

        // Business rule: a deactivated account may not sign in, even with the
        // correct password
        if (!user.IsActive)
        {
            throw new AccountDeactivatedException(
                "This account has been deactivated. Please contact a Backoffice officer.");
        }

        // Build the token that carries the user's id, name, email and role
        var token = _tokenService.CreateToken(user);

        return new LoginResponseDto
        {
            Token = token.Token,
            ExpiresAt = token.ExpiresAt,
            User = UserMapper.ToResponse(user)
        };
    }

    /// <summary>
    /// Verifies a prosumer's NIC (or email) and password and returns a signed
    /// token. Used by the Android app.
    /// </summary>
    public async Task<ProsumerLoginResponseDto> ProsumerLoginAsync(ProsumerLoginRequestDto dto)
    {
        // An email address always contains "@" and a NIC never does, so that
        // decides which lookup to use; both are normalised the same way they
        // were when the prosumer registered
        var identifier = dto.Identifier.Trim();
        var prosumer = identifier.Contains('@')
            ? await _prosumerRepository.GetByEmailAsync(identifier.ToLowerInvariant())
            : await _prosumerRepository.GetByNicAsync(NicValidator.Normalize(identifier));

        // Business rule: an unknown NIC or email and a wrong password produce the
        // same message, so nobody can discover which NICs are registered
        if (prosumer is null || !_passwordHasher.Verify(dto.Password, prosumer.PasswordHash))
        {
            throw new InvalidCredentialsException("Invalid NIC, email or password. Please check your details and try again.");
        }

        // Business rule: a deactivated prosumer may not sign in, even with the
        // correct password, until a Backoffice officer reactivates the account
        if (prosumer.Status == ProsumerStatus.Deactivated)
        {
            throw new AccountDeactivatedException(
                "This account has been deactivated. Please contact a Backoffice officer to reactivate it.");
        }

        // The token carries the NIC and the Prosumer role
        var token = _tokenService.CreateToken(prosumer);

        return new ProsumerLoginResponseDto
        {
            Token = token.Token,
            ExpiresAt = token.ExpiresAt,
            Role = UserRoles.Prosumer,
            User = ProsumerResponse.FromModel(prosumer)
        };
    }

    /// <summary>
    /// Returns the profile of the user identified by the supplied token id.
    /// </summary>
    public async Task<UserResponseDto> GetCurrentUserAsync(string userId)
    {
        // The id comes from the validated token, so a miss means the account
        // was removed after the token was issued
        var user = await _repository.GetByIdAsync(userId)
                   ?? throw new NotFoundException("User not found.");

        return UserMapper.ToResponse(user);
    }

    /// <summary>
    /// Changes the signed in user's own password.
    /// </summary>
    public async Task ChangePasswordAsync(string userId, ChangePasswordDto dto)
    {
        var user = await _repository.GetByIdAsync(userId)
                   ?? throw new NotFoundException("User not found.");

        // Business rule: the current password must be proved before it is replaced
        if (!_passwordHasher.Verify(dto.CurrentPassword, user.PasswordHash))
        {
            throw new BusinessRuleException("The current password is incorrect.");
        }

        // Business rule: the new password must actually be different
        if (_passwordHasher.Verify(dto.NewPassword, user.PasswordHash))
        {
            throw new BusinessRuleException("The new password must be different from the current password.");
        }

        user.PasswordHash = _passwordHasher.Hash(dto.NewPassword);
        user.UpdatedAt = DateTime.UtcNow;

        await _repository.UpdateAsync(user);
    }
}
