/*
 * File:        HealthController.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Controllers
 * Author:      BDA Cooray (IT22189530)
 * Created:     2026-09-19
 * Description: Provides a health check endpoint that confirms the API can
 *              connect to the MongoDB database.
 */

using Microsoft.AspNetCore.Mvc;
using SmartSolar.Api.Data;

namespace SmartSolar.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class HealthController : ControllerBase
{
    private readonly MongoDbContext _context;

    /// <summary>
    /// Receives the MongoDbContext through dependency injection.
    /// </summary>
    public HealthController(MongoDbContext context)
    {
        // Store the injected database context for use in the endpoint
        _context = context;
    }

    /// <summary>
    /// GET api/health/db — pings MongoDB and reports whether the connection works.
    /// </summary>
    [HttpGet("db")]
    public async Task<IActionResult> CheckDatabase()
    {
        // Return 200 if the ping succeeds, otherwise 503 with the error message
        try
        {
            await _context.PingAsync();

            return Ok(new
            {
                success = true,
                message = "Connected to MongoDB",
                database = _context.Database.DatabaseNamespace.DatabaseName
            });
        }
        catch (Exception ex)
        {
            return StatusCode(503, new
            {
                success = false,
                message = ex.Message
            });
        }
    }
}