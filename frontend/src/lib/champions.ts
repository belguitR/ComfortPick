export type ChampionReference = {
  id: number
  name: string
  alias: string
  image: string
}

export const CHAMPIONS: ChampionReference[] = [
  { id: 266, name: 'Aatrox', alias: 'Aatrox', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Aatrox.png' },
  { id: 103, name: 'Ahri', alias: 'Ahri', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Ahri.png' },
  { id: 84, name: 'Akali', alias: 'Akali', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Akali.png' },
  { id: 166, name: 'Akshan', alias: 'Akshan', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Akshan.png' },
  { id: 12, name: 'Alistar', alias: 'Alistar', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Alistar.png' },
  { id: 799, name: 'Ambessa', alias: 'Ambessa', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Ambessa.png' },
  { id: 32, name: 'Amumu', alias: 'Amumu', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Amumu.png' },
  { id: 34, name: 'Anivia', alias: 'Anivia', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Anivia.png' },
  { id: 1, name: 'Annie', alias: 'Annie', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Annie.png' },
  { id: 523, name: 'Aphelios', alias: 'Aphelios', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Aphelios.png' },
  { id: 22, name: 'Ashe', alias: 'Ashe', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Ashe.png' },
  { id: 136, name: 'Aurelion Sol', alias: 'AurelionSol', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/AurelionSol.png' },
  { id: 893, name: 'Aurora', alias: 'Aurora', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Aurora.png' },
  { id: 268, name: 'Azir', alias: 'Azir', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Azir.png' },
  { id: 432, name: 'Bard', alias: 'Bard', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Bard.png' },
  { id: 200, name: 'Bel\'Veth', alias: 'Belveth', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Belveth.png' },
  { id: 53, name: 'Blitzcrank', alias: 'Blitzcrank', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Blitzcrank.png' },
  { id: 63, name: 'Brand', alias: 'Brand', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Brand.png' },
  { id: 201, name: 'Braum', alias: 'Braum', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Braum.png' },
  { id: 233, name: 'Briar', alias: 'Briar', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Briar.png' },
  { id: 51, name: 'Caitlyn', alias: 'Caitlyn', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Caitlyn.png' },
  { id: 164, name: 'Camille', alias: 'Camille', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Camille.png' },
  { id: 69, name: 'Cassiopeia', alias: 'Cassiopeia', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Cassiopeia.png' },
  { id: 31, name: 'Cho\'Gath', alias: 'Chogath', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Chogath.png' },
  { id: 42, name: 'Corki', alias: 'Corki', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Corki.png' },
  { id: 122, name: 'Darius', alias: 'Darius', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Darius.png' },
  { id: 131, name: 'Diana', alias: 'Diana', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Diana.png' },
  { id: 36, name: 'Dr. Mundo', alias: 'DrMundo', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/DrMundo.png' },
  { id: 119, name: 'Draven', alias: 'Draven', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Draven.png' },
  { id: 245, name: 'Ekko', alias: 'Ekko', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Ekko.png' },
  { id: 60, name: 'Elise', alias: 'Elise', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Elise.png' },
  { id: 28, name: 'Evelynn', alias: 'Evelynn', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Evelynn.png' },
  { id: 81, name: 'Ezreal', alias: 'Ezreal', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Ezreal.png' },
  { id: 9, name: 'Fiddlesticks', alias: 'Fiddlesticks', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Fiddlesticks.png' },
  { id: 114, name: 'Fiora', alias: 'Fiora', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Fiora.png' },
  { id: 105, name: 'Fizz', alias: 'Fizz', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Fizz.png' },
  { id: 3, name: 'Galio', alias: 'Galio', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Galio.png' },
  { id: 41, name: 'Gangplank', alias: 'Gangplank', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Gangplank.png' },
  { id: 86, name: 'Garen', alias: 'Garen', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Garen.png' },
  { id: 150, name: 'Gnar', alias: 'Gnar', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Gnar.png' },
  { id: 79, name: 'Gragas', alias: 'Gragas', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Gragas.png' },
  { id: 104, name: 'Graves', alias: 'Graves', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Graves.png' },
  { id: 887, name: 'Gwen', alias: 'Gwen', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Gwen.png' },
  { id: 120, name: 'Hecarim', alias: 'Hecarim', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Hecarim.png' },
  { id: 74, name: 'Heimerdinger', alias: 'Heimerdinger', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Heimerdinger.png' },
  { id: 910, name: 'Hwei', alias: 'Hwei', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Hwei.png' },
  { id: 420, name: 'Illaoi', alias: 'Illaoi', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Illaoi.png' },
  { id: 39, name: 'Irelia', alias: 'Irelia', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Irelia.png' },
  { id: 427, name: 'Ivern', alias: 'Ivern', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Ivern.png' },
  { id: 40, name: 'Janna', alias: 'Janna', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Janna.png' },
  { id: 59, name: 'Jarvan IV', alias: 'JarvanIV', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/JarvanIV.png' },
  { id: 24, name: 'Jax', alias: 'Jax', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Jax.png' },
  { id: 126, name: 'Jayce', alias: 'Jayce', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Jayce.png' },
  { id: 202, name: 'Jhin', alias: 'Jhin', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Jhin.png' },
  { id: 222, name: 'Jinx', alias: 'Jinx', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Jinx.png' },
  { id: 145, name: 'Kai\'Sa', alias: 'Kaisa', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Kaisa.png' },
  { id: 429, name: 'Kalista', alias: 'Kalista', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Kalista.png' },
  { id: 43, name: 'Karma', alias: 'Karma', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Karma.png' },
  { id: 30, name: 'Karthus', alias: 'Karthus', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Karthus.png' },
  { id: 38, name: 'Kassadin', alias: 'Kassadin', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Kassadin.png' },
  { id: 55, name: 'Katarina', alias: 'Katarina', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Katarina.png' },
  { id: 10, name: 'Kayle', alias: 'Kayle', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Kayle.png' },
  { id: 141, name: 'Kayn', alias: 'Kayn', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Kayn.png' },
  { id: 85, name: 'Kennen', alias: 'Kennen', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Kennen.png' },
  { id: 121, name: 'Kha\'Zix', alias: 'Khazix', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Khazix.png' },
  { id: 203, name: 'Kindred', alias: 'Kindred', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Kindred.png' },
  { id: 240, name: 'Kled', alias: 'Kled', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Kled.png' },
  { id: 96, name: 'Kog\'Maw', alias: 'KogMaw', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/KogMaw.png' },
  { id: 897, name: 'K\'Sante', alias: 'KSante', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/KSante.png' },
  { id: 7, name: 'LeBlanc', alias: 'Leblanc', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Leblanc.png' },
  { id: 64, name: 'Lee Sin', alias: 'LeeSin', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/LeeSin.png' },
  { id: 89, name: 'Leona', alias: 'Leona', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Leona.png' },
  { id: 876, name: 'Lillia', alias: 'Lillia', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Lillia.png' },
  { id: 127, name: 'Lissandra', alias: 'Lissandra', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Lissandra.png' },
  { id: 236, name: 'Lucian', alias: 'Lucian', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Lucian.png' },
  { id: 117, name: 'Lulu', alias: 'Lulu', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Lulu.png' },
  { id: 99, name: 'Lux', alias: 'Lux', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Lux.png' },
  { id: 54, name: 'Malphite', alias: 'Malphite', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Malphite.png' },
  { id: 90, name: 'Malzahar', alias: 'Malzahar', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Malzahar.png' },
  { id: 57, name: 'Maokai', alias: 'Maokai', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Maokai.png' },
  { id: 11, name: 'Master Yi', alias: 'MasterYi', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/MasterYi.png' },
  { id: 800, name: 'Mel', alias: 'Mel', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Mel.png' },
  { id: 902, name: 'Milio', alias: 'Milio', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Milio.png' },
  { id: 21, name: 'Miss Fortune', alias: 'MissFortune', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/MissFortune.png' },
  { id: 82, name: 'Mordekaiser', alias: 'Mordekaiser', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Mordekaiser.png' },
  { id: 25, name: 'Morgana', alias: 'Morgana', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Morgana.png' },
  { id: 950, name: 'Naafiri', alias: 'Naafiri', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Naafiri.png' },
  { id: 267, name: 'Nami', alias: 'Nami', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Nami.png' },
  { id: 75, name: 'Nasus', alias: 'Nasus', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Nasus.png' },
  { id: 111, name: 'Nautilus', alias: 'Nautilus', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Nautilus.png' },
  { id: 518, name: 'Neeko', alias: 'Neeko', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Neeko.png' },
  { id: 76, name: 'Nidalee', alias: 'Nidalee', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Nidalee.png' },
  { id: 895, name: 'Nilah', alias: 'Nilah', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Nilah.png' },
  { id: 56, name: 'Nocturne', alias: 'Nocturne', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Nocturne.png' },
  { id: 20, name: 'Nunu & Willump', alias: 'Nunu', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Nunu.png' },
  { id: 2, name: 'Olaf', alias: 'Olaf', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Olaf.png' },
  { id: 61, name: 'Orianna', alias: 'Orianna', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Orianna.png' },
  { id: 516, name: 'Ornn', alias: 'Ornn', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Ornn.png' },
  { id: 80, name: 'Pantheon', alias: 'Pantheon', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Pantheon.png' },
  { id: 78, name: 'Poppy', alias: 'Poppy', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Poppy.png' },
  { id: 555, name: 'Pyke', alias: 'Pyke', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Pyke.png' },
  { id: 246, name: 'Qiyana', alias: 'Qiyana', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Qiyana.png' },
  { id: 133, name: 'Quinn', alias: 'Quinn', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Quinn.png' },
  { id: 497, name: 'Rakan', alias: 'Rakan', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Rakan.png' },
  { id: 33, name: 'Rammus', alias: 'Rammus', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Rammus.png' },
  { id: 421, name: 'Rek\'Sai', alias: 'RekSai', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/RekSai.png' },
  { id: 526, name: 'Rell', alias: 'Rell', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Rell.png' },
  { id: 888, name: 'Renata Glasc', alias: 'Renata', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Renata.png' },
  { id: 58, name: 'Renekton', alias: 'Renekton', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Renekton.png' },
  { id: 107, name: 'Rengar', alias: 'Rengar', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Rengar.png' },
  { id: 92, name: 'Riven', alias: 'Riven', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Riven.png' },
  { id: 68, name: 'Rumble', alias: 'Rumble', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Rumble.png' },
  { id: 13, name: 'Ryze', alias: 'Ryze', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Ryze.png' },
  { id: 360, name: 'Samira', alias: 'Samira', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Samira.png' },
  { id: 113, name: 'Sejuani', alias: 'Sejuani', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Sejuani.png' },
  { id: 235, name: 'Senna', alias: 'Senna', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Senna.png' },
  { id: 147, name: 'Seraphine', alias: 'Seraphine', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Seraphine.png' },
  { id: 875, name: 'Sett', alias: 'Sett', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Sett.png' },
  { id: 35, name: 'Shaco', alias: 'Shaco', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Shaco.png' },
  { id: 98, name: 'Shen', alias: 'Shen', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Shen.png' },
  { id: 102, name: 'Shyvana', alias: 'Shyvana', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Shyvana.png' },
  { id: 27, name: 'Singed', alias: 'Singed', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Singed.png' },
  { id: 14, name: 'Sion', alias: 'Sion', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Sion.png' },
  { id: 15, name: 'Sivir', alias: 'Sivir', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Sivir.png' },
  { id: 72, name: 'Skarner', alias: 'Skarner', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Skarner.png' },
  { id: 901, name: 'Smolder', alias: 'Smolder', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Smolder.png' },
  { id: 37, name: 'Sona', alias: 'Sona', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Sona.png' },
  { id: 16, name: 'Soraka', alias: 'Soraka', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Soraka.png' },
  { id: 50, name: 'Swain', alias: 'Swain', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Swain.png' },
  { id: 517, name: 'Sylas', alias: 'Sylas', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Sylas.png' },
  { id: 134, name: 'Syndra', alias: 'Syndra', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Syndra.png' },
  { id: 223, name: 'Tahm Kench', alias: 'TahmKench', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/TahmKench.png' },
  { id: 163, name: 'Taliyah', alias: 'Taliyah', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Taliyah.png' },
  { id: 91, name: 'Talon', alias: 'Talon', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Talon.png' },
  { id: 44, name: 'Taric', alias: 'Taric', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Taric.png' },
  { id: 17, name: 'Teemo', alias: 'Teemo', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Teemo.png' },
  { id: 412, name: 'Thresh', alias: 'Thresh', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Thresh.png' },
  { id: 18, name: 'Tristana', alias: 'Tristana', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Tristana.png' },
  { id: 48, name: 'Trundle', alias: 'Trundle', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Trundle.png' },
  { id: 23, name: 'Tryndamere', alias: 'Tryndamere', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Tryndamere.png' },
  { id: 4, name: 'Twisted Fate', alias: 'TwistedFate', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/TwistedFate.png' },
  { id: 29, name: 'Twitch', alias: 'Twitch', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Twitch.png' },
  { id: 77, name: 'Udyr', alias: 'Udyr', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Udyr.png' },
  { id: 6, name: 'Urgot', alias: 'Urgot', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Urgot.png' },
  { id: 110, name: 'Varus', alias: 'Varus', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Varus.png' },
  { id: 67, name: 'Vayne', alias: 'Vayne', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Vayne.png' },
  { id: 45, name: 'Veigar', alias: 'Veigar', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Veigar.png' },
  { id: 161, name: 'Vel\'Koz', alias: 'Velkoz', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Velkoz.png' },
  { id: 711, name: 'Vex', alias: 'Vex', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Vex.png' },
  { id: 254, name: 'Vi', alias: 'Vi', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Vi.png' },
  { id: 234, name: 'Viego', alias: 'Viego', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Viego.png' },
  { id: 112, name: 'Viktor', alias: 'Viktor', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Viktor.png' },
  { id: 8, name: 'Vladimir', alias: 'Vladimir', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Vladimir.png' },
  { id: 106, name: 'Volibear', alias: 'Volibear', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Volibear.png' },
  { id: 19, name: 'Warwick', alias: 'Warwick', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Warwick.png' },
  { id: 62, name: 'Wukong', alias: 'MonkeyKing', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/MonkeyKing.png' },
  { id: 498, name: 'Xayah', alias: 'Xayah', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Xayah.png' },
  { id: 101, name: 'Xerath', alias: 'Xerath', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Xerath.png' },
  { id: 5, name: 'Xin Zhao', alias: 'XinZhao', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/XinZhao.png' },
  { id: 157, name: 'Yasuo', alias: 'Yasuo', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Yasuo.png' },
  { id: 777, name: 'Yone', alias: 'Yone', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Yone.png' },
  { id: 83, name: 'Yorick', alias: 'Yorick', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Yorick.png' },
  { id: 804, name: 'Yunara', alias: 'Yunara', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Yunara.png' },
  { id: 350, name: 'Yuumi', alias: 'Yuumi', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Yuumi.png' },
  { id: 904, name: 'Zaahen', alias: 'Zaahen', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Zaahen.png' },
  { id: 154, name: 'Zac', alias: 'Zac', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Zac.png' },
  { id: 238, name: 'Zed', alias: 'Zed', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Zed.png' },
  { id: 221, name: 'Zeri', alias: 'Zeri', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Zeri.png' },
  { id: 115, name: 'Ziggs', alias: 'Ziggs', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Ziggs.png' },
  { id: 26, name: 'Zilean', alias: 'Zilean', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Zilean.png' },
  { id: 142, name: 'Zoe', alias: 'Zoe', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Zoe.png' },
  { id: 143, name: 'Zyra', alias: 'Zyra', image: 'https://ddragon.leagueoflegends.com/cdn/16.9.1/img/champion/Zyra.png' },
]

function normalizeChampionText(value: string): string {
  return value.toLowerCase().replace(/[^a-z0-9]/g, '')
}

export function getChampionById(championId: number): ChampionReference | undefined {
  return CHAMPIONS.find((champion) => champion.id === championId)
}

export function resolveChampionQuery(query: string): ChampionReference | undefined {
  const normalizedQuery = normalizeChampionText(query)
  if (!normalizedQuery) {
    return undefined
  }

  const exactMatch = CHAMPIONS.find((champion) => {
    return (
      normalizeChampionText(champion.name) === normalizedQuery ||
      normalizeChampionText(champion.alias) === normalizedQuery ||
      String(champion.id) === normalizedQuery
    )
  })

  if (exactMatch) {
    return exactMatch
  }

  return CHAMPIONS.find((champion) => {
    return (
      normalizeChampionText(champion.name).includes(normalizedQuery) ||
      normalizeChampionText(champion.alias).includes(normalizedQuery)
    )
  })
}
