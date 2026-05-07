import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../lib/api/client'
import { getPersonalMatchupDetail } from '../lib/api/comfortpick'
import type { PersonalMatchupDetailResponse } from '../lib/api/comfortpick'
import { getChampionById } from '../lib/champions'

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
      <section className="profile-state">
        <p className="section-label">Matchup error</p>
        <h1>Could not load matchup detail</h1>
        <p className="state-copy">The route is missing a valid summoner or champion id.</p>
        <Link className="inline-link" to="/">
          Back to search
        </Link>
      </section>
    )
  }

  if (status === 'loading') {
    return (
      <section className="profile-state">
        <p className="section-label">Matchup loading</p>
        <h1>Loading detailed matchup view</h1>
        <p className="state-copy">Fetching the stored reasoning, stats, and recent games.</p>
      </section>
    )
  }

  if (status === 'error' || detail == null) {
    return (
      <section className="profile-state">
        <p className="section-label">Matchup error</p>
        <h1>Could not load matchup detail</h1>
        <p className="state-copy">{errorMessage}</p>
        <Link
          className="inline-link"
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
  const userChampionName = getChampionById(detail.userChampionId)?.name ?? `Champion ${detail.userChampionId}`
  const enemyChampionName = getChampionById(detail.enemyChampionId)?.name ?? `Champion ${detail.enemyChampionId}`

  return (
    <section className="profile-layout">
      <header className="profile-header">
        <div>
          <p className="section-label">Personal matchup detail</p>
          <h1>
            {userChampionName} into {enemyChampionName}
            <span>{detail.role ?? 'Unknown role'}</span>
          </h1>
          <p className="hero-copy">{detail.reasoning}</p>
        </div>
        <div className="toolbar-actions">
          <Link
            className="secondary-link"
            to={`/profiles/${parsedIds.summonerId}/enemies/${parsedIds.enemyChampionId}`}
          >
            Back to counters
          </Link>
        </div>
      </header>

      <div className="stat-strip" aria-label="Matchup summary">
        <article className="stat-card">
          <span>Personal score</span>
          <strong>{detail.personalScore != null ? detail.personalScore.toFixed(1) : 'No data'}</strong>
        </article>
        <article className="stat-card">
          <span>Confidence</span>
          <strong>{formatLabel(detail.confidence)}</strong>
        </article>
        <article className="stat-card">
          <span>Status</span>
          <strong>{formatLabel(detail.status)}</strong>
        </article>
      </div>

      {!detail.hasData ? (
        <section className="empty-panel">
          <p className="section-label">No stored matchup data</p>
          <h2>No personal data exists yet for this champion matchup.</h2>
          <p className="state-copy">{detail.reasoning}</p>
        </section>
      ) : (
        <>
          <div className="dashboard-grid detail-grid">
            <section className="dashboard-panel">
              <div className="panel-heading">
                <h2>Performance snapshot</h2>
              </div>
              <dl className="detail-list">
                <div><dt>Games</dt><dd>{detail.games}</dd></div>
                <div><dt>Wins / losses</dt><dd>{detail.wins} / {detail.losses}</dd></div>
                <div><dt>Winrate</dt><dd>{detail.winrate.toFixed(1)}%</dd></div>
                <div><dt>Average KDA</dt><dd>{detail.averageKda?.toFixed(2) ?? 'N/A'}</dd></div>
                <div><dt>Average CS</dt><dd>{detail.averageCs?.toFixed(1) ?? 'N/A'}</dd></div>
                <div><dt>Average gold</dt><dd>{detail.averageGold?.toFixed(0) ?? 'N/A'}</dd></div>
                <div><dt>Average damage</dt><dd>{detail.averageDamage?.toFixed(0) ?? 'N/A'}</dd></div>
                <div><dt>Last update</dt><dd>{detail.lastUpdatedAt ? formatDateTime(detail.lastUpdatedAt) : 'N/A'}</dd></div>
              </dl>
            </section>

            <section className="dashboard-panel">
              <div className="panel-heading">
                <h2>Build recommendation</h2>
              </div>
              {hasBuildData ? (
                <dl className="detail-list">
                  <div><dt>First completed item</dt><dd>{detail.build.firstCompletedItemId ?? 'N/A'}</dd></div>
                  <div><dt>Support games</dt><dd>{detail.build.firstCompletedItemGames}</dd></div>
                  <div><dt>Item set</dt><dd>{detail.build.itemSet ?? 'N/A'}</dd></div>
                  <div><dt>Item set games</dt><dd>{detail.build.itemSetGames}</dd></div>
                  <div><dt>Build score</dt><dd>{detail.build.score?.toFixed(1) ?? 'N/A'}</dd></div>
                </dl>
              ) : (
                <p className="state-copy">No winning personal build data is available for this matchup.</p>
              )}
            </section>

            <section className="dashboard-panel">
              <div className="panel-heading">
                <h2>Rune recommendation</h2>
              </div>
              {hasRuneData ? (
                <dl className="detail-list">
                  <div><dt>Primary rune</dt><dd>{detail.runes.primaryRuneId ?? 'N/A'}</dd></div>
                  <div><dt>Primary rune games</dt><dd>{detail.runes.primaryRuneGames}</dd></div>
                  <div><dt>Secondary rune</dt><dd>{detail.runes.secondaryRuneId ?? 'N/A'}</dd></div>
                  <div><dt>Secondary rune games</dt><dd>{detail.runes.secondaryRuneGames}</dd></div>
                  <div><dt>Rune score</dt><dd>{detail.runes.score?.toFixed(1) ?? 'N/A'}</dd></div>
                </dl>
              ) : (
                <p className="state-copy">No winning personal rune data is available for this matchup.</p>
              )}
            </section>
          </div>

          <section className="dashboard-panel">
            <div className="panel-heading">
              <h2>Recent games</h2>
              <p>Newest stored games first for the selected role.</p>
            </div>

            {detail.recentGames.length === 0 ? (
              <p className="state-copy">No recent stored games are available for this matchup yet.</p>
            ) : (
              <div className="recent-games">
                {detail.recentGames.map((game) => (
                  <article key={game.riotMatchId} className={`recent-game ${game.win ? 'recent-win' : 'recent-loss'}`}>
                    <div className="recent-game-header">
                      <strong>{game.riotMatchId}</strong>
                      <span>{game.win ? 'Win' : 'Loss'}</span>
                    </div>
                    <div className="recent-game-meta">
                      <span>{formatDateTime(game.gameCreation)}</span>
                      <span>{game.kills}/{game.deaths}/{game.assists} KDA line</span>
                      <span>CS {game.totalCs ?? 'N/A'}</span>
                      <span>Gold {game.goldEarned ?? 'N/A'}</span>
                      <span>Damage {game.totalDamageToChampions ?? 'N/A'}</span>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </section>
  )
}

function formatLabel(value: string): string {
  return value.toLowerCase().replace('_', ' ')
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function getDetailErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'SUMMONER_PROFILE_NOT_FOUND') {
      return 'This stored summoner id does not exist in the backend.'
    }

    return error.message
  }

  return 'Unexpected frontend error while loading matchup detail.'
}
