# ComfortPick Current Business Logic

This document describes the business logic that is currently implemented in code.

It is intentionally implementation-driven. If code changes later, this file should be updated with the new rules.

## 1. Product intent

ComfortPick is meant to answer:

- Given a stored summoner profile
- and later an enemy champion
- which champion worked best for that player personally

The core idea is personal historical performance, not global counter data.

## 2. Current implemented backend scope

Implemented today:

- Task 1: persistence schema
- Task 2: scoring service
- Task 3: Riot API boundary
- Task 4: summoner search with DB reuse
- Task 5: match import with duplicate protection
- Task 6: enemy lane opponent detection
- Task 7: matchup stats recalculation

Not implemented yet:

- counter endpoint from precomputed matchup stats
- profile dashboard
- matchup detail endpoint
- build/rune recommendation endpoint logic

That means some business logic below is already active in the running app, and some is implemented as domain logic but not yet wired into an endpoint.

## 3. Database truth model

Current storage model:

- `riot_accounts`
  - one row per tracked summoner
- `matches`
  - one row per imported Riot match
- `player_matchups`
  - one row per imported personal matchup for one tracked summoner in one match
- `personal_matchup_stats`
  - intended aggregated table for future fast reads
- `personal_build_stats`
  - intended aggregated build summary table for future fast reads

Current source of truth for raw historical performance:

- `player_matchups`

Current intended source of truth for future counter recommendations:

- `personal_matchup_stats`

## 4. Summoner search logic

Endpoint:

- `GET /api/summoners/{region}/{gameName}/{tagLine}`

Current search rule:

1. Parse `{region}` into `RiotRoutingRegion`
2. Check local DB by:
   - `region`
   - `gameName`
   - `tagLine`
3. If account exists and `updatedAt` is still fresh, return DB result
4. If missing or stale, call Riot Account API
5. Upsert by `puuid`

Freshness window:

- `24 hours`

Current result source values:

- `DATABASE`
- `RIOT_API`

## 5. Riot API usage rules currently implemented

### 5.1 Search summoner request

If DB hit is fresh:

- `0` Riot API calls

If DB miss or stale:

- `1` Riot API call
  - `GET /riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}`

### 5.2 Match import request

Endpoint:

- `POST /api/summoners/{summonerId}/matches/import`

Current request flow:

1. Load stored summoner by internal `summonerId`
2. Call Riot match ID list for the summoner `puuid`
3. Compare returned Riot match IDs to local `matches`
4. Only fetch details for match IDs not already stored
5. Store new matches and extracted personal matchup rows in one transaction
6. If at least one new `player_matchups` row was written, recalculate `personal_matchup_stats` for that summoner

Current response fields:

- `importedMatchCount`
  - number of new `matches` rows stored
- `existingMatchCount`
  - number of Riot match IDs already present in local `matches`
- `importedMatchupCount`
  - number of new `player_matchups` rows written
- `skippedMatchupCount`
  - number of newly stored matches where personal matchup extraction failed

Current recalculation trigger rule:

- if `importedMatchupCount > 0`, recalculate personal matchup stats for that summoner
- if `importedMatchupCount = 0`, do not recalculate

Current Riot API call count for one import request:

- always `1` call to fetch recent match IDs
  - `GET /lol/match/v5/matches/by-puuid/{puuid}/ids`
- plus `N` calls to fetch match details
  - where `N = number of new match IDs not already in DB`

So:

- total Riot API calls per import request = `1 + newMatchCount`

Current recent match fetch window:

- `100` recent match IDs

This is controlled by:

- `RECENT_MATCH_COUNT = 100`

Current duplicate protection rule:

- if a Riot match ID already exists in local `matches`, details are not fetched again

## 6. Match import storage logic

For each new Riot match:

1. Save one `matches` row
2. Attempt to extract exactly one personal matchup row for the tracked summoner
3. Save one `player_matchups` row only if extraction succeeds

Current import behavior for extraction:

- if the match does not contain the tracked summoner, the `matches` row is kept and the matchup row is skipped
- if the tracked summoner has no valid `teamPosition`, the `matches` row is kept and the matchup row is skipped
- if no enemy player exists on the opposite team with the same `teamPosition`, the `matches` row is kept and the matchup row is skipped

Current transaction rule:

- match row and matchup row are written in the same request transaction
- extraction failures are handled as business outcomes, so the `matches` row remains stored
- unexpected persistence/runtime failures still roll back the transaction

## 7. Current matchup extraction rule

Current extractor logic is in:

- `PlayerMatchupExtractor`

Rule:

1. Find the participant whose `puuid` matches the stored summoner
2. Read that participant’s `teamPosition`
3. Find the first enemy participant where:
   - `teamId` is different
   - `teamPosition` matches the user role, case-insensitive
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

Current role matching assumptions:

- role comes directly from Riot `teamPosition`
- matching is exact after trimming, case-insensitive
- no fallback heuristics are implemented yet

Current extraction result model:

- success:
  - returns one detected personal matchup
- failure:
  - `USER_PARTICIPANT_NOT_FOUND`
  - `MISSING_ROLE`
  - `OPPONENT_NOT_FOUND`

Current import behavior for extraction failures:

- the `matches` row is still stored
- the `player_matchups` row is skipped
- the match will not be refetched later because it is already known in local storage

This is important:

- current extraction is based on one imported match
- future counter recommendation should be based on the full historical set of relevant `player_matchups` in the DB, not just recent in-memory API data

## 8. Matchup stats recalculation

Current recalculation use case:

- `RecalculatePersonalMatchupStatsUseCase`

Current rule:

- recalculate stats for one summoner only
- use the full stored `player_matchups` history for that summoner
- group by:
  - `enemyChampionId`
  - `userChampionId`
  - `role`
- upsert one `personal_matchup_stats` row per group
- delete stale `personal_matchup_stats` rows for that summoner when they no longer correspond to any grouped matchup history

Current aggregated fields per group:

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

Current metric basis:

- all stored matchup rows for the summoner are considered
- `overallChampionGames` is the count of all stored `player_matchups` for the same `userChampionId`
- recent performance uses the latest `5` stored matchup rows in the same grouped matchup

Current KDA formula for aggregation:

- if `deaths <= 0`: `kills + assists`
- else: `(kills + assists) / deaths`

## 9. Counter calculation scope

Current recommendation scoring logic exists in code, but it is not yet connected to a live counter endpoint.

The intended basis for one counter recommendation is:

- all stored matchup rows for:
  - one summoner
  - one enemy champion
  - one user champion
  - one role

So yes, the recommendation should be based on the whole relevant matchup history already stored in the DB.

It should not be based only on the latest API response once stats recalculation is wired.

## 10. Current scoring formula

Current scoring implementation is in:

- `RecommendationScoringService`

Final score formula:

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

Final score is:

- rounded to 1 decimal
- clamped to `0.0..100.0`

### 10.1 Winrate

Formula:

```text
winrate = wins / games * 100
```

Rules:

- if `games <= 0`, winrate = `0`
- `winrateScore = winrate`, clamped to `0..100`

Weight:

- `0.35`

### 10.2 Champion comfort

Input:

- `overallChampionGames`

Current mapping:

- `<= 0` -> `0`
- `1..3` -> `25`
- `4..10` -> `50`
- `11..25` -> scales from `75` up to `90`
- `> 25` -> `100`

Weight:

- `0.25`

### 10.3 KDA score

Input:

- `averageKda`

Current mapping:

- `< 1.5` -> `25`
- `< 2.5` -> `50`
- `<= 4.0` -> `75`
- `> 4.0` -> `100`

Weight:

- `0.15`

### 10.4 CS and gold score

Inputs:

- `averageCs`
- `averageGold`

Current CS mapping:

- `null` -> `50`
- `< 120` -> `30`
- `< 160` -> `50`
- `< 200` -> `75`
- `>= 200` -> `100`

Current gold mapping:

- `null` -> `50`
- `< 8000` -> `30`
- `< 10000` -> `50`
- `< 12000` -> `75`
- `>= 12000` -> `100`

Current combined rule:

```text
csGoldScore = (csScore + goldScore) / 2
```

Weight:

- `0.10`

### 10.5 Recent performance score

Inputs:

- `recentWinrate`
- `recentKda`

Current rule:

- `recentWinrateScore = recentWinrate`, clamped to `0..100`, else `50` if null
- `recentKdaScore = calculateKdaScore(recentKda)`, else `50` if null

Formula:

```text
recentPerformanceScore = (recentWinrateScore + recentKdaScore) / 2
```

Weight:

- `0.10`

### 10.6 Global fallback score

Current constant:

- `50`

Weight:

- `0.05`

### 10.7 Low sample penalty

Current penalty mapping:

- `0 games` -> `100`
- `1 game` -> `30`
- `2 games` -> `20`
- `3 games` -> `10`
- `>= 4 games` -> `0`

This penalty is subtracted after the weighted score is calculated.

## 11. Current confidence logic

Current confidence mapping:

- `0 games` -> `NO_DATA`
- `1..2 games` -> `LOW`
- `3..6 games` -> `MEDIUM`
- `>= 7 games` -> `HIGH`

## 12. Current recommendation status logic

Current status mapping:

1. if `games <= 0` -> `NO_DATA`
2. if `score < 45` and confidence is not `NO_DATA` -> `AVOID`
3. if confidence is `LOW` and `score >= 50` -> `LOW_DATA`
4. if `score >= 80` and confidence is at least `MEDIUM` -> `BEST_PICK`
5. if `score >= 65` -> `GOOD_PICK`
6. if `score >= 50` -> `OK_PICK`
7. else -> `AVOID`

Possible status values:

- `BEST_PICK`
- `GOOD_PICK`
- `OK_PICK`
- `LOW_DATA`
- `AVOID`
- `NO_DATA`

## 13. Important current limitations

These are current implementation realities, not bugs unless we decide they are unacceptable:

- scoring logic exists but is not yet powering a public counter endpoint
- import currently looks at only the latest `100` Riot match IDs per request
- role/opponent detection is same-role only and still simple
- no retry policy for Riot rate limiting yet
- malformed extraction cases keep the `matches` row but produce no `player_matchups` row
- no queue filtering yet
- no patch filtering yet
- no recency weighting stored in DB yet

## 14. Rules most likely to change later

These are the highest-probability future edits:

- `RECENT_MATCH_COUNT = 100`
- `RECENT_MATCHUP_SAMPLE_SIZE = 5`
- 24-hour account freshness window
- scoring weights
- low sample penalties
- confidence thresholds
- role/opponent fallback heuristics
- whether malformed matches should be skipped instead of failing the whole import
- whether import should page through more than 100 match IDs
- how "recent performance" is computed once full stat recalculation is wired

## 15. Current code references

Main files for these rules:

- [SearchSummonerUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/SearchSummonerUseCase.kt>)
- [ImportMatchHistoryUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/ImportMatchHistoryUseCase.kt>)
- [RecalculatePersonalMatchupStatsUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/RecalculatePersonalMatchupStatsUseCase.kt>)
- [PlayerMatchupExtractor.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/service/PlayerMatchupExtractor.kt>)
- [RecommendationScoringService.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/domain/service/RecommendationScoringService.kt>)
