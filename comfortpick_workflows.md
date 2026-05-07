# ComfortPick Workflow Diagrams

This file captures the current implemented app flow.

It is a snapshot of how the product works today, not a future-state design.

## 1. Product flow

```mermaid
flowchart LR
    A["User searches Riot account"] --> B["Summoner lookup"]
    B --> C["Stored profile opens"]
    C --> D["Background sync builds history"]
    D --> E["Raw player matchups stored"]
    E --> F["Aggregated matchup stats recalculated"]
    F --> G["Dashboard reads from DB"]
    F --> H["Counters page reads from DB"]
    F --> I["Matchup detail reads from DB"]
```

## 2. High-level architecture

```mermaid
flowchart TB
    UI["Frontend (React / Vite)"]
    API["API layer (Spring controllers)"]
    APP["Application layer (use cases / ports)"]
    DOMAIN["Domain layer (scoring / analysis)"]
    INFRA["Infrastructure layer (Riot adapter / persistence adapters)"]
    DB["PostgreSQL"]
    RIOT["Riot API"]

    UI --> API
    API --> APP
    APP --> DOMAIN
    APP --> INFRA
    INFRA --> DB
    INFRA --> RIOT
```

## 3. Search and profile-open flow

```mermaid
sequenceDiagram
    participant U as User
    participant FE as Frontend
    participant API as Backend
    participant DB as PostgreSQL
    participant RIOT as Riot API

    U->>FE: Enter game name, tag, region
    FE->>API: GET /api/summoners/{region}/{gameName}/{tagLine}
    API->>DB: Check riot_accounts by region + gameName + tagLine

    alt Fresh stored account exists
        DB-->>API: Stored account
    else Missing or stale
        API->>RIOT: Account lookup by Riot ID
        RIOT-->>API: Account payload
        API->>DB: Upsert riot_accounts by puuid
    end

    API-->>FE: Summoner profile id
    FE->>API: POST /api/profiles/{summonerId}/sync
    FE->>FE: Navigate to /profiles/{summonerId}
    FE->>API: GET /api/profiles/{summonerId}
    API->>DB: Read dashboard data only
    DB-->>API: Stored profile snapshot
    API-->>FE: Dashboard response
```

## 4. Background sync flow

```mermaid
flowchart TD
    A["Sync requested"] --> B["riot_accounts.syncStatus = ACTIVE"]
    B --> C["Scheduler picks due account"]
    C --> D["Mark account RUNNING"]
    D --> E["Fetch newest 10 Riot match IDs (start = 0)"]
    E --> F["Import missing head matches"]
    F --> G["Advance backfill cursor by newly imported head matches"]
    G --> H{"Target reached?"}
    H -- "No" --> I{"Head page full?"}
    I -- "Yes" --> J["Fetch one older 10-match page"]
    J --> K["Import missing older matches"]
    K --> L["Advance backfill cursor by fetched older page size"]
    I -- "No" --> M["History end reached"]
    L --> N{"More work remains?"}
    N -- "Yes" --> O["Set ACTIVE and nextRunAt = now + 20s"]
    N -- "No" --> P["Set COMPLETE"]
    M --> P
```

## 5. Sync failure and repair behavior

```mermaid
flowchart TD
    A["Profile sync request"] --> B{"Coverage suspiciously low?"}
    B -- "Yes" --> C["Reset syncBackfillCursor to 0"]
    B -- "No" --> D["Keep current cursor"]
    C --> E["Run repair pass through already scanned history"]
    D --> F["Continue normal sync"]

    G["During sync cycle"] --> H{"Riot 429?"}
    H -- "Yes" --> I["Set RATE_LIMITED"]
    I --> J["nextRunAt = now + retryAfterSeconds"]
    H -- "No" --> K{"Other exception?"}
    K -- "Yes" --> L["Set FAILED"]
    L --> M["nextRunAt = now + 5 minutes"]
    K -- "No" --> N["Complete normal cycle"]
```

## 6. Match import and extraction flow

```mermaid
sequenceDiagram
    participant SYNC as Sync worker / import use case
    participant RIOT as Riot API
    participant DB as PostgreSQL
    participant EX as Matchup extractor
    participant RECALC as Stats recalculation

    SYNC->>RIOT: Get match IDs page
    RIOT-->>SYNC: Riot match IDs
    SYNC->>DB: Find existing matches
    SYNC->>DB: Find existing player_matchups for this account

    loop For each match ID needing processing
        SYNC->>RIOT: Get match details
        RIOT-->>SYNC: Match payload
        alt Match row not stored yet
            SYNC->>DB: Insert into matches
        end
        SYNC->>EX: Extract personal matchup row
        alt Extraction success
            EX-->>SYNC: user champ vs enemy champ matchup
            SYNC->>DB: Insert into player_matchups
        else Extraction failure
            EX-->>SYNC: USER_PARTICIPANT_NOT_FOUND / MISSING_ROLE / OPPONENT_NOT_FOUND
            SYNC->>SYNC: Keep matches row, skip matchup row
        end
    end

    alt At least one matchup row inserted
        SYNC->>RECALC: Recalculate personal_matchup_stats
        RECALC->>DB: Read full player_matchups for account
        RECALC->>DB: Upsert personal_matchup_stats
    end
```

## 7. Role resolution and opponent detection

```mermaid
flowchart TD
    A["Find user participant by puuid"] --> B{"Found?"}
    B -- "No" --> X["Fail: USER_PARTICIPANT_NOT_FOUND"]
    B -- "Yes" --> C["Resolve user role"]
    C --> D["Use teamPosition first"]
    D --> E{"Non-empty valid role?"}
    E -- "No" --> F["Fallback to individualPosition"]
    F --> G{"Valid normalized role?"}
    G -- "No" --> Y["Fail: MISSING_ROLE"]
    E -- "Yes" --> H["Resolved role"]
    G -- "Yes" --> H
    H --> I["Find first enemy participant with same normalized role"]
    I --> J{"Found?"}
    J -- "No" --> Z["Fail: OPPONENT_NOT_FOUND"]
    J -- "Yes" --> K["Create player_matchup row"]
```

## 8. Stats recalculation flow

```mermaid
flowchart TD
    A["Read all player_matchups for one account"] --> B["Group by enemyChampionId + userChampionId + role"]
    B --> C["Compute games / wins / losses / averages"]
    C --> D["Take latest 5 rows for recent performance"]
    D --> E["Run RecommendationScoringService"]
    E --> F["Write personalScore + confidence"]
    F --> G["Upsert personal_matchup_stats rows"]
    G --> H["Delete stale stats rows for that account"]
```

## 9. Counters page read flow

```mermaid
sequenceDiagram
    participant U as User
    participant FE as Frontend
    participant API as Backend
    participant DB as PostgreSQL

    U->>FE: Search enemy champion name
    FE->>API: GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters
    API->>DB: Read personal_matchup_stats for account + enemy champion
    DB-->>API: Stored counter rows
    API->>API: Sort by personalScore desc
    API->>API: Derive status from score + confidence + games
    API-->>FE: Counter list
```

## 10. Matchup detail read flow

```mermaid
sequenceDiagram
    participant U as User
    participant FE as Frontend
    participant API as Backend
    participant DB as PostgreSQL

    U->>FE: Open one champion-vs-champion matchup
    FE->>API: GET /api/profiles/{summonerId}/enemies/{enemyChampionId}/counters/{userChampionId}
    API->>DB: Read matching personal_matchup_stats rows
    API->>API: Pick best role row by score, then games, then role
    API->>DB: Read latest player_matchups for selected role
    API->>API: Build recent games
    API->>API: Build item and rune recommendations from stored wins
    API-->>FE: Matchup detail response
```

## 11. Current database model

```mermaid
erDiagram
    RIOT_ACCOUNTS ||--o{ PLAYER_MATCHUPS : owns
    RIOT_ACCOUNTS ||--o{ PERSONAL_MATCHUP_STATS : owns
    RIOT_ACCOUNTS ||--o{ PERSONAL_BUILD_STATS : owns
    MATCHES ||--o{ PLAYER_MATCHUPS : source_match

    RIOT_ACCOUNTS {
        uuid id
        string puuid
        string game_name
        string tag_line
        string region
        string sync_status
        int sync_target_match_count
        int sync_backfill_cursor
        datetime sync_next_run_at
        datetime sync_last_sync_at
    }

    MATCHES {
        uuid id
        string riot_match_id
        string region
        int queue_id
        datetime game_creation
        int game_duration_seconds
        string patch
    }

    PLAYER_MATCHUPS {
        uuid id
        uuid riot_account_id
        uuid match_id
        string user_puuid
        int user_champion_id
        int enemy_champion_id
        string role
        boolean win
        int kills
        int deaths
        int assists
        int total_cs
        int gold_earned
        int total_damage_to_champions
    }

    PERSONAL_MATCHUP_STATS {
        uuid id
        uuid riot_account_id
        int enemy_champion_id
        int user_champion_id
        string role
        int games
        int wins
        int losses
        double winrate
        double personal_score
        string confidence
    }

    PERSONAL_BUILD_STATS {
        uuid id
        uuid riot_account_id
        int enemy_champion_id
        int user_champion_id
        string role
        string item_sequence
        int games
        int wins
        double build_score
    }
```

## 12. Current frontend route map

```mermaid
flowchart LR
    A["/"] --> B["Search page"]
    B --> C["/profiles/{summonerId}"]
    C --> D["Dashboard"]
    C --> E["Enemy search"]
    E --> F["/profiles/{summonerId}/enemies/{enemyChampionId}"]
    F --> G["Counters page"]
    G --> H["/profiles/{summonerId}/enemies/{enemyChampionId}/counters/{userChampionId}"]
    H --> I["Matchup detail page"]
```

## 13. What the numbers mean

```mermaid
flowchart TD
    A["Synced matches"] --> B["Matches scanned/imported into matches"]
    C["Analyzed matchups"] --> D["Matches that produced a usable player_matchups row"]
    E["Why they differ"] --> F["Role missing"]
    E --> G["Opponent role not found"]
    E --> H["Earlier strict extraction left gaps"]
    E --> I["Repair sync is now used when coverage is suspiciously low"]
```

