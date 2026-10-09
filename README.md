# Argon — Minecraft 26.2 test branch

This branch targets Minecraft Java Edition **26.2** on Fabric so the client can be tested with the graphics backend options available to this game version. It is intentionally separate from the 26.3 development branch.

## Cloud build

GitHub Actions uses Java 25 and Gradle 9.6.0. A successful run uploads a single runtime JAR named `argon-mc26.2-0.1.0.jar`. Do not use the source JAR from other builds.

## Install requirements

- Minecraft Java Edition 26.2
- Java 25
- Fabric Loader 0.19.5 or newer
- Fabric API 0.161.0+26.2 or newer for this game version

Use a separate Fabric instance for testing. Back up worlds before testing any optimization mod.

## Current scope

- Safe config loading and the `/argon status` diagnostic command
- World-render-pass interval samples through Fabric `LevelRenderEvents.END_MAIN`, disabled by `telemetry.enabled=false`
- Min/average/max/P50/P95 interval summaries
- Opt-in cleanup of already-cancelled entries in Minecraft's native section-task queue
- Unit tests and a Gradle Mixin metadata/target declaration check

Queue cleanup is disabled by default with `chunks.cancelled-task-cleanup.enabled=false`. It does not replace vanilla chunk scheduling or its distance ordering. Its runtime behavior and performance need to be tested in-game; no FPS improvement is claimed.

These samples are world-render-pass intervals, not GPU timestamps or a guaranteed FPS counter. A green CI build proves compilation and unit tests, not successful in-game startup or performance gains.

See [docs/IN_GAME_TEST_PLAN.md](docs/IN_GAME_TEST_PLAN.md).
