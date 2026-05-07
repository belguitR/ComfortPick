import type { FormEvent } from 'react'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../lib/api/client'
import { requestProfileSync, searchSummoner } from '../lib/api/comfortpick'
import type { RoutingRegion, SearchSummonerResponse } from '../lib/api/comfortpick'

type SearchFormState = {
  gameName: string
  tagLine: string
  region: RoutingRegion
}

type SearchPhase = 'idle' | 'searching' | 'queueing'

const REGION_OPTIONS: Array<{ value: RoutingRegion; label: string; detail: string }> = [
  { value: 'EUROPE', label: 'Europe', detail: 'EUW, EUNE, TR, RU' },
  { value: 'AMERICAS', label: 'Americas', detail: 'NA, BR, LAN, LAS' },
  { value: 'ASIA', label: 'Asia', detail: 'KR, JP' },
  { value: 'SEA', label: 'SEA', detail: 'SG, PH, TH, TW, VN' },
]

const DEFAULT_FORM: SearchFormState = {
  gameName: '',
  tagLine: '',
  region: 'EUROPE',
}

export function SearchPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<SearchFormState>(DEFAULT_FORM)
  const [phase, setPhase] = useState<SearchPhase>('idle')
  const [validationError, setValidationError] = useState<string | null>(null)
  const [requestError, setRequestError] = useState<string | null>(null)

  const isSubmitting = phase !== 'idle'
  const submitLabel = useMemo(() => {
    if (phase === 'searching') return 'Searching Riot account...'
    if (phase === 'queueing') return 'Queueing background sync...'
    return 'Analyze profile'
  }, [phase])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const gameName = form.gameName.trim()
    const tagLine = form.tagLine.trim()
    if (!gameName || !tagLine) {
      setValidationError('Game name and tagline are required.')
      setRequestError(null)
      return
    }

    setValidationError(null)
    setRequestError(null)

    try {
      setPhase('searching')
      const summoner = await searchSummoner({
        region: form.region,
        gameName,
        tagLine,
      })

      setPhase('queueing')
      const syncState = await tryQueueSyncOrCaptureError(summoner)
      navigate(`/profiles/${summoner.id}`, { state: syncState })
    } catch (error) {
      setRequestError(getSearchErrorMessage(error))
      setPhase('idle')
      return
    }

    setPhase('idle')
  }

  return (
    <section className="search-layout" aria-labelledby="search-title">
      <div className="hero-panel">
        <p className="section-label">Stored personal matchup engine</p>
        <h1 id="search-title">Search a Riot account, queue sync, then draft from your own history.</h1>
        <p className="hero-copy">
          ComfortPick stores summoner history locally, recalculates personal matchup results,
          and surfaces which champions actually work for that player instead of leaning on
          generic counter charts.
        </p>

        <div className="feature-grid" aria-label="Workflow summary">
          <article className="feature-tile">
            <span className="feature-eyebrow">1. Search</span>
            <strong>Resolve Riot identity with cache reuse</strong>
            <p>Fresh accounts return from the database before Riot is touched again.</p>
          </article>
          <article className="feature-tile">
            <span className="feature-eyebrow">2. Sync</span>
            <strong>Backfill in controlled 10-match batches</strong>
            <p>A background worker keeps walking deeper into history instead of forcing one large import.</p>
          </article>
          <article className="feature-tile">
            <span className="feature-eyebrow">3. Use</span>
            <strong>Read counters from precomputed stats</strong>
            <p>Profile, counter, and matchup reads stay database-first while sync continues.</p>
          </article>
        </div>
      </div>

      <form className="search-card" onSubmit={handleSubmit} noValidate>
        <div className="card-heading">
          <p className="section-label">Analyze summoner</p>
          <h2>Queue background history sync</h2>
          <p>Search the Riot account, queue a background sync, and land on the stored profile immediately.</p>
        </div>

        <label className="field">
          <span>Game name</span>
          <input
            name="gameName"
            autoComplete="off"
            placeholder="Rami"
            value={form.gameName}
            onChange={(event) => setForm((current) => ({ ...current, gameName: event.target.value }))}
            disabled={isSubmitting}
          />
        </label>

        <label className="field">
          <span>Tagline</span>
          <input
            name="tagLine"
            autoComplete="off"
            placeholder="EUW"
            value={form.tagLine}
            onChange={(event) => setForm((current) => ({ ...current, tagLine: event.target.value }))}
            disabled={isSubmitting}
          />
        </label>

        <label className="field">
          <span>Routing region</span>
          <select
            name="region"
            value={form.region}
            onChange={(event) =>
              setForm((current) => ({ ...current, region: event.target.value as RoutingRegion }))
            }
            disabled={isSubmitting}
          >
            {REGION_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label} - {option.detail}
              </option>
            ))}
          </select>
        </label>

        {(validationError || requestError) && (
          <div className="form-alert" role="alert">
            {validationError ?? requestError}
          </div>
        )}

        <button className="primary-button" type="submit" disabled={isSubmitting}>
          {submitLabel}
        </button>

        <div className="request-note">
          <span>Backend behavior</span>
          <ul>
            <li>1 Riot account lookup only when the profile is missing or stale.</li>
            <li>Profile open queues a background sync that pulls 10-match batches toward a 500-match target.</li>
            <li>Profile reads stay on the database while the sync worker keeps backfilling.</li>
          </ul>
        </div>
      </form>
    </section>
  )
}

async function tryQueueSyncOrCaptureError(
  summoner: SearchSummonerResponse,
): Promise<{
  syncWarning?: string
  summoner: SearchSummonerResponse
}> {
  try {
    await requestProfileSync(summoner.id)

    return {
      summoner,
    }
  } catch (error) {
    if (error instanceof ApiError) {
      return {
        syncWarning: 'Profile opened, but the background sync request did not complete.',
        summoner,
      }
    }

    throw error
  }
}

function getSearchErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'SUMMONER_NOT_FOUND':
        return 'Riot could not find that account for the selected routing region.'
      case 'RIOT_API_RATE_LIMIT':
        return error.retryAfterSeconds != null
          ? `Riot rate limit reached. Retry in about ${error.retryAfterSeconds} seconds.`
          : 'Riot rate limit reached. Retry in a moment.'
      case 'RIOT_API_UNAUTHORIZED':
        return 'The Riot API key is unavailable or invalid on the backend.'
      case 'RIOT_API_UNAVAILABLE':
        return 'Riot API is temporarily unavailable.'
      case 'BAD_REQUEST':
        return error.message
      default:
        return error.message
    }
  }

  return 'Unexpected frontend error while starting analysis.'
}
