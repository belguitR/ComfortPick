export type SummonerSpell = {
  id: number
  name: string
  image: string
}

const BASE_URL = 'https://ddragon.leagueoflegends.com/cdn/15.10.1/img/spell'

export const SUMMONER_SPELLS: Record<number, SummonerSpell> = {
  1: { id: 1, name: 'Cleanse', image: `${BASE_URL}/SummonerBoost.png` },
  3: { id: 3, name: 'Exhaust', image: `${BASE_URL}/SummonerExhaust.png` },
  4: { id: 4, name: 'Flash', image: `${BASE_URL}/SummonerFlash.png` },
  6: { id: 6, name: 'Ghost', image: `${BASE_URL}/SummonerHaste.png` },
  7: { id: 7, name: 'Heal', image: `${BASE_URL}/SummonerHeal.png` },
  11: { id: 11, name: 'Smite', image: `${BASE_URL}/SummonerSmite.png` },
  12: { id: 12, name: 'Teleport', image: `${BASE_URL}/SummonerTeleport.png` },
  13: { id: 13, name: 'Clarity', image: `${BASE_URL}/SummonerMana.png` },
  14: { id: 14, name: 'Ignite', image: `${BASE_URL}/SummonerDot.png` },
  21: { id: 21, name: 'Barrier', image: `${BASE_URL}/SummonerBarrier.png` },
  32: { id: 32, name: 'Mark', image: `${BASE_URL}/SummonerSnowball.png` },
}

export function getSummonerSpellById(id: number | null | undefined): SummonerSpell | null {
  if (id == null) {
    return null
  }

  return SUMMONER_SPELLS[id] ?? null
}
