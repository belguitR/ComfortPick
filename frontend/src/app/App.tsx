import { Link, Outlet, useLocation } from 'react-router-dom'

export function App() {
  const location = useLocation()
  const pathParts = location.pathname.split('/').filter(Boolean)
  const isWorkspaceRoute = pathParts[0] === 'profiles'
  const summonerId = isWorkspaceRoute ? pathParts[1] : null
  const enemyChampionId = pathParts[2] === 'enemies' ? pathParts[3] : null
  const userChampionId = pathParts[4] === 'counters' ? pathParts[5] : null

  const dashboardHref = summonerId ? `/profiles/${summonerId}` : '/'
  const counterSearchHref = summonerId ? `/profiles/${summonerId}#counter-search` : '/'
  const countersHref = summonerId && enemyChampionId
    ? `/profiles/${summonerId}/enemies/${enemyChampionId}`
    : counterSearchHref
  const matchupHref = summonerId && enemyChampionId && userChampionId
    ? `/profiles/${summonerId}/enemies/${enemyChampionId}/counters/${userChampionId}`
    : countersHref
  const currentPath = location.pathname
  const activeSection = !isWorkspaceRoute
    ? 'search'
    : userChampionId
      ? 'matchups'
      : enemyChampionId
        ? 'counters'
        : 'dashboard'

  return (
    <div className={`app-shell ${isWorkspaceRoute ? 'workspace-active' : 'landing-active'}`}>
      <header className="app-topbar">
        <Link className="brand-mark" to="/" aria-label="ComfortPick home">
          ComfortPick
        </Link>

        {!isWorkspaceRoute ? (
          <nav className="app-nav" aria-label="Primary navigation">
            <TopLink to="/" active={currentPath === '/'}>Search</TopLink>
          </nav>
        ) : (
          <div className="topbar-spacer" />
        )}

        <div className="topbar-tools" aria-label="Workspace actions">
          {isWorkspaceRoute ? (
            <Link className="topbar-link" to="/">
              New Search
            </Link>
          ) : (
            <div className="topbar-spacer" />
          )}
        </div>
      </header>

      {isWorkspaceRoute ? (
        <div className="workspace-shell">
          <aside className="workspace-sidebar">
            <div className="sidebar-profile-card">
              <div className="sidebar-profile-avatar">CP</div>
              <div>
                <strong>ComfortPick</strong>
                <span>Personal draft lab</span>
              </div>
            </div>

            <nav className="sidebar-nav" aria-label="Workspace navigation">
              <SidebarLink to={dashboardHref} label="Profile Dashboard" icon="D" active={activeSection === 'dashboard'} />
              <SidebarLink to={countersHref} label="Counter Engine" icon="C" active={activeSection === 'counters'} />
              <SidebarLink to={matchupHref} label="Matchup Analysis" icon="M" active={activeSection === 'matchups'} />
            </nav>

            <div className="sidebar-bottom">
              <Link className="sidebar-cta" to={countersHref}>
                Find Counters
              </Link>
              <Link className="sidebar-meta-link" to="/">
                New Search
              </Link>
            </div>
          </aside>

          <main className="workspace-page">
            <Outlet />
          </main>
        </div>
      ) : (
        <main className="landing-page">
          <Outlet />
        </main>
      )}
    </div>
  )
}

function TopLink({ to, active, children }: { to: string; active: boolean; children: string }) {
  return (
    <Link
      to={to}
      className={`app-nav-link${active ? ' app-nav-link-active' : ''}`}
    >
      {children}
    </Link>
  )
}

function SidebarLink({ to, label, icon, active }: { to: string; label: string; icon: string; active: boolean }) {
  return (
    <Link
      to={to}
      className={`sidebar-link${active ? ' sidebar-link-active' : ''}`}
    >
      <span className="sidebar-link-icon" aria-hidden="true">
        {icon}
      </span>
      <span>{label}</span>
    </Link>
  )
}
