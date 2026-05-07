import { createBrowserRouter } from 'react-router-dom'
import { App } from './App'
import { SearchPage } from '../pages/SearchPage'
import { ProfilePage } from '../pages/ProfilePage'
import { EnemyCountersPage } from '../pages/EnemyCountersPage'
import { MatchupDetailPage } from '../pages/MatchupDetailPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      {
        index: true,
        element: <SearchPage />,
      },
      {
        path: 'profiles/:summonerId',
        element: <ProfilePage />,
      },
      {
        path: 'profiles/:summonerId/enemies/:enemyChampionId',
        element: <EnemyCountersPage />,
      },
      {
        path: 'profiles/:summonerId/enemies/:enemyChampionId/counters/:userChampionId',
        element: <MatchupDetailPage />,
      },
    ],
  },
])
