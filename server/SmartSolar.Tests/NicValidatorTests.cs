/*
 * File:        NicValidatorTests.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Tests
 * Author:      Dilshan Anupriya (IT22189530)
 * Created:     2026-09-20
 * Description: Unit tests for NicValidator covering valid old and new NIC
 *              formats, normalisation and invalid inputs.
 */

using SmartSolar.Api.Validators;

namespace SmartSolar.Tests;

public class NicValidatorTests
{
    /// <summary>
    /// Valid old and new format NICs should pass validation.
    /// </summary>
    [Theory]
    [InlineData("991234567V")]
    [InlineData("991234567X")]
    [InlineData("991234567v")]
    [InlineData("199912345678")]
    [InlineData("  991234567V  ")]
    public void IsValid_ReturnsTrue_ForValidNics(string nic)
    {
        // Each input is a correctly formatted NIC
        Assert.True(NicValidator.IsValid(nic));
    }

    /// <summary>
    /// Wrong lengths, wrong letters and empty values should fail validation.
    /// </summary>
    [Theory]
    [InlineData("")]
    [InlineData(null)]
    [InlineData("12345678V")]
    [InlineData("9912345678V")]
    [InlineData("991234567A")]
    [InlineData("19991234567")]
    [InlineData("1999123456789")]
    [InlineData("ABCDEFGHIJKL")]
    public void IsValid_ReturnsFalse_ForInvalidNics(string? nic)
    {
        // Each input breaks at least one format rule
        Assert.False(NicValidator.IsValid(nic));
    }

    /// <summary>
    /// Normalize should trim spaces and convert to uppercase.
    /// </summary>
    [Fact]
    public void Normalize_TrimsAndUppercases()
    {
        // Lowercase v with spaces should become a clean uppercase NIC
        Assert.Equal("991234567V", NicValidator.Normalize("  991234567v "));
    }
}