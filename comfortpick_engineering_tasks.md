# ComfortPick Engineering Task Plan

This document breaks ComfortPick into small implementation tasks. Each task should be coded, tested, reviewed, and kept aligned with hexagonal architecture.

## Engineering Standards

- Backend uses hexagonal architecture:
  - domain contains pure business rules and models
  - application contains use cases and ports
  - infrastructure contains Riot API, persistence, cache, and external adapters
  - api contains controllers and DTO mapping
- Frontend uses professional React + TypeScript structure:
  - typed API clients
  - reusable components
  - clear loading, error, empty, and success states
  - no fake polish that hides missing functionality
- Database is the source of truth for imported match data and precomputed matchup stats.
- Riot API calls must be minimized:
  - never refetch a match already stored by `riot_match_id`
  - reuse existing account and match data where possible
  - refresh only when the user requests it or stored data is stale
  - recompute matchup stats only after new matches are imported
- Every backend task must include tests:
  - unit tests for domain scoring and business rules
  - integration tests for persistence and API endpoints when applicable
  - adapter tests or mocked integration tests for Riot API behavior
- Every frontend task must include:
  - typed data contracts
  - basic component coverage where useful
  - manual browser verification for user-facing flows

## Task 0 - Project Foundation

Status: DONE

Goal: Create the repository foundation for backend, frontend, database, and local development.

Backend scope:
- Create Kotlin Spring Boot backend project.
- Add package structure:
  - `domain`
  - `application`
  - `infrastructure`
  - `api`
- Add health check endpoint.
- Add test setup.

Frontend scope:
- Create React + TypeScript app.
- Add routing foundation.
- Add base layout.
- Add API client foundation.

Infrastructure scope:
- Add Docker Compose for PostgreSQL and Redis.
- Add environment variable examples.

Acceptance criteria:
- Backend starts locally.
- Frontend starts locally.
- PostgreSQL and Redis start through Docker Compose.
- Health endpoint returns success.
- Backend test command passes.
- Frontend typecheck passes.

## Task 1 - Database Schema and Persistence Foundation

Status: DONE

Goal: Create the persistence model needed to store account, match, player matchup, and aggregated matchup data.

Backend scope:
- Add migrations for:
  - `riot_accounts`
  - `matches`
  - `player_matchups`
  - `personal_matchup_stats`
  - `personal_build_stats`
- Add persistence entities and repositories.
- Add database indexes for high-frequency queries.

Required indexes:
- `riot_accounts(puuid)`
- `riot_accounts(region, game_name, tag_line)`
- `matches(riot_match_id)`
- `player_matchups(match_id)`
- `player_matchups(user_puuid)`
- `player_matchups(user_champion_id, role)`
- `personal_matchup_stats(riot_account_id, enemy_champion_id)`
- `personal_matchup_stats(riot_account_id, enemy_champion_id, user_champion_id)`

Acceptance criteria:
- Migrations run from an empty database.
- Duplicate Riot match IDs cannot be inserted.
- Duplicate PUUID accounts cannot be inserted.
- Repository integration tests pass against PostgreSQL.

## Task 2 - Domain Models and Scoring Rules

Status: DONE

Goal: Implement pure domain logic before connecting external systems.

Domain scope:
- Add models:
  - `RiotAccount`
  - `Match`
  - `PlayerMatchup`
  - `PersonalMatchupStats`
  - `PersonalCounter`
  - `ConfidenceLevel`
  - `RecommendationStatus`
- Add scoring service:
  - winrate score
  - champion comfort score
  - KDA score
  - CS/gold score
  - recent performance score
  - low sample penalty
- Add confidence calculation:
  - 0 games: `NO_DATA`
  - 1-2 games: `LOW`
  - 3-6 games: `MEDIUM`
  - 7+ games: `HIGH`

Acceptance criteria:
- One-game 100% winrate does not outrank a stronger medium/high-confidence result.
- Low sample penalties are covered by tests.
- Recommendation status is deterministic and tested.
- Domain tests do not require Spring, PostgreSQL, Redis, or Riot API.

## Task 3 - Riot API Client Port and Adapter

Status: DONE

Goal: Create a clean Riot API boundary without leaking Riot DTOs into the domain.

Application scope:
- Add Riot API port interfaces:
  - get account by Riot ID
  - get match IDs by PUUID
  - get match details by match ID

Infrastructure scope:
- Implement Riot API adapter.
- Add Riot DTOs.
- Map Riot DTOs into application/domain-friendly models.
- Handle:
  - not found
  - rate limit
  - unauthorized API key
  - temporary Riot outage

Acceptance criteria:
- Riot API key is read from environment configuration.
- Riot DTOs do not enter domain services.
- Adapter tests cover success and common error cases.
- No controller calls Riot API directly.

## Task 4 - Summoner Search with DB Reuse

Status: DONE

Goal: Search a summoner while avoiding unnecessary Riot API usage.

Application scope:
- Add `SearchSummonerUseCase`.
- First check DB by region, game name, and tag line.
- If found and not stale, return DB account.
- If missing or stale, call Riot API and upsert account.

Staleness rule for MVP:
- Account identity can be considered fresh for 24 hours.

API scope:
- Add endpoint:
  - `GET /api/summoners/{region}/{gameName}/{tagLine}`

Acceptance criteria:
- Existing fresh summoner returns from DB without Riot API call.
- Missing summoner calls Riot API once and stores the result.
- Summoner not found returns a clear API error.
- API integration tests cover DB hit, Riot API hit, and not found.

## Task 5 - Match Import with Duplicate Protection

Status: DONE

Goal: Import recent matches once and store only new data.

Application scope:
- Add `ImportMatchHistoryUseCase`.
- Fetch recent match IDs from Riot API.
- Check DB for already stored match IDs.
- Fetch details only for missing matches.
- Store matches and player matchup rows transactionally.

API scope:
- Add endpoint:
  - `POST /api/summoners/{summonerId}/matches/import`

Acceptance criteria:
- Already stored matches are not fetched again.
- Duplicate match imports are safe.
- Imported match count and existing match count are returned.
- Matchup row extraction is tested.
- Transaction rollback works if match detail storage fails.

## Task 6 - Enemy Lane Opponent Detection

Status: DONE

Goal: Identify the likely enemy matchup from stored match rows.

Domain/application scope:
- For MVP, detect the enemy participant with:
  - opposite team
  - same `teamPosition`
- Ignore games where user participant has missing or invalid role.
- Return a clear reason when opponent cannot be detected.

Acceptance criteria:
- Mid vs mid, top vs top, jungle vs jungle, bottom vs bottom, and support vs support cases are tested.
- Missing `teamPosition` is handled safely.
- Remake or malformed matches do not crash calculation.

## Task 7 - Matchup Stats Recalculation

Status: DONE

Goal: Persist matchup stats and update them only when needed.

Application scope:
- Add `RecalculatePersonalMatchupStatsUseCase`.
- Trigger recalculation after new matches are imported.
- Recalculate stats for the affected summoner only.
- Upsert `personal_matchup_stats`.

Stats scope:
- games
- wins
- losses
- winrate
- average KDA
- average CS
- average gold
- average damage
- personal score
- confidence

Acceptance criteria:
- Importing zero new matches does not recalculate stats.
- Importing new matches recalculates only the affected summoner.
- Recalculation is deterministic.
- Persistence integration tests verify upsert behavior.

## Task 8 - Personal Counters Endpoint

Status: DONE

Goal: Serve the core product feature from precomputed DB stats.

Application scope:
- Add `GetEnemyChampionCountersUseCase`.
- Query `personal_matchup_stats` by summoner and enemy champion.
- Sort by personal score descending.
- Mark low-data and avoid picks clearly.

API scope:
- Add endpoint:
  - `GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters`

Acceptance criteria:
- Endpoint does not call Riot API.
- Results are sorted by score.
- Low confidence is visible in response.
- Avoid picks are visible in response.
- API integration tests cover normal, empty, low-data, and avoid cases.

## Task 9 - Profile Dashboard Endpoint

Status: DONE

Goal: Provide a useful profile overview from stored data.

Application scope:
- Add `GetProfileDashboardUseCase`.
- Return:
  - summoner
  - analyzed matches
  - main role
  - most played champions
  - best personal counters
  - worst matchups
  - last update date

API scope:
- Add endpoint:
  - `GET /api/profiles/{summonerId}`

Acceptance criteria:
- Endpoint does not call Riot API.
- Empty profile state is handled.
- Dashboard data is based on stored matches and matchup stats.
- API integration tests cover populated and empty profile states.

## Task 10 - Matchup Detail Endpoint

Status: DONE

Goal: Show detailed reasoning for one user champion into one enemy champion.

Application scope:
- Add `GetPersonalMatchupDetailUseCase`.
- Return:
  - personal score
  - confidence
  - detailed stats
  - recent games
  - reasoning text

API scope:
- Add endpoint:
  - `GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters/{userChampionId}`

Acceptance criteria:
- Endpoint does not call Riot API.
- Missing matchup returns a clear no-data response.
- Recent games are ordered newest first.
- Reasoning text matches the actual stats and confidence.
- API integration tests cover found and missing details.

## Task 11 - Build and Rune Analysis

Status: DONE

Goal: Add first-version build and rune recommendations from personal wins.

Application scope:
- Add build analysis:
  - most common first completed item in wins
  - most common item set in wins
  - basic build score
- Add rune analysis:
  - most common primary rune in wins
  - most common secondary rune in wins
  - basic rune score

Acceptance criteria:
- Build/rune recommendations are based on personal games first.
- No personal data produces a clear empty recommendation.
- Scores do not overstate one-game samples.
- Domain tests cover common, tied, and empty cases.

## Task 12 - Frontend Summoner Search Flow

Status: DONE

Goal: Let the user search a Riot ID and start analysis.

Frontend scope:
- Build search page.
- Add region selector.
- Add game name and tag line fields.
- Call summoner search endpoint.
- Trigger match import.
- Navigate to profile page.

Acceptance criteria:
- Loading state is visible.
- Riot not-found error is clear.
- API rate-limit error is clear.
- Form validation prevents empty submit.
- Browser verification passes on desktop and mobile widths.

Implementation note:
- Desktop browser verification is complete.
- Mobile verification is intentionally deferred by user instruction so frontend work can stay desktop-first for now.
- Initial frontend-triggered import now uses a reduced recent-match window to keep Riot development-key usage practical.

## Task 13 - Frontend Profile Dashboard

Status: DONE

Goal: Show the player’s stored analysis summary.

Frontend scope:
- Build profile page.
- Show analyzed matches, main role, most played champions, best counters, worst matchups.
- Add enemy champion search entry point.
- Add refresh button.

Acceptance criteria:
- Empty profile state is useful.
- Refresh does not duplicate matches.
- Loading/error states are implemented.
- Layout is responsive and professional.

Implementation note:
- Verified on desktop with both empty and populated states.
- Refresh uses the existing backend import endpoint, so duplicate protection remains backend-driven.

## Task 14 - Frontend Enemy Champion Counters Page

Status: DONE

Goal: Show the core personal counter ranking UI.

Frontend scope:
- Build enemy champion page.
- Show ranked table/cards:
  - champion
  - score
  - games
  - winrate
  - KDA
  - confidence
  - status
- Link each row to matchup detail.

Acceptance criteria:
- Low-data picks are visually distinct.
- Avoid picks are visually distinct.
- Sorting matches API order.
- Empty no-personal-data state is clear.
- Browser verification passes on desktop and mobile widths.

Implementation note:
- Desktop browser verification is complete for both empty and populated states.
- Mobile verification is intentionally deferred by user instruction.

## Task 15 - Frontend Matchup Detail Page

Status: DONE

Goal: Explain why a champion is recommended.

Frontend scope:
- Build matchup detail page.
- Show:
  - personal score
  - confidence
  - stats
  - recent games
  - reason
  - build and rune sections when available

Acceptance criteria:
- Reasoning is readable and tied to displayed stats.
- Missing build/rune data has a clean empty state.
- Recent games are scannable.
- Browser verification passes on desktop and mobile widths.

Implementation note:
- Desktop browser verification is complete with populated stored data.
- Mobile verification is intentionally deferred by user instruction.

## Task 16 - Progressive History Sync

Status: DONE

Goal: Replace one-shot frontend-driven imports with a resumable background sync model that can walk toward deeper history without burning Riot dev limits immediately.

Backend scope:
- Add persistent sync state on `riot_accounts`.
- Add a profile sync request endpoint.
- Add a scheduled backend sync worker.
- Process one due summoner per tick.
- On each cycle:
  - check the newest `10` matches at history start
  - import only missing head matches
  - shift the older-history cursor when new head matches appear
  - backfill one older `10`-match batch toward the target depth
- Pause and retry cleanly on Riot rate limits.
- Expose sync progress on the profile dashboard response.

Frontend scope:
- Stop forcing one-shot import from the search page.
- Queue background sync after search and on profile open.
- Poll the profile dashboard while sync is active.
- Show sync status and progress in the profile UI.

Acceptance criteria:
- Searching a summoner no longer requires immediate Riot-heavy import to open the profile.
- Background sync progresses in `10`-match batches toward a `500`-match target.
- Existing match IDs still prevent duplicate detail fetches.
- Profile dashboard exposes sync state and progress.
- Backend tests cover sync request and one scheduled sync cycle.
- Frontend build, lint, and typecheck pass with the new flow.

## Task 17 - Production Hardening

Goal: Make the app reliable enough to keep building on.

Backend scope:
- Add structured logging.
- Add validation.
- Add request error model.
- Add rate-limit-aware retry policy for Riot API.
- Add database indexes review.

Frontend scope:
- Add global error boundary.
- Add API error mapping.
- Add basic accessibility pass.
- Add performance pass for large tables.

Acceptance criteria:
- No unhandled backend exceptions leak raw stack traces.
- API error responses are consistent.
- Frontend handles backend errors gracefully.
- Core backend integration tests pass.
- Frontend typecheck and test commands pass.

## Recommended Execution Order

1. Task 0 - Project Foundation
2. Task 1 - Database Schema and Persistence Foundation
3. Task 2 - Domain Models and Scoring Rules
4. Task 3 - Riot API Client Port and Adapter
5. Task 4 - Summoner Search with DB Reuse
6. Task 5 - Match Import with Duplicate Protection
7. Task 6 - Enemy Lane Opponent Detection
8. Task 7 - Matchup Stats Recalculation
9. Task 8 - Personal Counters Endpoint
10. Task 12 - Frontend Summoner Search Flow
11. Task 14 - Frontend Enemy Champion Counters Page
12. Task 9 - Profile Dashboard Endpoint
13. Task 13 - Frontend Profile Dashboard
14. Task 10 - Matchup Detail Endpoint
15. Task 11 - Build and Rune Analysis
16. Task 15 - Frontend Matchup Detail Page
17. Task 16 - Progressive History Sync
18. Task 17 - Production Hardening

## Working Agreement

For each future task:
- implement only the requested task plus necessary supporting code
- keep architecture boundaries clean
- add or update tests
- run the relevant verification commands
- report what changed, what passed, and any remaining risk
- do not move to unrelated features without explicit request
