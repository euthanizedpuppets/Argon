package dev.argon.mixin.chunk;

import com.mojang.blaze3d.textures.GpuSampler;
import dev.argon.ArgonClient;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Measures vanilla's opaque/translucent terrain submission passes when the
 * opt-in renderer experiment and local telemetry are both enabled.
 */
@Mixin(ChunkSectionsToRender.class)
public abstract class ChunkSectionsToRenderMixin {
    @Inject(method = "renderGroup", at = @At("HEAD"))
    private void argon$beginTerrainGroup(
            ChunkSectionLayerGroup group, GpuSampler sampler, CallbackInfo ci) {
        if (ArgonClient.config().telemetryEnabled()
                && ArgonClient.config().experimentalRendererEnabled()) {
            ArgonClient.terrainRenderObserver().beginPass(
                    (ChunkSectionsToRender) (Object) this, group);
        }
    }

    @Inject(method = "renderGroup", at = @At("RETURN"))
    private void argon$finishTerrainGroup(
            ChunkSectionLayerGroup group, GpuSampler sampler, CallbackInfo ci) {
        if (ArgonClient.config().telemetryEnabled()
                && ArgonClient.config().experimentalRendererEnabled()) {
            ArgonClient.terrainRenderObserver().finishPass(
                    (ChunkSectionsToRender) (Object) this, group);
        }
    }
}