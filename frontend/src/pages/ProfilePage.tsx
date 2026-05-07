import type { FormEvent } from 'react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../lib/api/client'
import { getProfileDashboard, importMatchHistory } from '../lib/api/comfortpick'
import type {
  ImportMatchHistoryResponse,
  ProfileDashboardResponse,
  SearchSummonerResponse,
} from '../lib/api/comfortpick'

type NavigationState = {
  importResult?: ImportMatchHistoryResponse
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
  const [importSummaryState, setImportSummaryState] = useState<ImportMatchHistoryResponse | null>(
    navigationState?.importResult ?? null,
  )
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

  const importSummary = useMemo(() => {
    const result = importSummaryState
    if (!result) return null

    return `${result.importedMatchCount} new matches, ${result.existingMatchCount} already stored, ${result.importedMatchupCount} matchup rows added, ${result.skippedMatchupCount} skipped.`
  }, [importSummaryState])

  async function refreshProfile() {
    if (!summonerId) {
      return
    }

    setRefreshStatus('refreshing')
    setEntryError(null)

    try {
      const importResult = await importMatchHistory(summonerId)
      const refreshedDashboard = await getProfileDashboard(summonerId)
      setImportSummaryState(importResult)
      setDashboard(refreshedDashboard)
      setStatus('ready')
    } catch (error) {
      setStatus('error')
      setErrorMessage(getProfileErrorMessage(error))
    } finally {
      setRefreshStatus('idle')
    }
  }

  function handleEnemySubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!summonerId) {
      return
    }

    const normalizedValue = Number(enemyChampionInput.trim())
    if (!Number.isInteger(normalizedValue) || normalizedValue <= 0) {
      setEntryError('Enter a positive champion id.')
      return
    }

    setEntryError(null)
    navigate(`/profiles/${summonerId}/enemies/${normalizedValue}`)
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
            Routing region {dashboard.summoner.region}. This profile page is fed from stored
            dashboard data after the import cycle completes.
          </p>
        </div>
        <div className="toolbar-actions">
          <button
            className="secondary-button"
            type="button"
            onClick={refreshProfile}
            disabled={refreshStatus === 'refreshing'}
          >
            {refreshStatus === 'refreshing' ? 'Refreshing...' : 'Refresh matches'}
          </button>
          <Link className="secondary-link" to="/">
            Analyze another account
          </Link>
        </div>
      </header>

      {importSummary && (
        <div className="import-banner" role="status">
          <strong>Latest import</strong>
          <span>{importSummary}</span>
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
      </div>

      <section className="dashboard-panel">
        <div className="panel-heading">
          <h2>Find counters for one enemy champion</h2>
          <p>Enter a champion id to load the stored personal counter ranking.</p>
        </div>

        <form className="inline-form" onSubmit={handleEnemySubmit} noValidate>
          <label className="field inline-field">
            <span>Enemy champion id</span>
            <input
              name="enemyChampionId"
              inputMode="numeric"
              value={enemyChampionInput}
              onChange={(event) => setEnemyChampionInput(event.target.value)}
              placeholder="238"
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
      </section>

      {!hasAnalysis ? (
        <section className="empty-panel">
          <p className="section-label">No stored analysis yet</p>
          <h2>This summoner exists locally, but no usable matchup history has been imported yet.</h2>
          <p className="state-copy">
            Run refresh after your next import cycle, or start loading counters directly once
            matchup stats exist.
          </p>
        </section>
      ) : (
        <div className="dashboard-grid">
          <section className="dashboard-panel">
            <div className="panel-heading">
              <h2>Most played champions</h2>
              <p>Top stored matchup counts by champion id.</p>
            </div>
            <ul className="stack-list">
              {dashboard.mostPlayedChampions.map((entry) => (
                <li key={entry.championId}>
                  <strong>Champion {entry.championId}</strong>
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
                  <strong>Champion {entry.userChampionId} into {entry.enemyChampionId}</strong>
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
                  <strong>Champion {entry.userChampionId} into {entry.enemyChampionId}</strong>
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
