/*
 * File:        Program.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Startup
 * Author:      BDA Cooray (IT22189530)
 * Created:     2026-09-19
 * Description: Application entry point. Registers configuration, the MongoDB
 *              context and controllers, and builds the HTTP request pipeline.
 */

using SmartSolar.Api.Data;

var builder = WebApplication.CreateBuilder(args);

// Bind the "MongoDbSettings" section (appsettings.json + user-secrets) to MongoDbSettings
builder.Services.Configure<MongoDbSettings>(
    builder.Configuration.GetSection("MongoDbSettings"));

// Register one MongoDbContext for the whole app (MongoClient manages its own connection pool)
builder.Services.AddSingleton<MongoDbContext>();

builder.Services.AddControllers();
builder.Services.AddOpenApi();

var app = builder.Build();

// Expose the OpenAPI document only in Development
if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}

app.UseHttpsRedirection();
app.UseAuthorization();
app.MapControllers();

app.Run();