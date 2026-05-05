import { Link, Outlet } from 'react-router-dom'

export function App() {
  return (
    <div className="app-shell">
      <header className="topbar">
        <Link className="brand" to="/" aria-label="ComfortPick home">
          ComfortPick
        </Link>
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
