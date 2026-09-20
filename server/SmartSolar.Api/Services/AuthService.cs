/*
 * File:        AuthService.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Services
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Authentication business logic. Verifies credentials, blocks
 *              deactivated accounts from signing in, issues the JWT that carries
 *              the user's role, and handles password changes. All of these rules
 *              live here rather than in the clients, as the FAT service pattern
 *              requires.
 */

using SmartSolar.Api.DTOs;
using SmartSolar.Api.Exceptions;
using SmartSolar.Api.Repositories;
using SmartSolar.Api.Security;

namespace SmartSolar.Api.Services;

public class AuthService : IAuthService
{
    private readonly IWebUserRepository _repository;
    private readonly IPasswordHasher _passwordHasher;
    private readonly ITokenService _tokenService;

    /// <summary>
    /// Receives the repository, the password hasher and the token service.
    /// </summary>
    public AuthService(
        IWebUserRepository repository,
        IPasswordHasher passwordHasher,
        ITokenService tokenService)
    {
        // Store the collaborators this service needs
        _repository = repository;
        _passwordHasher = passwordHasher;
        _tokenService = tokenService;
    }

    /// <summary>
    /// Verifies credentials and returns a signed token on success.
    /// </summary>
    public async Task<LoginResponseDto> LoginAsync(LoginRequestDto dto)
    {
        // Look the account up by email; a missing account is handled below
        var user = await _repository.GetByEmailAsync(dto.Email);

        // Business rule: an unknown email and a wrong password must produce the
        // exact same message, so an attacker cannot discover which emails exist
        if (user is null || !_passwordHasher.Verify(dto.Password, user.PasswordHash))
        {
            throw new InvalidCredentialsException("Invalid email or password.");
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
