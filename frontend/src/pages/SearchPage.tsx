export function SearchPage() {
  return (
    <section className="search-page" aria-labelledby="search-title">
      <div className="search-copy">
        <p className="eyebrow">Personal League matchup assistant</p>
        <h1 id="search-title">Pick your best counter, not the generic counter.</h1>
        <p className="lede">
          ComfortPick will analyze your stored match history and rank the
          champions that worked best for you into each enemy pick.
        </p>
      </div>

      <form className="search-form">
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
      </form>
    </section>
  )
}
