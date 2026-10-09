# Target Versions

Initial target: Minecraft Java Edition 26.3.

| Component | Pinned target | Notes |
|---|---|---|
| Java toolchain | 25 | Required by the selected Minecraft generation |
| Minecraft | 26.3 | Initial supported version |
| Fabric Loader | 0.19.5 | Verify resolution in CI |
| Fabric Loom | 1.17.21 | Stable 1.17 line at bootstrap time |
| Gradle | 9.6.0 | Installed by GitHub Actions |

## Verification requirements

The CI build is the authoritative compatibility check. It must resolve pinned artifacts, compile, package a JAR, and upload it. Do not bump versions solely because a newer release exists; verify compatibility and update the dependency matrix deliberately.

Minecraft 26.3 uses the modern non-obfuscated Loom plugin ID net.fabricmc.fabric-loom. Do not copy the older remapping configuration from pre-26.1 templates without adapting it.

Fabric API is omitted from the first bootstrap because the entry point only requires Fabric Loader. Add a 26.3-compatible Fabric API release only when needed.