package com.minecraftabnormals.mindful_eating.compat;

import com.minecraftabnormals.mindful_eating.core.MindfulEatingConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public class ModCompat {
    private static final Minecraft mc = Minecraft.getInstance();
    
    private static MobEffect nourishmentEffect = null;

    public static boolean isLSOLoaded() {
        return ModList.get().isLoaded(MindfulEatingConstants.LSO_MODID);
    }

    public static boolean isFarmersDelightLoaded() {
        return ModList.get().isLoaded(MindfulEatingConstants.FARMERS_DELIGHT_MODID);
    }

    public static boolean isAppleskinLoaded() {
        return ModList.get().isLoaded(MindfulEatingConstants.APPLESKIN_MODID);
    }

    public static MobEffect getNourishmentEffect() {
        if (nourishmentEffect == null && isFarmersDelightLoaded()) {
            nourishmentEffect = ForgeRegistries.MOB_EFFECTS.getValue(MindfulEatingConstants.NOURISHMENT_EFFECT_RL);
        }
        return nourishmentEffect;
    }

    public static boolean showAppleskinSaturation() {
        return isAppleskinLoaded() && AppleskinCompat.SHOW_SATURATION_OVERLAY;
    }

    public static boolean isNourishedHungerOverlayEnabled() {
        return isFarmersDelightLoaded() && FarmersDelightCompat.NOURISHED_HUNGER_OVERLAY;
    }

    public static boolean isStackableSoupEnabled() {
        return isFarmersDelightLoaded() && FarmersDelightCompat.ENABLE_STACKABLE_SOUP_ITEMS;
    }

    public static void setNourishedHungerOverlay(boolean flag) {
        if (isFarmersDelightLoaded()) {
            FarmersDelightCompat.setNourishedHungerOverlay(flag);
        }
    }

    public static void resetNourishedHungerOverlay() {
        if (isFarmersDelightLoaded()) {
            FarmersDelightCompat.resetNourishedHungerOverlay();
        }
    }

    public static boolean isPlayerCold() {
        if (!isLSOLoaded()) return false;
        Player player = mc.player;
        if (player == null) return false;
        
        return player.getActiveEffects().stream().anyMatch(effect -> {
            MobEffect type = effect.getEffect();
            ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(type);
            if (key == null) return false;
            return key.getNamespace().equals(MindfulEatingConstants.LSO_MODID) && 
                   (key.getPath().equals("cold_hunger") || key.getPath().equals("frostbite"));
        });
    }
}
