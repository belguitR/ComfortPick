import { apiRequest } from './client'

export type RoutingRegion = 'AMERICAS' | 'ASIA' | 'EUROPE' | 'SEA'

export type SearchSummonerResponse = {
  id: string
  puuid: string
  gameName: string
  tagLine: string
  region: string
  source: 'DATABASE' | 'RIOT_API'
}

export type ImportMatchHistoryResponse = {
  importedMatchCount: number
  existingMatchCount: number
  importedMatchupCount: number
  skippedMatchupCount: number
}

export type ProfileDashboardResponse = {
  summoner: {
    id: string
    gameName: string
    tagLine: string
    region: string
  }
  analyzedMatches: number
  mainRole: string | null
  mostPlayedChampions: Array<{
    championId: number
    games: number
  }>
  bestCounters: Array<{
    enemyChampionId: number
    userChampionId: number
    role: string
    games: number
    winrate: number
    personalScore: number
  }>
  worstMatchups: Array<{
    enemyChampionId: number
    userChampionId: number
    role: string
    games: number
    winrate: number
    personalScore: number
  }>
  lastUpdateAt: string | null
}

export type PersonalCounterResponse = {
  enemyChampionId: number
  userChampionId: number
  role: string
  games: number
  wins: number
  losses: number
  winrate: number
  averageKda: number
  averageCs: number | null
  averageGold: number | null
  averageDamage: number | null
  personalScore: number
  confidence: 'NO_DATA' | 'LOW' | 'MEDIUM' | 'HIGH'
  status: 'BEST_PICK' | 'GOOD_PICK' | 'OK_PICK' | 'LOW_DATA' | 'AVOID' | 'NO_DATA'
}

export type PersonalCountersResponse = {
  counters: PersonalCounterResponse[]
}

export type PersonalMatchupDetailResponse = {
  hasData: boolean
  enemyChampionId: number
  userChampionId: number
  role: string | null
  games: number
  wins: number
  losses: number
  winrate: number
  averageKda: number | null
  averageCs: number | null
  averageGold: number | null
  averageDamage: number | null
  personalScore: number | null
  confidence: 'NO_DATA' | 'LOW' | 'MEDIUM' | 'HIGH'
  status: 'BEST_PICK' | 'GOOD_PICK' | 'OK_PICK' | 'LOW_DATA' | 'AVOID' | 'NO_DATA'
  reasoning: string
  lastUpdatedAt: string | null
  recentGames: Array<{
    riotMatchId: string
    gameCreation: string
    win: boolean
    kills: number
    deaths: number
    assists: number
    totalCs: number | null
    goldEarned: number | null
    totalDamageToChampions: number | null
  }>
  build: {
    firstCompletedItemId: number | null
    firstCompletedItemGames: number
    itemSet: string | null
    itemSetGames: number
    score: number | null
  }
  runes: {
    primaryRuneId: number | null
    primaryRuneGames: number
    secondaryRuneId: number | null
    secondaryRuneGames: number
    score: number | null
  }
}

export async function searchSummoner(input: {
  region: RoutingRegion
  gameName: string
  tagLine: string
}): Promise<SearchSummonerResponse> {
  return apiRequest<SearchSummonerResponse>(
    `/api/summoners/${encodeURIComponent(input.region)}/${encodeURIComponent(input.gameName)}/${encodeURIComponent(input.tagLine)}`,
  )
}

export async function importMatchHistory(summonerId: string): Promise<ImportMatchHistoryResponse> {
  return apiRequest<ImportMatchHistoryResponse>(
    `/api/summoners/${encodeURIComponent(summonerId)}/matches/import`,
    {
      method: 'POST',
    },
  )
}

export async function getProfileDashboard(summonerId: string): Promise<ProfileDashboardResponse> {
  return apiRequest<ProfileDashboardResponse>(
    `/api/profiles/${encodeURIComponent(summonerId)}`,
  )
}

export async function getEnemyChampionCounters(
  summonerId: string,
  enemyChampionId: number,
): Promise<PersonalCountersResponse> {
  return apiRequest<PersonalCountersResponse>(
    `/api/profiles/${encodeURIComponent(summonerId)}/enemies/${encodeURIComponent(String(enemyChampionId))}/counters`,
  )
}

export async function getPersonalMatchupDetail(
  summonerId: string,
  enemyChampionId: number,
  userChampionId: number,
): Promise<PersonalMatchupDetailResponse> {
  return apiRequest<PersonalMatchupDetailResponse>(
    `/api/profiles/${encodeURIComponent(summonerId)}/enemies/${encodeURIComponent(String(enemyChampionId))}/counters/${encodeURIComponent(String(userChampionId))}`,
  )
}
