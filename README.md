# Argon

Argon is a Java-first performance optimization mod for Minecraft Java Edition 26.3 on Fabric.

The goal is to improve rendering overhead, chunk rebuild scheduling, memory allocation pressure, and frame pacing while preserving gameplay correctness. Experimental renderer work remains opt-in until correctness, performance, and compatibility are demonstrated.

## Cloud builds

Compilation and tests run in GitHub Actions so contributors do not need to compile Minecraft on low-end hardware. Open the Actions tab and download the JAR artifact from a successful Argon CI run. The workflow provisions JDK 25 and Gradle 9.6.0.

## Current scope

- Fabric client bootstrap
- Conservative feature flags
- GitHub Actions compilation and artifact upload
- Architecture and benchmarking documentation

Chunk scheduling changes, memory pools, adaptive budgets, and a replacement renderer are not implemented yet. They will be added only after a baseline is measurable.

## OpenGL and Vulkan

Argon targets Minecraft's supported graphics backends, including OpenGL and Vulkan where supported by the selected game release and runtime. Shared code should use Minecraft rendering abstractions rather than raw backend-specific calls.

A custom Vulkan renderer is not part of the bootstrap. Experimental backend-specific work must be isolated, feature-gated, and validated independently.

A green build proves compilation, not FPS gains. See docs/ARCHITECTURE.md and docs/PERFORMANCE_METHODOLOGY.md.