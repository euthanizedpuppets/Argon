package dev.argon;

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
        FrameTimeTracker tracker = ArgonClient.frameTimes();
        FrameTimeTracker.Summary summary = tracker.summary();

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
            report.append("\nNative cancelled tasks pruned: ")
                    .append(ArgonClient.cancelledChunkTasksPruned());
        } else {
            report.append("\nNative cancelled-task metrics: not collected");
        }

        report.append("\nArgon utility queue: ")
                .append(ArgonClient.chunkQueue().size())
                .append('/')
                .append(ArgonClient.chunkQueue().capacity())
                .append("\nWorld-pass interval samples: ")
                .append(summary.sampleCount())
                .append('/')
                .append(tracker.capacity());

        if (!ArgonClient.config().telemetryEnabled()) {
            report.append(" (disabled by config)");
        } else if (summary.sampleCount() == 0) {
            report.append(" (waiting for world rendering)");
        } else {
            report.append("\nWorld-pass interval min/avg/max: ")
                    .append(formatMillis(summary.minimumNanos()))
                    .append(" / ")
                    .append(formatMillis(summary.averageNanos()))
                    .append(" / ")
                    .append(formatMillis(summary.maximumNanos()))
                    .append("\nWorld-pass interval P50/P95: ")
                    .append(formatMillis(summary.p50Nanos()))
                    .append(" / ")
                    .append(formatMillis(summary.p95Nanos()));
        }

        for (ArgonFeature feature : ArgonFeature.values()) {
            report.append("\n")
                    .append(feature.name())
                    .append(": ")
                    .append(FeatureFlags.status(feature));
        }

        report.append("\nWorld-pass intervals are not GPU timings or a guaranteed FPS measurement.");
        report.append("\nNative queue cleanup removes cancelled entries only; vanilla task ordering is retained.");
        return report.toString();
    }

    private static String formatMillis(double nanos) {
        return String.format(Locale.ROOT, "%.2f ms", nanos / 1_000_000.0);
    }
}
