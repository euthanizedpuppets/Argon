package dev.argon;

import dev.argon.core.ArgonFeature;
import dev.argon.core.FeatureFlags;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;

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
                .append("\nFrame samples: ")
                .append(ArgonClient.frameTimes().sampleCount())
                .append('/')
                .append(ArgonClient.frameTimes().capacity())
                .append(" (frame lifecycle integration pending)");

        for (ArgonFeature feature : ArgonFeature.values()) {
            report.append("\n")
                    .append(feature.name())
                    .append(": ")
                    .append(FeatureFlags.status(feature));
        }

        report.append("\nNo FPS improvement is claimed by these diagnostic counters.");
        return report.toString();
    }
}
