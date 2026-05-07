# ComfortPick Agent Handoff

This file is for any AI agent resuming work on this repository.

Read these files first:

1. [comfortpick_engineering_tasks.md](</C:/Users/errmi/Documents/New project/comfortpick_engineering_tasks.md>)
2. [comfortpick_current_business_logic.md](</C:/Users/errmi/Documents/New project/comfortpick_current_business_logic.md>)
3. [backend/pom.xml](</C:/Users/errmi/Documents/New project/backend/pom.xml>)
4. [backend/src/main/resources/application.yml](</C:/Users/errmi/Documents/New project/backend/src/main/resources/application.yml>)

## Current status

- Tasks 0 through 16 are implemented locally.
- Latest local work adds progressive history sync and dashboard sync state.
- Check `git status` and `git log` before claiming remote state for the latest task.
- Current next planned task: Task 17, production hardening.
- Frontend is temporarily desktop-first by user instruction. Mobile verification is intentionally deferred for now.

## Product rule

ComfortPick answers:

- for one stored summoner
- against one enemy champion
- which champion worked best for that summoner personally

It is based on the summoner's own stored match history, not global counter data.

## Architecture rules

Backend is hexagonal:

- `domain`: pure models and scoring logic
- `application`: use cases and ports
- `infrastructure`: Riot API, persistence, external adapters
- `api`: controllers and API DTOs

Do not leak Riot DTOs into `domain`.
Do not call Riot API directly from controllers.

## Current persistence model

- `riot_accounts`: tracked summoner identity
- `matches`: imported Riot match metadata
- `player_matchups`: one stored personal matchup row per imported match when extraction succeeds
- `personal_matchup_stats`: future aggregated fast-read table
- `personal_build_stats`: future aggregated build summary table

Raw source of truth today:

- `player_matchups`

## Current match import behavior

Endpoint:

- `POST /api/summoners/{summonerId}/matches/import`

Current implemented rules:

- fetch match IDs in bounded batches
- do not insert a duplicate `riot_match_id` into `matches`
- do refetch Riot match details when the global match already exists but the current summoner still has no `player_matchups` row for it
- store every new match in `matches`
- try to extract one personal matchup row for the tracked summoner
- if extraction fails because the role/opponent cannot be determined, keep the `matches` row and skip the `player_matchups` row
- unexpected runtime or persistence failures should still roll back the transaction

Current batch limits:

- default count: `10`
- maximum count: `20`

Current response fields:

- `importedMatchCount`: new `matches` rows stored
- `existingMatchCount`: Riot match IDs already known locally
- `importedMatchupCount`: new `player_matchups` rows stored
- `skippedMatchupCount`: new matches where extraction failed
- `fetchedMatchCount`: Riot match IDs returned for that request page
- if `importedMatchupCount > 0`, trigger recalculation of `personal_matchup_stats` for that summoner

## Current background sync behavior

Endpoints:

- `POST /api/profiles/{summonerId}/sync`
- `GET /api/profiles/{summonerId}`

Current implemented rules:

- sync state is persisted on `riot_accounts`
- search queues sync after summoner lookup
- profile page queues sync on open
- if scanned depth is at least `50` and stored matchup coverage is below `50%`, the next sync request resets the backfill cursor to `0` for a repair pass
- one scheduler tick processes at most one account
- each cycle:
  - checks the newest `10` matches at `start = 0`
  - imports only missing head matches
  - shifts the older-history cursor by the number of new head matches
  - backfills one older `10`-match page toward the `500`-match target
- scheduler interval is currently `20 seconds`
- dashboard exposes:
  - sync status
  - backfill cursor
  - remaining match count
  - next run time
  - last sync time
  - last error

Current sync statuses:

- `IDLE`
- `ACTIVE`
- `RUNNING`
- `RATE_LIMITED`
- `FAILED`
- `COMPLETE`

## Current matchup stats recalculation

Implemented in:

- [RecalculatePersonalMatchupStatsUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/RecalculatePersonalMatchupStatsUseCase.kt>)

Current rule:

- recalculate for one summoner only
- use the full stored `player_matchups` history for that summoner
- group by `enemyChampionId + userChampionId + role`
- upsert one `personal_matchup_stats` row per group
- delete stale `personal_matchup_stats` rows for that summoner
- recent performance currently uses the latest `5` matchup rows inside the same grouped matchup

## Current counters endpoint

Implemented in:

- [ProfileController.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/api/profile/ProfileController.kt>)
- [GetEnemyChampionCountersUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/GetEnemyChampionCountersUseCase.kt>)

Endpoint:

- `GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters`

Current rule:

- reads only from `personal_matchup_stats`
- does not call Riot API
- returns counters sorted by stored `personalScore` descending
- derives `status` from stored score, confidence, and games
- returns an empty list when the profile exists but has no stored counters for that enemy champion

## Current profile dashboard endpoint

Implemented in:

- [ProfileController.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/api/profile/ProfileController.kt>)
- [GetProfileDashboardUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/GetProfileDashboardUseCase.kt>)

Endpoint:

- `GET /api/profiles/{summonerId}`

Current rule:

- reads only from:
  - `riot_accounts`
  - `player_matchups`
  - `personal_matchup_stats`
- does not call Riot API
- returns the stored summoner identity plus:
  - analyzed match count
  - main role
  - most played champions
  - best counters
  - worst matchups
  - last update timestamp
- returns a valid empty dashboard when the profile exists but has no stored analysis yet

## Current matchup detail endpoint

Implemented in:

- [ProfileController.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/api/profile/ProfileController.kt>)
- [GetPersonalMatchupDetailUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/GetPersonalMatchupDetailUseCase.kt>)

Endpoint:

- `GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters/{userChampionId}`

Current rule:

- reads only from stored DB data
- does not call Riot API
- returns one detailed matchup view for the requested champion pair
- if the pair exists in multiple roles, choose the row with:
  - highest `personalScore`
  - then highest `games`
  - then alphabetical `role`
- recent games are limited to the latest `5` stored `player_matchups` rows for the selected role
- missing stored matchup data returns a `200` no-data response, not a `404`

## Current build and rune analysis

Implemented in:

- [GetPersonalMatchupDetailUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/GetPersonalMatchupDetailUseCase.kt>)
- [BuildRuneAnalysisService.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/domain/service/BuildRuneAnalysisService.kt>)

Current rule:

- build/rune recommendations are returned inside the matchup detail endpoint
- analysis uses only stored `player_matchups` for the selected champion pair and role
- only winning games are considered
- empty recommendations are returned when there are no winning games
- `firstCompletedItemId` is currently approximated from the first non-zero final inventory slot, not purchase timeline data

## Current opponent detection

Implemented in:

- [PlayerMatchupExtractor.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/service/PlayerMatchupExtractor.kt>)

Rule:

- find the participant by `puuid`
- resolve role from `teamPosition`, then `individualPosition`
- normalize accepted roles to `TOP`, `JUNGLE`, `MIDDLE`, `BOTTOM`, `UTILITY`
- find the first enemy participant on the opposite team with the same normalized role

Failure reasons:

- `USER_PARTICIPANT_NOT_FOUND`
- `MISSING_ROLE`
- `OPPONENT_NOT_FOUND`

## Current scoring

Implemented in:

- [RecommendationScoringService.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/domain/service/RecommendationScoringService.kt>)

This is heuristic MVP scoring, not a final validated model.
Read `comfortpick_current_business_logic.md` for the exact formula and thresholds before changing it.

## Local environment

- backend uses Maven, Kotlin, Spring Boot
- frontend uses Vite, React, TypeScript
- Docker is used for local PostgreSQL and Redis
- Riot API key is expected in local `backend/.env`
- `.env` files are local and should not be committed

## Verification commands

Backend:

```powershell
mvn test
mvn clean install
```

Frontend:

```powershell
npm run typecheck
npm run lint
npm run build
```

Infra:

```powershell
docker compose up -d
docker compose ps
```

## Working rules for the next agent

- check `git status` before starting
- do not assume local changes are already pushed
- keep task scope tight
- update tests with every backend behavior change
- update `comfortpick_current_business_logic.md` whenever business behavior changes
- update `comfortpick_engineering_tasks.md` when a task is completed
