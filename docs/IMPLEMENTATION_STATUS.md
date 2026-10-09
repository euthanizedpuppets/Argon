# Argon Implementation Status — Minecraft 26.2

## Branch isolation

`support/minecraft-26.2` is the compatibility/testing branch for Minecraft Java Edition 26.2. It is based on the current 26.3 development work but has independent version metadata, artifact naming, and documentation. It does not change `dev/argon-core` or `main`.

## Included

- Validated configuration parsing and safe fallback for malformed properties;
- Bounded rolling render-interval samples with min/average/max/P50/P95 summaries;
- Fabric `LevelRenderEvents.END_MAIN` integration when local metrics are enabled;
- A bounded, deduplicating priority queue for Argon's future pure-data tasks;
- An opt-in Mixin to prune already-cancelled entries from Minecraft's native section-task queue;
- Guarded feature flags and `/argon status`;
- Unit tests plus a Gradle Mixin metadata/target declaration check;
- A cloud build that uploads only `argon-mc26.2-0.1.0.jar`.

Native queue cleanup is disabled by default. Its runtime compatibility and performance are not yet verified in Minecraft 26.2. The feature does not claim an FPS uplift.

World-pass samples are elapsed intervals between render callbacks. They are not GPU timestamps, presentation timestamps, or a definitive FPS counter. Local metrics are opt-in via `telemetry.enabled=true`; no remote reporting exists.

## Stability gates

1. GitHub Actions compiles against Minecraft 26.2 with all unit tests passing.
2. The packaged 26.2 JAR artifact is produced.
3. Mixin resource metadata and target declarations pass the Gradle verification task.
4. The game starts with cleanup disabled and `/argon status` works.
5. If the default configuration is stable, test cleanup opt-in only in a disposable world.
6. Do not claim FPS gains until repeatable, controlled comparisons support them.
