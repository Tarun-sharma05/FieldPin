# FieldPin: Feature Specs

Spec-driven development: each spec below is a unit of work. Do one at a time, in order. A spec is done when every acceptance criterion passes, tests are written, `./gradlew assembleDebug` succeeds, and one clean commit exists.

## Spec template (copy for new work)
```
## S<n>: <name>
Goal:
Inputs / outputs:
Acceptance criteria:
Tests:
Out of scope:
Open decisions:
```

## Build order
S0 -> S1 -> S2 -> S3 -> S4 -> S5 -> S6 -> S7 -> S8 -> S9 -> S10. Bonus work only after S1 to S9 work.

---

## S0: Setup
Goal: a project that builds on a clean checkout and has the structure and rules in place.
Acceptance criteria:
- `./gradlew assembleDebug` succeeds after a fresh clone with no extra files.
- Real package name, minSdk 26, targetSdk 35 or 36.
- `gradle/libs.versions.toml` holds all versions. Koin starts in the Application class.
- `BuildConfig` fields exist per build type. No URL is hardcoded in Kotlin.
- Package folders from `ARCHITECTURE.md` exist. `AGENTS.md` and `docs/` are committed.
Tests: build only.

## S1: Geometry (domain/geometry), written by you
Goal: pure Kotlin functions with no Android imports.
Functions (signatures are a suggestion):
- `polygonAreaSqm(points: List<LatLng>): Double`
- `polygonPerimeterM(points: List<LatLng>): Double`
- `isPointInPolygon(point: LatLng, polygon: List<LatLng>): Boolean`
- `distanceMeters(a: LatLng, b: LatLng): Double`
- `distanceToPolygonMeters(point: LatLng, polygon: List<LatLng>): Double`
- `sqmToAcres(sqm: Double): Double`
Acceptance criteria:
- Area and perimeter are computed on the earth's surface, not by treating lat/lng as flat x/y. State the method in the README.
- Invalid input (fewer than 3 points) is handled explicitly, not by crashing.
- A point exactly on the boundary has a defined result, stated in the README.
- Outside distance is the distance to the nearest edge.
Tests (minimum): a roughly 100 m by 100 m square near the chosen city is about 10,000 m² (within 1%); triangle; concave polygon with a point in the notch; point inside, outside, on an edge, on a vertex; area is the same regardless of point order direction; acres conversion.
Open decisions: calculation method (spherical excess, equal-area projection, or geodesic library), and which earth radius.

## S2: Data layer
Goal: Room is the source of truth, with seed data.
Acceptance criteria:
- Entities and DAOs match `DATABASE.md`. Schema export is committed.
- 20 or more valid seed sites load once on first launch.
- Repositories expose `Flow`. Search and status filter work at the query level.
Tests: DAO or repository tests for search and filter (optional if time is short), seed file validity.

## S3: Login and session
Goal: mock authentication that survives a restart.
Acceptance criteria:
- A demo account works. Wrong credentials show an error. Empty fields are validated.
- The session is stored in DataStore. The app opens on the Sites list after a restart.
- Logout clears DataStore, Room tables and photo files, and returns to Login.
Tests: auth repository (valid, invalid), session persistence logic.

## S4: Sites list
Acceptance criteria:
- Each card shows name, category, area, last inspection date, status colour, Pending sync badge.
- Search by name and status filter work together. Empty and loading states exist.
- "+" opens New site. Bottom navigation switches List and Map.
Tests: filter and search combination logic.

## S5: Map screen
Acceptance criteria:
- All sites drawn as polygons coloured by status. The map is behind our own composable `SitesMap(sites, userLocation, selectedSiteId, onSiteTap)`.
- Live location dot and a my-location button. Location starts and stops with the screen's visibility.
- Tapping a polygon selects it (outlined) and opens a bottom sheet with name, area and distance. Check-in is disabled while outside.
- Permission asked on first use with an explanation. Deny and "Don't ask again" both handled, with a Settings link. GPS-off state shown.
- Works in airplane mode (polygons and location still draw; tiles may be blank, documented).
Tests: distance and selection logic through domain code.

## S6: Map a new site
Acceptance criteria:
- Tapping the map adds a corner. Undo removes the last corner.
- Area (m² and acres) and perimeter (m) update live, using S1 functions.
- Save is disabled below 3 points. The saved site is `PENDING` and appears in the list immediately.
Tests: metrics are produced by S1, not duplicated in UI code.

## S7: Inspection with geofenced check-in
Acceptance criteria:
- Start inspection works only when the device is inside the polygon (S1). Outside, the screen shows how far away the user is.
- Saves check-in time, coordinates and accuracy, condition (Good / Fair / Poor) and notes.
- Submitting updates the site's last inspection date and marks rows `PENDING`.
Tests: check-in rule (inside, outside, boundary, no location fix).
Open decisions: whether a very inaccurate GPS reading should block check-in. Decide, then document.

## S8: Camera and stamp
Acceptance criteria:
- CameraX preview, capture, flip, flash. Camera permission asked on first use with an explanation and handled when denied.
- The saved file has the stamp drawn into it: site name, latitude/longitude, accuracy, date-time. Check by opening the file in the gallery.
- Orientation of the stamped image is correct. Photos are linked to the inspection.
Tests: stamp text formatting (pure function); the rest is a manual check.

## S9: Fake remote and sync
Acceptance criteria:
- `SiteRemoteDataSource` interface with a fake implementation that adds a delay and fails some of the time (rate from build config).
- "Sync now" pushes `PENDING` rows. Successes become `SYNCED`, failures become `FAILED` with a reason, and can be retried.
- Offline: nothing is lost, and sync succeeds after airplane mode is turned off.
- The conflict strategy from `DATABASE.md` is implemented exactly as written.
Tests: sync success, failure then retry, conflict rule.

## S10: Release and README
Acceptance criteria:
- README covers: build and run, demo login, architecture and a short diagram, how area and inside/outside are calculated, how offline sync and conflicts work, what was cut and why, what you would do next, and an honest AI-use note.
- A clean clone builds with `./gradlew assembleDebug`. Unit tests pass.
- The APK is attached to a GitHub Release (not committed). The release APK shows a working map.
- History check: no key, keystore, `local.properties` or hardcoded URL in any commit.
- Optional: GitHub Actions workflow that builds and tests on every push.
