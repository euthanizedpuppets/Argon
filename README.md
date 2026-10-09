# Argon

Argon is a Java-first performance optimization mod for Minecraft Java Edition 26.3 on Fabric.

The goal is to improve rendering overhead, chunk rebuild scheduling, memory allocation pressure, and frame pacing while preserving gameplay correctness. Experimental changes remain opt-in until correctness, performance, and compatibility are demonstrated.

## Cloud builds

Compilation and tests run in GitHub Actions so contributors do not need to compile Minecraft on low-end hardware. Open the Actions tab and download the JAR artifact from a successful Argon CI run. The workflow provisions JDK 25 and Gradle 9.6.0.

## Current scope

- Fabric client bootstrap and safe configuration loading
- `/argon status` client-side diagnostic command
- World-pass interval sampling from Minecraft 26.3's Fabric `LevelRenderEvents.END_MAIN` event, disabled by `telemetry.enabled=false`
- Min/average/max/P50/P95 interval summaries from one rolling-window sort
- Opt-in cleanup of already-cancelled entries in Minecraft's native section-task queue
- Conservative feature flags, bounded queue primitives, and GitHub Actions tests/artifact upload

Native queue cleanup is disabled by default through `chunks.cancelled-task-cleanup.enabled=false`. When enabled, it removes cancelled entries before new tasks are appended, periodically and only above a queue-size threshold. It does not replace vanilla's camera-distance prioritization, task quota, worker, or buffer lifecycle. Its compatibility and performance still require in-game validation; no FPS gain is claimed.

World-pass intervals are a diagnostic estimate, not a GPU timestamp or guaranteed FPS measurement. A custom Vulkan renderer, wholesale replacement chunk scheduler, memory pools, and adaptive budgets are not implemented.

A green build proves compilation and unit tests, not actual in-game compatibility or FPS gains. See docs/ARCHITECTURE.md and docs/PERFORMANCE_METHODOLOGY.md.
