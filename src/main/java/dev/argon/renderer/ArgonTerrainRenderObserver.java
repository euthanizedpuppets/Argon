package dev.argon.renderer;

import dev.argon.performance.FrameTimeTracker;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * Opt-in observer around vanilla's prepared terrain draw groups.
 *
 * This is deliberately not a replacement renderer yet: Minecraft still owns
 * GPU submission, terrain pipelines, textures, and buffer lifetimes. The
 * observer measures the actual renderGroup call and counts the existing draw
 * groups so the first renderer experiment has a real baseline.
 */
public final class ArgonTerrainRenderObserver {
    private final FrameTimeTracker opaquePassTimes;
    private final FrameTimeTracker translucentPassTimes;
    private final LongAdder opaqueCalls = new LongAdder();
    private final LongAdder translucentCalls = new LongAdder();
    private final LongAdder opaqueDrawGroups = new LongAdder();
    private final LongAdder translucentDrawGroups = new LongAdder();
    private final LongAdder opaqueDrawEntries = new LongAdder();
    private final LongAdder translucentDrawEntries = new LongAdder();
    private final ThreadLocal<ArrayDeque<PassStart>> inFlight =
            ThreadLocal.withInitial(ArrayDeque::new);

    public ArgonTerrainRenderObserver(int sampleWindow) {
        this.opaquePassTimes = new FrameTimeTracker(sampleWindow);
        this.translucentPassTimes = new FrameTimeTracker(sampleWindow);
    }

    public void beginPass(ChunkSectionsToRender sections, ChunkSectionLayerGroup group) {
        inFlight.get().push(new PassStart(sections, group, System.nanoTime()));
    }

    public void finishPass(ChunkSectionsToRender sections, ChunkSectionLayerGroup group) {
        ArrayDeque<PassStart> stack = inFlight.get();
        PassStart start = stack.poll();
        if (stack.isEmpty()) {
            inFlight.remove();
        }
        if (start == null || start.sections() != sections || start.group() != group) {
            return;
        }

        long elapsed = System.nanoTime() - start.startedNanos();
        DrawCounts counts = countDraws(sections, group);
        if (group == ChunkSectionLayerGroup.OPAQUE) {
            opaqueCalls.increment();
            opaquePassTimes.recordFrame(elapsed);
            opaqueDrawGroups.add(counts.groups());
            opaqueDrawEntries.add(counts.entries());
        } else if (group == ChunkSectionLayerGroup.TRANSLUCENT) {
            translucentCalls.increment();
            translucentPassTimes.recordFrame(elapsed);
            translucentDrawGroups.add(counts.groups());
            translucentDrawEntries.add(counts.entries());
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(
                opaqueCalls.sum(), translucentCalls.sum(),
                opaqueDrawGroups.sum(), translucentDrawGroups.sum(),
                opaqueDrawEntries.sum(), translucentDrawEntries.sum(),
                opaquePassTimes.summary(), translucentPassTimes.summary());
    }

    public record Snapshot(
            long opaqueCalls,
            long translucentCalls,
            long opaqueDrawGroups,
            long translucentDrawGroups,
            long opaqueDrawEntries,
            long translucentDrawEntries,
            FrameTimeTracker.Summary opaqueTime,
            FrameTimeTracker.Summary translucentTime) {
    }

    private static DrawCounts countDraws(
            ChunkSectionsToRender sections, ChunkSectionLayerGroup group) {
        EnumMap<ChunkSectionLayer, ?> byLayer = sections.drawGroupsPerLayer();
        long groupCount = 0;
        long entryCount = 0;
        ChunkSectionLayer[] layers = group == ChunkSectionLayerGroup.OPAQUE
                ? new ChunkSectionLayer[] {ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT}
                : new ChunkSectionLayer[] {ChunkSectionLayer.TRANSLUCENT};

        for (ChunkSectionLayer layer : layers) {
            Object layerGroups = byLayer.get(layer);
            if (!(layerGroups instanceof Map<?, ?> groups)) {
                continue;
            }
            groupCount += groups.size();
            for (Object drawGroup : groups.values()) {
                if (drawGroup instanceof Collection<?> entries) {
                    entryCount += entries.size();
                }
            }
        }
        return new DrawCounts(groupCount, entryCount);
    }

    private record PassStart(
            ChunkSectionsToRender sections,
            ChunkSectionLayerGroup group,
            long startedNanos) {
    }

    private record DrawCounts(long groups, long entries) {
    }
}