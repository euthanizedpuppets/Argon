package dev.argon;

import dev.argon.chunks.ChunkCleanupMetrics;
import dev.argon.chunks.ChunkBuildMetrics;
import dev.argon.chunks.NativeChunkQueueMetrics;
import dev.argon.core.ArgonFeature;
import dev.argon.core.FeatureFlags;
import dev.argon.performance.FrameTimeTracker;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Registers read-only client diagnostics; this command does not toggle features. */
public final class ArgonCommands {
    private ArgonCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext) ->
                dispatcher.register(ClientCommands.literal("argon")
                        .then(ClientCommands.literal("status").executes(context -> {
                            context.getSource().sendFeedback(Component.literal(statusReport()));
                            return 1;
                        }))));
    }

    static String statusReport() {
        FrameTimeTracker renderTracker = ArgonClient.frameTimes();
        FrameTimeTracker.Summary render = renderTracker.summary();
        FrameTimeTracker serverTracker = ArgonClient.integratedServerTickTimes();
        FrameTimeTracker.Summary server = serverTracker.summary();
        ChunkCleanupMetrics.Snapshot cleanup = ArgonClient.chunkCleanupMetrics().snapshot();
        NativeChunkQueueMetrics.Snapshot nativeQueue = ArgonClient.nativeChunkQueueMetrics().snapshot();
        ChunkBuildMetrics buildMetrics = ArgonClient.chunkBuildMetrics();

        StringBuilder report = new StringBuilder()
                .append("Argon diagnostics")
                .append("\nLocal metrics enabled: ")
                .append(ArgonClient.config().telemetryEnabled())
                .append("\nChunk scheduler requested: ")
                .append(ArgonClient.config().chunkSchedulerEnabled())
                .append("\nExperimental renderer requested: ")
                .append(ArgonClient.config().experimentalRendererEnabled())
                .append("\nCancelled-task cleanup enabled: ")
                .append(ArgonClient.config().cancelledChunkTaskCleanupEnabled());

        if (ArgonClient.config().telemetryEnabled()) {
            report.append("\nCleanup scans / inspected / pruned: ")
                    .append(cleanup.scanCount())
                    .append(" / ")
                    .append(cleanup.entriesInspected())
                    .append(" / ")
                    .append(cleanup.entriesPruned());
            if (cleanup.scanCount() > 0) {
                report.append("\nCleanup avg/max scan cost: ")
                        .append(formatMillis(cleanup.averageDurationNanos()))
                        .append(" / ")
                        .append(formatMillis(cleanup.maximumDurationNanos()));
            }
        } else {
            report.append("\nCleanup scan metrics: not collected");
        }

        report.append("\nArgon utility queue (not Minecraft's native queue): ")
                .append(ArgonClient.chunkQueue().size())
                .append('/')
                .append(ArgonClient.chunkQueue().capacity());

        if (ArgonClient.config().telemetryEnabled()) {
            report.append("\nNative chunk queue depth current/peak: ")
                    .append(nativeQueue.currentDepth())
                    .append('/')
                    .append(nativeQueue.peakDepth())
                    .append("\nNative chunk queue adds/poll calls/tasks returned: ")
                    .append(nativeQueue.tasksAdded())
                    .append('/')
                    .append(nativeQueue.pollCalls())
                    .append('/')
                    .append(nativeQueue.tasksPolled())
                    .append("\nNative chunk queue clears/entries cleared: ")
                    .append(nativeQueue.clearCalls())
                    .append('/')
                    .append(nativeQueue.tasksCleared());
        } else {
            report.append("\nNative chunk queue metrics: not collected");
        }

        FrameTimeTracker.Summary queueWait = buildMetrics.queueWaitNanos().summary();
        FrameTimeTracker.Summary compile = buildMetrics.compileDurationNanos().summary();
        FrameTimeTracker.Summary upload = buildMetrics.uploadPassDurationNanos().summary();
        if (ArgonClient.config().telemetryEnabled()) {
            report.append("\nSection-task queue-wait samples / total: ")
                    .append(queueWait.sampleCount()).append('/')
                    .append(buildMetrics.totalQueueWaitSamples());
            if (queueWait.sampleCount() > 0) {
                report.append("\nSection-task queue wait avg/P50/P95/max: ")
                        .append(formatMillis(queueWait.averageNanos())).append(" / ")
                        .append(formatMillis(queueWait.p50Nanos())).append(" / ")
                        .append(formatMillis(queueWait.p95Nanos())).append(" / ")
                        .append(formatMillis(queueWait.maximumNanos()));
            }

            report.append("\nSection mesh compile samples / total: ")
                    .append(compile.sampleCount()).append('/')
                    .append(buildMetrics.totalCompileSamples());
            if (compile.sampleCount() > 0) {
                report.append("\nMesh compile avg/P50/P95/max: ")
                        .append(formatMillis(compile.averageNanos())).append(" / ")
                        .append(formatMillis(compile.p50Nanos())).append(" / ")
                        .append(formatMillis(compile.p95Nanos())).append(" / ")
                        .append(formatMillis(compile.maximumNanos()));
            }

            report.append("\nTerrain upload-pass samples / total: ")
                    .append(upload.sampleCount()).append('/')
                    .append(buildMetrics.totalUploadPassSamples());
            if (upload.sampleCount() > 0) {
                report.append("\nUpload method CPU time avg/P50/P95/max: ")
                        .append(formatMillis(upload.averageNanos())).append(" / ")
                        .append(formatMillis(upload.p50Nanos())).append(" / ")
                        .append(formatMillis(upload.p95Nanos())).append(" / ")
                        .append(formatMillis(upload.maximumNanos()));
            }
        } else {
            report.append("\nChunk build pipeline metrics: not collected");
        }

        report.append("\nWorld-pass interval samples: ")
                .append(render.sampleCount())
                .append('/')
                .append(renderTracker.capacity());

        if (!ArgonClient.config().telemetryEnabled()) {
            report.append(" (disabled by config)");
        } else if (render.sampleCount() == 0) {
            report.append(" (waiting for world rendering)");
        } else {
            report.append("\nWorld-pass interval min/avg/max: ")
                    .append(formatMillis(render.minimumNanos()))
                    .append(" / ")
                    .append(formatMillis(render.averageNanos()))
                    .append(" / ")
                    .append(formatMillis(render.maximumNanos()))
                    .append("\nWorld-pass interval P50/P95: ")
                    .append(formatMillis(render.p50Nanos()))
                    .append(" / ")
                    .append(formatMillis(render.p95Nanos()));
        }

        report.append("\nIntegrated server tick-work samples: ")
                .append(server.sampleCount())
                .append('/')
                .append(serverTracker.capacity());

        if (!ArgonClient.config().telemetryEnabled()) {
            report.append(" (disabled by config)");
        } else if (server.sampleCount() == 0) {
            report.append(" (available in single-player only)");
        } else {
            report.append("\nIntegrated tick-work min/avg/max: ")
                    .append(formatMillis(server.minimumNanos()))
                    .append(" / ")
                    .append(formatMillis(server.averageNanos()))
                    .append(" / ")
                    .append(formatMillis(server.maximumNanos()))
                    .append("\nIntegrated tick-work P50/P95: ")
                    .append(formatMillis(server.p50Nanos()))
                    .append(" / ")
                    .append(formatMillis(server.p95Nanos()));
        }

        for (ArgonFeature feature : ArgonFeature.values()) {
            report.append("\n")
                    .append(feature.name())
                    .append(": ")
                    .append(FeatureFlags.status(feature));
        }

        report.append("\nWorld-pass intervals are not GPU timings or a guaranteed FPS measurement.");
        report.append("\nIntegrated tick-work excludes wall-clock scheduling delay and shutdown saving.");
        report.append("\nNative queue cleanup does not replace vanilla task ordering.");
        report.append("\nCompile timings cover CompileTask.doTask; upload timings are CPU wall time, not GPU completion.");
        return report.toString();
    }

    private static String formatMillis(double nanos) {
        return String.format(Locale.ROOT, "%.2f ms", nanos / 1_000_000.0);
    }
}
