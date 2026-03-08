package com.minecraftabnormals.mindful_eating.core.mixin;

import com.minecraftabnormals.mindful_eating.client.HungerOverlay;
import com.minecraftabnormals.mindful_eating.compat.ModCompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ForgeGui.class)
public class HungerGuiMixin {
    @Inject(method = "render", at = @At("HEAD"))
    public void onRenderHead(GuiGraphics guiGraphics, float partialTicks, CallbackInfo ci) {
        HungerOverlay.hungerBarRightHeight = -1;
    }

    @Inject(method = "renderFood", at = @At("HEAD"), remap = false, cancellable = true)
    public void onRenderFood(int width, int height, GuiGraphics guiGraphics, CallbackInfo ci) {
        // If we are not suppressed by LSO, we take over the rendering
        if (ModCompat.isPlayerCold()) {
            // Let LSO handle it
            return;
        }

        // We are taking over!
        HungerOverlay.hungerBarRightHeight = ((ForgeGui) (Object) this).rightHeight;
        ((ForgeGui) (Object) this).rightHeight += 10;
        ci.cancel(); // Stop vanilla rendering
    }
}
