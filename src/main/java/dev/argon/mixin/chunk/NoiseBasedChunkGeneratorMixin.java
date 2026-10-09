package dev.argon.mixin.chunk;

import dev.argon.ArgonClient;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Times vanilla's central noise-fill stage without changing generation logic.
 * Chunk generation may execute concurrently on multiple worker threads.
 *
 * Minecraft 26.2's doFill descriptor takes Blender, StructureManager,
 * RandomState, ChunkAccess, and two integers, and returns ChunkAccess.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    @Inject(method = "doFill", at = @At("HEAD"))
    private void argon$beginNoiseFill(
            Blender blender,
            StructureManager structureManager,
            RandomState randomState,
            ChunkAccess chunk,
            int x,
            int z,
            CallbackInfoReturnable<ChunkAccess> cir) {
        if (ArgonClient.config().telemetryEnabled()
                && ArgonClient.config().chunkGenerationProfilingEnabled()) {
            ArgonClient.chunkGenerationMetrics().beginFill();
        }
    }

    @Inject(method = "doFill", at = @At("RETURN"))
    private void argon$finishNoiseFill(
            Blender blender,
            StructureManager structureManager,
            RandomState randomState,
            ChunkAccess chunk,
            int x,
            int z,
            CallbackInfoReturnable<ChunkAccess> cir) {
        if (ArgonClient.config().telemetryEnabled()
                && ArgonClient.config().chunkGenerationProfilingEnabled()) {
            ArgonClient.chunkGenerationMetrics().finishFill();
        }
    }
}
