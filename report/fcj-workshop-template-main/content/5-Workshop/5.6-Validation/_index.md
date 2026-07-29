---
title: "Post-deployment validation"
date: 2024-01-01
weight: 6
chapter: false
pre: " <b> 5.6. </b> "
---

## Objective

Collect build, health, network, and production workflow evidence after deployment.

## Technical checks

| Item | Expected | Recorded |
| --- | --- | --- |
| Backend tests | 68 pass | Yes |
| Backend package | Successful | Yes |
| Frontend build/lint | Successful | Yes |
| Frontend automated tests | No script exists | Not applicable |
| ALB target | Healthy | Yes |
| Actuator | `/actuator/health` returns `UP` | Project configuration/check evidence |
| Direct EC2:8080 | Blocked | Yes |
| Protected API without JWT | `401` | Proves only that request reached Spring Security |

> **Image required:** Backend tests, frontend build/lint, ALB Healthy, and health UP.

<!-- IMAGE_PATH: /images/5-Workshop/5.6-Validation/backend-tests.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.6-Validation/frontend-build-lint.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.6-Validation/alb-health.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.6-Validation/actuator-health.png -->

## Production smoke tests

Test registration, verification/resend, login, profile, recipient lookup, simulated deposit, transfer, payment, history, administration, and forgot/reset password. Review SES Sending Statistics for sends, deliveries, bounces, and complaints. Hide email, phone, token, SMTP credentials, and personal data.

> **Image required:** Customer and administrator workflows, email received through Amazon SES, and SES Sending Statistics.

<!-- IMAGE_PATH: /images/5-Workshop/5.6-Validation/user-workflows.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.6-Validation/admin-workflows.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.6-Validation/email-workflows.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.6-Validation/ses-sending-statistics.png -->

## Monitoring

Actuator provides health/liveness/readiness. CloudWatch offers default service metrics; custom log groups, dashboards, alarms, or agents are not repository-confirmed. Add only live evidence.

> **Image required:** Actually configured CloudWatch metrics or logs.

<!-- IMAGE_PATH: /images/5-Workshop/5.6-Validation/cloudwatch-evidence.png -->

## Result

Every conclusion must map to evidence. A `401` does not replace end-to-end workflow testing.
