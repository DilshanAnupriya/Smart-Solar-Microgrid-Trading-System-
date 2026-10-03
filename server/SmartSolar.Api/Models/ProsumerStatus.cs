/*
 * File:        ProsumerStatus.cs
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Models
 * Author:      Dilshan Anupriya (IT22189530)
 * Created:     2026-09-20
 * Description: Account status values for a prosumer profile.
 *              PendingDeactivation: the prosumer asked to deactivate their
 *              account from the mobile app. The account is blocked until a
 *              Backoffice officer approves (Deactivated) or rejects (Active).
 */

namespace SmartSolar.Api.Models;

public enum ProsumerStatus
{
    Active,
    PendingDeactivation,
    Deactivated
}