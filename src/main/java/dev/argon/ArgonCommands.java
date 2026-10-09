package dev.argon;

import dev.argon.core.ArgonFeature;
import dev.argon.core.FeatureFlags;
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
        int sampleCount = ArgonClient.frameTimes().sampleCount();
        long p95Interval = ArgonClient.frameTimes().percentile(0.95);

        StringBuilder report = new StringBuilder()
                .append("Argon diagnostics")
                .append("\nLocal metrics configured: ")
                .append(ArgonClient.config().telemetryEnabled())
                .append("\nChunk scheduler requested: ")
                .append(ArgonClient.config().chunkSchedulerEnabled())
                .append("\nExperimental renderer requested: ")
                .append(ArgonClient.config().experimentalRendererEnabled())
                .append("\nQueued chunk tasks: ")
                .append(ArgonClient.chunkQueue().size())
                .append('/')
                .append(ArgonClient.chunkQueue().capacity())
                .append("\nWorld-pass interval samples: ")
                .append(sampleCount)
                .append('/')
                .append(ArgonClient.frameTimes().capacity());

        if (sampleCount == 0) {
            report.append(" (waiting for world rendering)");
        } else {
            report.append("\nP95 world-pass interval: ")
                    .append(String.format(Locale.ROOT, "%.2f ms", p95Interval / 1_000_000.0));
        }

        for (ArgonFeature feature : ArgonFeature.values()) {
            report.append("\n")
                    .append(feature.name())
                    .append(": ")
                    .append(FeatureFlags.status(feature));
        }

        report.append("\nIntervals are measured at the end of the main world render pass; "
                + "they are not GPU timings or a guaranteed FPS measurement.");
        return report.toString();
    }
}
