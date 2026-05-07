import type { FormEvent } from 'react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../lib/api/client'
import { getEnemyChampionCounters } from '../lib/api/comfortpick'
import type { PersonalCounterResponse } from '../lib/api/comfortpick'
import { CHAMPIONS, getChampionById, resolveChampionQuery } from '../lib/champions'

const SCORE_FORMULA_TOOLTIP =
  'Score = winrate x 0.35 + champion comfort x 0.25 + KDA x 0.15 + CS/gold x 0.10 + recent performance x 0.10 + fallback x 0.05 - low sample penalty.'

export function EnemyCountersPage() {
  const navigate = useNavigate()
  const { summonerId, enemyChampionId } = useParams<{
    summonerId: string
    enemyChampionId: string
  }>()

  const parsedEnemyChampionId = useMemo(() => {
    const numericValue = Number(enemyChampionId)
    return Number.isInteger(numericValue) && numericValue > 0 ? numericValue : null
  }, [enemyChampionId])
  const requestKey = summonerId != null && parsedEnemyChampionId != null
    ? `${summonerId}:${parsedEnemyChampionId}`
    : null

  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [counters, setCounters] = useState<PersonalCounterResponse[]>([])
  const [enemyChampionInput, setEnemyChampionInput] = useState('')
  const [loadedKey, setLoadedKey] = useState<string | null>(null)
  const [showScoreFormula, setShowScoreFormula] = useState(false)

  useEffect(() => {
    if (parsedEnemyChampionId == null) {
      return
    }

    const champion = getChampionById(parsedEnemyChampionId)
    const timeoutId = window.setTimeout(() => {
      setEnemyChampionInput(champion?.name ?? '')
    }, 0)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [parsedEnemyChampionId])

  useEffect(() => {
    if (!summonerId || parsedEnemyChampionId == null) {
      return
    }

    let active = true

    void getEnemyChampionCounters(summonerId, parsedEnemyChampionId)
      .then((response) => {
        if (!active) return
        setCounters(response.counters)
        setLoadedKey(requestKey)
        setStatus('ready')
      })
      .catch((error: unknown) => {
        if (!active) return
        setLoadedKey(requestKey)
        setStatus('error')
        setErrorMessage(getCountersErrorMessage(error))
      })

    return () => {
      active = false
    }
  }, [parsedEnemyChampionId, requestKey, summonerId])

  const isLoading = requestKey != null && (status === 'loading' || loadedKey !== requestKey)

  if (!summonerId || parsedEnemyChampionId == null) {
    return (
      <section className="profile-state">
        <p className="section-label">Counters error</p>
        <h1>Could not load counters</h1>
        <p className="state-copy">The enemy champion in the route is missing or invalid.</p>
        <Link className="inline-link" to={summonerId ? `/profiles/${summonerId}` : '/'}>
          Back to profile
        </Link>
      </section>
    )
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const champion = resolveChampionQuery(enemyChampionInput.trim())
    if (!champion) {
      setStatus('error')
      setErrorMessage('Enter a valid champion name.')
      return
    }

    navigate(`/profiles/${summonerId}/enemies/${champion.id}`)
  }

  const enemyChampionName = getChampionById(parsedEnemyChampionId)?.name ?? `Champion ${parsedEnemyChampionId}`

  return (
    <section className="profile-layout">
      <header className="profile-header">
        <div>
          <p className="section-label">Enemy matchup search</p>
          <h1>
            {enemyChampionName}
            <span>Stored counters only</span>
          </h1>
          <p className="hero-copy">
            Results below come from precomputed personal matchup stats. No Riot API request is
            made for this page.
          </p>
        </div>
        <div className="toolbar-actions">
          <Link className="secondary-link" to={`/profiles/${summonerId}`}>
            Back to profile
          </Link>
        </div>
      </header>

      <section className="dashboard-panel">
        <form className="inline-form" onSubmit={handleSubmit} noValidate>
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
            Load counters
          </button>
        </form>
        <datalist id="champion-options">
          {CHAMPIONS.map((champion) => (
            <option key={champion.id} value={champion.name} />
          ))}
        </datalist>
      </section>

      {isLoading && (
        <section className="profile-state">
          <p className="section-label">Counters loading</p>
          <h2>Loading stored counter recommendations</h2>
          <p className="state-copy">Reading precomputed matchup stats from the backend.</p>
        </section>
      )}

      {!isLoading && status === 'error' && (
        <section className="profile-state">
          <p className="section-label">Counters error</p>
          <h2>Could not load counters</h2>
          <p className="state-copy">{errorMessage}</p>
        </section>
      )}

      {!isLoading && status === 'ready' && counters.length === 0 && (
        <section className="empty-panel">
          <p className="section-label">No stored counters</p>
          <h2>No personal matchup data exists yet for {enemyChampionName}.</h2>
          <p className="state-copy">Import more matches or try another enemy champion.</p>
        </section>
      )}

      {!isLoading && status === 'ready' && counters.length > 0 && (
        <section className="dashboard-panel">
          <div className="panel-heading">
            <h2>Personal counter ranking</h2>
            <p>Sorted by stored personal score from the backend.</p>
          </div>
          <div
            id="score-formula-panel"
            className={`score-formula-panel ${showScoreFormula ? 'score-formula-visible' : ''}`}
            role="note"
            aria-label="Score formula explanation"
          >
            <strong>How score is calculated</strong>
            <p>{SCORE_FORMULA_TOOLTIP}</p>
          </div>

          <div className="counter-table" role="table" aria-label="Enemy champion counters">
            <div className="counter-row counter-head" role="row">
              <span role="columnheader">Champion</span>
              <span role="columnheader">Role</span>
              <span role="columnheader">
                <button
                  className="score-help-button"
                  type="button"
                  onMouseEnter={() => setShowScoreFormula(true)}
                  onMouseLeave={() => setShowScoreFormula(false)}
                  onFocus={() => setShowScoreFormula(true)}
                  onBlur={() => setShowScoreFormula(false)}
                  aria-expanded={showScoreFormula}
                  aria-controls="score-formula-panel"
                >
                  Score*
                </button>
              </span>
              <span role="columnheader">Games</span>
              <span role="columnheader">Winrate</span>
              <span role="columnheader">KDA</span>
              <span role="columnheader">Confidence</span>
            </div>

            {counters.map((counter) => (
              <div
                key={`${counter.enemyChampionId}-${counter.userChampionId}-${counter.role}`}
                className={`counter-row status-${counter.status.toLowerCase()}`}
                role="row"
              >
                <Link
                  className="counter-link"
                  role="cell"
                  to={`/profiles/${summonerId}/enemies/${counter.enemyChampionId}/counters/${counter.userChampionId}`}
                >
                  <span className="champion-cell">
                    <img
                      className="champion-avatar"
                      src={getChampionById(counter.userChampionId)?.image}
                      alt=""
                    />
                    <span>{getChampionById(counter.userChampionId)?.name ?? `Champion ${counter.userChampionId}`}</span>
                  </span>
                </Link>
                <span role="cell">{counter.role}</span>
                <span role="cell">{counter.personalScore.toFixed(1)}</span>
                <span role="cell">{counter.games}</span>
                <span role="cell">{counter.winrate.toFixed(1)}%</span>
                <span role="cell">{counter.averageKda.toFixed(2)}</span>
                <span role="cell">{formatConfidence(counter.confidence)}</span>
              </div>
            ))}
          </div>
        </section>
      )}
    </section>
  )
}

function formatConfidence(value: PersonalCounterResponse['confidence']): string {
  return value.toLowerCase().replace('_', ' ')
}

function getCountersErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'SUMMONER_PROFILE_NOT_FOUND') {
      return 'This stored summoner id does not exist in the backend.'
    }

    return error.message
  }

  return 'Unexpected frontend error while loading counters.'
}
