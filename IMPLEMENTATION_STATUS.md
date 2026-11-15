# FoodBridge — Implementation Status

**Updated:** 2026-11-15

## Implemented and verified

| Area | Status | Verification |
|---|---|---|
| Backend | Spring Boot modular monolith; auth, users, donations, claims, pickups, NGO requests, notifications, impact, audit, admin | `mvn verify` |
| Database | Flyway V1/V2 migrations, normalized MySQL-compatible schema, foreign keys, indexes, claim uniqueness constraint | Verified in workspace on MariaDB 11.8.6 |
| Authentication/security | Donor/NGO registration, admin login, JWT access tokens, hashed/rotating refresh tokens, BCrypt, role and ownership checks | Backend integration/unit tests + smoke tests |
| Donation/claim lifecycle | Donation create/edit/cancel, filtering/paging, atomic claim guard and lifecycle transitions | Claim concurrency and service integration tests |
| Pickup and impact | Scheduling, donor/NGO confirmations, completion impact log | Pickup workflow integration tests |
| Notifications | Persistent notifications and secured SSE token validation | Notification integration tests and live endpoint probe |
| Admin/public API | Platform totals, user and NGO management, reports, categories, audit/activity, public stats | Backend integration tests + smoke tests |
| Frontend | React 18 + JavaScript/JSX SPA; public, donor, NGO, shared, and admin routes | Vite production build; Vitest/RTL **15/15**; ESLint clean |
| Backend tests | Unit and integration suite | `mvn -B verify`: **58 passed, 0 failed** |
| Bundled app | SPA in Spring static resources; deep-link fallback | Live preview on port 8080 verified (HTML, asset, API docs, health) |
| Documentation | Root `README.md` with setup, run, architecture, configuration, testing, and known limitations | Added/updated |

## Current known issues / incomplete operational work

- A local MySQL 8.0 startup has failed at V1 `CREATE TABLE claims` with error 1215 (foreign-key constraint). The exact cause is not yet established. The migration passes in this workspace on MariaDB 11.8.6. Do not blindly run Flyway repair or delete the failed history row; inspect the exact target schema and InnoDB diagnostic first. See `README.md` troubleshooting.
- Dockerfiles/Compose and GitHub Actions workflow files are not present.
- Additional requested documents (architecture, database design, API, testing, deployment, security, business rules, interview preparation) are not yet created; currently only the root README and this status file are present.
- Email delivery for password-reset links is not configured; local development exposes a reset token. No deployment is claimed.
- GitHub remote/repository has not been created or pushed from this workspace.

## Frontend language migration

The frontend has been converted from TypeScript/TSX to JavaScript/JSX. TypeScript source and compiler configs were removed, and TypeScript is no longer a direct frontend dependency. (MSW retains an optional TypeScript peer dependency in the lockfile.) Build, tests, and lint were run after conversion.
