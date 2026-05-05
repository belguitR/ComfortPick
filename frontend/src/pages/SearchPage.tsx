export function SearchPage() {
  return (
    <section className="search-page" aria-labelledby="search-title">
      <div className="search-copy">
        <p className="eyebrow">Personal League matchup assistant</p>
        <h1 id="search-title">Stop drafting theory. Start drafting what you win on.</h1>
        <p className="lede">
          ComfortPick turns your own match history into a practical draft tool.
          Search a Riot ID, import matches once, then query any enemy pick and
          see which champions actually perform for that player.
        </p>
        <div className="signal-grid" aria-label="Product overview">
          <article className="signal-card">
            <span className="signal-label">Recommendation model</span>
            <strong>Personal score over global tier lists</strong>
            <p>Sample size, champion comfort, and matchup results stay visible.</p>
          </article>
          <article className="signal-card">
            <span className="signal-label">Stored analysis</span>
            <strong>DB-first refresh flow</strong>
            <p>Imported matches are persisted and only new data triggers recalculation.</p>
          </article>
          <article className="signal-card">
            <span className="signal-label">Result shape</span>
            <strong>Clear picks, low-data flags, avoid warnings</strong>
            <p>The product explains both what to lock in and what to stop forcing.</p>
          </article>
        </div>
      </div>

      <form className="search-form">
        <div className="form-copy">
          <p className="form-kicker">Analyze profile</p>
          <h2>Search a Riot account</h2>
          <p>Task 0 ships the real project surface and local infrastructure base.</p>
        </div>
        <label>
          Game name
          <input name="gameName" autoComplete="off" placeholder="Rami" />
        </label>
        <label>
          Tagline
          <input name="tagLine" autoComplete="off" placeholder="EUW" />
        </label>
        <label>
          Region
          <select name="region" defaultValue="EUW1">
            <option value="EUW1">Europe West</option>
            <option value="EUN1">Europe Nordic & East</option>
            <option value="NA1">North America</option>
            <option value="KR">Korea</option>
          </select>
        </label>
        <button type="submit">Analyze</button>
        <div className="preview-card" aria-label="Example result preview">
          <span className="preview-title">Example output</span>
          <div className="preview-row">
            <strong>Ahri into Zed</strong>
            <span className="preview-score">86</span>
          </div>
          <div className="preview-meta">
            <span>7 games</span>
            <span>71% win rate</span>
            <span>High confidence</span>
          </div>
        </div>
      </form>
    </section>
  )
}
