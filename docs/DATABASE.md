# FieldPin: Database Design (Room)

Database name: `fieldpin.db`. Version 1. `exportSchema = true` and commit the generated schema folder.

## Entities

### site
| Column | Type | Notes |
|---|---|---|
| id | TEXT PK | UUID generated on the device, so offline creation never collides |
| name | TEXT | indexed for search |
| category | TEXT | e.g. Park, Water tank, Solar site |
| status | TEXT | `ACTIVE`, `NEEDS_REPAIR`, `CLOSED` (indexed for filtering) |
| boundaryJson | TEXT | list of lat/lng points, via a TypeConverter (kotlinx-serialization) |
| areaSqm | REAL | computed by the geometry code on save |
| perimeterM | REAL | computed by the geometry code on save |
| lastInspectionAt | INTEGER? | epoch millis, nullable |
| createdAt | INTEGER | epoch millis |
| updatedAt | INTEGER | epoch millis, set on every local edit |
| serverVersion | INTEGER | the version last received from the server, `0` if never synced |
| syncState | TEXT | `SYNCED`, `PENDING`, `FAILED` |

### inspection
| Column | Type | Notes |
|---|---|---|
| id | TEXT PK | UUID |
| siteId | TEXT FK -> site.id | indexed, cascade delete |
| checkInAt | INTEGER | epoch millis |
| checkInLat, checkInLng | REAL | position at check-in |
| checkInAccuracyM | REAL | GPS accuracy at check-in |
| condition | TEXT | `GOOD`, `FAIR`, `POOR` |
| notes | TEXT | |
| status | TEXT | `DRAFT`, `SUBMITTED` |
| createdAt | INTEGER | |
| syncState | TEXT | as above |

### photo
| Column | Type | Notes |
|---|---|---|
| id | TEXT PK | UUID |
| inspectionId | TEXT FK -> inspection.id | indexed, cascade delete |
| filePath | TEXT | app-private storage path of the stamped image |
| takenAt | INTEGER | |
| lat, lng, accuracyM | REAL | values drawn into the stamp |

### Session (not in Room)
Kept in DataStore: signed-in user id and email, a fake token. Logout clears DataStore, all Room tables, and the photo files.

## Relationships
`site 1 -> N inspection`, `inspection 1 -> N photo`. Queries needed: all sites as Flow, sites filtered by search text and status, site by id, inspections for a site, photos for an inspection, rows with `syncState != SYNCED`.

## Seeding
At least 20 sites with real boundary coordinates in one Indian city, stored as a JSON file in `assets/`. On first launch the file is loaded into Room. Check that every polygon is valid (3 or more points, not self-crossing, plausible area), and that you can state where the coordinates came from (traced by hand on a map is fine).

## Pending sync
A row is `PENDING` after any local create or edit. The Sites list shows the "Pending sync" badge when the site or any of its inspections is `PENDING` or `FAILED`.

## Conflict strategy: DECISION FOR YOU
The README must explain what happens when the same site is edited on two phones while both are offline. Pick one and write why:

| Option | How it works | What you lose |
|---|---|---|
| Last write wins (by `updatedAt`) | Newest edit overwrites | The older edit is silently lost; clock differences matter |
| Version check, reject stale | Client sends `serverVersion` with the edit, server rejects if it changed | Needs a conflict screen or an automatic rule |
| Field-level merge | Only changed fields are compared | More logic and more tests |

Default suggestion for a 3-day scope: **version check plus a simple rule**. Inspections are append-only, so they never conflict. A conflicting site edit keeps the server copy and marks the local copy `FAILED` with the reason, so nothing is lost silently. Whatever you choose, implement exactly that and explain it in your own words.

## Migrations
Version 1 only for this submission. If the schema changes during development, bump the version and either write a migration or, before submission, reset to version 1 (acceptable here because the app has no released users).
