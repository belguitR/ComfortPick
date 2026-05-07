import type { FormEvent } from 'react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../lib/api/client'
import { getProfileDashboard, requestProfileSync } from '../lib/api/comfortpick'
import { CHAMPIONS, getChampionById, resolveChampionQuery } from '../lib/champions'
import { getItemById } from '../lib/items'
import { getRuneById } from '../lib/runes'
import { getSummonerSpellById } from '../lib/spells'
import type { ProfileDashboardResponse, SearchSummonerResponse } from '../lib/api/comfortpick'

type NavigationState = {
  syncWarning?: string
  summoner?: SearchSummonerResponse
}

export function ProfilePage() {
  const navigate = useNavigate()
  const { summonerId } = useParams<{ summonerId: string }>()
  const location = useLocation()
  const navigationState = (location.state as NavigationState | null) ?? null
  const [dashboard, setDashboard] = useState<ProfileDashboardResponse | null>(null)
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [refreshStatus, setRefreshStatus] = useState<'idle' | 'refreshing'>('idle')
  const [enemyChampionInput, setEnemyChampionInput] = useState('')
  const [entryError, setEntryError] = useState<string | null>(null)
  const [syncWarning, setSyncWarning] = useState<string | null>(navigationState?.syncWarning ?? null)
  const syncRequestedRef = useRef(false)
  const missingSummonerId = !summonerId

  useEffect(() => {
    if (missingSummonerId || !summonerId) {
      return
    }

    let active = true

    void getProfileDashboard(summonerId)
      .then((response) => {
        if (!active) return
        setDashboard(response)
        setStatus('ready')
      })
      .catch((error: unknown) => {
        if (!active) return
        setStatus('error')
        setErrorMessage(getProfileErrorMessage(error))
      })

    return () => {
      active = false
    }
  }, [missingSummonerId, summonerId])

  useEffect(() => {
    if (!summonerId || syncRequestedRef.current) {
      return
    }

    syncRequestedRef.current = true
    void requestProfileSync(summonerId).catch((error: unknown) => {
      if (error instanceof ApiError) {
        setSyncWarning(getProfileErrorMessage(error))
      } else {
        setSyncWarning('History is taking longer than usual to refresh.')
      }
    })
  }, [summonerId])

  useEffect(() => {
    if (!summonerId || !dashboard) {
      return
    }

    const shouldPoll = dashboard.sync.enabled && (
      dashboard.sync.status === 'ACTIVE' ||
      dashboard.sync.status === 'RUNNING' ||
      dashboard.sync.status === 'RATE_LIMITED'
    )
    if (!shouldPoll) {
      return
    }

    const intervalMs = Math.max(dashboard.sync.dashboardPollIntervalSeconds, 5) * 1000
    const handle = window.setInterval(() => {
      void getProfileDashboard(summonerId)
        .then((response) => {
          setDashboard(response)
        })
        .catch(() => {
          setSyncWarning('Live progress is delayed. The profile will update again shortly.')
        })
    }, intervalMs)

    return () => {
      window.clearInterval(handle)
    }
  }, [dashboard, summonerId])

  const syncSummary = useMemo(() => {
    if (!dashboard) return null

    const syncedMatches = Math.max(dashboard.sync.targetMatchCount - dashboard.sync.remainingMatchCount, 0)
    return `${syncedMatches} of ${dashboard.sync.targetMatchCount} matches synced`
  }, [dashboard])

  async function refreshProfile() {
    if (!summonerId) {
      return
    }

    setRefreshStatus('refreshing')
    setEntryError(null)

    try {
      await requestProfileSync(summonerId)
      const refreshedDashboard = await getProfileDashboard(summonerId)
      setSyncWarning(null)
      setDashboard(refreshedDashboard)
      setStatus('ready')
    } catch (error) {
      if (error instanceof ApiError) {
        setSyncWarning(getProfileErrorMessage(error))
      } else {
        setStatus('error')
        setErrorMessage(getProfileErrorMessage(error))
      }
    } finally {
      setRefreshStatus('idle')
    }
  }

  function handleEnemySubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!summonerId) {
      return
    }

    const champion = resolveChampionQuery(enemyChampionInput.trim())
    if (!champion) {
      setEntryError('Enter an enemy champion name to continue.')
      return
    }

    setEntryError(null)
    navigate(`/profiles/${summonerId}/enemies/${champion.id}`)
  }

  if (missingSummonerId) {
    return (
      <section className="workspace-state">
        <h1>We could not open that profile.</h1>
        <p>Please go back and search for the account again.</p>
        <Link className="ghost-link" to="/">
          Back to search
        </Link>
      </section>
    )
  }

  if (status === 'loading') {
    return (
      <section className="workspace-state">
        <h1>Loading profile</h1>
        <p>Pulling the latest dashboard view.</p>
      </section>
    )
  }

  if (status === 'error' || dashboard == null) {
    return (
      <section className="workspace-state">
        <h1>We could not open that profile.</h1>
        <p>{errorMessage}</p>
        <Link className="ghost-link" to="/">
          Back to search
        </Link>
      </section>
    )
  }

  const hasAnalysis =
    dashboard.analyzedMatches > 0 ||
    dashboard.latestGames.length > 0
  const primaryChampion = dashboard.mostPlayedChampions[0]
  const syncTone = getSyncTone(dashboard.sync.status)
  const showSyncBanner = dashboard.sync.status !== 'COMPLETE' || syncWarning != null

  return (
    <section className="workspace-layout">
      <header className="workspace-header">
        <div className="workspace-header-copy">
          <h1 className="workspace-title">
            {dashboard.summoner.gameName} <span>#{dashboard.summoner.tagLine}</span>
          </h1>
          <p className="workspace-subtitle">
            {formatRegion(dashboard.summoner.region)} | Personal draft dashboard
          </p>
        </div>

        <div className="workspace-header-actions">
          <button
            className="workspace-action-button secondary-action"
            type="button"
            onClick={refreshProfile}
            disabled={refreshStatus === 'refreshing'}
          >
            {refreshStatus === 'refreshing' ? 'Refreshing...' : 'Refresh'}
          </button>
          <Link className="workspace-action-button" to="/">
            New Search
          </Link>
        </div>
      </header>

      <section className="workspace-summary-strip" aria-label="Profile summary">
        <article className="summary-metric-card">
          <span>Analyzed Matchups</span>
          <strong>{dashboard.analyzedMatches}</strong>
        </article>
        <article className="summary-metric-card">
          <span>Main Role</span>
          <strong>{formatRole(dashboard.mainRole)}</strong>
        </article>
        <article className="summary-metric-card">
          <span>Last Update</span>
          <strong>{dashboard.lastUpdateAt ? formatRelativeOrDate(dashboard.lastUpdateAt) : 'In progress'}</strong>
        </article>
        <article className="summary-metric-card">
          <span>Sync Status</span>
          <strong className={`status-pill status-pill-${syncTone}`}>{formatSyncStatus(dashboard.sync.status)}</strong>
        </article>
      </section>

      {showSyncBanner && (
        <section className="workspace-banner">
          {dashboard.sync.status !== 'COMPLETE' && syncSummary && <strong>{syncSummary}</strong>}
          {syncWarning && <span>{syncWarning}</span>}
        </section>
      )}

      <section id="counter-search" className="dashboard-search-panel">
        <div className="dashboard-search-copy">
          <h2>Pick your opponent</h2>
          <p>Search an enemy champion and open the personal counter list for that lane.</p>
        </div>

        <form className="dashboard-search-form" onSubmit={handleEnemySubmit} noValidate>
          <input
            name="enemyChampion"
            value={enemyChampionInput}
            onChange={(event) => setEnemyChampionInput(event.target.value)}
            placeholder="Search enemy champion (for example Zed or Ahri)"
            list="champion-options"
          />
          <button className="dashboard-search-button" type="submit">
            Find Counters
          </button>
        </form>

        {entryError && (
          <div className="page-alert compact-alert" role="alert">
            {entryError}
          </div>
        )}

        <datalist id="champion-options">
          {CHAMPIONS.map((champion) => (
            <option key={champion.id} value={champion.name} />
          ))}
        </datalist>
      </section>

      {!hasAnalysis ? (
        <section className="workspace-empty-card">
          <h2>We are still building this profile.</h2>
          <p>Match history is still syncing. Check back shortly for counters and matchup details.</p>
        </section>
      ) : (
        <section className="insight-grid">
          <article className="insight-card">
            <div className="insight-card-head">
              <h2>Most Played</h2>
              <span>Season view</span>
            </div>
            <div className="profile-list">
              {dashboard.mostPlayedChampions.map((entry) => {
                const champion = getChampionById(entry.championId)
                return (
                  <div key={entry.championId} className="profile-list-row">
                    <div className="profile-list-identity">
                      <img className="profile-list-avatar" src={champion?.image} alt="" />
                      <div>
                        <strong>{champion?.name ?? `Champion ${entry.championId}`}</strong>
                        <span>{entry.games} games played</span>
                      </div>
                    </div>
                    <span className="profile-list-emphasis">
                      {dashboard.analyzedMatches > 0
                        ? `${Math.round((entry.games / dashboard.analyzedMatches) * 100)}% share`
                        : `${entry.games} games`}
                    </span>
                  </div>
                )
              })}
            </div>
          </article>

          <article className="insight-card latest-games-card">
            <div className="insight-card-head">
              <h2>Latest Games</h2>
              <span>Recent analyzed history</span>
            </div>
            <div className="latest-games-list">
              {dashboard.latestGames.map((game) => {
                const champion = getChampionById(game.userChampionId)
                const kda = `${game.kills} / ${game.deaths} / ${game.assists}`
                const itemSet = game.itemIds.map((itemId) => getItemById(itemId)).filter((item) => item != null)
                const runes = [
                  game.primaryRuneId != null ? getRuneById(game.primaryRuneId) : undefined,
                  game.secondaryRuneId != null ? getRuneById(game.secondaryRuneId) : undefined,
                ].filter((rune) => rune != null)
                const spells = [getSummonerSpellById(game.summonerSpell1Id), getSummonerSpellById(game.summonerSpell2Id)].filter((spell) => spell != null)

                return (
                  <div key={game.riotMatchId} className="latest-game-row">
                    <div className="latest-game-identity">
                      <img className="latest-game-avatar" src={champion?.image} alt="" />
                      <div>
                        <strong>{champion?.name ?? `Champion ${game.userChampionId}`}</strong>
                        <span>{formatRole(game.role)} • {formatRelativeOrDate(game.gameCreation)}</span>
                      </div>
                    </div>

                    <div className="latest-game-kda">
                      <span className={`result-pill ${game.win ? 'result-win' : 'result-loss'}`}>
                        {game.win ? 'Win' : 'Loss'}
                      </span>
                      <strong>{kda}</strong>
                      <small>{formatKdaRatio(game.kills, game.deaths, game.assists)} KDA</small>
                    </div>

                    <div className="latest-game-meta">
                      <span>{game.totalCs != null ? `${game.totalCs} CS` : 'No CS'}</span>
                      <span>{game.goldEarned != null ? formatGold(game.goldEarned) : 'No gold'}</span>
                    </div>

                    <div className="latest-game-loadout">
                      <div className="latest-game-icon-strip">
                        {itemSet.slice(0, 6).map((item) => (
                          <img key={`item-${game.riotMatchId}-${item.id}`} src={item.image} alt={item.name} title={item.name} />
                        ))}
                      </div>
                      <div className="latest-game-icon-strip latest-game-icon-strip-compact">
                        {spells.map((spell) => (
                          <img key={`spell-${game.riotMatchId}-${spell.id}`} src={spell.image} alt={spell.name} title={spell.name} />
                        ))}
                        {runes.map((rune) => (
                          <img key={`rune-${game.riotMatchId}-${rune.id}`} src={rune.image} alt={rune.name} title={rune.name} />
                        ))}
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          </article>
        </section>
      )}

      <footer className="workspace-footer">
        <div className="workspace-footer-brand">
          <strong>ComfortPick Analytics</strong>
          <p>
            {primaryChampion
              ? `${dashboard.summoner.gameName}'s profile is currently led by ${getChampionById(primaryChampion.championId)?.name ?? 'their top champion'}.`
              : 'ComfortPick turns personal match history into faster draft decisions.'}
          </p>
        </div>
        <div className="workspace-footer-links">
          <span>Privacy Policy</span>
          <span>Terms of Service</span>
          <span>Support</span>
        </div>
      </footer>
    </section>
  )
}

function formatRegion(region: string): string {
  switch (region.toUpperCase()) {
    case 'AMERICAS':
      return 'North America'
    case 'EUROPE':
      return 'Europe West'
    case 'ASIA':
      return 'Korea / Japan'
    case 'SEA':
      return 'Southeast Asia'
    default:
      return region
  }
}

function formatRole(role: string | null): string {
  if (!role) return 'No main role yet'

  switch (role.toUpperCase()) {
    case 'TOP':
      return 'Top Lane'
    case 'JUNGLE':
      return 'Jungle'
    case 'MIDDLE':
      return 'Mid Lane'
    case 'BOTTOM':
      return 'Bot Lane'
    case 'UTILITY':
      return 'Support'
    default:
      return role.toLowerCase().replace('_', ' ')
  }
}

function formatRelativeOrDate(value: string): string {
  const date = new Date(value)
  const diffMs = Date.now() - date.getTime()
  const diffMinutes = Math.round(diffMs / 60000)

  if (diffMinutes < 1) return 'Just now'
  if (diffMinutes < 60) return `${diffMinutes}m ago`

  const diffHours = Math.round(diffMinutes / 60)
  if (diffHours < 24) return `${diffHours}h ago`

  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(date)
}

function formatGold(value: number): string {
  if (value >= 1000) {
    return `${(value / 1000).toFixed(1)}k gold`
  }

  return `${value} gold`
}

function formatKdaRatio(kills: number, deaths: number, assists: number): string {
  const ratio = (kills + assists) / Math.max(deaths, 1)
  return ratio.toFixed(1)
}

function formatSyncStatus(status: ProfileDashboardResponse['sync']['status']): string {
  switch (status) {
    case 'ACTIVE':
    case 'RUNNING':
      return 'Syncing'
    case 'RATE_LIMITED':
      return 'Cooling down'
    case 'FAILED':
      return 'Retrying'
    case 'COMPLETE':
      return 'Live'
    default:
      return 'Ready'
  }
}

function getSyncTone(status: ProfileDashboardResponse['sync']['status']): 'green' | 'blue' | 'amber' {
  switch (status) {
    case 'COMPLETE':
      return 'green'
    case 'RATE_LIMITED':
    case 'FAILED':
      return 'amber'
    default:
      return 'blue'
  }
}

function getProfileErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'BACKEND_UNAVAILABLE') {
      return 'ComfortPick is unavailable right now. Please retry in a moment.'
    }

    if (error.code === 'SUMMONER_PROFILE_NOT_FOUND') {
      return 'We could not find that profile.'
    }

    if (error.code === 'RIOT_API_RATE_LIMIT') {
      return error.retryAfterSeconds != null
        ? `History is updating slowly right now. Try again in about ${error.retryAfterSeconds} seconds.`
        : 'History is updating slowly right now. Try again in a moment.'
    }

    return 'This profile is temporarily unavailable.'
  }

  return 'This profile is temporarily unavailable.'
}
