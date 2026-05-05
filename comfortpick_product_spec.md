# ComfortPick — Product & Technical Specification

## 1. Project Summary

**ComfortPick** is a League of Legends personal matchup assistant.

The goal is not to tell the user the best theoretical counter champion based only on global statistics. The goal is to tell the user:

> “Against this enemy champion, what champion worked best for you personally?”

Example:

A normal counter website might say:

> “Zed is countered by Lissandra.”

ComfortPick should say:

> “Against Zed, your best personal pick is Ahri because you played Ahri into Zed 7 times, won 5, had a 4.1 KDA, and usually performed well in lane. You barely play Lissandra, so she is not recommended as your top pick.”

The application should analyze a player’s own Riot match history and produce personalized champion recommendations, builds, runes, stats, and confidence levels.

---

## 2. Core Idea

The app answers this question:

```text
Enemy champion → Which of my champions worked best against this champion?
```

Example flow:

```text
User searches enemy champion: Zed

App displays:
1. Ahri — Personal Score 86
2. Fizz — Personal Score 78
3. Lissandra — Personal Score 63
4. Yasuo — Personal Score 39 / Avoid
```

Then the user can click a champion, for example Ahri, and see:

```text
Ahri into Zed

Personal Score: 86 / 100
Build Score: 82 / 100
Rune Score: 76 / 100
Games: 7
Winrate: 71%
KDA: 4.1
Best build: Luden → Zhonya → Shadowflame
Best runes: Electrocute + Bone Plating
Reason: Best personal results against Zed with enough match history.
```

---

## 3. Product Positioning

ComfortPick should be positioned as:

> A personal League of Legends matchup assistant.

Main slogan idea:

> Stop picking the best counter. Pick your best counter.

The product is different from global counter websites because it focuses on:

```text
Personal data
Champion comfort
Matchup-specific history
Builds and runes that worked for the user
Confidence level based on sample size
```

---

## 4. Target Users

### Primary Users

League of Legends players who:

- play ranked or normal games regularly
- want better champion picks in champ select
- do not blindly trust global counter stats
- have a personal champion pool
- want to know what actually works for them

### Secondary Users

- players reviewing their match history
- players trying to build a champion pool
- players preparing for ranked climbing
- coaches or analysts reviewing a player’s matchup performance

---

## 5. Main Problem

Existing League tools often show:

```text
Global counter data
Champion winrates
Meta builds
Generic matchup advice
```

But this is incomplete because:

```text
A global counter is useless if the user cannot play that champion.
A 52% winrate counter might not be good for this specific player.
A user may perform better on a comfort pick than on a theoretical counter.
```

ComfortPick solves this by using the player’s own match history.

---

## 6. Main Features

## 6.1 Summoner Search

The user enters:

```text
Riot Game Name
Tagline
Region
```

Example:

```text
Rami
EUW
#1234
```

The app fetches the player’s account data from Riot API.

### Requirements

The app must:

- search Riot account by game name and tagline
- retrieve PUUID
- fetch recent match IDs
- fetch match details
- store matches in the database
- extract participant data
- identify the user’s champion
- identify the enemy lane opponent
- calculate personal matchup stats

---

## 6.2 Profile Dashboard

After searching a summoner, the user should see a profile dashboard.

### Dashboard should show:

```text
Summoner name
Region
Last update date
Total analyzed matches
Most played champions
Best personal counters
Worst matchups
Recently played champions
```

Example:

```text
Rami#EUW

Analyzed matches: 120
Main role: Mid
Most played: Ahri, Yasuo, Sylas
Best personal counter: Ahri into Zed
Worst matchup: Yasuo into Malzahar
```

---

## 6.3 Enemy Champion Page

This is the heart of the app.

The user chooses or searches an enemy champion.

Example:

```text
Zed
```

The app shows:

```text
Your counters to Zed
```

### Example UI Table

| Rank | Champion | Personal Score | Games | Winrate | KDA | Confidence | Status |
|---:|---|---:|---:|---:|---:|---|---|
| 1 | Ahri | 86 | 7 | 71% | 4.1 | High | Best Pick |
| 2 | Fizz | 78 | 6 | 66% | 3.4 | Medium | Good |
| 3 | Lissandra | 63 | 2 | 100% | 2.9 | Low | Low Data |
| 4 | Yasuo | 39 | 10 | 40% | 1.8 | High | Avoid |

### Important Rule

The ranking must not be based only on winrate.

Example:

```text
Lissandra: 1 win / 1 game = 100% winrate
Ahri: 5 wins / 7 games = 71% winrate

Ahri should still rank higher because she has more reliable data.
```

---

## 6.4 Personal Counter Detail Page

When the user clicks one personal counter, they see a detailed page.

Example route:

```text
/{summoner}/enemy/zed/pick/ahri
```

Page title:

```text
Ahri into Zed
```

### Page should show:

```text
Personal Score
Build Score
Rune Score
Games played
Wins / losses
Winrate
Average KDA
Average CS
Average gold
Average damage
Laning stats if available
Best build
Best runes
Recent games
Reasoning text
```

### Example

```text
Ahri into Zed

Personal Score: 86 / 100
Confidence: High

Stats:
- 7 games
- 5 wins / 2 losses
- 71% winrate
- 4.1 average KDA
- +8 average CS difference at 15 minutes
- +430 average gold difference at 15 minutes

Best Build:
Luden → Zhonya → Shadowflame

Build Score: 82 / 100

Best Runes:
Electrocute
Taste of Blood
Ultimate Hunter
Bone Plating

Reason:
Ahri is your strongest personal pick into Zed because you have high winrate, positive laning stats, good KDA, and enough games for reliable confidence.
```

---

## 6.5 Build Recommendation

For each personal matchup, the app should recommend the build that worked best for the user.

### First Version

Use simple logic:

```text
Look at the user’s wins with champion X against enemy champion Y.
Find the most common completed items.
Find the most common first item.
Find the most common defensive item.
Calculate build score.
```

Example:

```text
Against Zed with Ahri:

Best build:
1. Luden
2. Zhonya
3. Shadowflame

Reason:
This build appears in most of your wins and includes early defensive protection against Zed.
```

### Later Version

Improve with:

```text
Item timing
Patch version
Winrate per build path
Average KDA per build
Survival stats
Damage stats
Lane performance
```

---

## 6.6 Rune Recommendation

The app should recommend runes that worked best in the user’s matchup history.

### First Version

Use:

```text
Most common primary rune in wins
Most common secondary rune in wins
Winrate by rune page
```

Example:

```text
Best runes with Ahri into Zed:

Primary: Electrocute
Secondary: Resolve
Important rune: Bone Plating
Rune Score: 76 / 100
```

---

## 6.7 Confidence System

Every recommendation must have a confidence level.

### Confidence Levels

```text
No Data: 0 games
Low: 1-2 games
Medium: 3-6 games
High: 7+ games
```

### Example

```text
Lissandra into Zed
Games: 1
Winrate: 100%
Confidence: Low

Reason:
You won your only game, but there is not enough data to strongly recommend this pick.
```

---

## 6.8 Avoid Recommendations

The app should also show picks the user should avoid.

Example:

```text
Avoid picking Yasuo into Zed

Reason:
You played this matchup 10 times and only won 4 games. Your average KDA is low and your lane stats are negative.
```

This is useful because the app should not only say what to pick, but also what not to pick.

---

## 7. Core User Stories

## 7.1 Search Summoner

As a user,  
I want to enter my Riot ID and region,  
so that the app can analyze my League match history.

### Acceptance Criteria

- User can enter game name, tagline, and region.
- App retrieves Riot account data.
- App fetches recent matches.
- App displays profile dashboard.
- If the summoner is not found, show a clear error.

---

## 7.2 View Personal Counters for Enemy Champion

As a user,  
I want to search an enemy champion,  
so that I can see my best personal counters against that champion.

### Acceptance Criteria

- User can search/select an enemy champion.
- App displays a ranked list of personal counters.
- Each counter includes champion, score, games, winrate, KDA, confidence.
- List is sorted by personal score.
- Low sample size recommendations are marked clearly.

---

## 7.3 View Detailed Matchup

As a user,  
I want to click a personal counter,  
so that I can understand why it is recommended.

### Acceptance Criteria

- User can click a champion from the personal counter list.
- App opens detailed matchup page.
- Page shows stats, build, runes, score, confidence, and reasoning.
- Page shows recent games for that matchup.

---

## 7.4 View Best Build

As a user,  
I want to see the build that worked best for me in a matchup,  
so that I can reuse it in future games.

### Acceptance Criteria

- App displays best item build.
- App displays build score.
- App explains why the build is recommended.
- Build is based on the user’s past games, not only global data.

---

## 7.5 View Best Runes

As a user,  
I want to see the rune setup that worked best for me in a matchup,  
so that I can prepare before the game.

### Acceptance Criteria

- App displays primary and secondary rune choices.
- App shows rune score.
- App uses the user’s own match history where possible.
- If no personal rune data exists, app can show fallback recommendation.

---

## 7.6 View Low Confidence Warning

As a user,  
I want to know when a recommendation is based on little data,  
so that I do not blindly trust unreliable results.

### Acceptance Criteria

- App shows confidence level.
- App warns when sample size is low.
- App does not rank 1-game 100% winrate too highly without context.

---

## 7.7 Refresh Match History

As a user,  
I want to refresh my match history,  
so that recommendations stay updated.

### Acceptance Criteria

- User can click refresh.
- App fetches new matches from Riot API.
- App avoids duplicating already stored matches.
- App updates stats after new matches are stored.

---

## 8. Technical Stack

## 8.1 Recommended Stack

```text
Backend: Kotlin + Spring Boot or Go
PC/Web Frontend: React or Angular
Mobile App: React Native later
Database: PostgreSQL
Cache / Queue: Redis
Deployment: Docker
```

### Recommended default choice

```text
Backend: Kotlin + Spring Boot
PC/Web Frontend: React + TypeScript
Mobile: React Native
Database: PostgreSQL
Cache: Redis
```

Angular is also valid for the PC/web frontend if the developer prefers a more structured enterprise-style framework.

---

## 8.2 Backend

Use one of these two options:

### Option A — Kotlin Backend

```text
Kotlin
Spring Boot
Spring Web
Spring Data JPA
PostgreSQL
Redis
Docker
```

Why Kotlin:

The project has a lot of business logic:

```text
Personal scoring
Confidence calculation
Matchup analysis
Build analysis
Rune analysis
Recommendation explanations
```

Kotlin is good for clean domain modeling and maintainable business rules.

### Option B — Go Backend

```text
Go
Gin, Fiber, or Chi
PostgreSQL
Redis
Docker
```

Why Go:

```text
Fast services
Simple deployment
Low memory usage
Good for APIs and background workers
```

Go is valid, but the calculations in this project are not heavy. The main challenge is data modeling and recommendation logic, not raw performance.

Recommended choice for this project:

```text
Kotlin + Spring Boot
```

---

## 8.3 PC/Web Frontend

Use either:

```text
React + TypeScript
```

or:

```text
Angular + TypeScript
```

### Recommended default

```text
React + TypeScript
Vite or Next.js
Tailwind CSS
TanStack Query
Recharts
```

### If Angular is chosen

```text
Angular
TypeScript
Angular Material or Tailwind CSS
RxJS
NgRx optional later
```

### Frontend Responsibilities

```text
Summoner search
Profile dashboard
Enemy champion search
Personal counter list
Detailed matchup page
Build/rune display
Stats charts
Loading/error states
```

---

## 8.4 Mobile App

Build mobile after the website MVP.

Recommended:

```text
React Native
TypeScript
```

Reason:

```text
The web app can use TypeScript.
Some types and logic can be shared.
Faster to build than Kotlin Multiplatform for this product.
```

Kotlin Multiplatform is possible, but it may slow down the first version.

---

## 8.5 Database

Use:

```text
PostgreSQL
```

Reason:

```text
Reliable relational data
Good for match history
Good for joins and aggregations
Easy to query matchup stats
```

---

## 8.6 Cache / Background Jobs

Use:

```text
Redis
```

Use Redis for:

```text
Caching Riot API responses
Rate limit handling
Background match refresh jobs
Temporary recommendation cache
```

---

## 9. Backend Architecture

Use clean architecture / hexagonal architecture.

```text
backend/
  src/main/kotlin/
    com/comfortpick/
      domain/
        model/
        service/
        recommendation/
      application/
        usecase/
      infrastructure/
        riot/
        database/
        cache/
      api/
        controller/
        dto/
```

If using Go, keep the same logical structure:

```text
backend/
  cmd/
    api/
  internal/
    domain/
    application/
    infrastructure/
    api/
```

---

## 9.1 Domain Layer

Contains business models and logic.

Example packages:

```text
domain/model
domain/service
domain/recommendation
```

### Important Domain Models

```text
RiotAccount
Match
Participant
Champion
Matchup
PersonalCounter
PersonalMatchupStats
BuildStats
RuneStats
Recommendation
ConfidenceLevel
```

Example Kotlin model:

```kotlin
data class PersonalMatchupStats(
    val enemyChampionId: Int,
    val userChampionId: Int,
    val games: Int,
    val wins: Int,
    val losses: Int,
    val averageKda: Double,
    val averageCs: Double,
    val averageGold: Double,
    val averageDamage: Double,
    val confidence: ConfidenceLevel
)
```

---

## 9.2 Application Layer

Contains use cases.

Example use cases:

```text
SearchSummonerUseCase
ImportMatchHistoryUseCase
CalculatePersonalCountersUseCase
GetEnemyChampionCountersUseCase
GetPersonalMatchupDetailUseCase
RefreshSummonerMatchesUseCase
```

---

## 9.3 Infrastructure Layer

Contains external integrations.

Example:

```text
RiotApiClient
RiotMatchMapper
PostgresRepositories
RedisCacheService
```

---

## 9.4 API Layer

Contains REST controllers and DTOs.

Example:

```text
SummonerController
ProfileController
CounterController
MatchupController
```

---

## 10. Riot API Integration

## 10.1 Required Riot API Data

The app needs:

```text
Account by Riot ID
PUUID
Match IDs by PUUID
Match details
Champion IDs
Item IDs
Rune IDs
Queue type
Game patch
Participant stats
```

---

## 10.2 Riot API Flow

When user searches a summoner:

```text
1. User enters gameName, tagLine, region.
2. Backend calls Riot Account API.
3. Backend gets PUUID.
4. Backend calls Match API to get match IDs.
5. Backend fetches match details.
6. Backend stores matches.
7. Backend extracts participant data.
8. Backend identifies the user’s champion.
9. Backend identifies the enemy lane opponent.
10. Backend calculates matchup stats.
11. Backend returns dashboard/recommendation data.
```

---

## 10.3 Enemy Lane Opponent Detection

For MVP:

```text
Find the enemy participant with the same teamPosition.
```

Example:

```text
User:
Champion: Ahri
Team Position: MIDDLE
Team: Blue

Enemy:
Champion: Zed
Team Position: MIDDLE
Team: Red

Result:
Ahri into Zed
```

This is not perfect, but it is good enough for version 1.

Later, improve using:

```text
lane data
timeline data
early lane opponent proximity
role detection correction
```

---

## 11. API Endpoints

## 11.1 Search Summoner

```http
GET /api/summoners/{region}/{gameName}/{tagLine}
```

Response:

```json
{
  "id": "uuid",
  "gameName": "Rami",
  "tagLine": "EUW",
  "region": "EUW1",
  "puuid": "riot-puuid",
  "lastUpdatedAt": "2026-05-05T12:00:00Z"
}
```

---

## 11.2 Import Match History

```http
POST /api/summoners/{summonerId}/matches/import
```

Request:

```json
{
  "limit": 50
}
```

Response:

```json
{
  "importedMatches": 42,
  "alreadyExistingMatches": 8,
  "totalAnalyzedMatches": 50
}
```

---

## 11.3 Get Profile Dashboard

```http
GET /api/profiles/{summonerId}
```

Response:

```json
{
  "summoner": {
    "gameName": "Rami",
    "tagLine": "EUW",
    "region": "EUW1"
  },
  "analyzedMatches": 120,
  "mainRole": "MIDDLE",
  "mostPlayedChampions": [
    {
      "champion": "Ahri",
      "games": 22,
      "winrate": 59.1
    }
  ],
  "bestPersonalCounters": [
    {
      "enemyChampion": "Zed",
      "userChampion": "Ahri",
      "personalScore": 86
    }
  ],
  "worstMatchups": [
    {
      "enemyChampion": "Malzahar",
      "userChampion": "Yasuo",
      "personalScore": 31
    }
  ]
}
```

---

## 11.4 Get Personal Counters Against Enemy Champion

```http
GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters
```

Response:

```json
{
  "enemyChampion": {
    "id": 238,
    "name": "Zed"
  },
  "personalCounters": [
    {
      "rank": 1,
      "champion": {
        "id": 103,
        "name": "Ahri"
      },
      "personalScore": 86,
      "games": 7,
      "wins": 5,
      "losses": 2,
      "winrate": 71.4,
      "averageKda": 4.1,
      "confidence": "HIGH",
      "status": "BEST_PICK",
      "reason": "Your best personal result into Zed with strong winrate and reliable sample size."
    },
    {
      "rank": 2,
      "champion": {
        "id": 105,
        "name": "Fizz"
      },
      "personalScore": 78,
      "games": 6,
      "wins": 4,
      "losses": 2,
      "winrate": 66.6,
      "averageKda": 3.4,
      "confidence": "MEDIUM",
      "status": "GOOD_PICK",
      "reason": "Good personal performance but slightly less reliable than Ahri."
    }
  ]
}
```

---

## 11.5 Get Detailed Personal Matchup

```http
GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters/{userChampionId}
```

Response:

```json
{
  "enemyChampion": {
    "id": 238,
    "name": "Zed"
  },
  "userChampion": {
    "id": 103,
    "name": "Ahri"
  },
  "personalScore": 86,
  "buildScore": 82,
  "runeScore": 76,
  "confidence": "HIGH",
  "stats": {
    "games": 7,
    "wins": 5,
    "losses": 2,
    "winrate": 71.4,
    "averageKda": 4.1,
    "averageKills": 8.2,
    "averageDeaths": 3.1,
    "averageAssists": 6.5,
    "averageCs": 204,
    "averageGold": 12300,
    "averageDamage": 26500
  },
  "bestBuild": {
    "items": [
      "Luden",
      "Zhonya",
      "Shadowflame"
    ],
    "reason": "This build appears most often in your wins against Zed."
  },
  "bestRunes": {
    "primary": "Electrocute",
    "secondary": "Resolve",
    "importantRunes": [
      "Taste of Blood",
      "Ultimate Hunter",
      "Bone Plating"
    ]
  },
  "recentGames": [
    {
      "matchId": "EUW1_123456",
      "win": true,
      "kills": 9,
      "deaths": 2,
      "assists": 8,
      "kda": 8.5,
      "items": [
        "Luden",
        "Zhonya",
        "Shadowflame"
      ]
    }
  ],
  "reason": "Ahri is your strongest personal pick into Zed because you have high winrate, strong KDA, and enough games for reliable confidence."
}
```

---

## 12. Database Schema

## 12.1 riot_accounts

```sql
CREATE TABLE riot_accounts (
    id UUID PRIMARY KEY,
    puuid TEXT NOT NULL UNIQUE,
    game_name TEXT NOT NULL,
    tag_line TEXT NOT NULL,
    region TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

---

## 12.2 matches

```sql
CREATE TABLE matches (
    id UUID PRIMARY KEY,
    riot_match_id TEXT NOT NULL UNIQUE,
    region TEXT NOT NULL,
    queue_id INT,
    game_creation TIMESTAMP,
    game_duration_seconds INT,
    patch TEXT,
    created_at TIMESTAMP NOT NULL
);
```

---

## 12.3 participants

```sql
CREATE TABLE participants (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL REFERENCES matches(id),
    puuid TEXT NOT NULL,
    summoner_name TEXT,
    champion_id INT NOT NULL,
    champion_name TEXT NOT NULL,
    team_id INT NOT NULL,
    team_position TEXT,
    win BOOLEAN NOT NULL,
    kills INT NOT NULL,
    deaths INT NOT NULL,
    assists INT NOT NULL,
    total_cs INT,
    gold_earned INT,
    total_damage_to_champions INT,
    item_0 INT,
    item_1 INT,
    item_2 INT,
    item_3 INT,
    item_4 INT,
    item_5 INT,
    item_6 INT,
    primary_rune_id INT,
    secondary_rune_id INT
);
```

---

## 12.4 personal_matchup_stats

This table can be calculated dynamically first. Later, use it as a precomputed table.

```sql
CREATE TABLE personal_matchup_stats (
    id UUID PRIMARY KEY,
    riot_account_id UUID NOT NULL REFERENCES riot_accounts(id),
    enemy_champion_id INT NOT NULL,
    user_champion_id INT NOT NULL,
    role TEXT,
    games INT NOT NULL,
    wins INT NOT NULL,
    losses INT NOT NULL,
    winrate DOUBLE PRECISION NOT NULL,
    average_kda DOUBLE PRECISION,
    average_cs DOUBLE PRECISION,
    average_gold DOUBLE PRECISION,
    average_damage DOUBLE PRECISION,
    personal_score DOUBLE PRECISION NOT NULL,
    confidence TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

---

## 12.5 personal_build_stats

```sql
CREATE TABLE personal_build_stats (
    id UUID PRIMARY KEY,
    riot_account_id UUID NOT NULL REFERENCES riot_accounts(id),
    enemy_champion_id INT NOT NULL,
    user_champion_id INT NOT NULL,
    item_sequence TEXT NOT NULL,
    games INT NOT NULL,
    wins INT NOT NULL,
    winrate DOUBLE PRECISION NOT NULL,
    build_score DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

---

## 13. Recommendation Scoring

## 13.1 Personal Score

The personal score should be from 0 to 100.

Do not use only winrate.

Suggested formula for MVP:

```text
Personal Score =
winrateScore * 0.35
+ championComfortScore * 0.25
+ kdaScore * 0.15
+ csGoldScore * 0.10
+ recentPerformanceScore * 0.10
+ globalFallbackScore * 0.05
- lowSamplePenalty
```

---

## 13.2 Score Components

### Winrate Score

```text
winrateScore = winrate percentage
```

Example:

```text
71% winrate = 71 score
```

---

### Champion Comfort Score

Based on how much the user plays this champion overall.

Example:

```text
0 games overall = 0
1-3 games = 25
4-10 games = 50
11-25 games = 75
26+ games = 100
```

---

### KDA Score

Approximate version:

```text
KDA < 1.5 = 25
KDA 1.5 - 2.5 = 50
KDA 2.5 - 4.0 = 75
KDA > 4.0 = 100
```

---

### CS / Gold Score

For MVP, use average CS and gold.

Later, use CS diff and gold diff at 15 minutes if timeline data is available.

---

### Recent Performance Score

Recent games should matter more than old games.

Example:

```text
Last 10 games on that champion
Recent winrate
Recent KDA
Recent role performance
```

---

### Global Fallback Score

If the user has little or no personal data, use global matchup data later.

For MVP, this can be set to neutral:

```text
50
```

Later, connect to external/global champion statistics if allowed.

---

## 13.3 Low Sample Penalty

Avoid ranking low-data results too high.

Example:

```text
0 games: cannot recommend as personal counter
1 game: -30 penalty
2 games: -20 penalty
3 games: -10 penalty
4+ games: no major penalty
```

Example:

```text
Lissandra into Zed
1 game
100% winrate
Raw score: 85
Penalty: -30
Final score: 55
Confidence: Low
```

---

## 13.4 Confidence Calculation

```kotlin
enum class ConfidenceLevel {
    NO_DATA,
    LOW,
    MEDIUM,
    HIGH
}
```

Rules:

```text
0 games = NO_DATA
1-2 games = LOW
3-6 games = MEDIUM
7+ games = HIGH
```

---

## 13.5 Recommendation Status

```kotlin
enum class RecommendationStatus {
    BEST_PICK,
    GOOD_PICK,
    OK_PICK,
    LOW_DATA,
    AVOID
}
```

Suggested rules:

```text
Score >= 80 and confidence Medium/High = BEST_PICK
Score >= 65 = GOOD_PICK
Score >= 50 = OK_PICK
Low confidence but decent score = LOW_DATA
Score < 45 with enough games = AVOID
```

---

## 14. Frontend Pages

## 14.1 Landing Page

Route:

```text
/
```

Content:

```text
Hero section
Summoner search bar
Short explanation
Example result card
```

Example copy:

```text
Find your personal counters in League of Legends.

ComfortPick analyzes your match history and tells you which champions worked best for you into each enemy champion.
```

---

## 14.2 Search Page

Route:

```text
/search
```

Fields:

```text
Game name
Tagline
Region
```

Button:

```text
Analyze
```

---

## 14.3 Profile Page

Route:

```text
/profile/{summonerId}
```

Sections:

```text
Summoner summary
Analyzed matches
Most played champions
Best personal counters
Worst matchups
Search enemy champion
```

---

## 14.4 Enemy Champion Page

Route:

```text
/profile/{summonerId}/enemy/{enemyChampionId}
```

Title:

```text
Your counters to Zed
```

Sections:

```text
Ranked personal counter list
Best pick card
Avoid picks
Low-data picks
```

---

## 14.5 Personal Matchup Detail Page

Route:

```text
/profile/{summonerId}/enemy/{enemyChampionId}/pick/{userChampionId}
```

Title:

```text
Ahri into Zed
```

Sections:

```text
Summary card
Personal score
Build score
Rune score
Stats
Best build
Best runes
Recent games
Reasoning
```

---

## 15. UI Components

```text
SummonerSearchBar
ChampionSearchInput
ChampionAvatar
PersonalCounterCard
CounterRankingTable
ScoreBadge
ConfidenceBadge
BuildPathCard
RunePageCard
MatchHistoryList
ReasonCard
AvoidPickCard
LoadingState
ErrorState
```

---

## 16. MVP Scope

## Version 0 — Prototype

Goal:

```text
Prove the core recommendation idea.
```

Features:

```text
Search summoner
Fetch last 20-50 matches
Store matches
Search enemy champion
Show personal counters
Show simple personal score
```

Do not include yet:

```text
Login
Mobile app
Subscriptions
Complex AI
Live champ select
Advanced timeline stats
```

---

## Version 1 — Real MVP

Features:

```text
Profile dashboard
Personal counter list
Detailed matchup page
Build recommendation
Rune recommendation
Confidence level
Refresh match history
```

---

## Version 2 — Improved Analysis

Features:

```text
Patch-based filtering
Recent performance weighting
Better build analysis
Better rune analysis
Better role detection
Worst matchup analysis
Champion pool suggestions
```

---

## Version 3 — Draft Assistant

Features:

```text
User enters enemy team picks
App recommends best personal pick
App considers team composition
App warns about low confidence
App suggests bans based on user history
```

---

## Version 4 — Mobile App

Features:

```text
React Native mobile app
Search summoner
View profile
View personal counters
View matchup details
Save favorite profiles
```

---

## 17. Development Milestones

## Milestone 1 — Backend Setup

Tasks:

```text
Create Kotlin Spring Boot project or Go project
Set up PostgreSQL
Set up Docker Compose
Create base package structure
Add health check endpoint
```

---

## Milestone 2 — Riot API Client

Tasks:

```text
Create RiotApiClient
Fetch account by Riot ID
Fetch match IDs by PUUID
Fetch match details
Handle Riot API errors
Handle rate limits
```

---

## Milestone 3 — Match Import

Tasks:

```text
Create match import use case
Store matches
Store participants
Avoid duplicate matches
Extract user participant
Extract enemy lane opponent
```

---

## Milestone 4 — Personal Counter Calculation

Tasks:

```text
Group matches by enemy champion
Group matches by user champion
Calculate games, wins, losses
Calculate winrate
Calculate KDA
Calculate personal score
Calculate confidence
Return sorted personal counter list
```

---

## Milestone 5 — Detail Page Data

Tasks:

```text
Calculate detailed matchup stats
Calculate best build
Calculate build score
Calculate best runes
Return recent games
Generate reasoning text
```

---

## Milestone 6 — Frontend MVP

Tasks:

```text
Create React or Angular app
Create landing page
Create summoner search
Create profile page
Create enemy champion page
Create matchup detail page
Connect frontend to backend API
```

---

## Milestone 7 — Polish

Tasks:

```text
Add loading states
Add error states
Add empty states
Improve UI
Add champion icons
Add item icons
Add rune icons
Improve mobile responsiveness
```

---

## 18. Error Handling

## 18.1 Summoner Not Found

Show:

```text
Summoner not found. Check the game name, tagline, and region.
```

---

## 18.2 Riot API Rate Limit

Show:

```text
Riot API limit reached. Please try again later.
```

Backend should retry only when appropriate.

---

## 18.3 No Match Data

Show:

```text
No recent matches found for this player.
```

---

## 18.4 No Personal Data Against Champion

Show:

```text
You have no personal games against Zed yet.
```

Then optionally show:

```text
Your most comfortable champions overall:
Ahri
Sylas
Vex
```

Later version can combine this with global matchup data.

---

## 19. Important Product Rules

## 19.1 Do Not Overtrust Winrate

A champion with 1 win in 1 game should not automatically rank first.

---

## 19.2 Always Show Confidence

Every recommendation must display confidence.

---

## 19.3 Personal Data Comes First

The app’s main promise is personal recommendation.

Global stats can be used later only as fallback.

---

## 19.4 Keep Explanation Clear

Every recommendation should answer:

```text
Why is this champion recommended?
```

Example:

```text
Ahri is recommended because you have strong personal results into Zed, a high winrate, good KDA, and enough games for reliable confidence.
```

---

## 19.5 Avoid Picks Are Useful

The app should also warn users about personal bad matchups.

Example:

```text
Avoid Yasuo into Zed based on your personal history.
```

---

## 20. Example Full User Flow

```text
1. User opens ComfortPick.
2. User enters Riot ID: Rami#EUW.
3. App fetches and imports recent matches.
4. User lands on profile dashboard.
5. User searches enemy champion: Zed.
6. App opens "Your counters to Zed".
7. App shows:
   - Ahri rank 1
   - Fizz rank 2
   - Lissandra rank 3, low confidence
   - Yasuo avoid
8. User clicks Ahri.
9. App opens "Ahri into Zed".
10. User sees score, stats, build, runes, recent games, and reason.
```

---

## 21. Main Differentiator

The app is not:

```text
A generic counter website.
```

The app is:

```text
A personal matchup memory system.
```

It remembers what worked for the player.

---

## 22. Suggested Repository Structure

```text
comfortpick/
  backend/
    src/
    Dockerfile
    docker-compose.yml
    build.gradle.kts or go.mod

  frontend/
    src/
    components/
    lib/
    types/
    package.json

  mobile/
    src/
    package.json

  docs/
    product-spec.md
    api-spec.md
    scoring-model.md
```

---

## 23. Suggested Backend Package Structure

### Kotlin version

```text
com.comfortpick
  api
    SummonerController
    ProfileController
    CounterController
    MatchupController

  application
    SearchSummonerUseCase
    ImportMatchHistoryUseCase
    GetPersonalCountersUseCase
    GetMatchupDetailUseCase

  domain
    model
      RiotAccount
      Match
      Participant
      PersonalCounter
      PersonalMatchupStats
      Recommendation
    service
      PersonalCounterService
      RecommendationScoringService
      BuildAnalysisService
      RuneAnalysisService

  infrastructure
    riot
      RiotApiClient
      RiotDto
      RiotMapper
    persistence
      RiotAccountEntity
      MatchEntity
      ParticipantEntity
      Repositories
    cache
      RedisCacheService
```

### Go version

```text
internal/
  api/
    handlers/
    dto/
  application/
    usecases/
  domain/
    model/
    service/
    recommendation/
  infrastructure/
    riot/
    persistence/
    cache/
cmd/
  api/
```

---

## 24. Initial API Priority

Build these endpoints first:

```http
GET /api/summoners/{region}/{gameName}/{tagLine}
POST /api/summoners/{summonerId}/matches/import
GET /api/profiles/{summonerId}
GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters
GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters/{userChampionId}
```

The most important endpoint is:

```http
GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters
```

Because it powers the main feature:

```text
Your counters to Zed
```

---

## 25. First Implementation Objective

The first working version should do only this:

```text
Given a summoner and enemy champion,
show the user's best personal champion picks against that enemy champion.
```

Example:

```text
Input:
Summoner: Rami#EUW
Enemy: Zed

Output:
1. Ahri — Score 86 — High confidence
2. Fizz — Score 78 — Medium confidence
3. Lissandra — Score 63 — Low confidence
4. Yasuo — Score 39 — Avoid
```

Everything else can come after.

---

## 26. Final Notes for the Agent

Build the project around the personal counter concept.

The core domain is:

```text
User's champion + enemy champion + past performance = personal recommendation
```

Do not start with mobile.

Do not start with authentication.

Do not start with complex AI.

Start with:

```text
Riot API integration
Match import
Participant extraction
Personal matchup calculation
Recommendation endpoint
Simple React or Angular frontend display
```

The product becomes valuable once the user can search:

```text
Enemy champion: Zed
```

and get:

```text
Your best personal counters to Zed.
```
