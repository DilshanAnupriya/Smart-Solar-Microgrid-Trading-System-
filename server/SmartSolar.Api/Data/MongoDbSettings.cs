/*
 * File:        MongoDbSettings.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Data
 * Author:      BDA Cooray (IT22189530)
 * Created:     2026-09-19
 * Description: Strongly typed settings for the MongoDB connection, bound from
 *              the "MongoDbSettings" section of appsettings.json and user-secrets.
 */

namespace SmartSolar.Api.Data;

public class MongoDbSettings
{
    // Full MongoDB Atlas connection string (stored in user-secrets, never in Git)
    public string ConnectionString { get; set; } = string.Empty;

    // Name of the database used by the application (e.g. SmartSolarDb)
    public string DatabaseName { get; set; } = string.Empty;
}