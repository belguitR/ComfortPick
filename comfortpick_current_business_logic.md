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
- Task 8: personal counters endpoint
- Task 9: profile dashboard endpoint
- Task 10: matchup detail endpoint
- Task 11: build and rune analysis
- Task 12: frontend summoner search flow
- Task 13: frontend profile dashboard
- Task 14: frontend enemy champion counters page
- Task 15: frontend matchup detail page

Not implemented yet:

- production hardening

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

Current counters endpoint:

- `GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters`

Current endpoint rule:

- read only from `personal_matchup_stats`
- do not call Riot API
- sort by stored `personalScore` descending
- derive `status` from:
  - stored `personalScore`
  - stored `confidence`
  - stored `games`
- return empty list when the summoner exists but has no counters for that enemy champion

Current returned fields per counter:

- `enemyChampionId`
- `userChampionId`
- `role`
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
- `status`

The intended basis for one counter recommendation is:

- all stored matchup rows for:
  - one summoner
  - one enemy champion
  - one user champion
  - one role

So yes, the recommendation should be based on the whole relevant matchup history already stored in the DB.

It should not be based only on the latest API response once stats recalculation is wired.

## 10. Profile dashboard logic

Current profile dashboard endpoint:

- `GET /api/profiles/{summonerId}`

Current endpoint rule:

- read only from:
  - `riot_accounts`
  - `player_matchups`
  - `personal_matchup_stats`
- do not call Riot API
- return dashboard data for an existing stored summoner even when no analysis has been imported yet

Current returned fields:

- `summoner`
  - internal id
  - `gameName`
  - `tagLine`
  - `region`
- `analyzedMatches`
- `mainRole`
- `mostPlayedChampions`
- `bestCounters`
- `worstMatchups`
- `lastUpdateAt`

Current field derivation:

- `analyzedMatches`
  - total count of stored `player_matchups` rows for the summoner
- `mainRole`
  - most frequent `role` across stored `player_matchups`
  - tie-breaker is alphabetical role ordering
- `mostPlayedChampions`
  - top `5` `userChampionId` values by stored `player_matchups` count
  - tie-breaker is lower `championId` first
- `bestCounters`
  - top `5` rows from stored `personal_matchup_stats`
  - sorted by:
    - `personalScore` descending
    - `games` descending
    - `userChampionId` ascending
- `worstMatchups`
  - bottom `5` rows from stored `personal_matchup_stats`
  - sorted by:
    - `personalScore` ascending
    - `games` descending
    - `userChampionId` ascending
- `lastUpdateAt`
  - max of:
    - latest `personal_matchup_stats.updatedAt`
    - latest `player_matchups.createdAt`
  - `null` when the summoner has no stored analysis data yet

Current empty dashboard behavior:

- if the stored summoner exists but has no `player_matchups` and no `personal_matchup_stats`:
  - `analyzedMatches = 0`
  - `mainRole = null`
  - `mostPlayedChampions = []`
  - `bestCounters = []`
  - `worstMatchups = []`
  - `lastUpdateAt = null`

## 11. Matchup detail logic

Current matchup detail endpoint:

- `GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters/{userChampionId}`

Current endpoint rule:

- read only from stored DB data
- do not call Riot API
- return one detailed matchup view for the requested:
  - summoner
  - enemy champion
  - user champion

Current detail selection rule:

- `personal_matchup_stats` is queried for all rows matching:
  - `summonerId`
  - `enemyChampionId`
  - `userChampionId`
- if multiple rows exist because the same champion pair was played in multiple roles:
  - choose the row with:
    - highest `personalScore`
    - then highest `games`
    - then alphabetical `role`

Current returned fields:

- `hasData`
- `enemyChampionId`
- `userChampionId`
- `role`
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
- `status`
- `reasoning`
- `lastUpdatedAt`
- `recentGames`

Current recent games rule:

- recent games are loaded from stored `player_matchups`
- only rows matching the selected:
  - `enemyChampionId`
  - `userChampionId`
  - `role`
- ordered by:
  - `match.gameCreation` descending
  - then `player_matchups.createdAt` descending
- limited to the latest `5` games

Current no-data behavior:

- if the summoner exists but no stored matchup stats exist for the requested champion pair:
  - return `200`
  - `hasData = false`
  - `status = NO_DATA`
  - `confidence = NO_DATA`
  - `reasoning = "No personal data yet for this champion matchup."`
  - `recentGames = []`

Current reasoning text mapping:

- `BEST_PICK`
  - `"Strong personal matchup: {games} games, {winrate}% win rate, {kda} KDA, and {confidence} confidence."`
- `GOOD_PICK`
  - `"Positive personal results: {games} games, {winrate}% win rate, {kda} KDA, and {confidence} confidence."`
- `OK_PICK`
  - `"Playable but not dominant: {games} games, {winrate}% win rate, {kda} KDA, and {confidence} confidence."`
- `LOW_DATA`
  - `"Promising but low-confidence: only {games} games, {winrate}% win rate, and {kda} KDA."`
- `AVOID`
  - `"This matchup has been poor for you: {games} games, {winrate}% win rate, and {kda} KDA."`
- `NO_DATA`
  - `"No personal data yet for this champion matchup."`

## 12. Current build and rune analysis

Current build/rune analysis is exposed through:

- `GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters/{userChampionId}`

Current rule:

- analysis uses stored `player_matchups` for the selected:
  - `enemyChampionId`
  - `userChampionId`
  - selected `role`
- only winning games are used for recommendations
- if there are no winning games, build/rune recommendations return empty values

Current build recommendation fields:

- `firstCompletedItemId`
- `firstCompletedItemGames`
- `itemSet`
- `itemSetGames`
- `score`

Current rune recommendation fields:

- `primaryRuneId`
- `primaryRuneGames`
- `secondaryRuneId`
- `secondaryRuneGames`
- `score`

Current MVP heuristics:

- `firstCompletedItemId`
  - first non-zero inventory slot among stored final items `item0..item5`
- `itemSet`
  - joined non-zero final inventory items from `item0..item5`
  - format: `itemA>itemB>itemC`
- `primaryRuneId`
  - most common `primaryRuneId` in winning games
- `secondaryRuneId`
  - most common `secondaryRuneId` in winning games

Current deterministic tie-breakers:

- item ids:
  - lower item id first when counts tie
- item sets:
  - lexical string order when counts tie
- rune ids:
  - lower rune id first when counts tie

Current build/rune score formula:

- one shared score is used for both build and rune recommendations
- inputs:
  - matchup win rate for the selected role
  - number of winning games supporting the recommendation

Formula:

```text
score = (winrate * 0.65 + sampleBoost * 0.35) - lowSamplePenalty
```

Current `sampleBoost` mapping:

- `>= 5 wins` -> `100`
- `4 wins` -> `90`
- `3 wins` -> `75`
- `2 wins` -> `60`
- `1 win` -> `40`

Current `lowSamplePenalty` mapping:

- `0 wins` -> `100`
- `1 win` -> `25`
- `2 wins` -> `10`
- `>= 3 wins` -> `0`

Final score is:

- rounded to 1 decimal
- clamped to `0.0..100.0`

Important current limitation:

- "first completed item" is an approximation based on final inventory slots, not an actual item purchase timeline

## 13. Current scoring formula

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

### 13.1 Winrate

Formula:

```text
winrate = wins / games * 100
```

Rules:

- if `games <= 0`, winrate = `0`
- `winrateScore = winrate`, clamped to `0..100`

Weight:

- `0.35`

### 13.2 Champion comfort

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

### 13.3 KDA score

Input:

- `averageKda`

Current mapping:

- `< 1.5` -> `25`
- `< 2.5` -> `50`
- `<= 4.0` -> `75`
- `> 4.0` -> `100`

Weight:

- `0.15`

### 13.4 CS and gold score

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

### 13.5 Recent performance score

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

### 13.6 Global fallback score

Current constant:

- `50`

Weight:

- `0.05`

### 13.7 Low sample penalty

Current penalty mapping:

- `0 games` -> `100`
- `1 game` -> `30`
- `2 games` -> `20`
- `3 games` -> `10`
- `>= 4 games` -> `0`

This penalty is subtracted after the weighted score is calculated.

## 14. Current confidence logic

Current confidence mapping:

- `0 games` -> `NO_DATA`
- `1..2 games` -> `LOW`
- `3..6 games` -> `MEDIUM`
- `>= 7 games` -> `HIGH`

## 15. Current recommendation status logic

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

## 16. Important current limitations

These are current implementation realities, not bugs unless we decide they are unacceptable:

- scoring is powering public recommendations, but the formula is still heuristic MVP logic
- import currently looks at only the latest `100` Riot match IDs per request
- role/opponent detection is same-role only and still simple
- no retry policy for Riot rate limiting yet
- malformed extraction cases keep the `matches` row but produce no `player_matchups` row
- no queue filtering yet
- no patch filtering yet
- no recency weighting stored in DB yet
- frontend is currently being verified desktop-first by user direction; mobile polish is intentionally deferred

## 17. Current frontend flow

Current frontend routes:

- `/`
  - validates `gameName` and `tagLine`
  - calls summoner search
  - then calls match import
  - then navigates to `/profiles/{summonerId}`
- `/profiles/{summonerId}`
  - loads stored profile dashboard from DB-backed API only
  - can trigger `Refresh matches`, which calls the import endpoint and reloads the dashboard
  - exposes an enemy champion id form that navigates to `/profiles/{summonerId}/enemies/{enemyChampionId}`
- `/profiles/{summonerId}/enemies/{enemyChampionId}`
  - loads the personal counters ranking for one enemy champion
  - reads only from the stored counters endpoint
- `/profiles/{summonerId}/enemies/{enemyChampionId}/counters/{userChampionId}`
  - loads one detailed stored matchup view
  - shows reasoning, summary stats, recent games, and build/rune recommendations when present

Current frontend API usage:

- search page:
  - `GET /api/summoners/{region}/{gameName}/{tagLine}`
  - `POST /api/summoners/{summonerId}/matches/import`
- profile dashboard:
  - `GET /api/profiles/{summonerId}`
  - optional refresh:
    - `POST /api/summoners/{summonerId}/matches/import`
    - then `GET /api/profiles/{summonerId}`
- enemy counters page:
  - `GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters`
- matchup detail page:
  - `GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters/{userChampionId}`

## 18. Rules most likely to change later

These are the highest-probability future edits:

- `RECENT_MATCH_COUNT = 100`
- `RECENT_GAMES_LIMIT = 5`
- `RECENT_MATCHUP_SAMPLE_SIZE = 5`
- 24-hour account freshness window
- scoring weights
- build/rune score formula
- low sample penalties
- confidence thresholds
- role/opponent fallback heuristics
- whether malformed matches should be skipped instead of failing the whole import
- whether import should page through more than 100 match IDs
- how "recent performance" is computed once full stat recalculation is wired

## 19. Current code references

Main files for these rules:

- [SearchSummonerUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/SearchSummonerUseCase.kt>)
- [ImportMatchHistoryUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/ImportMatchHistoryUseCase.kt>)
- [RecalculatePersonalMatchupStatsUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/RecalculatePersonalMatchupStatsUseCase.kt>)
- [GetProfileDashboardUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/GetProfileDashboardUseCase.kt>)
- [GetPersonalMatchupDetailUseCase.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/usecase/GetPersonalMatchupDetailUseCase.kt>)
- [BuildRuneAnalysisService.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/domain/service/BuildRuneAnalysisService.kt>)
- [PlayerMatchupExtractor.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/application/service/PlayerMatchupExtractor.kt>)
- [RecommendationScoringService.kt](</C:/Users/errmi/Documents/New project/backend/src/main/kotlin/com/comfortpick/domain/service/RecommendationScoringService.kt>)
