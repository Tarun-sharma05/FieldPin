# FieldPin

FieldPin is an offline-first Android app for mapping outdoor sites and recording field inspections.

## Build and run

Open the project in Android Studio and run the `app` configuration, or use `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`. The app seeds sample sites on first launch. Demo login: `officer@fieldpin.com` / `password123`.

Map polygons and saved site data are local, so they remain visible in airplane mode. MapLibre's online basemap tiles may be blank offline; the polygons and (when a device fix is available) location overlay are rendered independently of those tiles.

## Architecture

Compose screens and ViewModels depend on domain models and repository interfaces. Data implementations use Room, Android location/camera APIs and a configurable fake remote.

```text
Compose UI -> ViewModels -> domain repositories/use cases
                            ^                    ^
                            |                    |
                         Room/data  <------> remote adapters
```

Composables receive domain models, not Room entities. Geometry lives in pure Kotlin under `domain/geometry`.

## Geometry

Polygon area uses the Chamberlain–Duquette spherical excess formula with Earth radius 6,371,008.8 m; perimeter and point distances use the Haversine formula. A point within 0.1 m of an edge or vertex counts as inside. Outside distance is the shortest distance to an edge. Areas below three vertices are treated as zero; point-in-polygon returns false for invalid polygons. Acres use 1 acre = 4,046.8564224 m².

The Jaipur seed polygons are illustrative sample outlines stored in `app/src/main/assets/seed_sites.json`; they are not surveyed property boundaries and should not be used for operational decisions.

Check-in requires the point inside the boundary and GPS accuracy at or better than 50 m. This avoids accepting a fix whose uncertainty could place the officer outside.

## Offline sync and conflicts

Site edits and inspections are saved in Room first. “Sync now” retries pending and failed site rows. The fake remote applies the build-configured delay and failure rate; when the device is offline local rows stay saved and can be retried after connectivity returns. Successful site writes increment the server version and become `SYNCED`. Transient errors become `FAILED` with a reason. A stale server version is rejected: the server copy is retained locally and marked `FAILED` with a conflict reason, avoiding a silent overwrite. Inspections are append-only locally; this fake sync slice currently pushes sites only.

## Scope and next steps

Offline map tiles, clustering, real backend integration, and a production-grade conflict review flow are out of scope: they require durable infrastructure or substantially more UX and testing than this demo slice. The fake remote is process-memory only and does not simulate durable server storage. Next, connect a real authenticated API, sync inspections and photo metadata, add a conflict-resolution screen, and verify camera stamping and permission flows on physical devices.

## AI-use note

AI assistance was used to implement and document application slices, write unit tests, and diagnose build errors. The project owner should review and understand all changes before release. No AI-generated credentials or secrets are used.
