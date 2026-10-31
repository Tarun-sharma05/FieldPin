# FieldPin: Product Brief (PRD)

## 1. Goal
A small, offline-first Android app for inspecting outdoor sites. A field officer maps sites as polygons, checks in only when physically inside one, and records evidence (condition, notes, stamped photos). It works fully in airplane mode and syncs later.

Submitted as a take-home task. Deadline: **Sun 1 Nov 2026, 11:59 PM IST**. Live review call 3 to 5 Nov.

## 2. User
A field officer visiting parks, water tanks and solar sites, often with weak or no network.

## 3. In scope (required, in priority order)
1. Login with mock auth, session survives restart, logout clears local data.
2. Sites list: 20+ seeded sites, search by name, filter by status, "Pending sync" badge.
3. Map: all sites as polygons coloured by status, live user location, my-location button, tap a polygon for a bottom sheet (name, area, distance).
4. Map a new site: tap corners (3 or more), show area (m² and acres) and perimeter (m), computed on the earth's surface.
5. Inspection with geofenced check-in, condition, notes, in-app CameraX photos with a stamp burned into the saved file.
6. Offline-first: everything works offline, "Sync now" pushes to a fake server, conflict behaviour documented.

Cut order if time runs short: sync polish, then photos, then new-site polish. Items 1 to 4 working well beat all six half-done.

## 4. Out of scope (until all required items work)
Walk mode, self-crossing rejection, GeoJSON export, document attachments, WorkManager sync, contacts, offline tiles, dark mode. Anything cut is listed in the README with the reason.

## 5. Success criteria (mapped to the scoring)
| Area | Weight | What shows it |
|---|---|---|
| It works | 25% | Release APK installs, map a site, check in, take photos, airplane mode OK |
| Maps, location, GIS | 20% | Correct area/perimeter, correct inside/outside, live location |
| Code quality | 20% | Clear layers, no duplication, sensible error handling |
| Camera and permissions | 10% | Stamp in saved file, denied and GPS-off handled |
| Offline and sync | 10% | Nothing lost offline, sync works, conflicts explained |
| Engineering habits | 15% | Small commits, tests, README, .gitignore, clean-checkout build |

## 6. Screens (design description)
Layout and style are free. Each screen must contain at least the following, including its non-happy states.

**Login.** Email, password, Sign in. Demo account shown in README. States: loading, wrong credentials, empty fields.

**Sites list.** Search field, filter chips (All / Active / Repair / Closed), cards showing name, category, area, last inspection date, status colour, and a Pending sync badge. "+" button opens New site. Bottom navigation: List | Map. States: loading, empty (no match), error.

**Map.** All site polygons coloured by status (green Active, amber Needs repair, red Closed). User dot and my-location button. Tapping a polygon selects it (outlined) and opens a bottom sheet with name, area, distance from user, and a "check in" button that is disabled while the user is outside. States: no location permission, GPS off, no network (tiles blank, polygons still drawn).

**New site (map a boundary).** Tap the map to add corners, undo last corner, live area and perimeter shown below the map, Save site (enabled at 3 or more points). States: fewer than 3 points, GPS off.

**Inspection.** Header with site name and check-in time. Condition (Good / Fair / Poor), photo grid with "+", notes, Submit. Start is allowed only inside the polygon, otherwise show how far away the user is.

**Camera.** CameraX preview, capture, flip, flash. The stamp (site name, latitude/longitude, accuracy, date-time) is drawn into the saved image. States: permission denied, no camera.

## 7. Non-functional requirements
- minSdk 26, targetSdk 35 or 36. Kotlin and Jetpack Compose only.
- Location: Fused Location Provider, foreground only, stopped when the screen is not visible.
- Builds with `./gradlew assembleDebug` on a clean checkout, with no files from the reviewers.
- No secrets, keystores, local.properties, or hardcoded URLs anywhere in git history.
- Real package name (not `com.example.*`).

## 8. Risks
| Risk | Plan |
|---|---|
| MapLibre Compose API is unstable | Time-boxed spike. Keep the map behind our own composables so we can swap to osmdroid. |
| Returning to Android after a break | Use official docs plus AI for scaffolding. Own the geometry and sync logic personally. |
| Only about 3 days left | Follow the cut order. Commit daily. Start the README early. |
| Secret leaking into history | AGENTS.md rules, `git status` before every commit. |
