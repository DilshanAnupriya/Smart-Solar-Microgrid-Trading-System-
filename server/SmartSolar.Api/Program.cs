/*
 * File:        Program.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Startup
 * Author:      BDA Cooray (IT22189530)
 * Created:     2026-09-19
 * Modified:    2026-09-20 by N. Jayasinghe (IT2XXXXXXX) — added JWT
 *              authentication, role based authorization, CORS for the React
 *              client, dependency injection for the user management services,
 *              global exception handling and first-run database seeding.
 * Description: Application entry point. Registers configuration, the MongoDB
 *              context, security services and controllers, and builds the HTTP
 *              request pipeline.
 */

using System.Text;
using System.Text.Json;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Mvc;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi;
using SmartSolar.Api.Data;
using SmartSolar.Api.DTOs;
using SmartSolar.Api.Middleware;
using SmartSolar.Api.Repositories;
using SmartSolar.Api.Security;
using SmartSolar.Api.Services;

var builder = WebApplication.CreateBuilder(args);

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------

// Bind the "MongoDbSettings" section (appsettings.json + user-secrets)
builder.Services.Configure<MongoDbSettings>(
    builder.Configuration.GetSection("MongoDbSettings"));

// Bind the "JwtSettings" section used to sign and validate tokens
builder.Services.Configure<JwtSettings>(
    builder.Configuration.GetSection("JwtSettings"));

// Bind the "SeedAdmin" section used to create the very first Backoffice account
builder.Services.Configure<SeedAdminSettings>(
    builder.Configuration.GetSection("SeedAdmin"));

// ---------------------------------------------------------------------------
// Dependency injection
// ---------------------------------------------------------------------------

// Register one MongoDbContext for the whole app (MongoClient manages its own connection pool)
builder.Services.AddSingleton<MongoDbContext>();

// Security services
builder.Services.AddSingleton<IPasswordHasher, PasswordHasher>();
builder.Services.AddSingleton<ITokenService, TokenService>();

// Data access and business logic for web user management
builder.Services.AddScoped<IWebUserRepository, WebUserRepository>();
builder.Services.AddScoped<IAuthService, AuthService>();
builder.Services.AddScoped<IUserService, UserService>();

// Data access and business logic for energy slot reservation management
builder.Services.AddScoped<IReservationRepository, ReservationRepository>();
builder.Services.AddScoped<IReservationService, ReservationService>();

// Runs once at start-up to create indexes and seed the first Backoffice user
builder.Services.AddScoped<DatabaseSeeder>();

// ---------------------------------------------------------------------------
// Authentication and authorization
// ---------------------------------------------------------------------------

// Read the signing key directly so the token parameters can be built here
var jwtSettings = builder.Configuration.GetSection("JwtSettings").Get<JwtSettings>()
                  ?? throw new InvalidOperationException("JwtSettings section is missing from configuration.");

if (string.IsNullOrWhiteSpace(jwtSettings.SecretKey))
{
    throw new InvalidOperationException(
        "JwtSettings:SecretKey is not configured. Set it using dotnet user-secrets.");
}

builder.Services
    .AddAuthentication(options =>
    {
        // Every [Authorize] attribute is answered by the JWT bearer handler
        options.DefaultAuthenticateScheme = JwtBearerDefaults.AuthenticationScheme;
        options.DefaultChallengeScheme = JwtBearerDefaults.AuthenticationScheme;
    })
    .AddJwtBearer(options =>
    {
        // A token is accepted only if the issuer, audience, signature and
        // expiry time are all valid
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = jwtSettings.Issuer,

            ValidateAudience = true,
            ValidAudience = jwtSettings.Audience,

            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtSettings.SecretKey)),

            ValidateLifetime = true,

            // No extra grace period, so an expired token is rejected immediately
            ClockSkew = TimeSpan.Zero,

            // Tell ASP.NET Core which claims hold the user name and the role
            NameClaimType = System.Security.Claims.ClaimTypes.Name,
            RoleClaimType = System.Security.Claims.ClaimTypes.Role
        };
    });

builder.Services.AddAuthorization();

// ---------------------------------------------------------------------------
// CORS for the React client
// ---------------------------------------------------------------------------

const string WebClientCorsPolicy = "WebClientCorsPolicy";

// Origins are read from configuration so the deployed URL can be added later
var allowedOrigins = builder.Configuration
                         .GetSection("Cors:AllowedOrigins")
                         .Get<string[]>() ?? [];

builder.Services.AddCors(options =>
{
    options.AddPolicy(WebClientCorsPolicy, policy =>
    {
        policy.WithOrigins(allowedOrigins)
              .AllowAnyHeader()
              .AllowAnyMethod();
    });
});

// ---------------------------------------------------------------------------
// Controllers, model validation and Swagger
// ---------------------------------------------------------------------------

builder.Services.AddControllers();

// Return model validation failures in the same shape as every other error
builder.Services.Configure<ApiBehaviorOptions>(options =>
{
    options.InvalidModelStateResponseFactory = context =>
    {
        // Collect the message(s) attached to each invalid field
        var errors = context.ModelState
            .Where(entry => entry.Value is not null && entry.Value.Errors.Count > 0)
            .ToDictionary(
                entry => JsonNamingPolicy.CamelCase.ConvertName(entry.Key),
                entry => entry.Value!.Errors.Select(e => e.ErrorMessage).ToArray());

        var response = new ApiErrorResponse
        {
            Success = false,
            Message = "One or more fields are invalid.",
            Errors = errors
        };

        return new BadRequestObjectResult(response);
    };
});

builder.Services.AddEndpointsApiExplorer();

builder.Services.AddSwaggerGen(options =>
{
    options.SwaggerDoc("v1", new OpenApiInfo
    {
        Title = "Smart Solar Microgrid Trading System API",
        Version = "v1",
        Description = "Central web service for the web and Android clients."
    });

    // Adds the Authorize button so protected endpoints can be tested in Swagger
    options.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Description = "Paste the token returned by api/auth/login."
    });

    // Applies the Bearer scheme to every operation in the generated document
    options.AddSecurityRequirement(document => new OpenApiSecurityRequirement
    {
        {
            // Points at the "Bearer" definition registered just above
            new OpenApiSecuritySchemeReference("Bearer", document),
            new List<string>()
        }
    });
});

var app = builder.Build();

// ---------------------------------------------------------------------------
// First run: create indexes and seed the first Backoffice account
// ---------------------------------------------------------------------------

using (var scope = app.Services.CreateScope())
{
    // Seeding failures must not stop the API from starting, so they are logged
    try
    {
        var seeder = scope.ServiceProvider.GetRequiredService<DatabaseSeeder>();
        await seeder.RunAsync();
    }
    catch (Exception ex)
    {
        var logger = scope.ServiceProvider.GetRequiredService<ILogger<Program>>();
        logger.LogError(ex, "Database seeding failed. Check the MongoDB connection settings.");
    }
}

// ---------------------------------------------------------------------------
// HTTP request pipeline
// ---------------------------------------------------------------------------

// Registered first so it can catch exceptions thrown anywhere after it
app.UseMiddleware<ExceptionHandlingMiddleware>();

// Expose the API documentation only in Development
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI(options =>
    {
        options.SwaggerEndpoint("/swagger/v1/swagger.json", "Smart Solar API v1");
    });
}

// Outside Development only: forcing HTTPS during development would redirect the
// React dev server's API calls and break the browser's CORS preflight
if (!app.Environment.IsDevelopment())
{
    app.UseHttpsRedirection();
}

// CORS must run before authentication so the browser's preflight succeeds
app.UseCors(WebClientCorsPolicy);

// Authentication identifies the caller; authorization then checks their role
app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.Run();
