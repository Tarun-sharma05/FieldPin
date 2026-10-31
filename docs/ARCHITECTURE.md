# FieldPin: Architecture

## 1. Approach
Single Gradle module (`:app`) with strict package layers. Reason: three days of work. Multi-module would add build overhead without a payoff here, and is listed as a "next step" in the README.

Pattern: **MVVM with a clean-architecture layering**.

```
ui (Compose screens + ViewModels)
        |  state down, events up
        v
domain (models, repository interfaces, use cases, geometry)   <- pure Kotlin, no Android imports
        ^
        |  implements
data (Room, fake remote, repositories, sync, session, location)
```

Rules: `ui` depends on `domain`. `data` depends on `domain`. `domain` depends on nothing. `ui` never touches Room or the remote directly.

## 2. Package structure
Replace `<pkg>` with your real package name.

```
<pkg>/
  FieldPinApp.kt            Application, starts Koin
  MainActivity.kt           single activity, hosts navigation
  di/                       Koin modules (data, domain, ui)
  navigation/               routes + NavHost
  core/                     Result type, dispatchers, small utils
  domain/
    model/                  Site, Inspection, Photo, LatLng, SiteStatus, Condition, SyncState
    geometry/               area, perimeter, point-in-polygon, distance (pure, unit-tested)
    repository/             SiteRepository, InspectionRepository, AuthRepository, SyncRepository (interfaces)
    usecase/                CheckInUseCase, ComputeSiteMetrics, etc. (only where logic exists)
  data/
    local/                  AppDatabase, DAOs, entities, mappers, type converters, seeding
    remote/                 SiteRemoteDataSource (interface) + FakeSiteRemoteDataSource
    repository/             implementations of the domain interfaces
    sync/                   SyncManager (push pending changes, handle conflicts)
    session/                SessionStore (DataStore)
    location/               LocationProvider (Fused Location, exposed as Flow)
    photo/                  PhotoStamper (draws the stamp onto the saved image)
  ui/
    theme/  components/
    login/  sites/  map/  newsite/  inspection/  camera/
```

## 3. Libraries and dependencies
Declare everything in `gradle/libs.versions.toml`. Take versions from each project's official page. Do not copy numbers from chat or blogs.

| Purpose | Library |
|---|---|
| UI | Compose BOM, Material 3, `activity-compose`, `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose` |
| Navigation | Navigation Compose (type-safe routes) |
| DI | Koin (`koin-android`, `koin-androidx-compose`) |
| Local DB | Room (`room-runtime`, `room-ktx`, KSP compiler) |
| Session | Jetpack DataStore (Preferences) |
| Async | kotlinx-coroutines (android) |
| JSON | kotlinx-serialization-json (seed data, fake remote payloads, boundary converter) |
| Map | MapLibre Compose + one Android runtime (OpenGL), same version |
| Location | `play-services-location` (Fused Location Provider) |
| Camera | CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`) |
| Tests | JUnit 4, `kotlin-test`, `kotlinx-coroutines-test`, optionally Turbine and MockK |

Geometry is written by hand, with no GIS library. This keeps it explainable on the live call. If a library is used anywhere, name it in the README.

## 4. Data flow
- Room is the single source of truth. The UI only reads from Room through repositories (`Flow`).
- Writes go to Room first and are marked `PENDING`. The UI updates immediately, so the app is offline-first.
- `SyncManager` pushes pending rows to `SiteRemoteDataSource` when "Sync now" is tapped. The fake remote adds a delay and fails randomly (configurable), so offline and retry paths are actually exercised.

## 5. Sync and conflicts (to be defined in `DATABASE.md`)
The README must explain what happens when one site is edited on two phones offline. Decide the strategy once, write it down, and implement exactly that.

## 6. Location
`LocationProvider` exposes a `Flow<LocationState>` (Fix(lat, lng, accuracy), NoPermission, GpsOff). Collection is tied to the screen lifecycle (`collectAsStateWithLifecycle` or `repeatOnLifecycle(STARTED)`), so updates stop when the screen is not visible. No background location permission in the manifest.

## 7. Camera and stamp
CameraX `ImageCapture` saves to app-private storage. `PhotoStamper` then decodes the saved image, respects its EXIF rotation, draws the stamp text (site name, lat/lng, accuracy, date-time) with a readable background, and writes the result back to the file. The stamp must be in the saved file, not only on the preview.

## 8. Build configuration
- `BuildConfig` fields per build type: `MAP_STYLE_URL`, `FAKE_API_FAILURE_RATE`, `FAKE_API_DELAY_MS`.
- The map style URL is read from `local.properties` if present, with a default in `build.gradle.kts` per build type so a clean checkout builds without extra files.
- `local.properties.example` lists the key names only.
- `minSdk = 26`, `targetSdk = 35` or `36`, Java and Kotlin toolchain set explicitly.

## 9. Testing
- Unit (JVM): geometry (area, perimeter, point-in-polygon, distance), check-in rule, sync conflict rule, search and filter logic.
- Manual checklist: airplane mode flow, permission denied, "Don't ask again" to Settings, GPS off.
- Optional: GitHub Actions workflow that runs `assembleDebug` and unit tests.

## 10. Decisions to explain on the live call
Why osmdroid or MapLibre. Why Koin. Why a single module. Why Room is the source of truth. The conflict strategy and what it loses. How area is computed on a sphere. How the map would scale to thousands of sites (load only visible bounds, simplify distant polygons, cluster when zoomed out).
