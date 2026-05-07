import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../lib/api/client'
import { getPersonalMatchupDetail } from '../lib/api/comfortpick'
import type { PersonalMatchupDetailResponse } from '../lib/api/comfortpick'
import { getChampionById } from '../lib/champions'
import { getItemById, parseItemSet } from '../lib/items'
import { getRuneById } from '../lib/runes'

export function MatchupDetailPage() {
  const { summonerId, enemyChampionId, userChampionId } = useParams<{
    summonerId: string
    enemyChampionId: string
    userChampionId: string
  }>()

  const parsedIds = useMemo(() => {
    const enemyId = Number(enemyChampionId)
    const userId = Number(userChampionId)
    if (!summonerId || !Number.isInteger(enemyId) || !Number.isInteger(userId) || enemyId <= 0 || userId <= 0) {
      return null
    }

    return {
      summonerId,
      enemyChampionId: enemyId,
      userChampionId: userId,
    }
  }, [enemyChampionId, summonerId, userChampionId])

  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [detail, setDetail] = useState<PersonalMatchupDetailResponse | null>(null)

  useEffect(() => {
    if (parsedIds == null) {
      return
    }

    let active = true

    void getPersonalMatchupDetail(
      parsedIds.summonerId,
      parsedIds.enemyChampionId,
      parsedIds.userChampionId,
    )
      .then((response) => {
        if (!active) return
        setDetail(response)
        setStatus('ready')
      })
      .catch((error: unknown) => {
        if (!active) return
        setStatus('error')
        setErrorMessage(getDetailErrorMessage(error))
      })

    return () => {
      active = false
    }
  }, [parsedIds])

  if (parsedIds == null) {
    return (
      <section className="workspace-state">
        <h1>We could not open that matchup.</h1>
        <p>Please return to the counter list and try again.</p>
        <Link className="ghost-link" to="/">
          Back to search
        </Link>
      </section>
    )
  }

  if (status === 'loading') {
    return (
      <section className="workspace-state">
        <h1>Loading matchup</h1>
        <p>Pulling the latest detail view.</p>
      </section>
    )
  }

  if (status === 'error' || detail == null) {
    return (
      <section className="workspace-state">
        <h1>We could not open that matchup.</h1>
        <p>{errorMessage}</p>
        <Link
          className="ghost-link"
          to={`/profiles/${parsedIds.summonerId}/enemies/${parsedIds.enemyChampionId}`}
        >
          Back to counters
        </Link>
      </section>
    )
  }

  const hasBuildData =
    detail.build.firstCompletedItemId != null ||
    detail.build.itemSet != null ||
    detail.build.score != null
  const hasRuneData =
    detail.runes.primaryRuneId != null ||
    detail.runes.secondaryRuneId != null ||
    detail.runes.score != null
  const userChampion = getChampionById(detail.userChampionId)
  const enemyChampion = getChampionById(detail.enemyChampionId)
  const userChampionName = userChampion?.name ?? `Champion ${detail.userChampionId}`
  const enemyChampionName = enemyChampion?.name ?? `Champion ${detail.enemyChampionId}`
  const firstItem = detail.build.firstCompletedItemId != null
    ? getItemById(detail.build.firstCompletedItemId) ?? {
        id: detail.build.firstCompletedItemId,
        name: `Item ${detail.build.firstCompletedItemId}`,
        image: '',
      }
    : null
  const itemSet = parseItemSet(detail.build.itemSet)
  const primaryRune = detail.runes.primaryRuneId != null
    ? getRuneById(detail.runes.primaryRuneId) ?? {
        id: detail.runes.primaryRuneId,
        name: `Rune ${detail.runes.primaryRuneId}`,
        image: '',
      }
    : null
  const secondaryRune = detail.runes.secondaryRuneId != null
    ? getRuneById(detail.runes.secondaryRuneId) ?? {
        id: detail.runes.secondaryRuneId,
        name: `Rune ${detail.runes.secondaryRuneId}`,
        image: '',
      }
    : null
  const scoreBarWidth = Math.max(12, Math.min(detail.personalScore ?? 0, 100))
  const confidenceBarWidth = getConfidenceBarWidth(detail.confidence)

  return (
    <section className="workspace-layout">
      <section className="matchup-hero-card">
        <div className="matchup-hero">
          <div className="matchup-hero-avatars">
            {userChampion?.image ? <img className="matchup-hero-avatar-large" src={userChampion.image} alt="" /> : null}
            {enemyChampion?.image ? <img className="matchup-hero-avatar-large" src={enemyChampion.image} alt="" /> : null}
          </div>
          <div className="matchup-hero-copy">
            <h1 className="workspace-title">{userChampionName} into {enemyChampionName}</h1>
            <p>{detail.reasoning}</p>
          </div>
          <Link
            className="workspace-action-button"
            to={`/profiles/${parsedIds.summonerId}/enemies/${parsedIds.enemyChampionId}`}
          >
            Find Counters
          </Link>
        </div>

        <div className="matchup-summary-bar">
          <article className="matchup-summary-item">
            <span>Personal Score</span>
            <strong>{detail.personalScore != null ? detail.personalScore.toFixed(1) : 'No data'}</strong>
            <div className="summary-bar-track"><div style={{ width: `${scoreBarWidth}%` }} /></div>
          </article>
          <article className="matchup-summary-item">
            <span>Confidence</span>
            <strong>{formatConfidence(detail.confidence)}</strong>
            <div className="summary-bar-track summary-bar-track-orange"><div style={{ width: `${confidenceBarWidth}%` }} /></div>
          </article>
          <article className="matchup-summary-item">
            <span>Recommendation</span>
            <strong className={`status-pill status-pill-${getStatusTone(detail.status)}`}>{formatStatus(detail.status)}</strong>
          </article>
          <article className="matchup-summary-item">
            <span>Updated</span>
            <strong>{detail.lastUpdatedAt ? formatDateTime(detail.lastUpdatedAt) : 'In progress'}</strong>
          </article>
        </div>
      </section>

      {!detail.hasData ? (
        <section className="workspace-empty-card">
          <h2>No personal matchup data yet.</h2>
          <p>Come back after more games have been synced for this champion pairing.</p>
        </section>
      ) : (
        <>
          <section className="matchup-detail-grid">
            <article className="insight-card detail-card">
              <div className="insight-card-head">
                <h2>Performance Snapshot</h2>
              </div>
              <div className="performance-highlight">
                <div>
                  <span>Win Rate</span>
                  <strong>{detail.winrate.toFixed(1)}%</strong>
                </div>
                <div>
                  <span>Total Games</span>
                  <strong>{detail.games}</strong>
                </div>
              </div>
              <dl className="detail-stat-list">
                <div><dt>Average KDA</dt><dd>{detail.averageKda?.toFixed(2) ?? 'N/A'}</dd></div>
                <div><dt>Average CS</dt><dd>{detail.averageCs?.toFixed(1) ?? 'N/A'}</dd></div>
                <div><dt>Average Gold</dt><dd>{detail.averageGold?.toFixed(0) ?? 'N/A'}</dd></div>
                <div><dt>Average Damage</dt><dd>{detail.averageDamage?.toFixed(0) ?? 'N/A'}</dd></div>
                <div><dt>Role</dt><dd>{formatRole(detail.role)}</dd></div>
                <div><dt>Record</dt><dd>{detail.wins} / {detail.losses}</dd></div>
              </dl>
            </article>

            <article className="insight-card detail-card">
              <div className="insight-card-head">
                <h2>Build Recommendation</h2>
              </div>
              {hasBuildData ? (
                <div className="detail-section-stack">
                  <div className="build-hero-row">
                    <div className="build-hero-avatar">
                      {firstItem?.image ? <img src={firstItem.image} alt="" /> : null}
                    </div>
                    <div>
                      <span>Core first item</span>
                      <strong>{firstItem?.name ?? 'No clear first item yet'}</strong>
                    </div>
                  </div>
                  <div>
                    <span className="detail-mini-label">Recommended build</span>
                    <div className="build-chip-grid">
                      {itemSet.length > 0 ? (
                        itemSet.map((item) => (
                          <span key={item.id} className="build-chip">
                            {item.image ? <img src={item.image} alt="" /> : null}
                            <span>{item.name}</span>
                          </span>
                        ))
                      ) : (
                        <span className="empty-inline-copy">No repeat build yet</span>
                      )}
                    </div>
                  </div>
                  <div className="detail-score-note">
                    <span>Build score</span>
                    <strong>{detail.build.score?.toFixed(1) ?? 'N/A'}</strong>
                  </div>
                </div>
              ) : (
                <p className="empty-inline-copy">No clear winning build pattern has formed yet.</p>
              )}
            </article>

            <article className="insight-card detail-card">
              <div className="insight-card-head">
                <h2>Rune Recommendation</h2>
              </div>
              {hasRuneData ? (
                <div className="detail-section-stack">
                  <div className="rune-column">
                    <span className="detail-mini-label">Primary</span>
                    {primaryRune ? <RuneCard rune={primaryRune} /> : <span className="empty-inline-copy">No primary rune yet</span>}
                  </div>
                  <div className="rune-column">
                    <span className="detail-mini-label">Secondary</span>
                    {secondaryRune ? <RuneCard rune={secondaryRune} /> : <span className="empty-inline-copy">No secondary rune yet</span>}
                  </div>
                  <div className="detail-score-note">
                    <span>Rune score</span>
                    <strong>{detail.runes.score?.toFixed(1) ?? 'N/A'}</strong>
                  </div>
                </div>
              ) : (
                <p className="empty-inline-copy">No clear winning rune pattern has formed yet.</p>
              )}
            </article>
          </section>

          <section className="workspace-panel">
            <div className="panel-header-row">
              <div>
                <h2>Recent Matchups</h2>
                <p>Latest games in this champion pairing.</p>
              </div>
            </div>

            {detail.recentGames.length === 0 ? (
              <p className="empty-inline-copy">No recent games are available for this matchup yet.</p>
            ) : (
              <div className="matchup-table">
                <div className="matchup-table-row matchup-table-head">
                  <span>Result</span>
                  <span>Opponent</span>
                  <span>KDA</span>
                  <span>CS / Gold</span>
                  <span>Date</span>
                  <span>Match ID</span>
                </div>
                {detail.recentGames.map((game) => (
                  <div key={game.riotMatchId} className="matchup-table-row">
                    <span>
                      <span className={`result-pill ${game.win ? 'result-win' : 'result-loss'}`}>
                        {game.win ? 'Victory' : 'Defeat'}
                      </span>
                    </span>
                    <span className="table-opponent-cell">
                      {enemyChampion?.image ? <img className="table-opponent-avatar" src={enemyChampion.image} alt="" /> : null}
                      <span>{enemyChampionName}</span>
                    </span>
                    <span>{game.kills} / {game.deaths} / {game.assists}</span>
                    <span>{game.totalCs ?? 'N/A'} CS | {game.goldEarned ?? 'N/A'} G</span>
                    <span>{formatDateShort(game.gameCreation)}</span>
                    <span>{game.riotMatchId}</span>
                  </div>
                ))}
              </div>
            )}
          </section>
        </>
      )}

      <footer className="workspace-footer">
        <div className="workspace-footer-brand">
          <strong>ComfortPick Analytics</strong>
          <p>One matchup page, your own numbers, and a cleaner draft decision.</p>
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

function formatConfidence(value: string): string {
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

function getConfidenceBarWidth(value: string): number {
  switch (value) {
    case 'HIGH':
      return 88
    case 'MEDIUM':
      return 62
    case 'LOW':
      return 38
    default:
      return 18
  }
}

function getStatusTone(value: string): 'green' | 'blue' | 'amber' {
  switch (value) {
    case 'BEST_PICK':
    case 'GOOD_PICK':
      return 'green'
    case 'AVOID':
      return 'amber'
    default:
      return 'blue'
  }
}

function formatStatus(value: string): string {
  switch (value) {
    case 'BEST_PICK':
      return 'Top choice'
    case 'GOOD_PICK':
      return 'Strong pick'
    case 'OK_PICK':
      return 'Playable'
    case 'LOW_DATA':
      return 'Low data'
    case 'AVOID':
      return 'Avoid'
    default:
      return 'Early read'
  }
}

function formatRole(value: string | null): string {
  if (!value) return 'Unknown role'

  switch (value.toUpperCase()) {
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
      return value.toLowerCase().replace('_', ' ')
  }
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatDateShort(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
  }).format(new Date(value))
}

function getDetailErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'SUMMONER_PROFILE_NOT_FOUND') {
      return 'We could not find that profile.'
    }

    return 'This matchup view is temporarily unavailable.'
  }

  return 'This matchup view is temporarily unavailable.'
}

type DisplayRune = {
  id: number
  name: string
  image: string
}

function RuneCard({ rune }: { rune: DisplayRune }) {
  return (
    <span className="build-chip rune-chip">
      {rune.image ? <img src={rune.image} alt="" /> : null}
      <span>{rune.name}</span>
    </span>
  )
}
