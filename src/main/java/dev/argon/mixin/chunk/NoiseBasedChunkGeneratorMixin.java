package dev.argon.mixin.chunk;

import dev.argon.ArgonClient;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Times vanilla's central noise-fill stage without changing generation logic.
 * Chunk generation may execute concurrently on multiple worker threads.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    @Inject(method = "doFill", at = @At("HEAD"))
    private void argon$beginNoiseFill(
            NoiseChunk noiseChunk, ChunkAccess chunk, CallbackInfo ci) {
        if (ArgonClient.config().telemetryEnabled()
                && ArgonClient.config().chunkGenerationProfilingEnabled()) {
            ArgonClient.chunkGenerationMetrics().beginFill();
        }
    }

    @Inject(method = "doFill", at = @At("RETURN"))
    private void argon$finishNoiseFill(
            NoiseChunk noiseChunk, ChunkAccess chunk, CallbackInfo ci) {
        if (ArgonClient.config().telemetryEnabled()
                && ArgonClient.config().chunkGenerationProfilingEnabled()) {
            ArgonClient.chunkGenerationMetrics().finishFill();
        }
    }
}