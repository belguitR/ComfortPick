import { Link, Outlet } from 'react-router-dom'

export function App() {
  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand-block">
          <Link className="brand" to="/" aria-label="ComfortPick home">
            ComfortPick
          </Link>
          <span className="brand-tag">Personal matchup intelligence</span>
        </div>
        <nav className="topnav" aria-label="Primary navigation">
          <Link to="/">Search</Link>
        </nav>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </div>
  )
}
