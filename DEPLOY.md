# ComfortPick deployment

Current recommended public setup:

- frontend: Vercel
- backend API: Render web service
- database: Render Postgres

## 1. Current database backup

The current local database backup is:

- [backups/comfortpick-2026-05-07.sql](</C:/Users/errmi/Documents/New project/backups/comfortpick-2026-05-07.sql>)

It is a plain PostgreSQL SQL dump.

## 2. Deploy backend on Render

Render is configured from:

- [render.yaml](</C:/Users/errmi/Documents/New project/render.yaml>)

Create the Blueprint from this repository.

Required secret values during Blueprint creation:

- `RIOT_API_KEY`
- `COMFORTPICK_WEB_ALLOWED_ORIGINS`

Set `COMFORTPICK_WEB_ALLOWED_ORIGINS` to the final Vercel site origin, for example:

```text
https://comfort-pick.vercel.app
```

After deploy, note the Render backend URL, for example:

```text
https://comfortpick-api.onrender.com
```

## 3. Restore the database into Render Postgres

From any machine with `psql` installed:

```powershell
psql "<RENDER_EXTERNAL_DATABASE_URL>" -f "C:\Users\errmi\Documents\New project\backups\comfortpick-2026-05-07.sql"
```

Use the external database URL from the Render Postgres dashboard.

## 4. Deploy frontend on Vercel

Deploy the `frontend` directory as the Vercel project root.

Set:

```text
VITE_API_BASE_URL=https://<your-render-backend>.onrender.com
```

SPA rewrites are already configured in:

- [frontend/vercel.json](</C:/Users/errmi/Documents/New project/frontend/vercel.json>)

## 5. Final check

After both sides are live:

1. open the Vercel site
2. search a stored account
3. confirm profile/dashboard loads
4. confirm sync requests reach the Render backend
5. confirm the restored data is present
