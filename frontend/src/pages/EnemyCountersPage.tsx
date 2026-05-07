import type { FormEvent } from 'react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../lib/api/client'
import { getEnemyChampionCounters } from '../lib/api/comfortpick'
import type { PersonalCounterResponse } from '../lib/api/comfortpick'
import { CHAMPIONS, getChampionById, resolveChampionQuery } from '../lib/champions'

const SCORE_FORMULA_TOOLTIP =
  'Win rate and champion comfort matter most, then KDA, farm and gold, recent form, and a small fallback value. Small samples are penalized so one lucky game does not dominate the list.'

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
      <section className="workspace-state">
        <h1>We couldn’t open that counter page.</h1>
        <p>Please go back and search for a champion again.</p>
        <Link className="ghost-link" to={summonerId ? `/profiles/${summonerId}` : '/'}>
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
      setErrorMessage('Enter an enemy champion name to continue.')
      return
    }

    navigate(`/profiles/${summonerId}/enemies/${champion.id}`)
  }

  const enemyChampion = getChampionById(parsedEnemyChampionId)
  const enemyChampionName = enemyChampion?.name ?? `Champion ${parsedEnemyChampionId}`

  return (
    <section className="workspace-layout">
      <header className="workspace-header">
        <div className="matchup-hero-title">
          {enemyChampion?.image ? <img className="matchup-hero-avatar" src={enemyChampion.image} alt="" /> : null}
          <div>
            <h1 className="workspace-title">{enemyChampionName}</h1>
            <p className="workspace-subtitle">Your strongest answers to this matchup.</p>
          </div>
        </div>
        <Link className="workspace-action-button secondary-action" to={`/profiles/${summonerId}`}>
          Profile Dashboard
        </Link>
      </header>

      <section className="dashboard-search-panel compact-panel">
        <div className="dashboard-search-copy">
          <h2>Try another enemy pick</h2>
          <p>Swap the opposing champion to refresh your counter list.</p>
        </div>

        <form className="dashboard-search-form" onSubmit={handleSubmit} noValidate>
          <input
            name="enemyChampion"
            value={enemyChampionInput}
            onChange={(event) => setEnemyChampionInput(event.target.value)}
            placeholder="Search enemy champion"
            list="champion-options"
          />
          <button className="dashboard-search-button" type="submit">
            Load Counters
          </button>
        </form>
        <datalist id="champion-options">
          {CHAMPIONS.map((champion) => (
            <option key={champion.id} value={champion.name} />
          ))}
        </datalist>
      </section>

      {isLoading && (
        <section className="workspace-state">
          <h1>Loading counters</h1>
          <p>Pulling your latest picks for this enemy champion.</p>
        </section>
      )}

      {!isLoading && status === 'error' && (
        <section className="workspace-state">
          <h1>We couldn’t load those counters.</h1>
          <p>{errorMessage}</p>
        </section>
      )}

      {!isLoading && status === 'ready' && counters.length === 0 && (
        <section className="workspace-empty-card">
          <h2>No personal counter data yet for {enemyChampionName}.</h2>
          <p>Give the sync a little more time, then check this matchup again.</p>
        </section>
      )}

      {!isLoading && status === 'ready' && counters.length > 0 && (
        <section className="workspace-panel">
          <div className="panel-header-row">
            <div>
              <h2>Counter ranking</h2>
              <p>Best personal picks for this enemy champion.</p>
            </div>
            <div className="score-help-wrap">
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
              <div
                id="score-formula-panel"
                className={`score-formula-panel ${showScoreFormula ? 'score-formula-visible' : ''}`}
                role="note"
                aria-label="Score formula explanation"
              >
                <strong>How score works</strong>
                <p>{SCORE_FORMULA_TOOLTIP}</p>
              </div>
            </div>
          </div>

          <div className="counter-table-shell" role="table" aria-label="Enemy champion counters">
            <div className="counter-grid counter-grid-head" role="row">
              <span role="columnheader">Champion</span>
              <span role="columnheader">Role</span>
              <span role="columnheader">Score</span>
              <span role="columnheader">Games</span>
              <span role="columnheader">Win Rate</span>
              <span role="columnheader">KDA</span>
              <span role="columnheader">Confidence</span>
            </div>

            {counters.map((counter) => {
              const champion = getChampionById(counter.userChampionId)
              return (
                <Link
                  key={`${counter.enemyChampionId}-${counter.userChampionId}-${counter.role}`}
                  className="counter-grid counter-grid-row"
                  role="row"
                  to={`/profiles/${summonerId}/enemies/${counter.enemyChampionId}/counters/${counter.userChampionId}`}
                >
                  <span className="counter-champion-cell" role="cell">
                    <img className="counter-avatar" src={champion?.image} alt="" />
                    <span>{champion?.name ?? `Champion ${counter.userChampionId}`}</span>
                  </span>
                  <span role="cell">{formatRole(counter.role)}</span>
                  <span role="cell">{counter.personalScore.toFixed(1)}</span>
                  <span role="cell">{counter.games}</span>
                  <span role="cell">{counter.winrate.toFixed(1)}%</span>
                  <span role="cell">{counter.averageKda.toFixed(2)}</span>
                  <span role="cell">{formatConfidence(counter.confidence)}</span>
                </Link>
              )
            })}
          </div>
        </section>
      )}

      <footer className="workspace-footer">
        <div className="workspace-footer-brand">
          <strong>ComfortPick Analytics</strong>
          <p>Shortlist your best answers before the draft locks in.</p>
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

function formatRole(value: string): string {
  switch (value.toUpperCase()) {
    case 'TOP':
      return 'Top'
    case 'JUNGLE':
      return 'Jungle'
    case 'MIDDLE':
      return 'Mid'
    case 'BOTTOM':
      return 'Bot'
    case 'UTILITY':
      return 'Support'
    default:
      return value.toLowerCase().replace('_', ' ')
  }
}

function formatConfidence(value: PersonalCounterResponse['confidence']): string {
  switch (value) {
    case 'HIGH':
      return 'High'
    case 'MEDIUM':
      return 'Medium'
    case 'LOW':
      return 'Low'
    default:
      return 'Early'
  }
}

function getCountersErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'SUMMONER_PROFILE_NOT_FOUND') {
      return 'We could not find that profile.'
    }

    return 'This counter list is temporarily unavailable.'
  }

  return 'This counter list is temporarily unavailable.'
}
