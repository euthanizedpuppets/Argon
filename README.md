# Argon — Minecraft 26.2 test branch

This branch targets Minecraft Java Edition **26.2** on Fabric so the client can be tested with the graphics backend options available to this game version. It is intentionally separate from the 26.3 development branch.

## Cloud build

GitHub Actions uses Java 25 and Gradle 9.6.0. A successful run uploads a single runtime JAR named `argon-mc26.2-0.1.1.jar`. Do not use the source JAR from other builds.

## Install requirements

- Minecraft Java Edition 26.2
- Java 25
- Fabric Loader 0.19.5 or newer
- Fabric API 0.161.0+26.2 or newer for this game version

Use a separate Fabric instance for testing. Back up worlds before testing any optimization mod.

## Current scope

- Safe config loading and the `/argon status` diagnostic command
- World-render-pass interval samples through Fabric `LevelRenderEvents.END_MAIN`, disabled by `telemetry.enabled=false`
- Integrated-server tick-work min/average/max/P50/P95 samples in single-player worlds
- Min/average/max/P50/P95 render interval summaries
- Opt-in cleanup of already-cancelled entries in Minecraft's native section-task queue
- Stable linear compaction when pruning cancelled tasks from the native random-access queue
- Optional-cleanup scan counts, entries inspected/pruned, and average/maximum scan-cost diagnostics
- Native section-task queue add/poll/clear counters plus current and peak queue depth, collected only while local telemetry is enabled
- Unit tests and a Gradle Mixin metadata/target declaration check

Native queue metrics observe the existing queue without changing scheduling or task order. They count queue operations and record depth after additions/polls; they do not count GPU work or prove a performance gain. Integrated tick-work times can help determine whether single-player server processing itself is taking too long. They don't measure remote multiplayer server performance, scheduling delay, or save time during shutdown.

Queue cleanup is disabled by default with `chunks.cancelled-task-cleanup.enabled=false`. It does not replace vanilla chunk scheduling or its distance ordering. Its runtime behavior and performance need to be tested in-game; the new scan-cost metrics help quantify the extra work if you opt in. No FPS improvement is claimed.

World-render samples are intervals between world-render callbacks, not GPU timestamps or a guaranteed FPS counter. A green CI build proves compilation and unit tests, not successful in-game startup or performance gains.

See [docs/IN_GAME_TEST_PLAN.md](docs/IN_GAME_TEST_PLAN.md).
