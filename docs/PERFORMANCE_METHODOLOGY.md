# Argon Performance Methodology

## Baseline first

Do not claim performance gains until vanilla and Argon runs use the same system, game version, driver, JVM options, world, settings, render distance, camera path, warm-up, and run duration.

## Scenarios

1. Stationary view in dense terrain.
2. Fast camera rotation across a landscape.
3. Fast travel into unexplored terrain.
4. Village with entities and block entities.
5. Repeated block placement and destruction.
6. Long-duration session to reveal memory growth.

## Metrics

- Average FPS and median frame time.
- 95th and 99th percentile frame time.
- 1% low FPS, with the calculation method recorded.
- Chunk rebuild latency and queue depth.
- CPU time and allocation rate.
- Garbage-collection pause duration.
- Heap usage and memory growth over time.
- GPU timings where supported by appropriate tooling.

CPU frame timing alone does not establish whether a workload is GPU-bound. Use Java Flight Recorder or an equivalent profiler to locate CPU and allocation hot spots.

## Reporting

Record hardware, OS, graphics driver, Minecraft version, JVM flags, mod list, scenario, warm-up, repetitions, and measurement tool. Report repeated-run median and variability. Do not compare a cold vanilla run with a warmed-up Argon run.

Hosted CI is for compilation and correctness tests, not absolute FPS thresholds. Performance numbers should come from controlled hardware.