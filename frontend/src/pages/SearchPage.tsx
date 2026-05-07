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

const REGION_OPTIONS: Array<{ value: RoutingRegion; label: string }> = [
  { value: 'EUROPE', label: 'Europe West' },
  { value: 'AMERICAS', label: 'North America' },
  { value: 'ASIA', label: 'Korea / Japan' },
  { value: 'SEA', label: 'Southeast Asia' },
]

const DEFAULT_FORM: SearchFormState = {
  gameName: '',
  tagLine: '',
  region: 'EUROPE',
}

const HERO_IMAGE = 'https://ddragon.leagueoflegends.com/cdn/img/champion/splash/Ahri_0.jpg'

export function SearchPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<SearchFormState>(DEFAULT_FORM)
  const [phase, setPhase] = useState<SearchPhase>('idle')
  const [validationError, setValidationError] = useState<string | null>(null)
  const [requestError, setRequestError] = useState<string | null>(null)

  const isSubmitting = phase !== 'idle'
  const submitLabel = useMemo(() => {
    if (phase === 'searching') return 'Finding account...'
    if (phase === 'queueing') return 'Starting sync...'
    return 'Sync History'
  }, [phase])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const gameName = form.gameName.trim()
    const tagLine = form.tagLine.trim()
    if (!gameName || !tagLine) {
      setValidationError('Enter both game name and tagline.')
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
    <section className="search-page">
      <section className="landing-hero">
        <div className="landing-hero-copy">
          <h1>Search a Riot account, sync the history, then draft from what actually works for you.</h1>
          <p>
            ComfortPick turns your own match history into personal counter picks, matchup reads,
            and build guidance.
          </p>
        </div>

        <form className="hero-search-card" onSubmit={handleSubmit} noValidate>
          <label className="hero-field">
            <span>Game Name</span>
            <input
              name="gameName"
              autoComplete="off"
              placeholder="Rami"
              value={form.gameName}
              onChange={(event) => setForm((current) => ({ ...current, gameName: event.target.value }))}
              disabled={isSubmitting}
            />
          </label>

          <label className="hero-field">
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

          <label className="hero-field">
            <span>Region</span>
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
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <button className="hero-submit-button" type="submit" disabled={isSubmitting}>
            {submitLabel}
          </button>
        </form>

        {(validationError || requestError) && (
          <div className="page-alert" role="alert">
            {validationError ?? requestError}
          </div>
        )}
      </section>

      <section className="landing-steps" aria-label="How it works">
        <article className="landing-step-card">
          <div className="step-icon">S</div>
          <strong>1. Search</strong>
          <p>Open any Riot account and pull its personal draft history into one place.</p>
        </article>
        <article className="landing-step-card">
          <div className="step-icon">Y</div>
          <strong>2. Sync</strong>
          <p>We keep building the profile in the background so the dashboard gets richer over time.</p>
        </article>
        <article className="landing-step-card">
          <div className="step-icon">P</div>
          <strong>3. Pick</strong>
          <p>Read your strongest counters, weak lanes, builds, and runes before the draft locks in.</p>
        </article>
      </section>

      <section className="landing-promo-grid">
        <article
          className="landing-promo-hero"
          style={{ backgroundImage: `linear-gradient(180deg, rgba(17,17,24,0.12), rgba(17,17,24,0.82)), url(${HERO_IMAGE})` }}
        >
          <span>Personal Drafting Engine</span>
          <strong>Counter picks backed by your own games, not generic ladder averages.</strong>
        </article>

        <article className="landing-promo-stat">
          <div className="promo-dot promo-dot-green" aria-hidden="true" />
          <strong>Fast reads</strong>
          <p>Profiles open first, then keep filling in as more match history syncs.</p>
        </article>

        <article className="landing-promo-stat">
          <div className="promo-dot promo-dot-purple" aria-hidden="true" />
          <strong>Built for draft</strong>
          <p>Enemy champion lookup, personal counters, and matchup detail stay one click away.</p>
        </article>
      </section>

      <footer className="page-footer">
        <div>
          <strong>ComfortPick Analytics</strong>
          <p>ComfortPick uses your match history to surface the picks you perform best on.</p>
        </div>
        <div className="page-footer-links">
          <span>Privacy Policy</span>
          <span>Terms of Service</span>
          <span>Support</span>
        </div>
      </footer>
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
        syncWarning: 'Profile opened. Match history will keep syncing shortly.',
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
        return 'We could not find that Riot account. Check the game name, tagline, and region.'
      case 'RIOT_API_RATE_LIMIT':
        return error.retryAfterSeconds != null
          ? `Too many requests right now. Try again in about ${error.retryAfterSeconds} seconds.`
          : 'Too many requests right now. Try again in a moment.'
      case 'RIOT_API_UNAUTHORIZED':
      case 'RIOT_API_UNAVAILABLE':
        return 'Account lookup is temporarily unavailable.'
      case 'BAD_REQUEST':
        return error.message
      default:
        return 'We could not start that search right now.'
    }
  }

  return 'We could not start that search right now.'
}
