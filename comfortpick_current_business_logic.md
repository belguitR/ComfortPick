# ComfortPick Current Business Logic

This file describes the business logic that is currently implemented in code.

## 1. Product intent

ComfortPick answers:

- for one stored summoner
- against one enemy champion
- which champion worked best for that summoner personally

The product is based on the summoner's own stored history, not global counter data.

## 2. Implemented scope

Implemented today:

- Task 1: persistence schema
- Task 2: scoring service
- Task 3: Riot API boundary
- Task 4: summoner search with DB reuse
- Task 5: match import with duplicate protection
- Task 6: enemy lane opponent detection
- Task 7: matchup stats recalculation
- Task 8: personal counters endpoint
- Task 9: profile dashboard endpoint
- Task 10: matchup detail endpoint
- Task 11: build and rune analysis
- Task 12: frontend summoner search flow
- Task 13: frontend profile dashboard
- Task 14: frontend enemy champion counters page
- Task 15: frontend matchup detail page
- Task 16: progressive history sync

Not implemented yet:

- production hardening

## 3. Database truth model

Current tables:

- `riot_accounts`
  - tracked summoner identity
  - also stores background history sync state
- `matches`
  - one row per imported Riot match
- `player_matchups`
  - one stored personal matchup row per imported match when extraction succeeds
- `personal_matchup_stats`
  - aggregated fast-read matchup stats
- `personal_build_stats`
  - reserved aggregated build summary table

Raw source of truth:

- `player_matchups`

Fast-read source of truth:

- `personal_matchup_stats`

## 4. Summoner search logic

Endpoint:

- `GET /api/summoners/{region}/{gameName}/{tagLine}`

Current rule:

1. Parse `{region}` into `RiotRoutingRegion`
2. Check local DB by:
   - `region`
   - `gameName`
   - `tagLine`
3. If account exists and `updatedAt` is fresh, return DB result
4. If missing or stale, call Riot Account API
5. Upsert by `puuid`

Freshness window:

- `24 hours`

Search result source values:

- `DATABASE`
- `RIOT_API`

## 5. Riot API usage rules

### 5.1 Search request

If DB hit is fresh:

- `0` Riot API calls

If DB miss or stale:

- `1` Riot API call
  - account by Riot ID

### 5.2 Match import batch

Endpoint:

- `POST /api/summoners/{summonerId}/matches/import`

Current inputs:

- `start`
- `count`

Current limits:

- default count: `10`
- maximum count: `20`

Current flow:

1. Load stored summoner by internal id
2. Fetch Riot match IDs with `{start, count}`
3. Compare those Riot IDs against local `matches`
4. Fetch details only for Riot IDs not already stored
5. Store each new `matches` row
6. Attempt to extract exactly one personal matchup row for the tracked summoner
7. If at least one new `player_matchups` row was written, recalculate `personal_matchup_stats`

Current response fields:

- `fetchedMatchCount`
- `importedMatchCount`
- `existingMatchCount`
- `importedMatchupCount`
- `skippedMatchupCount`

Current duplicate rule:

- a stored `riot_match_id` is never refetched for details

## 6. Background history sync

Current sync request endpoint:

- `POST /api/profiles/{summonerId}/sync`

Purpose:

- queue or refresh a resumable background history sync for one stored summoner

Current sync state lives on `riot_accounts`:

- `autoSyncEnabled`
- `syncStatus`
- `syncTargetMatchCount`
- `syncBackfillCursor`
- `syncNextRunAt`
- `syncLastSyncAt`
- `syncLastErrorCode`
- `syncLastErrorMessage`

Current sync constants:

- target depth: `500`
- batch size: `10`
- scheduler interval: `30 seconds`
- dashboard poll interval hint: `10 seconds`
- max accounts per scheduler tick: `1`

Current scheduler rule:

- one backend worker runs every configured interval
- it processes due summoners where:
  - sync is enabled
  - `syncNextRunAt <= now`
  - status is not `RUNNING`

Current per-cycle rule:

1. Mark the account as `RUNNING`
2. Fetch the newest `10` Riot match IDs at `start = 0`
3. Import only missing head matches
4. Advance `syncBackfillCursor` by the number of newly imported head matches
   - this compensates for new matches pushing older history deeper
5. If the target is not reached and the head page was full:
   - fetch one older-history page at `start = syncBackfillCursor`
   - import only missing older matches
   - advance `syncBackfillCursor` by the size of that returned older page
6. Set final status:
   - `COMPLETE` when target depth is reached or history ends
   - `ACTIVE` when another cycle is still needed

Current failure handling:

- Riot `429`:
  - status becomes `RATE_LIMITED`
  - `syncNextRunAt = now + retryAfterSeconds`
  - partial cursor progress from a completed head batch is preserved
- other failures:
  - status becomes `FAILED`
  - `syncNextRunAt = now + 5 minutes`
  - error code/message are stored

Current login/profile-open rule:

- search page queues sync after summoner lookup
- profile page queues sync on open
- profile page polls the dashboard while sync status is:
  - `ACTIVE`
  - `RUNNING`
  - `RATE_LIMITED`

## 7. Matchup extraction rule

Implemented in:

- `PlayerMatchupExtractor`

Current rule:

1. Find the participant whose `puuid` matches the stored summoner
2. Read that participant's `teamPosition`
3. Find the first enemy participant where:
   - `teamId` is different
   - `teamPosition` matches after trimming, case-insensitive
4. Save:
   - user champion
   - enemy champion
   - role
   - win/loss
   - kills/deaths/assists
   - CS
   - gold
   - damage to champions
   - item IDs
   - primary rune
   - secondary rune

Failure reasons:

- `USER_PARTICIPANT_NOT_FOUND`
- `MISSING_ROLE`
- `OPPONENT_NOT_FOUND`

Current import behavior for extraction failure:

- keep the `matches` row
- skip the `player_matchups` row

## 8. Matchup stats recalculation

Implemented in:

- `RecalculatePersonalMatchupStatsUseCase`

Current rule:

- recalculate for one summoner only
- use the full stored `player_matchups` history for that summoner
- group by:
  - `enemyChampionId`
  - `userChampionId`
  - `role`
- upsert one `personal_matchup_stats` row per group
- delete stale rows for that summoner

Current aggregated fields:

- `games`
- `wins`
- `losses`
- `winrate`
- `averageKda`
- `averageCs`
- `averageGold`
- `averageDamage`
- `personalScore`
- `confidence`

Recent performance basis:

- latest `5` stored matchup rows inside the same grouped matchup

## 9. Counters endpoint

Endpoint:

- `GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters`

Current rule:

- reads only from `personal_matchup_stats`
- does not call Riot API
- sorts by stored `personalScore` descending
- derives `status` from:
  - stored `personalScore`
  - stored `confidence`
  - stored `games`
- returns an empty list when the summoner exists but has no counters for that enemy champion

## 10. Profile dashboard endpoint

Endpoint:

- `GET /api/profiles/{summonerId}`

Current rule:

- reads only from:
  - `riot_accounts`
  - `player_matchups`
  - `personal_matchup_stats`
- does not call Riot API
- returns dashboard data even if analysis is still empty

Current returned fields:

- `summoner`
- `analyzedMatches`
- `mainRole`
- `mostPlayedChampions`
- `bestCounters`
- `worstMatchups`
- `lastUpdateAt`
- `sync`

Current `sync` fields:

- `enabled`
- `status`
- `targetMatchCount`
- `backfillCursor`
- `remainingMatchCount`
- `nextRunAt`
- `lastSyncAt`
- `lastErrorCode`
- `lastErrorMessage`
- `dashboardPollIntervalSeconds`

## 11. Matchup detail endpoint

Endpoint:

- `GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters/{userChampionId}`

Current rule:

- reads only from stored DB data
- does not call Riot API
- if multiple role rows exist for the same champion pair:
  - choose highest `personalScore`
  - then highest `games`
  - then alphabetical `role`
- recent games are the newest `5` stored rows for the selected role

No-data behavior:

- return `200`
- `hasData = false`
- `status = NO_DATA`
- `confidence = NO_DATA`
- reasoning says no personal data exists yet

## 12. Build and rune analysis

Exposed through matchup detail.

Current rule:

- use stored `player_matchups` for the selected:
  - `enemyChampionId`
  - `userChampionId`
  - `role`
- only winning games are considered
- if there are no wins, build/rune recommendations return empty values

Current MVP heuristics:

- `firstCompletedItemId`
  - first non-zero final inventory slot among `item0..item5`
- `itemSet`
  - joined non-zero final inventory items
- `primaryRuneId`
  - most common primary rune in wins
- `secondaryRuneId`
  - most common secondary rune in wins

## 13. Scoring formula

Implemented in:

- `RecommendationScoringService`

Current formula:

```text
score =
  (
    winrateScore * 0.35 +
    championComfortScore * 0.25 +
    kdaScore * 0.15 +
    csGoldScore * 0.10 +
    recentPerformanceScore * 0.10 +
    globalFallbackScore * 0.05
  )
  - lowSamplePenalty
```

Current constants:

- global fallback score: `50`
- recent matchup sample size: `5`

Current confidence mapping:

- `0 games` -> `NO_DATA`
- `1..2 games` -> `LOW`
- `3..6 games` -> `MEDIUM`
- `>= 7 games` -> `HIGH`

Current status mapping:

1. `NO_DATA` if `games <= 0`
2. `AVOID` if `score < 45`
3. `LOW_DATA` if confidence is `LOW` and score is at least `50`
4. `BEST_PICK` if `score >= 80` and confidence is at least `MEDIUM`
5. `GOOD_PICK` if `score >= 65`
6. `OK_PICK` if `score >= 50`
7. else `AVOID`

## 14. Current frontend flow

Current routes:

- `/`
  - validates `gameName` and `tagLine`
  - calls summoner search
  - queues background sync
  - navigates to `/profiles/{summonerId}`
- `/profiles/{summonerId}`
  - loads DB-backed dashboard
  - queues sync on open
  - polls dashboard while sync is active
  - exposes enemy champion name search
- `/profiles/{summonerId}/enemies/{enemyChampionId}`
  - loads stored personal counters
- `/profiles/{summonerId}/enemies/{enemyChampionId}/counters/{userChampionId}`
  - loads stored matchup detail

Current frontend API usage:

- search:
  - `GET /api/summoners/{region}/{gameName}/{tagLine}`
  - `POST /api/profiles/{summonerId}/sync`
- profile:
  - `GET /api/profiles/{summonerId}`
  - `POST /api/profiles/{summonerId}/sync`
- counters:
  - `GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters`
- detail:
  - `GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters/{userChampionId}`

## 15. Important current limitations

- scoring is still heuristic MVP logic
- sync is local-process scheduling, not a distributed queue
- each cycle processes only one head batch and one older batch of `10`
- reaching `500` scanned history slots can therefore take many cycles by design
- role/opponent detection is still same-role only
- rate-limit handling is resumable but still simple
- malformed extraction cases keep the `matches` row but produce no `player_matchups` row
- no patch filtering yet
- no recency weighting persisted in DB yet
- frontend is intentionally desktop-first right now

## 16. Code references

- [SearchSummonerUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/SearchSummonerUseCase.kt>)
- [ImportMatchHistoryUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/ImportMatchHistoryUseCase.kt>)
- [RequestSummonerHistorySyncUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/RequestSummonerHistorySyncUseCase.kt>)
- [RunHistorySyncCycleUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/RunHistorySyncCycleUseCase.kt>)
- [RecalculatePersonalMatchupStatsUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/RecalculatePersonalMatchupStatsUseCase.kt>)
- [GetProfileDashboardUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/GetProfileDashboardUseCase.kt>)
- [GetPersonalMatchupDetailUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/GetPersonalMatchupDetailUseCase.kt>)
- [BuildRuneAnalysisService.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/domain/service/BuildRuneAnalysisService.kt>)
- [PlayerMatchupExtractor.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/service/PlayerMatchupExtractor.kt>)
- [RecommendationScoringService.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/domain/service/RecommendationScoringService.kt>)
