import type { FormEvent } from 'react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../lib/api/client'
import { getProfileDashboard, requestProfileSync } from '../lib/api/comfortpick'
import { CHAMPIONS, getChampionById, resolveChampionQuery } from '../lib/champions'
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

    const loadDashboard = async () => {
      try {
        const response = await getProfileDashboard(summonerId)
        if (!active) return
        setDashboard(response)
        setStatus('ready')
      } catch (error: unknown) {
        if (!active) return
        setStatus('error')
        setErrorMessage(getProfileErrorMessage(error))
      }
    }

    void loadDashboard()

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
        setSyncWarning('The background sync request did not complete.')
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
        .catch((error: unknown) => {
          setErrorMessage(getProfileErrorMessage(error))
        })
    }, intervalMs)

    return () => {
      window.clearInterval(handle)
    }
  }, [dashboard, summonerId])

  const syncSummary = useMemo(() => {
    if (!dashboard) return null

    const lastSyncText = dashboard.sync.lastSyncAt
      ? `Last sync ${formatDateTime(dashboard.sync.lastSyncAt)}.`
      : 'No sync completed yet.'

    return `${dashboard.sync.backfillCursor}/${dashboard.sync.targetMatchCount} history slots scanned. ${dashboard.sync.remainingMatchCount} remaining to target. ${lastSyncText}`
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
      setEntryError('Enter a valid champion name.')
      return
    }

    setEntryError(null)
    navigate(`/profiles/${summonerId}/enemies/${champion.id}`)
  }

  if (missingSummonerId) {
    return (
      <section className="profile-state">
        <p className="section-label">Profile error</p>
        <h1>Could not load this profile</h1>
        <p className="state-copy">Missing summoner id.</p>
        <Link className="inline-link" to="/">
          Back to search
        </Link>
      </section>
    )
  }

  if (status === 'loading') {
    return (
      <section className="profile-state">
        <p className="section-label">Profile loading</p>
        <h1>Loading stored profile</h1>
        <p className="state-copy">Fetching precomputed dashboard data from the backend.</p>
      </section>
    )
  }

  if (status === 'error' || dashboard == null) {
    return (
      <section className="profile-state">
        <p className="section-label">Profile error</p>
        <h1>Could not load this profile</h1>
        <p className="state-copy">{errorMessage}</p>
        <Link className="inline-link" to="/">
          Back to search
        </Link>
      </section>
    )
  }

  const hasAnalysis =
    dashboard.analyzedMatches > 0 ||
    dashboard.bestCounters.length > 0 ||
    dashboard.worstMatchups.length > 0

  return (
    <section className="profile-layout">
      <header className="profile-header">
        <div>
          <p className="section-label">Stored profile</p>
          <h1>
            {dashboard.summoner.gameName}
            <span>#{dashboard.summoner.tagLine}</span>
          </h1>
          <p className="hero-copy">
            Routing region {dashboard.summoner.region}. The page reads from stored data while
            the backend worker keeps pulling 10-match batches toward the target history depth.
          </p>
        </div>
        <div className="toolbar-actions">
          <button
            className="secondary-button"
            type="button"
            onClick={refreshProfile}
            disabled={refreshStatus === 'refreshing'}
          >
            {refreshStatus === 'refreshing' ? 'Queueing sync...' : 'Check for new matches'}
          </button>
          <Link className="secondary-link" to="/">
            Analyze another account
          </Link>
        </div>
      </header>

      {syncSummary && (
        <div className="import-banner" role="status">
          <strong>Background sync</strong>
          <span>{syncSummary}</span>
        </div>
      )}

      {syncWarning && (
        <div className="form-alert" role="status">
          {syncWarning}
        </div>
      )}

      <div className="stat-strip" aria-label="Profile summary">
        <article className="stat-card">
          <span>Analyzed matchups</span>
          <strong>{dashboard.analyzedMatches}</strong>
        </article>
        <article className="stat-card">
          <span>Main role</span>
          <strong>{dashboard.mainRole ?? 'Unknown'}</strong>
        </article>
        <article className="stat-card">
          <span>Last update</span>
          <strong>{dashboard.lastUpdateAt ? formatDateTime(dashboard.lastUpdateAt) : 'Not imported yet'}</strong>
        </article>
        <article className="stat-card">
          <span>Sync status</span>
          <strong>{formatSyncStatus(dashboard.sync.status)}</strong>
        </article>
      </div>

      <section className="dashboard-panel">
        <div className="panel-heading">
          <h2>Find counters for one enemy champion</h2>
          <p>Enter a champion name to load the stored personal counter ranking.</p>
        </div>

        <form className="inline-form" onSubmit={handleEnemySubmit} noValidate>
          <label className="field inline-field">
            <span>Enemy champion</span>
            <input
              name="enemyChampion"
              value={enemyChampionInput}
              onChange={(event) => setEnemyChampionInput(event.target.value)}
              placeholder="Zed"
              list="champion-options"
            />
          </label>
          <button className="primary-button compact-button" type="submit">
            Open counters
          </button>
        </form>

        {entryError && (
          <div className="form-alert inline-alert" role="alert">
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
        <section className="empty-panel">
          <p className="section-label">No stored analysis yet</p>
          <h2>This summoner exists locally, but no usable matchup history has been imported yet.</h2>
          <p className="state-copy">
            Keep this profile open while the background sync keeps pulling 10-match batches, or
            manually queue another head check after the next game.
          </p>
        </section>
      ) : (
        <div className="dashboard-grid">
          <section className="dashboard-panel">
            <div className="panel-heading">
              <h2>Most played champions</h2>
              <p>Top stored matchup counts by champion.</p>
            </div>
            <ul className="stack-list">
              {dashboard.mostPlayedChampions.map((entry) => (
                <li key={entry.championId}>
                  <strong className="champion-cell">
                    <img
                      className="champion-avatar"
                      src={getChampionById(entry.championId)?.image}
                      alt=""
                    />
                    <span>{getChampionById(entry.championId)?.name ?? `Champion ${entry.championId}`}</span>
                  </strong>
                  <span>{entry.games} games</span>
                </li>
              ))}
            </ul>
          </section>

          <section className="dashboard-panel">
            <div className="panel-heading">
              <h2>Best counters</h2>
              <p>Top stored recommendations by personal score.</p>
            </div>
            <ul className="stack-list">
              {dashboard.bestCounters.map((entry) => (
                <li key={`${entry.enemyChampionId}-${entry.userChampionId}-${entry.role}`}>
                  <strong>
                    {getChampionById(entry.userChampionId)?.name ?? `Champion ${entry.userChampionId}`} into {getChampionById(entry.enemyChampionId)?.name ?? `Champion ${entry.enemyChampionId}`}
                  </strong>
                  <span>{entry.personalScore.toFixed(1)} score | {entry.games} games | {entry.role}</span>
                </li>
              ))}
            </ul>
          </section>

          <section className="dashboard-panel">
            <div className="panel-heading">
              <h2>Worst matchups</h2>
              <p>Lowest stored personal scores.</p>
            </div>
            <ul className="stack-list">
              {dashboard.worstMatchups.map((entry) => (
                <li key={`${entry.enemyChampionId}-${entry.userChampionId}-${entry.role}`}>
                  <strong>
                    {getChampionById(entry.userChampionId)?.name ?? `Champion ${entry.userChampionId}`} into {getChampionById(entry.enemyChampionId)?.name ?? `Champion ${entry.enemyChampionId}`}
                  </strong>
                  <span>{entry.personalScore.toFixed(1)} score | {entry.games} games | {entry.role}</span>
                </li>
              ))}
            </ul>
          </section>
        </div>
      )}
    </section>
  )
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatSyncStatus(status: ProfileDashboardResponse['sync']['status']): string {
  switch (status) {
    case 'ACTIVE':
      return 'Queued'
    case 'RUNNING':
      return 'Running'
    case 'RATE_LIMITED':
      return 'Rate limited'
    case 'FAILED':
      return 'Retry scheduled'
    case 'COMPLETE':
      return 'Target reached'
    default:
      return 'Idle'
  }
}

function getProfileErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'SUMMONER_PROFILE_NOT_FOUND') {
      return 'This stored summoner id does not exist in the backend.'
    }

    if (error.code === 'RIOT_API_UNAUTHORIZED') {
      return 'The Riot API key is unavailable or invalid on the backend.'
    }

    if (error.code === 'RIOT_API_RATE_LIMIT') {
      return error.retryAfterSeconds != null
        ? `Riot rate limit reached. Retry in about ${error.retryAfterSeconds} seconds.`
        : 'Riot rate limit reached. Retry in a moment.'
    }

    return error.message
  }

  return 'Unexpected frontend error while loading the profile.'
}
