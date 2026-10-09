# Argon — Minecraft 26.2 renderer prototype

This experimental branch is isolated from support/minecraft-26.2, dev/argon-core, and main. It is a renderer-observer prototype, not yet a replacement terrain renderer.

## Cloud build

GitHub Actions uses Java 25 and Gradle 9.6.0. A successful run uploads a single runtime JAR named argon-mc26.2-0.2.0-renderer-prototype.jar.

## What this build does

- Keeps Minecraft's terrain GPU submissions, chunk mesh compiler, textures, render pipelines, and GPU buffer lifecycle unchanged.
- Adds an opt-in observer around ChunkSectionsToRender.renderGroup.
- Records opaque and translucent terrain-group CPU wall time, P50/P95/max, plus observed native draw groups and draw entries.
- Exposes results via /argon status so current terrain-draw cost can be measured before writing a replacement backend.
- Keeps the actual experimental renderer feature marked UNAVAILABLE until Argon can submit terrain independently.

This is intentionally a no-behavior-change instrumentation milestone. It does not claim FPS gains and does not yet provide a custom drawing algorithm. The next implementation stage can use this data to choose between a custom render-pass/submission implementation and a less invasive optimization.

## Enable the observer

Use a separate Minecraft 26.2 Fabric instance and a disposable world. Set these properties in config/argon.properties, close and restart the game:

    telemetry.enabled=true
    renderer.experimental.enabled=true
    chunks.cancelled-task-cleanup.enabled=false

Then enter the world, travel around for a couple of minutes, and run /argon status. The observer adds CPU work, so final performance comparisons should use a separate run with telemetry.enabled=false. Do not compare the observer-enabled run directly against an uninstrumented run for FPS.

## Compatibility

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.5+
- Fabric API 0.161.0+26.2
- Java 25

Keep backups and test only in a disposable world. A passing cloud build means code compiles and tests pass; it does not prove successful in-game startup, renderer correctness, or performance improvement.
